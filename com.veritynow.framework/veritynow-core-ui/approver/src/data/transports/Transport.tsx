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

export enum MediaKind {
  image='image',
  pdf='pdf',
  video='video',
  download='download',
} 
export type MediaResource = {
  id: string;
  url: string;
  kind: MediaKind;
  filename?: string;       // <- new
  size?: number;           // <- new (bytes)
  contentType?: string;    // <- new (MIME)
};

export interface Transport {
  mode?: ModeTypes;
  label?: TransportTypes;  
  
  list(params: ListParams): Promise<PageResult<RecordItem>>;
  create(payload: Partial<RecordItem>): Promise<RecordItem>;
  update(id: number, patch: Partial<RecordItem>): Promise<RecordItem>;
  uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }>;
  imageUrl(id: string): Promise<string>;
  mediaFor(id: string): Promise<MediaResource>;
}


export interface ListParams {
  page: number;
  size: number;
  query?: string;
  sort?: string;
  dir?: "asc" | "desc";
}

export interface PageResult<T> { items: T[]; page: number; size: number; total: number; }

