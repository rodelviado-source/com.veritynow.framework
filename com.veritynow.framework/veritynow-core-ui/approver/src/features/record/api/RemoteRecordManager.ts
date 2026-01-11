// src/data/transports/RemoteTransport.ts
// Minimal, dependency-free transport that talks to the Spring Boot API (or any REST with same shape).
// If you need a different base URL, pass it to the constructor.
import type {
  RecordDTO,
  ListRecordsResponse,
} from "@/features/record/dto";

export class RemoteRecordManager {
  readonly baseUrl: string;

  constructor(baseUrl: string = "") {
    this.baseUrl = baseUrl.replace(/\/$/, ""); // trim trailing slash
  }

  // ---- low-level fetch passthrough (kept public for DataFacade.fetch) ----
  async fetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const url = typeof input === "string" ? this.baseUrl + input : input;
    return fetch(url as any, init);
  }

  // ================== RECORDS ==================

  async listRecords(opts?: { page?: number; size?: number; sort?: string; dir?: "asc"|"desc"; }): Promise<ListRecordsResponse> {
    const { page = 0, size = 50, sort = "", dir = "asc" } = opts || {};
    const qs = new URLSearchParams({ page: String(page), size: String(size), dir, ...(sort ? { sort } : {}) });
    const res = await this.fetch(`/api/records?${qs.toString()}`, { method: "GET" });
    if (!res.ok) throw new Error(`listRecords failed: ${res.status}`);
    return res.json();
  }

  async getRecord(id: string): Promise<RecordDTO> {
    const res = await this.fetch(`/api/records/${encodeURIComponent(id)}`, { method: "GET" });
    if (!res.ok) throw new Error(`getRecord failed: ${res.status}`);
    return res.json();
  }

  async createRecord(body: Partial<RecordDTO>): Promise<RecordDTO> {
    const res = await this.fetch(`/api/records`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error(`createRecord failed: ${res.status}`);
    return res.json();
  }

  async putRecord(id: string, body: RecordDTO): Promise<RecordDTO> {
    const res = await this.fetch(`/api/records/${encodeURIComponent(id)}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error(`putRecord failed: ${res.status}`);
    return res.json();
  }

  async patchRecord(id: string, patch: Partial<RecordDTO>): Promise<RecordDTO> {
    const res = await this.fetch(`/api/records/${encodeURIComponent(id)}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(patch),
    });
    if (!res.ok) throw new Error(`patchRecord failed: ${res.status}`);
    return res.json();
  }

  async deleteRecord(id: string): Promise<{ id: string; deleted: boolean; }> {
    const res = await this.fetch(`/api/records/${encodeURIComponent(id)}`, { method: "DELETE" });
    if (!res.ok) throw new Error(`deleteRecord failed: ${res.status}`);
    return res.json();
  }
}

export default RemoteRecordManager;
