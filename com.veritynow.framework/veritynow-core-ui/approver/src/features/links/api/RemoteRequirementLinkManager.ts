// features/links/api/RemoteRequirementLinkManager.ts
import type { CreateRequirementLinkRequest, RequirementLinkDto } from "@/features/links/index";


export class RemoteRequirementLinkManager {
  constructor(private readonly baseUrl: string = "") {
    this.baseUrl = this.baseUrl.replace(/\/$/, "");
  }
  
  // ---- low-level fetch passthrough (kept public for DataFacade.fetch) ----
  async fetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const url = typeof input === "string" ? this.baseUrl + input : input;
    return this.fetch(url, init);
  }

  async create(body: CreateRequirementLinkRequest): Promise<RequirementLinkDto> {
    const res = await this.fetch(`${this.baseUrl}/requirement-links`, {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error(`create link failed: ${res.status}`);
    return res.json();
  }
  async listByRecord(recordId: string): Promise<{ content: RequirementLinkDto[] }> {
    const res = await this.fetch(`${this.baseUrl}/requirement-links?recordId=${encodeURIComponent(recordId)}&size=999`);
    if (!res.ok) throw new Error(`list failed: ${res.status}`);
    return res.json();
  }
  async primary(recordId: string, requirementKey: string): Promise<RequirementLinkDto | null> {
    const res = await this.fetch(`${this.baseUrl}/requirement-links/primary?recordId=${encodeURIComponent(recordId)}&requirementKey=${encodeURIComponent(requirementKey)}`);
    if (res.status === 404) return null;
    if (!res.ok) throw new Error(`primary failed: ${res.status}`);
    return res.json();
  }
  async summary(recordId: string): Promise<Record<string, { count: number; hasPrimary: boolean }>> {
    const res = await this.fetch(`${this.baseUrl}/requirement-links/summary?recordId=${encodeURIComponent(recordId)}`);
    if (!res.ok) throw new Error(`summary failed: ${res.status}`);
    return res.json();
  }
  async unlinkAsset(recordId: string, assetId: string): Promise<void> {
    const res = await this.fetch(`${this.baseUrl}/records/${encodeURIComponent(recordId)}/assets/${encodeURIComponent(assetId)}`, { method: "DELETE" });
    if (!res.ok && res.status !== 204) throw new Error(`unlink failed: ${res.status}`);
  }

  
}
export default RemoteRequirementLinkManager;
