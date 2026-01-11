// Lightweight client for /api/requirement-links
export type RequirementLinkState = "pending" | "attached" | "rejected" | "superseded";
export interface CreateRequirementLinkRequest {
  recordId: string;
  requirementKey: string;
  assetId: string;
  state?: RequirementLinkState;
  label?: string;
  notes?: string;
  metadata?: Record<string, unknown>;
}
export interface RequirementLinkDto extends CreateRequirementLinkRequest {
  id: string;
  createdAt: string;
  createdBy?: string;
  verifiedAt?: string;
  state: RequirementLinkState;
}
async function j<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const t = await res.text().catch(() => "");
    throw new Error(`${res.status} ${res.statusText}${t ? " - " + t : ""}`);
  }
  return res.json() as Promise<T>;
}
export const RequirementLinksClient = {
  async create(req: CreateRequirementLinkRequest): Promise<RequirementLinkDto> {
    const res = await fetch("/api/requirement-links", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req),
    });
    return j<RequirementLinkDto>(res);
  },
  async listByRecord(recordId: string): Promise<RequirementLinkDto[]> {
    const res = await fetch(`/api/requirement-links?recordId=${encodeURIComponent(recordId)}&size=999`);
    const page = await j<{ content: RequirementLinkDto[] }>(res);
    return page.content ?? [];
  },
  async primary(recordId: string, requirementKey: string): Promise<RequirementLinkDto> {
    const res = await fetch(`/api/requirement-links/primary?recordId=${encodeURIComponent(recordId)}&requirementKey=${encodeURIComponent(requirementKey)}`);
    return j<RequirementLinkDto>(res);
  },
};
