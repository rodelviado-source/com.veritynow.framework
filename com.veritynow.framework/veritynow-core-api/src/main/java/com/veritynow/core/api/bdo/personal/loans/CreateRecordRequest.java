package com.veritynow.core.api.bdo.personal.loans;

import java.time.Instant;

public class CreateRecordRequest {
	public String agentId;
	public String agentFirstName;
	public String agentMiddleName;
	public String agentLastName;
	public String agentSuffix;

	public String clientId;
	public String clientFirstName;
	public String clientMiddleName;
	public String clientLastName;
	public String clientSuffix;

	public Integer priority;
	public String status;
	public String requirements;

	// optional, server will set if null
	public Instant createdAt;
}
