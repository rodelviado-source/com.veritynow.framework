import type { AssetSummary } from "@/features/asset/index";

export type ListOptions = {
  page?: number;
  size?: number;
  sort?: keyof AssetSummary;
  dir?: "asc" | "desc";
};

export interface StoreManager {
  put(ownerId: string, assetId: string, file: File): Promise<AssetSummary>;
  getAssetSummary(assetId: string): Promise<AssetSummary | null>;
  getAssetSummaryByOwner(ownerId: string, assetId: string): Promise<AssetSummary | null>;
  getAssetFile(assetId: string): Promise<File | null>;
  delete(assetId: string): Promise<boolean>;
  deleteByOwner(ownerId: string, assetId: string): Promise<boolean>;
  updateAssetSummary(assetId: string, patch: Partial<Omit<AssetSummary, "id" | "ownerId" | "filename" | "contentType" | "size">>): Promise<AssetSummary>;
  list(ownerId: string | "*", opts?: ListOptions): Promise<{ items: AssetSummary[]; total: number; page: number; size: number }>;
  saveFiles(ownerId: string, files: File[]): Promise<AssetSummary[]>;
  bulkDelete(assetIds: string[]): Promise<{ deleted: string[]; failed: { id: string; reason: string }[] }>;
  exists(assetId: string): Promise<boolean>;
  clearOwner(ownerId: string): Promise<number>;
  clearAll(): Promise<number>;
}
