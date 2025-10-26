package com.veritynow.core.api.bdo.personal.loans;

import java.time.Instant;
import java.util.List;

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

	public String title;
	public Integer priority;
	public String status;
	public String description;

	// optional, server will set if null
	public Instant createdAt;
}
