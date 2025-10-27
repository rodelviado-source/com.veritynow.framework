const API_BASE = import.meta.env.VITE_API_BASE ?? "";

function isEmbedded(): boolean {
  return (localStorage.getItem("vn_store_mode") as "auto"|"remote"|"embedded") === "embedded";
}
function assertNotEmbedded() {
  if (isEmbedded()) {
    // Prevent accidental remote calls when user expects offline
    throw new Error("Remote transport disabled in embedded mode");
  }
}

export class RemoteTransport implements Transport {
  label = "Remote";

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
}
