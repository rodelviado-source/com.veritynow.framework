package com.veritynow.v2.txn.adapters.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "vn_subtree_lock")
public class SubtreeLockEntity {
    @Id
    @Column(name = "lock_id", nullable = false, length = 128)
    private String lockId;

    @Column(name = "lock_root", nullable = false, length = 1024)
    private String lockRoot;

    @Column(name = "owner_txn_id", nullable = false, length = 64)
    private String ownerTxnId;

    @Column(name = "expires_at_ms", nullable = false)
    private long expiresAtMs;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;

    public SubtreeLockEntity() {}

    public SubtreeLockEntity(String lockId, String lockRoot, String ownerTxnId, long expiresAtMs, long fencingToken) {
        this.lockId = lockId;
        this.lockRoot = lockRoot;
        this.ownerTxnId = ownerTxnId;
        this.expiresAtMs = expiresAtMs;
        this.fencingToken = fencingToken;
    }

    public String getLockId() { return lockId; }
    public void setLockId(String lockId) { this.lockId = lockId; }
    public String getLockRoot() { return lockRoot; }
    public void setLockRoot(String lockRoot) { this.lockRoot = lockRoot; }
    public String getOwnerTxnId() { return ownerTxnId; }
    public void setOwnerTxnId(String ownerTxnId) { this.ownerTxnId = ownerTxnId; }
    public long getExpiresAtMs() { return expiresAtMs; }
    public void setExpiresAtMs(long expiresAtMs) { this.expiresAtMs = expiresAtMs; }
    public long getFencingToken() { return fencingToken; }
    public void setFencingToken(long fencingToken) { this.fencingToken = fencingToken; }
}
