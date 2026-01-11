import { ModeTypes } from "@/core/transport/types";

import { OPFSAssetStore, EmbeddedAssetManager, RemoteAssetManager, AssetSummary } from "@/features/asset/index";
import { OPFSRecordStore, EmbeddedRecordManager, RemoteRecordManager } from "@/features/record/index";
import type { CreateRequirementLinkRequest, RequirementLinkDto } from "@/features/links/index";
import { OPFSRequirementLinkStore, EmbeddedRequirementLinkManager, RemoteRequirementLinkManager } from "@/features/links/index";
import { toQuery } from "@/core/util/http";

const API_BASE = "/api";



let _mode: ModeTypes = (localStorage.getItem("vn.mode") as ModeTypes) || ModeTypes.embedded;
export function setMode(m: ModeTypes) { _mode = m; localStorage.setItem("vn.mode", m); }
export function getMode(): ModeTypes { return _mode; }

// Embedded Store managers
const embeddedAssetStore = new EmbeddedAssetManager(new OPFSAssetStore(), API_BASE);
const embeddedRecordStore = new EmbeddedRecordManager(new OPFSRecordStore(), API_BASE);
const embeddedLinkStore = new EmbeddedRequirementLinkManager(new OPFSRequirementLinkStore(), new OPFSAssetStore(), API_BASE);

// Remote Store managers
const remoteAssetStore = new RemoteAssetManager(API_BASE);
const remoteRecordStore = new RemoteRecordManager(API_BASE);
const remoteLinksStore = new RemoteRequirementLinkManager(API_BASE);

// Asset:  Remote-and-Embedded-fetchers
const embeddedFetchAssets = embeddedAssetStore.fetch.bind(embeddedAssetStore);
const remoteFetchAssets = remoteAssetStore.fetch.bind(remoteAssetStore);

// Record: Remote-and-Embedded-fetchers
const embeddedFetchRecords = embeddedRecordStore.fetch.bind(embeddedRecordStore);
const remoteFetchRecords = remoteRecordStore.fetch.bind(remoteRecordStore);

// Requirement-links: Remote-and-Embedded-fetchers 
const remoteFetchLinks = remoteLinksStore.fetch.bind(remoteLinksStore);
const embeddedFetchLinks = embeddedLinkStore.fetch.bind(embeddedLinkStore);


function pickAssetFetch(): typeof fetch { return getMode() === ModeTypes.remote ? remoteFetchAssets : embeddedFetchAssets; }
function pickRecordFetch(): typeof fetch { return getMode() === ModeTypes.remote ? remoteFetchRecords : embeddedFetchRecords; }
function pickLinkFetch(): typeof fetch { return getMode() === ModeTypes.remote ? remoteFetchLinks : embeddedFetchLinks; }


async function j<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    throw new Error(`${res.status} ${res.statusText}${txt ? ` - ${txt}` : ""}`);
  }
  return res.json() as Promise<T>;
}

