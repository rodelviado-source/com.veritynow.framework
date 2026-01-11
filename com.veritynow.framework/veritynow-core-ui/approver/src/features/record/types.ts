export enum Statuses {
  NEW = "NEW",
  IN_REVIEW = "IN_REVIEW",
  APPROVED = "APPROVED",
  REJECTED = "REJECTED",
  CLOSED = "CLOSED"
}

export enum UploadStatuses {
  PENDING = "PENDING",
  UPLOADING = "UPLOADING",
  UPLOADED = "UPLOADED",
  FAILED = "FAILED"
}

export const StatusValues = Object.values(Statuses);

export interface RecordItem {
  id: string;
  agentId: string;
  agentFirstName?: string; agentMiddleName?: string; agentLastName?: string; agentSuffix?: string;
  clientId: string;
  clientFirstName?: string; clientMiddleName?: string; clientLastName?: string; clientSuffix?: string;

  requirements?: string;
  priority?: number;
  status?: Statuses;
  uploadStatus?: UploadStatuses;
  loadedStatus?: string;

  createdAt: string;
  assetIds: string[];
  updatedAt?: string;
}


