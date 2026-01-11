package com.veritynow.v2.txn.adapters.jpa;

import com.veritynow.v2.txn.core.TxnRecord;
import jakarta.persistence.*;

@Entity
@Table(name = "vn_txn")
public class TxnEntity {
    @Id
    @Column(name = "txn_id", nullable = false, length = 64)
    private String txnId;

    @Column(name = "lock_root", nullable = false, length = 1024)
    private String lockRoot;

    @Column(name = "lock_id", nullable = false, length = 128)
    private String lockId;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;

    @Column(name = "lock_expires_at_ms", nullable = false)
    private long lockExpiresAtMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private TxnRecord.State state;

    @Column(name = "principal", nullable = true, length = 256)
    private String principal;

    @Column(name = "created_at_ms", nullable = false)
    private long createdAtMs;

    @Column(name = "updated_at_ms", nullable = false)
    private long updatedAtMs;

    @Column(name = "failure_reason", nullable = true, length = 2048)
    private String failureReason;

    public TxnEntity() {}

    public static TxnEntity fromRecord(TxnRecord r) {
        TxnEntity e = new TxnEntity();
        e.txnId = r.txnId();
        e.lockRoot = r.lockRoot();
        e.lockId = r.lockId();
        e.fencingToken = r.fencingToken();
        e.lockExpiresAtMs = r.lockExpiresAtMs();
        e.state = r.state();
        e.principal = r.principal();
        e.createdAtMs = r.createdAtMs();
        e.updatedAtMs = r.updatedAtMs();
        e.failureReason = r.failureReason();
        return e;
    }

    public TxnRecord toRecord() {
        return new TxnRecord(txnId, lockRoot, lockId, fencingToken, lockExpiresAtMs, state, principal, createdAtMs, updatedAtMs, failureReason);
    }

    // getters/setters
    public String getTxnId() { return txnId; }
    public void setTxnId(String txnId) { this.txnId = txnId; }
    public String getLockRoot() { return lockRoot; }
    public void setLockRoot(String lockRoot) { this.lockRoot = lockRoot; }
    public String getLockId() { return lockId; }
    public void setLockId(String lockId) { this.lockId = lockId; }
    public long getFencingToken() { return fencingToken; }
    public void setFencingToken(long fencingToken) { this.fencingToken = fencingToken; }
    public long getLockExpiresAtMs() { return lockExpiresAtMs; }
    public void setLockExpiresAtMs(long lockExpiresAtMs) { this.lockExpiresAtMs = lockExpiresAtMs; }
    public TxnRecord.State getState() { return state; }
    public void setState(TxnRecord.State state) { this.state = state; }
    public String getPrincipal() { return principal; }
    public void setPrincipal(String principal) { this.principal = principal; }
    public long getCreatedAtMs() { return createdAtMs; }
    public void setCreatedAtMs(long createdAtMs) { this.createdAtMs = createdAtMs; }
    public long getUpdatedAtMs() { return updatedAtMs; }
    public void setUpdatedAtMs(long updatedAtMs) { this.updatedAtMs = updatedAtMs; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
