package com.veritynow.core.api.bdo.personal.loans;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "records", uniqueConstraints = @UniqueConstraint(columnNames = { "agent_id", "client_id" }))
public class RecordEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// ----- Agent -----
	@Column(name = "agent_id", nullable = false, length = 64)
	private String agentId;
	private String agentFirstName;
	private String agentMiddleName;
	private String agentLastName;
	private String agentSuffix;

	// ----- Client -----
	@Column(name = "client_id", nullable = false, length = 64)
	private String clientId;
	private String clientFirstName;
	private String clientMiddleName;
	private String clientLastName;
	private String clientSuffix;

	// ----- Business fields -----
	private String title;
	@Column(length = 2000)
	private String description;
	private Integer priority;
	@Column(length = 40)
	private String status;

	private Instant createdAt;

	// multiple images stored as IDs in separate table
	@ElementCollection
	@CollectionTable(name = "record_images", joinColumns = @JoinColumn(name = "record_id"))
	@Column(name = "image_id", length = 128)
	private List<String> imageIds = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getAgentFirstName() {
		return agentFirstName;
	}

	public void setAgentFirstName(String agentFirstName) {
		this.agentFirstName = agentFirstName;
	}

	public String getAgentMiddleName() {
		return agentMiddleName;
	}

	public void setAgentMiddleName(String agentMiddleName) {
		this.agentMiddleName = agentMiddleName;
	}

	public String getAgentLastName() {
		return agentLastName;
	}

	public void setAgentLastName(String agentLastName) {
		this.agentLastName = agentLastName;
	}

	public String getAgentSuffix() {
		return agentSuffix;
	}

	public void setAgentSuffix(String agentSuffix) {
		this.agentSuffix = agentSuffix;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientFirstName() {
		return clientFirstName;
	}

	public void setClientFirstName(String clientFirstName) {
		this.clientFirstName = clientFirstName;
	}

	public String getClientMiddleName() {
		return clientMiddleName;
	}

	public void setClientMiddleName(String clientMiddleName) {
		this.clientMiddleName = clientMiddleName;
	}

	public String getClientLastName() {
		return clientLastName;
	}

	public void setClientLastName(String clientLastName) {
		this.clientLastName = clientLastName;
	}

	public String getClientSuffix() {
		return clientSuffix;
	}

	public void setClientSuffix(String clientSuffix) {
		this.clientSuffix = clientSuffix;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public List<String> getImageIds() {
		return imageIds;
	}

	public void setImageIds(List<String> imageIds) {
		this.imageIds = imageIds;
	}

	// getters/setters ...
	// (Add all new getters & setters for the fields above)

}
