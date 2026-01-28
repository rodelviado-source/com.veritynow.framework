package com.veritynow.core.store.immutablestore;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_BLOB;

import java.util.Objects;
import java.util.Optional;

import org.jooq.DSLContext;

import com.veritynow.core.store.persistence.jooq.tables.records.VnBlobRecord;

public class ImmutableRepository {

  private final DSLContext dsl;
  
  @FunctionalInterface
  public interface ThrowingFunction<T, R> {
    R apply(T t) throws Exception;
  }

  public ImmutableRepository(DSLContext dsl) {
    this.dsl = Objects.requireNonNull(dsl, "dsl");
  }

  public Optional<VnBlobRecord> findByHashAndAlgo(String hash, String algo) {
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(algo, "algorithm");

    return Optional.ofNullable(
      dsl.selectFrom(VN_BLOB)
         .where(VN_BLOB.HASH.eq(hash).and(VN_BLOB.HASH_ALGORITHM.eq(algo)))
         .fetchOne()
    );
  }
  
  
  public Optional<VnBlobRecord> findByHashAndAlgo(DSLContext dsl, String hash, String algo) {
	    Objects.requireNonNull(hash, "hash");
	    Objects.requireNonNull(algo, "algorithm");

	    return Optional.ofNullable(
	      dsl.selectFrom(VN_BLOB)
	         .where(VN_BLOB.HASH.eq(hash).and(VN_BLOB.HASH_ALGORITHM.eq(algo)))
	         .fetchOne()
	    );
	  }
  

  /**
   * Correct EXISTS: fetchExists() compiles to SELECT EXISTS(...)
   */
  public boolean exists(String hash, String algo) {
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(algo, "algorithm");

    return dsl.fetchExists(
      dsl.selectOne()
         .from(VN_BLOB)
         .where(VN_BLOB.HASH.eq(hash).and(VN_BLOB.HASH_ALGORITHM.eq(algo)))
    );
  }

  /**
   * Insert; will throw on duplicate PK unless caller uses insertIgnore or lock.
   */
  public int insert(VnBlobRecord blobRecord) {
    Objects.requireNonNull(blobRecord, "blobRecord");
    Objects.requireNonNull(blobRecord.getHash(), "hash");
    Objects.requireNonNull(blobRecord.getHashAlgorithm(), "algorithm");

    // Prefer explicit columns to avoid surprises if the table evolves.
    return dsl.insertInto(VN_BLOB)
      .set(VN_BLOB.HASH, blobRecord.getHash())
      .set(VN_BLOB.HASH_ALGORITHM, blobRecord.getHashAlgorithm())
      .set(VN_BLOB.SIZE, blobRecord.getSize())
      .set(VN_BLOB.MIME_TYPE, blobRecord.getMimeType())
      .set(VN_BLOB.NAME, blobRecord.getName())
      .execute();
  }
  
  /**
   * Insert; will throw on duplicate PK unless caller uses insertIgnore or lock.
   */
  public int insert(DSLContext dsl, VnBlobRecord blobRecord) {
    Objects.requireNonNull(blobRecord, "blobRecord");
    Objects.requireNonNull(blobRecord.getHash(), "hash");
    Objects.requireNonNull(blobRecord.getHashAlgorithm(), "algorithm");

    // Prefer explicit columns to avoid surprises if the table evolves.
    return dsl.insertInto(VN_BLOB)
      .set(VN_BLOB.HASH, blobRecord.getHash())
      .set(VN_BLOB.HASH_ALGORITHM, blobRecord.getHashAlgorithm())
      .set(VN_BLOB.SIZE, blobRecord.getSize())
      .set(VN_BLOB.MIME_TYPE, blobRecord.getMimeType())
      .set(VN_BLOB.NAME, blobRecord.getName())
      .execute();
  }

  /**
   * Insert but do nothing if PK already exists (Postgres ON CONFLICT DO NOTHING).
   * Returns rows inserted (0 or 1).
   */
  public int insertIgnore(VnBlobRecord blobRecord) {
    Objects.requireNonNull(blobRecord, "blobRecord");
    Objects.requireNonNull(blobRecord.getHash(), "hash");
    Objects.requireNonNull(blobRecord.getHashAlgorithm(), "algorithm");

    return dsl.insertInto(VN_BLOB)
      .set(VN_BLOB.HASH, blobRecord.getHash())
      .set(VN_BLOB.HASH_ALGORITHM, blobRecord.getHashAlgorithm())
      .set(VN_BLOB.SIZE, blobRecord.getSize())
      .set(VN_BLOB.MIME_TYPE, blobRecord.getMimeType())
      .set(VN_BLOB.NAME, blobRecord.getName())
      .onConflict(VN_BLOB.HASH, VN_BLOB.HASH_ALGORITHM)
      .doNothing()
      .execute();
  }

  /**
   * Cross-process mutex for (hash,algo) using Postgres advisory tx locks.
   * IMPORTANT: call this around: recheck -> write bytes -> insert meta.
   *
   * Uses hashtextextended() to generate a stable bigint lock key from text.
   * Lock is released automatically at transaction end.
   */
  public <T> T withHashLockTx(String hash, String algo, ThrowingFunction<DSLContext, T> work) {
	    Objects.requireNonNull(hash, "hash");
	    Objects.requireNonNull(algo, "algorithm");
	    Objects.requireNonNull(work, "work");

	    final String key = hash + ":" + algo;
	    
	    return dsl.transactionResult(cfg -> {
	    	DSLContext tx = cfg.dsl();
	      
		  tx.query("select pg_advisory_xact_lock(hashtextextended(?, 0))", key).execute();
	      try {
	        return work.apply(tx);
	      } catch (RuntimeException re) {
	        throw re;
	      } catch (Exception e) {
	        throw new RuntimeException(e);
	      }
	    });
	  }
}
