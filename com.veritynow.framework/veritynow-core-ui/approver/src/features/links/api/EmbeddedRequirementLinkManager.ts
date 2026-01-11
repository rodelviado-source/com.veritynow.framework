// features/links/api/EmbeddedRequirementLinkManager.ts
import { AbstractRoutedRequirementLinkManager } from "./AbstractRoutedRequirementLinkManager";
import { StoreManager as LinkStoreManager} from "@/features/links/index";
import { StoreManager as AssetStoreManager} from "@/features/asset/index";
import type { CreateRequirementLinkRequest, RequirementLinkDto } from "@/features/links/index";

export class EmbeddedRequirementLinkManager extends AbstractRoutedRequirementLinkManager {
  constructor( 
    private readonly linkStore: LinkStoreManager, 
    private readonly assetStore:AssetStoreManager, base = "/api") { super(base);

     }
  
 
  async create(body: CreateRequirementLinkRequest): Promise<RequirementLinkDto> {
    return this.linkStore.append(body);
  }
  async listByRecord(recordId: string): Promise<RequirementLinkDto[]> {
    return this.linkStore.listByRecord(recordId);
  }
  async listByKey(recordId: string, requirementKey: string): Promise<RequirementLinkDto[]> {
    return this.linkStore.listByKey(recordId, requirementKey);
  }
  async primary(recordId: string, requirementKey: string): Promise<RequirementLinkDto | null> {
    const arr = await this.linkStore.listByKey(recordId, requirementKey);
    if (!arr.length) return null;
    const prim = arr.find(l => (l.metadata as any)?.primary === true);
    if (prim) return prim;
    return arr.sort((a,b) => (a.createdAt < b.createdAt ? 1 : -1))[0];
  }
  async summary(recordId: string): Promise<Record<string, { count: number; hasPrimary: boolean }>> {
    const list = await this.linkStore.listByRecord(recordId);
    const by: Record<string, { count: number; hasPrimary: boolean }> = {};
    for (const l of list) {
      if (l.state !== "attached") continue;
      const meta = await this.assetStore.getAssetSummary(l.assetId);
      if (!meta) continue;
      const k = l.requirementKey;
      if (!by[k]) by[k] = { count: 0, hasPrimary: false };
      by[k].count++;
      if ((l.metadata as any)?.primary === true) by[k].hasPrimary = true;
    }
    return by;
  }
  async unlinkAsset(recordId: string, assetId: string): Promise<string[]> {
    return this.linkStore.removeByAsset(recordId, assetId);
  }
}
export default EmbeddedRequirementLinkManager;
