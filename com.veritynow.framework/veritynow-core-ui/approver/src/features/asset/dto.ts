export interface AssetSummaryDTO {
  id: string;            // global asset id
  ownerId: string;       // global owner id
  filename: string;
  contentType: string;
  size: number;
  kind?: string;
  checksum?: string | null;
  createdAt?: string;
}

export interface PaginationDTO { page: number; size: number; total: number; }

export interface ListAssetsResponse {
  items: AssetSummaryDTO[];
  page?: PaginationDTO;
}

export type GetAssetSummaryResponse = AssetSummaryDTO;

export interface GetAssetFileResponse {
  id: string;
  ownerId?: string;
  blob: Blob;
  contentType: string;
  contentLength?: number;
}

export type PutAssetResponse = AssetSummaryDTO;
export type PatchAssetSummaryRequest = Partial<Omit<AssetSummaryDTO, "id" | "ownerId">>;
export type PatchAssetSummaryResponse = AssetSummaryDTO;

export interface DeleteAssetResponse { id: string; ownerId: string; deleted: boolean; }

export interface BulkDeleteAssetsRequest { ids: string[]; }
export interface BulkDeleteAssetsResponse { deleted: string[]; failed?: { id: string; reason: string }[]; }

export interface UploadAssetsResponse { items: AssetSummaryDTO[]; }
