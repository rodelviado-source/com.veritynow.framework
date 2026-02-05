package com.veritynow.core.store.txn;

import java.sql.Connection;
import java.sql.Savepoint;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.veritynow.core.store.versionstore.repo.PathKeyCodec;

public class TransactionContext  {
	final static Logger  LOGGER = LogManager.getLogger(); 
	private final static Map<String, Connection> txns = new ConcurrentHashMap<>();
	private final static Map<String, Savepoint> sps = new ConcurrentHashMap<>();
	private final static Map<String, List<Long>> lcks = new ConcurrentHashMap<>();
	 
	
	public final static boolean isLocked(String txnId, Long lockId) {
		if (!txns.containsKey(txnId)) {
			return false;
		}
		List<Long> lckIds = lcks.get(txnId);
		if (lckIds == null) return false;
		
		return lckIds.contains(lockId);
	}
	
	public final static void putLock(String txnId, Long lockId) {
		if (!txns.containsKey(txnId)) {
			return;
		}
		List<Long> lckIds = lcks.get(txnId);
		
		if (lckIds == null) {
			lckIds = new CopyOnWriteArrayList<>();
			lcks.put(txnId, lckIds);
		}
		
		lckIds.add(lockId);
	}
	
	public final static void removeLocks(String txnId) {
		if (!txns.containsKey(txnId)) {
			return;
		}
		List<Long> lckIds = lcks.get(txnId);
		if (lckIds != null) {
			lckIds.clear();
			return;
		}
	}
	
	public static Connection getConnection(String txn) {
		return txns.get(txn);
	}
	
	public static Connection putConnection(String txn, Connection ctx) {
		if (txns.containsKey(txn)) {
			LOGGER.warn("A transaction id is being resued txnId = {}", txn);
		}
		return txns.put(txn, ctx);
	}

	public static Connection removeConnection(String txn) {
		removeLocks(txn);
		removeSavepoint(txn);
		return txns.remove(txn);
	}
	
	public static Savepoint getSavepoint(String txn) {
		return sps.get(txn);
	}
	
	public static Savepoint putSavepoint(String txn, Savepoint sp) {
		return sps.put(txn, sp);
	}

	public static Savepoint removeSavepoint(String txn) {
		return sps.remove(txn);
	}
	
	public static void clear(String txn) {
		removeConnection(txn);
		removeSavepoint(txn);
	}
	
	public static void put(String txn, Connection conn, Savepoint sp) {
		putConnection(txn, conn);
		putSavepoint(txn, sp);
	}

	public static Optional<List<Long>> getActiveAdvisoryLocks(String txnId) {
		List<Long> lckIds = lcks.get(txnId);
		if (lckIds != null)
			return Optional.of(lckIds.stream().toList());
		return Optional.empty();
	}
	
	public static Optional<Long> getActiveAdvisoryLock(String txnId, String path) {
		Long key = PathKeyCodec.pathToLockKey(path);
		List<Long> lckIds = lcks.get(txnId);
		
		if (lckIds != null && lckIds.contains(key)) {
			Long l = lckIds.get(lckIds.indexOf(key));
			if (l != null)
				return Optional.of(l);
		}	
		return Optional.empty();
	}
	
}
