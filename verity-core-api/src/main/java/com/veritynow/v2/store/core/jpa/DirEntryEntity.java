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

@Entity
@Table(
    name = "vn_dir_entry",
    uniqueConstraints = @UniqueConstraint(name = "uq_dir_parent_name", columnNames = {"parent_inode_id", "name"}),
    indexes = {
        @Index(name = "ix_dir_parent", columnList = "parent_inode_id"),
        @Index(name = "ix_dir_child", columnList = "child_inode_id")
    }
)
public class DirEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dir_entry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_inode_id", nullable = false)
    private InodeEntity parent;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_inode_id", nullable = false)
    private InodeEntity child;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DirEntryEntity() { }

    public DirEntryEntity(InodeEntity parent, String name, InodeEntity child) {
        this.parent = parent;
        this.name = name;
        this.child = child;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public InodeEntity getParent() { return parent; }
    public String getName() { return name; }
    public InodeEntity getChild() { return child; }
    public Instant getCreatedAt() { return createdAt; }
}
