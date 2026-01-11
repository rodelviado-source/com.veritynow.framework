import type { PatchAssetSummaryRequest, BulkDeleteAssetsRequest, AssetSummary } from "@/features/asset/index";

function jsonResponse(body: unknown, init?: ResponseInit): Response {
  return new Response(JSON.stringify(body), {
    status: init?.status ?? 200,
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
  });
}
function blobResponse(blob: Blob, headers?: HeadersInit, init?: ResponseInit): Response {
  return new Response(blob, {
    status: init?.status ?? 200,
    headers: { "Content-Type": blob.type || "application/octet-stream", ...(headers ?? {}) },
  });
}
function errorResponse(status: number, message?: string, headers?: HeadersInit): Response {
  const body = message ? { error: message } : null;
  if (body) return jsonResponse(body, { status, headers });
  return new Response(null, { status, headers });
}

/** Routes /api asset URLs to strongly-typed methods. */
export abstract class AbstractRoutedAssetManager {
  constructor(protected readonly base = "/api") {}

  abstract listAssets(ownerId: string, opts?: any): Promise<any>;
  abstract getAssetSummary(id: string): Promise<AssetSummary>;
  abstract patchAssetSummary(assetId: string, patch: PatchAssetSummaryRequest): Promise<any>;
  abstract getAssetFile(assetId: string): Promise<any>;
  abstract putAsset(assetId: string, file: File, ownerId: string): Promise<any>;
  abstract deleteAsset(assetId: string): Promise<any>;
  abstract uploadAssets(ownerId: string, files: File[]): Promise<any>;
  abstract bulkDelete(req: BulkDeleteAssetsRequest): Promise<any>;
    
  async fetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const req = new Request(input, init);
    const url = new URL(req.url, location.origin);
    if (!url.pathname.startsWith(this.base)) return errorResponse(404, "Not Found");

    try {
      const ownerList = new RegExp(`^${this.base}/owners/([^/]+)/assets$`);
      const ownerUpload = new RegExp(`^${this.base}/owners/([^/]+)/assets$`);
      const assetMeta = new RegExp(`^${this.base}/assets/([^/]+)/meta$`);
      const assetFile = new RegExp(`^${this.base}/assets/([^/]+)/file$`);
      const assetPut = new RegExp(`^${this.base}/assets/([^/]+)$`);
      const bulkDel = new RegExp(`^${this.base}/assets:bulk-delete$`);

      let m: RegExpExecArray | null;

      if (req.method === "GET") {
        if ((m = ownerList.exec(url.pathname))) {
          const ownerId = decodeURIComponent(m[1]);
          const data = await this.listAssets(ownerId, {});
          return jsonResponse(data);
        }
        if ((m = assetMeta.exec(url.pathname))) {
          const id = decodeURIComponent(m[1]);
          const data = await this.getAssetSummary(id);
          return jsonResponse(data);
        }
        if ((m = assetFile.exec(url.pathname))) {
          const id = decodeURIComponent(m[1]);
          const file = await this.getAssetFile(id); // { blob, contentType? }
          return blobResponse(file.blob, {
            "Content-Type": file.contentType ?? file.blob.type ?? "application/octet-stream",
            ...(file.contentLength ? { "Content-Length": String(file.contentLength) } : {}),
          });
        }
      }

      if (req.method === "PUT" && (m = assetPut.exec(url.pathname))) {
        const id = decodeURIComponent(m[1]);
        const ownerId = url.searchParams.get("ownerId") || "";
        if (!ownerId) return errorResponse(400, "ownerId required");
        const blob = await req.blob();
        const file = new File([blob], req.headers.get("X-Filename") || id, { type: blob.type || "application/octet-stream" });
        const data = await this.putAsset(id, file, ownerId);
        return jsonResponse(data);
      }

      if (req.method === "PATCH" && (m = assetMeta.exec(url.pathname))) {
        const id = decodeURIComponent(m[1]);
        const patch = (await req.json()) as PatchAssetSummaryRequest;
        const data = await this.patchAssetSummary(id, patch);
        return jsonResponse(data);
      }

      if (req.method === "DELETE" && (m = assetPut.exec(url.pathname))) {
        const id = decodeURIComponent(m[1]);
        const data = await this.deleteAsset(id);
        return jsonResponse(data);
      }

      if (req.method === "POST" && (m = ownerUpload.exec(url.pathname))) {
        const ownerId = decodeURIComponent(m[1]);
        const fd = await req.formData();
        const files: File[] = [];
        for (const [, v] of fd) if (v instanceof File) files.push(v);
        const data = await this.uploadAssets(ownerId, files);
        return jsonResponse(data);
      }

      if (req.method === "POST" && bulkDel.exec(url.pathname)) {
        const body = (await req.json()) as BulkDeleteAssetsRequest;
        const data = await this.bulkDelete(body);
        return jsonResponse(data);
      }

      const a = url.pathname.match(new RegExp(`^${this.base}/assets/([^/]+)$`));
      if (req.method === "GET" && a) {
        const id = decodeURIComponent(a[1]);
        const meta = await this.getAssetSummary(id); // abstract method
        return jsonResponse(meta);
      }

      return errorResponse(404, "Route not handled by AssetManager.fetch");
    } catch (e: any) {
      return errorResponse(500, e?.message || "Internal error");
    }
  }
}
