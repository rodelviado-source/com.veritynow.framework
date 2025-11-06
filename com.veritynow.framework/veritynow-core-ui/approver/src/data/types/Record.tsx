export enum Statuses {
  NEW = "NEW",
  IN_REVIEW = "IN_REVIEW",
  APPROVED ="APPROVED",
  REJECTED = "REJECTED",
  CLOSED= "CLOSED"
}
export const StatusValues = Object.values(Statuses);

export interface RecordItem {
  id: number;
  agentId: string;
  agentFirstName?: string; agentMiddleName?: string; agentLastName?: string; agentSuffix?: string;
  clientId: string;
  clientFirstName?: string; clientMiddleName?: string; clientLastName?: string; clientSuffix?: string;
  title: string;
  priority?: number;
  status?: Statuses;
  description?: string;
  createdAt: string;
  imageIds: string[];
}


export interface PageResult<T> { items: T[]; page: number; size: number; total: number }
