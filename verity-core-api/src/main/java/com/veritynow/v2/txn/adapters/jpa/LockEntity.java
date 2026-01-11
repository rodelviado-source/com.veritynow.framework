package com.veritynow.v2.txn.adapters.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "vn_lock",
       indexes = {
           @Index(name = "vn_lock_root_idx", columnList = "lockRoot"),
           @Index(name = "vn_lock_expires_idx", columnList = "expiresAtEpochMs")
       })
public class LockEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String lockId;

    @Column(length = 512, nullable = false)
    private String lockRoot;

    @Column(length = 64, nullable = false)
    private String ownerTxnId;

    @Column(nullable = false)
    private long expiresAtEpochMs;

    @Column(nullable = false)
    private long fencingToken;

    protected LockEntity() {}

    public LockEntity(String lockId, String lockRoot, String ownerTxnId, long expiresAtEpochMs, long fencingToken) {
        this.lockId = lockId;
        this.lockRoot = lockRoot;
        this.ownerTxnId = ownerTxnId;
        this.expiresAtEpochMs = expiresAtEpochMs;
        this.fencingToken = fencingToken;
    }

    public String getLockId() { return lockId; }
    public String getLockRoot() { return lockRoot; }
    public String getOwnerTxnId() { return ownerTxnId; }
    public long getExpiresAtEpochMs() { return expiresAtEpochMs; }
    public long getFencingToken() { return fencingToken; }

    public void setExpiresAtEpochMs(long expiresAtEpochMs) { this.expiresAtEpochMs = expiresAtEpochMs; }
}
