package com.veritynow.core.store.txn.jooq;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

import com.veritynow.core.store.txn.TransactionContext;
import com.veritynow.core.store.txn.TransactionFinalizer;
import com.veritynow.core.store.txn.TransactionService;

/**
 * jOOQ TransactionService backed by vn_txn_epoch.
 *
 * No JPA entities or repositories.
 */
public class JooqTransactionService implements TransactionService {

	private final TransactionFinalizer finalizer;
	private final DataSource dataSource;

	public JooqTransactionService(TransactionFinalizer finalizer, DataSource dataSource) {
		this.finalizer = Objects.requireNonNull(finalizer, "finalizer");
		this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
	}

	@Override
	public String begin(String txnId) {
		
		Objects.requireNonNull(txnId, "Transaction id is required");

		Connection conn = TransactionContext.getConnection(txnId);

		if (conn == null) {
			// try to get a valid connection and bind it to transactionId
			// this will be the connection for this transaction
			// it is not thread bound
			try {
				// Connection
				conn = dataSource.getConnection();
				conn.setAutoCommit(false);
			} catch (SQLException e) {
				try {
					if (conn != null)
						conn.close();
				} catch (Exception ignore) {
				}
				;
				throw new IllegalStateException("Cannot set the state of the connection ", e);
			}

			TransactionContext.putConnection(txnId, conn);
		}
		
		finalizer.begin(txnId);

		return txnId;
	}

	@Override
	public void commit(String txnId) {
		Objects.requireNonNull(txnId, "txnId");
		requireActiveMatchingTxn(txnId);
		finalizer.commit(txnId);
		closeTransaction(txnId);
	}

	@Override
	public void rollback(String txnId) {
		Objects.requireNonNull(txnId, "txnId");
		requireActiveMatchingTxn(txnId);

		try {
			finalizer.rollback(txnId);
		} finally {
			closeTransaction(txnId);
		}
	}

	private static void requireActiveMatchingTxn(String txnId) {
		if (TransactionContext.getConnection(txnId) == null) {
			throw new IllegalStateException("Called without an active transaction; use begin()");
		}
	}

	private void closeTransaction(String txnId) {
		try ( Connection conn = TransactionContext.removeConnection(txnId)) {
		} catch (Exception ignore) {} finally {TransactionContext.clear(txnId);} ;
	}
}
