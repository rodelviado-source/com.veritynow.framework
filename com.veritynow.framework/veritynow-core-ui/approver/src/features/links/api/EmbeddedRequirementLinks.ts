import { OPFSRequirementLinkStore } from "@/features/links/store/OPFSRequirementLinkStore";
import type { CreateRequirementLinkRequest, RequirementLinkDto } from "@/features/links/index";

export class EmbeddedRequirementLinks {
  private store = new OPFSRequirementLinkStore();

  async create(req: CreateRequirementLinkRequest): Promise<RequirementLinkDto> {
    return this.store.append(req);
  }

  async listByRecord(recordId: string): Promise<RequirementLinkDto[]> {
    return this.store.listByRecord(recordId);
  }

  async listByKey(recordId: string, requirementKey: string): Promise<RequirementLinkDto[]> {
    return this.store.listByKey(recordId, requirementKey);
  }

  async primary(recordId: string, requirementKey: string): Promise<RequirementLinkDto | null> {
    const arr = await this.store.listByKey(recordId, requirementKey);
    if (!arr.length) return null;
    // prefer primary metadata, else latest by createdAt
    const prim = arr.find(l => l.metadata && (l.metadata as any).primary === true);
    if (prim) return prim;
    return arr.sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))[0];
  }

  async summary(recordId: string): Promise<Record<string, { count: number; hasPrimary: boolean }>> {
    const links = await this.store.listByRecord(recordId);
    const byKey: Record<string, { count: number; hasPrimary: boolean }> = {};
    for (const l of links) {
      if (l.state !== "attached") continue;
      const k = l.requirementKey;
      if (!byKey[k]) byKey[k] = { count: 0, hasPrimary: false };
      byKey[k].count += 1;
      if (l.metadata && (l.metadata as any).primary === true) byKey[k].hasPrimary = true;
    }
    return byKey;
  }

  async unlinkAsset(recordId: string, assetId: string): Promise<string[]> {
    // returns keys that were affected (links removed)
    return this.store.removeByAsset(recordId, assetId);
  }
}
