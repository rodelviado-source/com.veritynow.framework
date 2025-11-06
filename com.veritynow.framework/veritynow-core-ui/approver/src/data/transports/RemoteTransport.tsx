import {Transport, ListParams, PageResult, TransportTypes, ModeTypes, MediaResource, MediaKind} from "@/data/transports/Transport";
import {RecordItem} from "@/data/types/Record";
import { DataFacade } from "../facade/DataFacade";

const API_BASE = import.meta.env.VITE_API_BASE ?? "";
const CT_PDF = 'application/pdf';

function isModeEmbedded(): boolean {
  return  DataFacade.getMode() ===  ModeTypes.embedded;
}

function assertNotEmbedded() {
  if (isModeEmbedded()) {
    // Prevent accidental remote calls when user expects offline
    throw new Error("Remote transport disabled in embedded mode");
  }
}  

function pickKind(mime: string | null): MediaKind {
  if (!mime) return MediaKind.download;
  const m = mime.toLowerCase();
  if (m === CT_PDF) return MediaKind.pdf;
  if (m.startsWith('image/')) return MediaKind.image;
  if (m.startsWith('video/')) return MediaKind.video;
  return MediaKind.download;
}

export class RemoteTransport implements Transport {
  label = TransportTypes.Remote;

    async list(params: ListParams): Promise<PageResult<RecordItem>> {
    assertNotEmbedded();
    const qs = new URLSearchParams({
      page: String(params.page),
      size: String(params.size),
      ...(params.query ? { query: params.query } : {}),
      ...(params.sort ? { sort: params.sort } : {}),
      ...(params.dir ? { dir: params.dir } : {}),
    }).toString();
    const r = await fetch(`${API_BASE}/api/records?${qs}`);
    if (!r.ok) throw new Error(`GET /api/records ${r.status}`);
    const raw = await r.json();
    return {
      items: raw.items ?? raw.content ?? [],
      page: raw.page ?? raw.number ?? params.page,
      size: raw.size ?? params.size,
      total: raw.total ?? raw.totalElements ?? 0,
    };
  }

  async create(payload: Partial<RecordItem>): Promise<RecordItem> {
    assertNotEmbedded();
    const r = await fetch(`${API_BASE}/api/records`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!r.ok) throw new Error(`POST /api/records ${r.status}`);
    return r.json();
  }

  async update(id: number, patch: Partial<RecordItem>): Promise<RecordItem> {
    assertNotEmbedded();
    const r = await fetch(`${API_BASE}/api/records/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(patch),
    });
    if (!r.ok) throw new Error(`PUT /api/records/${id} ${r.status}`);
    return r.json();
  }

  async delete(id: number): Promise<void> {
    assertNotEmbedded();
    const r = await fetch(`${API_BASE}/api/records/${id}`, { method: "DELETE" });
    if (!r.ok && r.status !== 204) throw new Error(`DELETE /api/records/${id} ${r.status}`);
  }

  async getByKey(agentId: string, clientId: string): Promise<RecordItem | null> {
    assertNotEmbedded();
    const qs = new URLSearchParams({ agentId, clientId }).toString();
    const r = await fetch(`${API_BASE}/api/records/by-key?${qs}`);
    if (r.status === 404) return null;
    if (!r.ok) throw new Error(`GET /api/records/by-key ${r.status}`);
    return r.json();
  }

  async updateByKey(agentId: string, clientId: string, patch: Partial<RecordItem>): Promise<RecordItem> {
    assertNotEmbedded();
    const qs = new URLSearchParams({ agentId, clientId }).toString();
    const r = await fetch(`${API_BASE}/api/records/by-key?${qs}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(patch),
    });
    if (!r.ok) throw new Error(`PUT /api/records/by-key ${r.status}`);
    return r.json();
  }

  async deleteByKey(agentId: string, clientId: string): Promise<void> {
    assertNotEmbedded();
    const qs = new URLSearchParams({ agentId, clientId }).toString();
    const r = await fetch(`${API_BASE}/api/records/by-key?${qs}`, { method: "DELETE" });
    if (!r.ok && r.status !== 204) throw new Error(`DELETE /api/records/by-key ${r.status}`);
  }

  async uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }> {
    assertNotEmbedded();
    const fd = new FormData();
    files.forEach(f => fd.append("files", f));
    const r = await fetch(`${API_BASE}/api/images/records/${id}`, { method: "POST", body: fd });
    if (!r.ok) throw new Error(`POST /api/images/records/${id} ${r.status}`);
    return r.json();
  }

  async imageUrl(imageId: string): Promise<string> {
    assertNotEmbedded();
    return `${API_BASE}/api/images/${imageId}`;
  }

  async mediaFor(imageId: string): Promise<MediaResource> {
    assertNotEmbedded();
    
    const meta = await this.tryFetchMeta(imageId);
    console.log(meta);
    if (meta) return {id:imageId, url: meta.url, kind: pickKind(meta.contentType) };

    // Fallback: if input looked like a direct image URL, use it
    return {id:imageId, url: await this.imageUrl(imageId), kind: MediaKind.download };
  }

  // --- API meta (remote) ---
async  tryFetchMeta(id: string): Promise<{ url: string; contentType: string ; filename: string} | null> {
  assertNotEmbedded();
  try {
    const u = `${await this.imageUrl(id)}/meta`;
    const r = await fetch(u);
    console.log(r);
    if (!r.ok) return null;
    const j = await r.json(); // { id, contentType, filename, size, url } 
    return { url: j.url, contentType: j.contentType , filename:j.filename};
  } catch {
    return null;
  }
}

}
