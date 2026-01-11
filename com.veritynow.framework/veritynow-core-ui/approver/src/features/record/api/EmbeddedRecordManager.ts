import { AbstractRoutedRecordManager } from "./AbstractRoutedRecordManager";
import type { StoreManager } from "@/features/record/index";
import type {
  ListRecordsResponse, GetRecordResponse, CreateRecordRequest, CreateRecordResponse,
  UpdateRecordRequest, UpdateRecordResponse, PatchRecordRequest, PatchRecordResponse,
  DeleteRecordResponse, RecordDTO
} from "@/features/record/dto";
import type { RecordItem } from "@/features/record/index";

export class EmbeddedRecordManager extends AbstractRoutedRecordManager {
  constructor(private readonly store: StoreManager, base = "/api") { super(base); }

  private toDTO(r: RecordItem): RecordDTO { return r; }

  async getRecordView(id: string, opts?: { syncJson?: boolean }): Promise<any> {
    const mod = await import("@/features/record/view/EmbeddedRecordView");
    return mod.getRecordViewEmbedded(id, opts);
  }
  
  async list(opts?: any): Promise<ListRecordsResponse> {
    const { items, total, page, size } = await this.store.list(opts);
    return { items: items.map(this.toDTO.bind(this)), page: { total, page, size } };
  }
  async get(id: string): Promise<GetRecordResponse> {
    const r = await this.store.get(id);
    if (!r) throw new Error("Not found");
    return this.toDTO(r);
  }
  async create(body: CreateRecordRequest): Promise<CreateRecordResponse> {
    const created = await this.store.create({ ...body, assetIds: body.assetIds ?? [] } as any);
    return this.toDTO(created);
  }
  async update(id: string, body: UpdateRecordRequest): Promise<UpdateRecordResponse> {
    const updated = await this.store.update(id, body as any);
    return this.toDTO(updated);
  }
  async patch(id: string, patch: PatchRecordRequest): Promise<PatchRecordResponse> {
    const updated = await this.store.patch(id, patch as any);
    return this.toDTO(updated);
  }
  async delete(id: string): Promise<DeleteRecordResponse> {
    const ok = await this.store.delete(id);
    return { id, deleted: ok };
  }
  
  
}
