import { AbstractRoutedAssetManager } from "@/features/asset/index";
import type {
  ListAssetsResponse, GetAssetSummaryResponse, GetAssetFileResponse,
  PutAssetResponse, PatchAssetSummaryRequest, PatchAssetSummaryResponse,
  DeleteAssetResponse, BulkDeleteAssetsRequest, BulkDeleteAssetsResponse,
  UploadAssetsResponse,
  AssetSummary
} from "@/features/asset/index";

export class RemoteAssetManager extends AbstractRoutedAssetManager {
  constructor(private readonly baseUrl = "/api", private readonly netFetch: typeof fetch = fetch) {
    super(baseUrl);
  }
  private async j<T>(res: Response) {
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    return res.json() as Promise<T>;
  }

  async listAssets(ownerId: string): Promise<ListAssetsResponse> {
    const res = await this.netFetch(`${this.baseUrl}/owners/${encodeURIComponent(ownerId)}/assets`);
    return this.j<ListAssetsResponse>(res);
  }

  async getAssetSummary(id: string): Promise<AssetSummary> {
    const res = await fetch(`${this.baseUrl}/assets/${encodeURIComponent(id)}`);
    if (!res.ok) throw new Error(`getAssetSummary failed: ${res.status}`);
    return res.json();
  }

  async getAssetFile(assetId: string): Promise<GetAssetFileResponse> {
    const res = await this.netFetch(`${this.baseUrl}/assets/${encodeURIComponent(assetId)}/file`);
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    const blob = await res.blob();
    return {
      id: assetId,
      blob,
      contentType: res.headers.get("Content-Type") ?? blob.type ?? "application/octet-stream",
      contentLength: res.headers.get("Content-Length") ? Number(res.headers.get("Content-Length")) : undefined,
    };
  }
  async putAsset(assetId: string, file: File, ownerId: string): Promise<PutAssetResponse> {
    const res = await this.netFetch(`${this.baseUrl}/assets/${encodeURIComponent(assetId)}?ownerId=${encodeURIComponent(ownerId)}`, {
      method: "PUT",
      headers: { "Content-Type": file.type || "application/octet-stream", "X-Filename": file.name },
      body: file,
    });
    return this.j<PutAssetResponse>(res);
  }
  async patchAssetSummary(assetId: string, patch: PatchAssetSummaryRequest): Promise<PatchAssetSummaryResponse> {
    const res = await this.netFetch(`${this.baseUrl}/assets/${encodeURIComponent(assetId)}/meta`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(patch),
    });
    return this.j<PatchAssetSummaryResponse>(res);
  }
  async deleteAsset(assetId: string): Promise<DeleteAssetResponse> {
    const res = await this.netFetch(`${this.baseUrl}/assets/${encodeURIComponent(assetId)}`, { method: "DELETE" });
    return this.j<DeleteAssetResponse>(res);
  }
  async uploadAssets(ownerId: string, files: File[]): Promise<UploadAssetsResponse> {
    const fd = new FormData();
    for (const f of files) fd.append("file", f, f.name);
    const res = await this.netFetch(`${this.baseUrl}/owners/${encodeURIComponent(ownerId)}/assets`, { method: "POST", body: fd });
    return this.j<UploadAssetsResponse>(res);
  }
  async bulkDelete(req: BulkDeleteAssetsRequest): Promise<BulkDeleteAssetsResponse> {
    const res = await this.netFetch(`${this.baseUrl}/assets:bulk-delete`, {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(req)
    });
    return this.j<BulkDeleteAssetsResponse>(res);
  }


}