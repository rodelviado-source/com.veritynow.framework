import type { Statuses, UploadStatuses } from "./types";

export interface RecordDTO {
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
  updatedAt?: string;
  assetIds: string[];
}


export interface PaginationDTO { page: number; size: number; total: number; }
export interface ListRecordsResponse { items: RecordDTO[]; page: PaginationDTO; }
export type GetRecordResponse = RecordDTO;
export type CreateRecordRequest = Omit<RecordDTO, "id" | "createdAt" | "updatedAt">;
export type CreateRecordResponse = RecordDTO;
export type UpdateRecordRequest = Omit<RecordDTO, "id" | "agentId" | "clientId" | "assetIds" | "createdAt" | "updatedAt">;
export type UpdateRecordResponse = RecordDTO;
export type PatchRecordRequest = Partial<Omit<RecordDTO, "id" | "createdAt">>;
export type PatchRecordResponse = RecordDTO;
export interface DeleteRecordResponse { id: string; deleted: boolean; }





