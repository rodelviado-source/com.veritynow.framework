import { AbstractRoutedAssetManager } from "@/features/asset/index";

import type {
  StoreManager, ListAssetsResponse, GetAssetSummaryResponse, GetAssetFileResponse,
  PutAssetResponse, PatchAssetSummaryRequest, PatchAssetSummaryResponse,
  DeleteAssetResponse, BulkDeleteAssetsRequest, BulkDeleteAssetsResponse,
  UploadAssetsResponse, AssetSummaryDTO, AssetSummary
} from "@/features/asset/index";


export class EmbeddedAssetManager extends AbstractRoutedAssetManager {
  constructor(private readonly store: StoreManager, base = "/api") { super(base); }

  private toDTO(s: AssetSummary): AssetSummaryDTO {
    const { id, ownerId, filename, contentType, size, kind, checksum, createdAt } = s;
    return { id, ownerId, filename, contentType, size, kind, checksum, createdAt };
  }

  async listAssets(ownerId: string): Promise<ListAssetsResponse> {
    const { items, total, page, size } = await this.store.list(ownerId);
    return { items: items.map(this.toDTO.bind(this)), page: { total, page, size } };
  }

   async getAssetSummary(id: string): Promise<AssetSummary> {
    const meta = await this.store.getAssetSummary(id); // OPFSAssetStore.getMeta
    if (!meta) throw new Error(`Asset not found: ${id}`);
    return meta; // already matches AssetSummary shape
  }

  async getAssetFile(assetId: string): Promise<GetAssetFileResponse> {
    const m = await this.store.getAssetSummary(assetId);
    if (!m) throw new Error("Not found");
    const f = await this.store.getAssetFile(assetId);
    if (!f) throw new Error("File not found");
    return { id: m.id, ownerId: m.ownerId, blob: f, contentType: f.type || m.contentType || "application/octet-stream" };
  }
  async putAsset(assetId: string, file: File, ownerId: string): Promise<PutAssetResponse> {
    const s = await this.store.put(ownerId, assetId, file);
    return this.toDTO(s);
  }
  async patchAssetSummary(assetId: string, patch: PatchAssetSummaryRequest): Promise<PatchAssetSummaryResponse> {
    const u = await this.store.updateAssetSummary(assetId, patch as any);
    return this.toDTO(u);
  }
  async deleteAsset(assetId: string): Promise<DeleteAssetResponse> {
    const meta = await this.store.getAssetSummary(assetId);
    const ok = await this.store.delete(assetId);
    return { id: assetId, ownerId: meta?.ownerId ?? "", deleted: ok };
  }
  async uploadAssets(ownerId: string, files: File[]): Promise<UploadAssetsResponse> {
    const items = await this.store.saveFiles(ownerId, files);
    return { items: items.map(this.toDTO.bind(this)) };
  }
  async bulkDelete(req: BulkDeleteAssetsRequest): Promise<BulkDeleteAssetsResponse> {
    return this.store.bulkDelete(req.ids);
  }


}
