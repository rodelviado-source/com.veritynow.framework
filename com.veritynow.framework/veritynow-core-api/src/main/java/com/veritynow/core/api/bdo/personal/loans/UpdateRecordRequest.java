package com.veritynow.core.api.bdo.personal.loans;

public class UpdateRecordRequest {
	// key fields are immutable for update-by-id; use /by-key to replace keys
	private Integer priority;
	private String status;
	private String requirements;

	private String agentFirstName;
	private String agentMiddleName;
	private String agentLastName;
	private String agentSuffix;

	private String clientFirstName;
	private String clientMiddleName;
	private String clientLastName;
	private String clientSuffix;

	

	public Integer getPriority() {
		return priority;
	}

	public String getRequirements() {
		return requirements;
	}

	public void setRequirements(String requirements) {
		this.requirements = requirements;
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

	

	// getters/setters...
}
