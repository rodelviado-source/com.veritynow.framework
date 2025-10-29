import type { RecordItem } from "@/data/types/Record";

/*-- Interfaces for implementors of Store and Transports --*/

export enum TransportTypes {
	Remote = "Remote",
	Embedded="Embedded"
}

export enum ModeTypes {
	remote = "remote",
	embedded="embedded",
	auto="auto"
} ;

export interface Transport {
  label: TransportTypes;  
  
  list(params: ListParams): Promise<PageResult<RecordItem>>;
  create(payload: Partial<RecordItem>): Promise<RecordItem>;
  update(id: number, patch: Partial<RecordItem>): Promise<RecordItem>;
  uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }>;
  imageUrl(id: string): Promise<string>;
}

export interface Store {
  mode: "remote" | "embedded";
  label: string;
  list(page: number, size: number): Promise<PageResult<RecordItem>>;
  create(payload: Partial<RecordItem>): Promise<RecordItem>;
  update(payload: Partial<RecordItem> & { id: number }): Promise<RecordItem>;
  uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }>;
  imageSrc(id: string): string | null;
}

export interface ListParams {
  page: number;
  size: number;
  query?: string;
  sort?: string;
  dir?: "asc" | "desc";
}

export interface PageResult<T> { items: T[]; page: number; size: number; total: number; }