export const DataFacade = {
  setMode, getMode,

  // ---------- unified fetch ----------
  async fetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const req = new Request(input, init);
    const url = new URL(req.url, location.origin);
    const p = url.pathname;


    const isAssetRoute = p.startsWith(`${API_BASE}/assets`) || p.startsWith(`${API_BASE}/owners/`);
    if (getMode() === ModeTypes.remote) {
      return isAssetRoute ? remoteFetchAssets(req, init) : remoteFetchRecords(req, init);
    }
    // embedded
    return isAssetRoute ? embeddedFetchAssets(req, init) : embeddedFetchRecords(req, init);
  },


  records: {
    async list(params?: any) {
      const f = pickRecordFetch();
      const res = await f(`${API_BASE}/records${params ? `?${toQuery(params)}` : ""}`, { method: "GET" });
      return j<import("@/features/record/dto").ListRecordsResponse>(res);
    },
    async get(id: string) {
      const f = pickRecordFetch();
      const res = await f(`${API_BASE}/records/${encodeURIComponent(id)}`, {
        method: "GET",
      });
      return j<import("@/features/record/dto").GetRecordResponse>(res);
    },
    async create(body: import("@/features/record/dto").CreateRecordRequest) {
      const f = pickRecordFetch();
      const res = await f(`${API_BASE}/records`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      return j<import("@/features/record/dto").CreateRecordResponse>(res);
    },
    async update(id: string, body: import("@/features/record/dto").UpdateRecordRequest) {
      const f = pickRecordFetch();
      const res = await f(`${API_BASE}/records/${encodeURIComponent(id)}`, {
        method: "PUT", headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });
      return j<import("@/features/record/dto").UpdateRecordResponse>(res);
    },
    async patch(id: string, body: Partial<import("@/features/record/dto").PatchRecordRequest>) {
      const f = pickRecordFetch();
      const res = await f(`${API_BASE}/records/${encodeURIComponent(id)}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });
      return j<import("@/features/record/dto").PatchRecordResponse>(res);
    },
    async delete(id: string) {
      const f = pickRecordFetch();
      const res = await f(`${API_BASE}/records/${encodeURIComponent(id)}`, { 
        method: "DELETE" });
      return res.ok;
    },

  },

  assets: {
    async list(ownerId: string, params?: any) {
      const f = pickAssetFetch();
      const res = await f(`${API_BASE}/owners/${encodeURIComponent(ownerId)}/assets${params ? `?${toQuery(params)}` : ""}`, {
        method: "GET"
      });
      return j<import("@/features/asset/dto").ListAssetsResponse>(res);
    },

    async getAssetFile(assetId: string) {
      const f = pickAssetFetch();
      const res = await f(`${API_BASE}/assets/${encodeURIComponent(assetId)}/file`);
      return res;
    },
    async put(assetId: string, file: File, ownerId: string) {
      const f = pickAssetFetch();
      const res = await f(
        `${API_BASE}/assets/${encodeURIComponent(assetId)}?ownerId=${encodeURIComponent(ownerId)}`,
        {
          method: "PUT",
          headers: {
            "Content-Type": file.type || "application/octet-stream",
            "X-Filename": file.name,
          },
          body: file,
        }
      );
      return j<import("@/features/asset/dto").PutAssetResponse>(res);
    },
    async patchAssetSummary(assetId: string, patch: import("@/features/asset/dto").PatchAssetSummaryRequest) {
      const f = pickAssetFetch();
      const res = await f(
        `${API_BASE}/assets/${encodeURIComponent(assetId)}/meta`,
        {
          method: "PATCH", headers: { "Content-Type": "application/json" },
          body: JSON.stringify(patch)
        }
      );
      return j<import("@/features/asset/dto").PatchAssetSummaryResponse>(res);
    },
    async delete(assetId: string) {
      const f = pickAssetFetch();
      const res = await f(`${API_BASE}/assets/${encodeURIComponent(assetId)}`, { method: "DELETE" });
      return j<import("@/features/asset/dto").DeleteAssetResponse>(res);
    },
    async upload(ownerId: string, files: File[]) {
      const fetcher = pickAssetFetch();
      const fd = new FormData();
      for (const file of files) fd.append("file", file, file.name);
      const res = await fetcher(`${API_BASE}/owners/${encodeURIComponent(ownerId)}/assets`, {
        method: "POST",
        body: fd
      });
      return j<import("@/features/asset/dto").UploadAssetsResponse>(res);
    },
    async bulkDelete(ids: string[]) {
      const f = pickAssetFetch();
      const res = await f(`${API_BASE}/assets:bulk-delete`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ids })
      });
      return j<import("@/features/asset/dto").BulkDeleteAssetsResponse>(res);
    },
    async getAssetSummary(id: string) {
      const f = pickAssetFetch();
      const res = await f(`${API_BASE}/assets/${encodeURIComponent(id)}`, { method: "GET" });
       return j<import("@/features/asset/dto").AssetSummaryDTO>(res);
    },
  },
  links: {
    async create(body: CreateRequirementLinkRequest): Promise<RequirementLinkDto> {
      const f = pickLinkFetch();
      const res = await f(`${API_BASE}/requirement-links`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      return j<RequirementLinkDto>(res);
    },
    async listByRecord(recordId: string): Promise<RequirementLinkDto[]> {
      const f = pickLinkFetch();
      const res = await f(`${API_BASE}/requirement-links?recordId=${encodeURIComponent(recordId)}&size=999`);
      const page = await j<{ content?: RequirementLinkDto[] }>(res);
      return page.content ?? [];
    },
    async primary(recordId: string, requirementKey: string): Promise<RequirementLinkDto | null> {
      const f = pickLinkFetch();
      const res = await f(`${API_BASE}/requirement-links/primary?recordId=${encodeURIComponent(recordId)}&requirementKey=${encodeURIComponent(requirementKey)}`);
      if (res.status === 404) return null;
      return j<RequirementLinkDto>(res);
    },
    async summary(recordId: string): Promise<Record<string, { count: number; hasPrimary: boolean }>> {
      const f = pickLinkFetch();
      const res = await f(`${API_BASE}/requirement-links/summary?recordId=${encodeURIComponent(recordId)}`);
      return j(res);
    },
    async unlinkAsset(recordId: string, assetId: string): Promise<string[]> {
      const f = pickLinkFetch();
      const res = await f(`${API_BASE}/records/${encodeURIComponent(recordId)}/assets/${encodeURIComponent(assetId)}`, { method: "DELETE" });
      if (res.ok || res.status === 204) return [];
      throw new Error(`Failed to unlink asset: ${res.status}`);
    },
  },

  views: {
    async getRecordView(id: string, opts?: { syncJson?: boolean }) {
      const f = pickRecordFetch();
      const q = new URLSearchParams();
      if (opts?.syncJson) q.set("syncJson", "true");
      const qs = q.toString();
      const res = await f(`${API_BASE}/records/${encodeURIComponent(id)}/view${qs ? `?${qs}` : ""}`, { method: "GET" });
      return j<any>(res);
    },
  },

};

export async function getAssetObjectUrl(assetId: string): Promise<string> {
  const res = await DataFacade.assets.getAssetFile(assetId);
  if (res.ok) {
    return URL.createObjectURL(await res.blob());
  }

  return "";

}



