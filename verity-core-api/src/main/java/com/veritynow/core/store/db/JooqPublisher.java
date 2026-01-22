package com.veritynow.core.store.db;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;

import java.io.IOException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.springframework.dao.DataAccessException;

import com.veritynow.core.lock.LockHandle;
import com.veritynow.core.lock.LockingService;
import com.veritynow.core.store.meta.VersionMeta;

/**
 * Publisher that moves HEAD rows using jOOQ generated tables.
 *
 * Semantics preserved:
 * - Fenced publish uses monotonic fence token (stale writers rejected).
 * - Unfenced publish is "degraded mode": updates only if existing row is also unfenced.
 * - Retry classification is SQLSTATE-based.
 */
public final class JooqPublisher {
  private static final Logger LOGGER = LogManager.getLogger();

  /**
   * Retryable SQLSTATEs (PostgreSQL):
   * - 40001: serialization_failure
   * - 40P01: deadlock_detected
   * - 55P03: lock_not_available
   */
  private static final Set<String> RETRYABLE_SQLSTATES = Set.of(
      "40001",
      "40P01",
      "55P03"
  );

  private final DSLContext dsl;
  private final LockingService lockingService;
  @SuppressWarnings("unused")
  private final InodeManager inodeManager;

  public JooqPublisher(DSLContext dsl, LockingService lockingService, InodeManager inodeManger) {
    this.dsl = Objects.requireNonNull(dsl, "dsl");
    this.lockingService = lockingService; // may be null if running without locking
    this.inodeManager = Objects.requireNonNull(inodeManger, "inodeManager");
  }

  /**
   * Contract:
   * - Success => HEAD moved (insert or fenced update).
   * - Retryable => throws IllegalStateException (caller retries).
   * - Non-retryable DB errors propagate unchanged.
   */
  private int persistAndPublishFenced(long inodeId, VersionMeta vm, long fenceToken) {
    Objects.requireNonNull(vm, "vm");

    // Insert the new version row, then move HEAD using the returned version id.
    // This keeps all store ids inside the persistence layer and avoids confusing inode_id vs version_id.
    CommonTableExpression<Record2<Long, Long>> ins = DSL.name("ins")
        .fields("version_id", "inode_id")
        .as(
            dsl.insertInto(
                    VN_NODE_VERSION,
                    VN_NODE_VERSION.INODE_ID,
                    VN_NODE_VERSION.TIMESTAMP,
                    VN_NODE_VERSION.PATH,
                    VN_NODE_VERSION.OPERATION,
                    VN_NODE_VERSION.PRINCIPAL,
                    VN_NODE_VERSION.CORRELATION_ID,
                    VN_NODE_VERSION.WORKFLOW_ID,
                    VN_NODE_VERSION.CONTEXT_NAME,
                    VN_NODE_VERSION.TRANSACTION_ID,
                    VN_NODE_VERSION.TRANSACTION_RESULT,
                    VN_NODE_VERSION.HASH,
                    VN_NODE_VERSION.NAME,
                    VN_NODE_VERSION.MIME_TYPE,
                    VN_NODE_VERSION.SIZE
                )
                .values(
                    inodeId,
                    vm.timestamp(),
                    vm.path(),
                    vm.operation(),
                    vm.principal(),
                    vm.correlationId(),
                    vm.workflowId(),
                    vm.contextName(),
                    vm.transactionId(),
                    vm.transactionResult(),
                    vm.hash(),
                    vm.name(),
                    vm.mimeType(),
                    vm.size()
                )
                .returningResult(VN_NODE_VERSION.ID, VN_NODE_VERSION.INODE_ID)
        );

    Field<Long> vId = DSL.field(DSL.name("ins", "version_id"), Long.class);
    Field<Long> iId = DSL.field(DSL.name("ins", "inode_id"), Long.class);

    Field<OffsetDateTime> now = DSL.field("now()", OffsetDateTime.class);

    try {
      int rows = dsl.with(ins)
          .insertInto(
              VN_NODE_HEAD,
              VN_NODE_HEAD.INODE_ID,
              VN_NODE_HEAD.VERSION_ID,
              VN_NODE_HEAD.FENCE_TOKEN,
              VN_NODE_HEAD.UPDATED_AT
          )
          .select(
              DSL.select(
                      iId,
                      vId,
                      DSL.val(fenceToken),
                      now
                  )
                  .from(DSL.table(DSL.name("ins")))
          )
          .onConflict(VN_NODE_HEAD.INODE_ID)
          .doUpdate()
          .set(VN_NODE_HEAD.VERSION_ID, DSL.excluded(VN_NODE_HEAD.VERSION_ID))
          .set(VN_NODE_HEAD.FENCE_TOKEN, DSL.excluded(VN_NODE_HEAD.FENCE_TOKEN))
          .set(VN_NODE_HEAD.UPDATED_AT, now)
          .where(
              DSL.coalesce(VN_NODE_HEAD.FENCE_TOKEN, DSL.inline(-1L))
                  .lt(DSL.excluded(VN_NODE_HEAD.FENCE_TOKEN))
          )
          .execute();

      if (rows == 0) {
        throw new IllegalStateException(
            "Lock conflict / stale fence: inodeId=" + inodeId + " incomingFence=" + fenceToken
        );
      }
      return rows;
    } catch (org.jooq.exception.DataAccessException | DataAccessException dae) {
      String sqlState = findSqlState(dae);
      if (sqlState != null && RETRYABLE_SQLSTATES.contains(sqlState)) {
        String msg = String.format(
            "Transient DB conflict (SQLSTATE %s) publishing HEAD: inodeId=%d fenceToken=%d",
            sqlState, inodeId, fenceToken
        );
        LOGGER.warn(msg, dae);
        throw new IllegalStateException(msg, dae);
      }
      throw dae;
    }
  }

