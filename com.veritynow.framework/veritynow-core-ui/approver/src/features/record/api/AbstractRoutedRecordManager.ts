import type {
  ListRecordsResponse, GetRecordResponse, CreateRecordRequest, CreateRecordResponse,
  UpdateRecordRequest, UpdateRecordResponse, PatchRecordRequest, PatchRecordResponse,
  DeleteRecordResponse
} from "@/features/record/index";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}
function err(status: number, msg: string) { return jsonResponse({ error: msg }, status); }

export abstract class AbstractRoutedRecordManager {
  constructor(protected readonly base = "/api") {}
  abstract getRecordView(id: string, opts?: { syncJson?: boolean }): Promise<any>;

  abstract list(opts?: any): Promise<ListRecordsResponse>;
  abstract get(id: string): Promise<GetRecordResponse>;
  abstract create(body: CreateRecordRequest): Promise<CreateRecordResponse>;
  abstract update(id: string, body: UpdateRecordRequest): Promise<UpdateRecordResponse>;
  abstract patch(id: string, patch: PatchRecordRequest): Promise<PatchRecordResponse>;
  abstract delete(id: string): Promise<DeleteRecordResponse>;

  async fetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const req = new Request(input, init);
    const url = new URL(req.url, location.origin);
    if (!url.pathname.startsWith(this.base)) return err(404, "Not Found");

    try {
      if (req.method === "GET" && url.pathname === `${this.base}/records`) {
        const opts = {
          page: url.searchParams.get("page") ? Number(url.searchParams.get("page")) : undefined,
          size: url.searchParams.get("size") ? Number(url.searchParams.get("size")) : undefined,
          sort: url.searchParams.get("sort") ?? undefined,
          dir: (url.searchParams.get("dir") as "asc" | "desc") ?? undefined,
          q: url.searchParams.get("q") ?? undefined,
          status: url.searchParams.get("status") ?? undefined,
          agentId: url.searchParams.get("agentId") ?? undefined,
          clientId: url.searchParams.get("clientId") ?? undefined,
        };
        const data = await this.list(opts);
        return jsonResponse(data);
      }

      if (req.method === "POST" && url.pathname === `${this.base}/records`) {
        const body = (await req.json()) as CreateRecordRequest;
        const data = await this.create(body);
        return jsonResponse(data, 201);
      }

      const m = new RegExp(`^${this.base}/records/([^/]+)$`).exec(url.pathname);
      if (m) {
        const id = decodeURIComponent(m[1]);
        if (req.method === "GET")  return jsonResponse(await this.get(id));
        if (req.method === "PUT")  return jsonResponse(await this.update(id, await req.json()));
        if (req.method === "PATCH")return jsonResponse(await this.patch(id, await req.json()));
        if (req.method === "DELETE") return jsonResponse(await this.delete(id));
      }

      const x = url.pathname.match(new RegExp(`^${this.base}/records/([^/]+)/view$`));
      if (req.method === "GET" && x) {
        const recordId = decodeURIComponent(x[1]);
        const syncJson = url.searchParams.get("syncJson") === "true";
        const data = await this.getRecordView(recordId, { syncJson });
        return jsonResponse(data, 200);
      }

      return err(404, "Route not handled by RecordManager.fetch");
    } catch (e: any) {
      return err(500, e?.message || "Internal error");
    }
  }
}
