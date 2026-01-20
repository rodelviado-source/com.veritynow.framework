package com.veritynow.core.store.jpa;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.veritynow.core.lock.LockHandle;
import com.veritynow.core.lock.LockingService;

public final class JdbcPublisher {
  private static final Logger LOGGER = LogManager.getLogger();

  /**
   * Retryable SQLSTATEs (PostgreSQL):
   * - 40001: serialization_failure
   * - 40P01: deadlock_detected
   * - 55P03: lock_not_available (NOWAIT/lock timeout style transient)
   *
   * Everything else is treated as non-retryable (deployment/schema/bug).
   */
  private static final Set<String> RETRYABLE_SQLSTATES = Set.of(
      "40001",
      "40P01",
      "55P03"
  );

  private static final String MOVE_HEAD_FENCED = """
    INSERT INTO vn_node_head (inode_id, version_id, fence_token, updated_at)
    VALUES (?, ?, ?, NOW())
    ON CONFLICT (inode_id)
    DO UPDATE
      SET version_id     = EXCLUDED.version_id,
          fence_token = EXCLUDED.fence_token,
          updated_at  = NOW()
    WHERE COALESCE(vn_node_head.fence_token, -1) < EXCLUDED.fence_token
    """;
  
  private static final String MOVE_HEAD = """
	INSERT INTO vn_node_head (inode_id, version_id, fence_token, updated_at)
	VALUES (?, ?, NULL, NOW())
	ON CONFLICT (inode_id)
	DO UPDATE
	  SET version_id = EXCLUDED.version_id, fence_token = NULL,  updated_at  = NOW()
	WHERE
		-- Degraded mode: only update if the existing row is also unfenced
		(vn_node_head.fence_token IS NULL)  
	""";

  private final JdbcTemplate jdbc;
  LockingService lockingService;

  public JdbcPublisher(JdbcTemplate jdbc, LockingService lockingService) {
    this.jdbc = Objects.requireNonNull(jdbc);
    this.lockingService = lockingService;
  }

  /**
   * Contract:
   * - Success => HEAD moved (insert or fenced update).
   * - Retryable => throws IllegalStateException (caller retries).
   * - Non-retryable DB errors propagate unchanged.
   */
  private int publish(VersionMetaEntity saved, long fenceToken) {
	Objects.requireNonNull(saved);
    final long inodeId = saved.getInode().getId();
    final long versionId = saved.getId();

    LOGGER.trace("Publish BEFORE moveHead inodeId={} versionId={} fenceToken={}", inodeId, versionId, fenceToken);

    int rows = 0;
    try {
      rows = jdbc.update(MOVE_HEAD_FENCED, inodeId, versionId, fenceToken);
    } catch (DataAccessException dae) {
	      // Only classify known transient concurrency errors as retryable.
	      String sqlState = findSqlState(dae);
	      if (sqlState != null && RETRYABLE_SQLSTATES.contains(sqlState)) {
	         String msg = String.format(
	        		 "Transient DB conflict (SQLSTATE %s) moving HEAD: inodeId=%d versionId=%d fenceToken=%d", 
	        		 sqlState,inodeId,versionId, fenceToken);
	        LOGGER.warn(msg, dae);
	        throw new IllegalStateException(msg, dae);
	      }
	
	      // Deployment/schema/real bugs: propagate unchanged.
	      throw dae;
    }

    // Fence predicate rejected the update => stale writer => retryable.
    if (rows == 0) {
      throw new IllegalStateException(
          "Lock conflict / stale fence: inodeId=" + inodeId
              + " versionId=" + versionId
              + " incomingFence=" + fenceToken
      );
      
      
    }
    LOGGER.trace("Publish AFTER moveHead inodeId={} versionId={} fenceToken={} rows={}", inodeId, versionId, fenceToken, rows);
    return rows;
  }
  
  /**
   * non fenced publish
   * @param saved
   * @return rows updated
   */
  public int publish(VersionMetaEntity saved) {
	    Objects.requireNonNull(saved);
	    final long inodeId = saved.getInode().getId();
	    final long versionId = saved.getId();

	    LOGGER.info("No Fence JPAPublish BEFORE moveHead inodeId={} versionId={}", inodeId, versionId);

	    int rows = jdbc.update(MOVE_HEAD, inodeId, versionId); // <-- only 2 params

	    if (rows == 0) {
	        throw new IllegalStateException(
	            "Unfenced publish rejected because inode_id=" + inodeId +
	            " is already fenced (locking likely enabled)."
	        );
	    }
	    return rows;
	}
      

  /**
   * Extract SQLState from the underlying SQLException if present.
   * Returns null if not available.
   */
  private static String findSqlState(Throwable t) {
    Throwable cur = t;
    while (cur != null) {
      if (cur instanceof SQLException se) {
        return se.getSQLState();
      }
      cur = cur.getCause();
    }
    return null;
  }
  
  
  public void acquireLockAndPublish(VersionMetaEntity saved) throws IOException {
	  Objects.requireNonNull(saved);
	  Objects.requireNonNull(lockingService, "Locking service required");
      final int maxAttempts = 3;
      long delayMs = 20;

      for (int attempt = 1; attempt <= maxAttempts; attempt++) {
          LockHandle h = null;
          try {
              h = lockingService.acquire(List.of(saved.getPath())); // REQUIRES_NEW
              publish(saved, h.fenceToken());               // just upsert/update head
              return;
          } catch (IllegalStateException e) { // "Lock conflict"
              if (attempt == maxAttempts) throw e;
              sleepBackoff(delayMs);
              delayMs = Math.min(delayMs * 2, 200);
          } finally {
              if (h != null) lockingService.release(h);      // REQUIRES_NEW
          }
      }
  }
  
  
  public boolean isLockingCapable() {
	  return lockingService != null;
  }

  private void sleepBackoff(long delayMs) {
      // add a little jitter to avoid herd effects
      long jitter = (long) (Math.random() * 10); // 0..9ms
      try {
          Thread.sleep(delayMs + jitter);
      } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Interrupted while waiting to publish", ie);
      }
  }
  
 
  
}
