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

export interface PageResult<T> { items: T[]; page: number; size: number; total: number; }

export interface Store {
  mode: "remote" | "embedded";
  label: string;
  list(page: number, size: number): Promise<PageResult<RecordItem>>;
  create(payload: Partial<RecordItem>): Promise<RecordItem>;
  update(payload: Partial<RecordItem> & { id: number }): Promise<RecordItem>;
  uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }>;
  imageSrc(id: string): string | null;
}

export const STORAGE_KEY = "vn_store_mode";
