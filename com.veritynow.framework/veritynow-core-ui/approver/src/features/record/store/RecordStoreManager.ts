import type { RecordItem, Statuses } from "@/features/record/index";

export type RecordListOptions = {
  page?: number;
  size?: number;
  sort?: keyof RecordItem;
  dir?: "asc" | "desc";
  q?: string;
  status?: Statuses;
  agentId?: string;
  clientId?: string;
};

export interface StoreManager {
  create(input: Omit<RecordItem, "id" | "createdAt" | "updatedAt">): Promise<RecordItem>;
  get(id: string): Promise<RecordItem | null>;
  update(id: string, record: Omit<RecordItem, "createdAt">): Promise<RecordItem>;
  patch(id: string, patch: Partial<Omit<RecordItem, "id" | "createdAt">>): Promise<RecordItem>;
  delete(id: string): Promise<boolean>;
  list(opts?: RecordListOptions): Promise<{ items: RecordItem[]; total: number; page: number; size: number }>;
  addImages(id: string, assetIds: string[]): Promise<RecordItem>;
  removeImages(id: string, assetIds: string[]): Promise<RecordItem>;
  clearAll(): Promise<number>;
}
