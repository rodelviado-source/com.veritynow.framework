package com.veritynow.core.store.db.deprecated;

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

@Entity
@Table(
    name = "vn_node_version",
    indexes = {
        @Index(name = "ix_ver_inode_timestamp", columnList = "inode_id, timestamp DESC, id DESC"),
    }
)
public class VersionMetaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "inode_id", nullable = false)
    private InodeEntity inode;

    /** The event **/
    // When (epoch millis)
    long timestamp;

    //What path was affected (absolute, namespace-qualified)
    String path;

    //How Operation (domain-level verb; not necessarily an HTTP verb)
    String operation;
    
    //Who (required, but can be anonymous)
    String principal;

    // Correlation (required)
    String correlationId;
    
    // Transaction (required)
    String workflowId;
    
    //String  (optional)
    String contextName;
    
    String transactionId;
    
    String transactionResult;
    
    /** the Blob **/
    //content hash
    String hash;
    
    //name of the blob
    String name;
    
    //type of the blob
    String mimeType;
    
    //content size of the blob
    long   size;

    protected VersionMetaEntity() { }

	public VersionMetaEntity(Long id, InodeEntity inode,  String path, long timestamp, String operation,
			String principal, String correlationId, String workflowId, String contextName, String transactionId, String transactionResult,
			String hash, String name, String mimeType, long size) {
		super();
		//DB generated
		this.id = id;
		this.inode = inode;
		this.path = path;
		this.timestamp = timestamp;
		this.operation = operation;
		this.principal = principal;
		this.correlationId = correlationId;
		this.workflowId = workflowId;
		this.contextName = contextName;
		this.transactionId = transactionId;
		this.transactionResult = transactionResult;
		this.hash = hash;
		this.name = name;
		this.mimeType = mimeType;
		this.size = size;
	}
	
	//Use to set DB generated id
	public VersionMetaEntity(Long id, VersionMetaEntity vme) {
		super();
		//DB generated
		this.id = id;
		this.inode = vme.getInode();
		this.path = vme.getPath();
		this.timestamp = vme.getTimestamp();
		this.operation = vme.getOperation();
		this.principal = vme.getPrincipal();
		this.correlationId = vme.getCorrelationId();
		this.workflowId = vme.getWorkflowId();
		this.contextName = vme.getContextName();
		this.transactionId = vme.getTransactionId();
		this.transactionResult = vme.getTransactionResult();
		this.hash = vme.getHash();
		this.name = vme.getName();
		this.mimeType = vme.getMimeType();
		this.size = vme.getSize();
	}
	

	public Long getId() { return id; }
	public InodeEntity getInode() {	return inode; }
	public long getTimestamp() { return timestamp; }
	public String getPath() {	return path;}
	public String getOperation() {	return operation;}
	public String getPrincipal() {	return principal;}
	public String getCorrelationId() {return correlationId;	}
	public String getWorkflowId() {return workflowId;}
	public String getContextName() {return contextName;	}
	public String getTransactionId() {return transactionId;	}
	public String getTransactionResult() {return transactionResult; }

	public String getHash() {return hash;}
	public String getName() {return name;}
	public String getMimeType() {return mimeType;}
	public long getSize() {	return size;}

	

}
