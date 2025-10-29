// src/stores/remote.ts
import { RecordItem } from "@/data/types/Record";
import { PageResult, TransportTypes, ModeTypes, Store } from "@/data/transports/Transport"

const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export const RemoteStore: Store = {
  mode: ModeTypes.remote,
  label:TransportTypes.Remote,

  async list(page, size) {
    // We keep client-side search/sort in the table, but you
    // can pass them through later (query/sort/dir) if you wish.
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
      // query: "...",            // wire this if you want server-side search
      // sort: "createdAt",       // or "priority" | "title" | "status" | "id"
      // dir: "desc",             // "asc" | "desc"
    });
    const res = await fetch(`${API_BASE}/api/records?${params.toString()}`);
    if (!res.ok) throw new Error(`GET /api/records failed: ${res.status}`);
    return res.json() as Promise<PageResult<RecordItem>>;
  },

  async create(payload) {
    const res = await fetch(`${API_BASE}/api/records`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload), // matches CreateRecordRequest
    });
    if (!res.ok) throw new Error(`POST /api/records failed: ${res.status}`);
    return res.json();
  },

  async update(payload) {
    const res = await fetch(`${API_BASE}/api/records/${payload.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload), // matches UpdateRecordRequest
    });
    if (!res.ok) throw new Error(`PUT /api/records/${payload.id} failed: ${res.status}`);
    return res.json();
  },

  async uploadImages(id, files) {
    // Keep as-is if your ImagesController is /api/images/records/{id}
    const fd = new FormData();
    files.forEach((f) => fd.append("files", f));
    const res = await fetch(`${API_BASE}/api/images/records/${id}`, { method: "POST", body: fd });
    if (!res.ok) throw new Error(`POST /api/images/records/${id} failed: ${res.status}`);
    return res.json();
  },

  imageSrc(id) {
    return `${API_BASE}/api/images/${id}`;
  },
};
