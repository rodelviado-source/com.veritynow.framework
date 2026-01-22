package com.veritynow.core.store.db.deprecated;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vn_node_head")
public class VersionMetaHeadEntity {

    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "fence_token", columnDefinition = "BIGINT", nullable = true )
    Long fenceToken; 

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @MapsId
    @JoinColumn(name = "inode_id")
    private InodeEntity inode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private VersionMetaEntity headVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected VersionMetaHeadEntity() { }

    public VersionMetaHeadEntity(InodeEntity inode, VersionMetaEntity headVersion) {
        this.inode = inode;
        this.headVersion = headVersion;
        this.updatedAt = Instant.now();
    }
    
    public VersionMetaHeadEntity(InodeEntity inode, VersionMetaEntity headVersion, Long fenceToken, Instant updatedAt) {
        this.inode = inode;
        this.headVersion = headVersion;
        this.fenceToken = fenceToken;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public InodeEntity getInode() { return inode; }
    public VersionMetaEntity getHeadVersion() { return headVersion; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getFenceToken() {return fenceToken;	}

	public void setHead(VersionMetaEntity head) {
        this.headVersion = head;
        this.updatedAt = Instant.now();
    }
}
