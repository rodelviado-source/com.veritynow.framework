package com.veritynow.core.store.db.deprecated;

import java.time.Instant;

import org.hibernate.annotations.Type;

import com.veritynow.core.store.db.DBPostgresLtreeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vn_inode")
public class InodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Store-owned: canonical scope key (ltree textual form like "h....h....")
    // Used later by the locking layer; mapped as String in JPA.
    @Type(value = DBPostgresLtreeType.class)
    @Column(name = "scope_key", columnDefinition = "ltree")
    private String scopeKey;

    protected InodeEntity() { }

    public InodeEntity(Instant createdAt) {
        this(createdAt, null);
    }

    public InodeEntity(Instant createdAt, String scopeKey) {
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.scopeKey = scopeKey;
    }
    
    public InodeEntity(Long id, Instant createdAt, String scopeKey) {
    	this.id = id;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.scopeKey = scopeKey;
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }

    public String getScopeKey() { return scopeKey; }

    void setScopeKeyInternal(String scopeKey) {
        this.scopeKey = scopeKey;
    }
    
}
