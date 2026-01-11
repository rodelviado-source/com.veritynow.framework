// features/links/api/AbstractRoutedRequirementLinkManager.ts
import type { CreateRequirementLinkRequest, RequirementLinkDto } from "@/features/links/index";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}
function err(status: number, msg: string) { return jsonResponse({ error: msg }, status); }

export abstract class AbstractRoutedRequirementLinkManager {
  constructor(protected readonly base = "/api") {}

  abstract create(body: CreateRequirementLinkRequest): Promise<RequirementLinkDto>;
  abstract listByRecord(recordId: string): Promise<RequirementLinkDto[]>;
  abstract listByKey(recordId: string, requirementKey: string): Promise<RequirementLinkDto[]>;
  abstract primary(recordId: string, requirementKey: string): Promise<RequirementLinkDto | null>;
  abstract summary(recordId: string): Promise<Record<string, { count: number; hasPrimary: boolean }>>;
  abstract unlinkAsset(recordId: string, assetId: string): Promise<string[]>;

  async fetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    try {
      const req = new Request(input, init);
      const url = new URL(req.url, location.origin);

      if (req.method === "POST" && url.pathname === `${this.base}/requirement-links`) {
        const body = await req.json();
        const created = await this.create(body);
        return jsonResponse(created, 201);
      }

      if (req.method === "GET" && url.pathname === `${this.base}/requirement-links`) {
        const recordId = url.searchParams.get("recordId") || "";
        const key = url.searchParams.get("requirementKey");
        if (!recordId) return err(400, "recordId is required");
        const items = key ? await this.listByKey(recordId, key) : await this.listByRecord(recordId);
        return jsonResponse({ content: items, totalElements: items.length, numberOfElements: items.length, pageable: true });
      }

      if (req.method === "GET" && url.pathname === `${this.base}/requirement-links/primary`) {
        const recordId = url.searchParams.get("recordId") || "";
        const key = url.searchParams.get("requirementKey") || "";
        const p = await this.primary(recordId, key);
        if (!p) return err(404, "Not found");
        return jsonResponse(p);
      }

      if (req.method === "GET" && url.pathname === `${this.base}/requirement-links/summary`) {
        const recordId = url.searchParams.get("recordId") || "";
        return jsonResponse(await this.summary(recordId));
      }

      const del = url.pathname.match(new RegExp(`^${this.base}/records/([^/]+)/assets/([^/]+)$`));
      if (del && req.method === "DELETE") {
        const recordId = decodeURIComponent(del[1]);
        const assetId = decodeURIComponent(del[2]);
        await this.unlinkAsset(recordId, assetId);
        return new Response(null, { status: 204 });
      }

      return err(404, "Route not handled by RequirementLinks.fetch");
    } catch (e: any) {
      return err(500, e?.message || "Internal error");
    }
  }
}
export default AbstractRoutedRequirementLinkManager;
