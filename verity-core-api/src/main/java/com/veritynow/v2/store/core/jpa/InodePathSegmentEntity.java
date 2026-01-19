package com.veritynow.v2.store.core.jpa;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Store-owned projection (Phase-1): provides fast inode -> full path segment materialization
 * without walking vn_dir_entry.
 *
 * This projection is initialized by the store (InodeManager) when new directories are created.
 * No backfill/repair is performed in Phase-1.
 */
@Entity
@Table(
    name = "vn_inode_path_segment",
    uniqueConstraints = @UniqueConstraint(name = "uq_inode_ord", columnNames = {"inode_id", "ord"}),
    indexes = {
        @Index(name = "ix_inode_path_inode", columnList = "inode_id")
    }
)
public class InodePathSegmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "inode_id", nullable = false)
    private InodeEntity inode;

    @Column(name = "ord", nullable = false)
    private int ord;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "dir_entry_id", nullable = false)
    private DirEntryEntity dirEntry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected InodePathSegmentEntity() {}

    public InodePathSegmentEntity(InodeEntity inode, int ord, DirEntryEntity dirEntry) {
        this.inode = inode;
        this.ord = ord;
        this.dirEntry = dirEntry;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public InodeEntity getInode() { return inode; }
    public int getOrd() { return ord; }
    public DirEntryEntity getDirEntry() { return dirEntry; }
    public Instant getCreatedAt() { return createdAt; }
}