  /**
   * Unfenced publish ("degraded mode"):
   * - Inserts/updates with fence_token = NULL
   * - Updates only if existing row is also unfenced
   */
  public int publish(VersionMeta vm) {
    Objects.requireNonNull(vm, "vm");
    
 // Store-owned inode resolution/creation (O(1) scope_key invariant via projection)
    Long inodeId = inodeManager.resolveOrCreateInode(vm.path()).getId();

    // Insert the new version row, then move HEAD in "degraded" mode (unfenced).
    // Update is only allowed if the existing head is also unfenced.
    CommonTableExpression<Record2<Long, Long>> ins = DSL.name("ins")
        .fields("version_id", "inode_id")
        .as(
            dsl.insertInto(
                    VN_NODE_VERSION,
                    VN_NODE_VERSION.INODE_ID,
                    VN_NODE_VERSION.TIMESTAMP,
                    VN_NODE_VERSION.PATH,
                    VN_NODE_VERSION.OPERATION,
                    VN_NODE_VERSION.PRINCIPAL,
                    VN_NODE_VERSION.CORRELATION_ID,
                    VN_NODE_VERSION.WORKFLOW_ID,
                    VN_NODE_VERSION.CONTEXT_NAME,
                    VN_NODE_VERSION.TRANSACTION_ID,
                    VN_NODE_VERSION.TRANSACTION_RESULT,
                    VN_NODE_VERSION.HASH,
                    VN_NODE_VERSION.NAME,
                    VN_NODE_VERSION.MIME_TYPE,
                    VN_NODE_VERSION.SIZE
                )
                .values(
                    inodeId,
                    vm.timestamp(),
                    vm.path(),
                    vm.operation(),
                    vm.principal(),
                    vm.correlationId(),
                    vm.workflowId(),
                    vm.contextName(),
                    vm.transactionId(),
                    vm.transactionResult(),
                    vm.hash(),
                    vm.name(),
                    vm.mimeType(),
                    vm.size()
                )
                .returningResult(VN_NODE_VERSION.ID, VN_NODE_VERSION.INODE_ID)
        );

    Field<Long> vId = DSL.field(DSL.name("ins", "version_id"), Long.class);
    Field<Long> iId = DSL.field(DSL.name("ins", "inode_id"), Long.class);
    Field<OffsetDateTime> now = DSL.field("now()", OffsetDateTime.class);

    int rows = dsl.with(ins)
        .insertInto(
            VN_NODE_HEAD,
            VN_NODE_HEAD.INODE_ID,
            VN_NODE_HEAD.VERSION_ID,
            VN_NODE_HEAD.FENCE_TOKEN,
            VN_NODE_HEAD.UPDATED_AT
        )
        .select(
            DSL.select(
                    iId,
                    vId,
                    DSL.val((Long) null),
                    now
                )
                .from(DSL.table(DSL.name("ins")))
        )
        .onConflict(VN_NODE_HEAD.INODE_ID)
        .doUpdate()
        .set(VN_NODE_HEAD.VERSION_ID, DSL.excluded(VN_NODE_HEAD.VERSION_ID))
        .set(VN_NODE_HEAD.FENCE_TOKEN, DSL.val((Long) null))
        .set(VN_NODE_HEAD.UPDATED_AT, now)
        .where(VN_NODE_HEAD.FENCE_TOKEN.isNull())
        .execute();

    if (rows == 0) {
      throw new IllegalStateException(
          "Unfenced publish rejected because inodeId=" + inodeId +
              " is already fenced (locking likely enabled)."
      );
    }
    return rows;
  }

  /**
   * Acquire lock (REQUIRES_NEW in caller's TX setup) and publish HEAD with fencing.
   */
  public void acquireLockAndPublish(VersionMeta vm) throws IOException {
    Objects.requireNonNull(vm, "vm");
    Objects.requireNonNull(lockingService, "Locking service required");

    // Store-owned inode resolution/creation (O(1) scope_key invariant via projection)
    Long inodeId = inodeManager.resolveOrCreateInode(vm.path()).getId();
    
    final int maxAttempts = 3;
    long delayMs = 20;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      LockHandle h = null;
      try {
        h = lockingService.acquire(List.of(vm.path())); // REQUIRES_NEW
        persistAndPublishFenced(inodeId, vm, h.fenceToken()); // insert version + fenced head move
        return;
      } catch (IllegalStateException e) {
        if (attempt == maxAttempts) throw e;
        sleepBackoff(delayMs);
        delayMs = Math.min(delayMs * 2, 200);
      } finally {
        if (h != null) lockingService.release(h);            // REQUIRES_NEW
      }
    }
  }

  public boolean isLockingCapable() {
    return lockingService != null;
  }

  private static String findSqlState(Throwable t) {
    Throwable cur = t;
    while (cur != null) {
      if (cur instanceof SQLException se) return se.getSQLState();
      cur = cur.getCause();
    }
    return null;
  }

  private void sleepBackoff(long delayMs) {
    long jitter = (long) (Math.random() * 10);
    try {
      Thread.sleep(delayMs + jitter);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting to publish", ie);
    }
  }
}
