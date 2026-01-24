package com.veritynow.core.store.db;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.currentOffsetDateTime;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;

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
import org.jooq.InsertSetMoreStep;
import org.jooq.Record2;
import org.springframework.dao.DataAccessException;

import com.veritynow.core.lock.LockHandle;
import com.veritynow.core.lock.LockingService;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.persistence.jooq.tables.records.VnNodeVersionRecord;

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
  
  private final RepositoryManager repositoryManager;

  public JooqPublisher(DSLContext dsl, LockingService lockingService, RepositoryManager repositoryManager) {
    this.dsl = Objects.requireNonNull(dsl, "dsl");
    this.lockingService = lockingService; // may be null if running without locking
    this.repositoryManager = Objects.requireNonNull(repositoryManager, "repositoryManager");
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
    CommonTableExpression<Record2<Long, Long>> ins = name("ins")
        .fields("version_id", "inode_id")
        .as(
        		insertVersionMeta(vm, inodeId)
                .returningResult(VN_NODE_VERSION.ID, VN_NODE_VERSION.INODE_ID)
        );

    Field<Long> vId = field(name("ins", "version_id"), Long.class);
    Field<Long> iId = field(name("ins", "inode_id"), Long.class);

    Field<OffsetDateTime> now =  currentOffsetDateTime();

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
              select(
                      iId,
                      vId,
                      val(fenceToken),
                     now
                  )
                  .from(table(name("ins")))
          )
          .onConflict(VN_NODE_HEAD.INODE_ID)
          .doUpdate()
          .set(VN_NODE_HEAD.VERSION_ID, excluded(VN_NODE_HEAD.VERSION_ID))
          .set(VN_NODE_HEAD.FENCE_TOKEN, excluded(VN_NODE_HEAD.FENCE_TOKEN))
          .set(VN_NODE_HEAD.UPDATED_AT, now)
          .where(
              coalesce(VN_NODE_HEAD.FENCE_TOKEN, inline(-1L))
                  .lt(excluded(VN_NODE_HEAD.FENCE_TOKEN))
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
    Long inodeId = repositoryManager.resolveOrCreateInode(vm.path()).id();

    // Insert the new version row, then move HEAD in "degraded" mode (unfenced).
    // Update is only allowed if the existing head is also unfenced.
    CommonTableExpression<Record2<Long, Long>> ins = name("ins")
        .fields("version_id", "inode_id")
        .as(
            insertVersionMeta(vm, inodeId)
                .returningResult(VN_NODE_VERSION.ID, VN_NODE_VERSION.INODE_ID)
        );

    Field<Long> vId = field(name("ins", "version_id"), Long.class);
    Field<Long> iId = field(name("ins", "inode_id"), Long.class);
    Field<OffsetDateTime> now = currentOffsetDateTime();

    int rows = dsl.with(ins)
        .insertInto(
            VN_NODE_HEAD,
            VN_NODE_HEAD.INODE_ID,
            VN_NODE_HEAD.VERSION_ID,
            VN_NODE_HEAD.FENCE_TOKEN,
            VN_NODE_HEAD.UPDATED_AT
        )
        .select(
            select(
                    iId,
                    vId,
                    val((Long) null),
                    now
                )
                .from(table(name("ins")))
        )
        .onConflict(VN_NODE_HEAD.INODE_ID)
        .doUpdate()
        .set(VN_NODE_HEAD.VERSION_ID, excluded(VN_NODE_HEAD.VERSION_ID))
        .set(VN_NODE_HEAD.FENCE_TOKEN, val((Long) null))
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
    Long inodeId = repositoryManager.resolveOrCreateInode(vm.path()).id();
    
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
  
  private  InsertSetMoreStep<VnNodeVersionRecord> insertVersionMeta(VersionMeta vm, Long inodeId) {
	  return dsl.insertInto(VN_NODE_VERSION).
          	set(VN_NODE_VERSION.INODE_ID, inodeId ).
            set(VN_NODE_VERSION.TIMESTAMP, DbTime.nowEpochMs()).
            set(VN_NODE_VERSION.PATH, vm.path()).
            set(VN_NODE_VERSION.OPERATION, vm.operation()).
            set(VN_NODE_VERSION.PRINCIPAL, vm.principal()).
            set(VN_NODE_VERSION.CORRELATION_ID, vm.correlationId()).
            set(VN_NODE_VERSION.WORKFLOW_ID, vm.workflowId()).
            set(VN_NODE_VERSION.CONTEXT_NAME, vm.contextName()).
            set(VN_NODE_VERSION.TRANSACTION_ID, vm.transactionId()).
            set(VN_NODE_VERSION.TRANSACTION_RESULT, vm.transactionResult()).
            set(VN_NODE_VERSION.HASH, vm.hash()).
            set(VN_NODE_VERSION.NAME, vm.name()).
            set( VN_NODE_VERSION.MIME_TYPE, vm.mimeType()).
            set(VN_NODE_VERSION.SIZE, vm.size());
  }
}
