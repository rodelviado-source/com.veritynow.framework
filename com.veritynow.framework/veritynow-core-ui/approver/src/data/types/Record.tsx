export type Status = "NEW" | "IN_REVIEW" | "APPROVED" | "REJECTED" | "CLOSED";

export interface RecordItem {
  id: number;
  agentId: string;
  agentFirstName?: string; agentMiddleName?: string; agentLastName?: string; agentSuffix?: string;
  clientId: string;
  clientFirstName?: string; clientMiddleName?: string; clientLastName?: string; clientSuffix?: string;
  title: string;
  priority?: number;
  status?: Status;
  description?: string;
  createdAt: string;
  imageIds: string[];
}




