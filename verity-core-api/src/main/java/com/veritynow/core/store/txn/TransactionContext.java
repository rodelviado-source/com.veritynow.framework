package com.veritynow.core.store.txn;

import java.sql.Connection;
import java.sql.Savepoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransactionContext  {
	final static Logger  LOGGER = LogManager.getLogger(); 
	private final static Map<String, Connection> txns = new ConcurrentHashMap<>();
	private final static Map<String, Savepoint> sps = new ConcurrentHashMap<>();
	 
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
	
}
