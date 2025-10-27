import type { PageResult, RecordItem } from "../types";

export interface ListParams {
  page: number;
  size: number;
  query?: string;
  sort?: string;
  dir?: "asc" | "desc";
}

export interface Transport {
  label: "Remote" | "Embedded";
  list(params: ListParams): Promise<PageResult<RecordItem>>;
  create(payload: Partial<RecordItem>): Promise<RecordItem>;
  update(id: number, patch: Partial<RecordItem>): Promise<RecordItem>;
  uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }>;
  imageUrl(id: string): Promise<string>;
}
