/**
 * Local (embedded) Record View builder to mirror server `/api/records/{id}/view`.
 * - Derives requirement booleans from requirement-links + existing assets
 * - Optionally synchronizes record.requirementsJson to the derived truth
 */

import { EmbeddedRequirementLinkManager, OPFSRequirementLinkStore } from "@/features/links/index";
import { EmbeddedAssetManager, OPFSAssetStore } from "@/features/asset/index";
import { EmbeddedRecordManager, OPFSRecordStore } from "@/features/record/index";

export type LinkedAsset = {
  assetId: string;
  requirementKey: string;
  primary: boolean;
};

export type RecordView = {
  record: any;
  requirements: Record<string, boolean>;
  linkedAssets: LinkedAsset[];
};

const links = new EmbeddedRequirementLinkManager(new OPFSRequirementLinkStore(), new OPFSAssetStore(), "/api");
const records = new EmbeddedRecordManager(new OPFSRecordStore(), "/api");
const assets = new EmbeddedAssetManager(new OPFSAssetStore(), "/api");

function parseReqJson(s?: string | null): any {
  if (!s) return {};
  try {
    return JSON.parse(s);
  } catch {
    return {};
  }
}

function writeJson(obj: any): string {
  try {
    return JSON.stringify(obj);
  } catch {
    return "{}";
  }
}

export async function getRecordViewEmbedded(
  recordId: string,
  opts?: { syncJson?: boolean }
): Promise<RecordView> {
  // 1) Load record
  const rec = await records.get(recordId);

  // 2) Load all requirement links for record
  const allLinks = await links.listByRecord(recordId);

  // 3) Keep only attached + asset still exists
  const kept: { key: string; assetId: string; meta?: any }[] = [];
  for (const l of allLinks) {
    if (l.state !== "attached") continue;
    const m = await assets.getAssetFile(l.assetId);
    if (!m) continue;
    kept.push({ key: l.requirementKey, assetId: l.assetId, meta: l.metadata });
  }

  // 4) Build derived requirement boolean map
  const requirements: Record<string, boolean> = {};
  const linkedAssets: LinkedAsset[] = [];

  for (const l of kept) {
    requirements[l.key] = true;
    linkedAssets.push({
      assetId: l.assetId,
      requirementKey: l.key,
      primary: !!(l.meta && (l.meta as any).primary === true),
    });
  }

  // 5) If requested, sync JSON back into record.requirementsJson
  if (opts?.syncJson) {
    const jsonMap = parseReqJson((rec as any).requirementsJson);
    const reqs =
      jsonMap.requirements && typeof jsonMap.requirements === "object"
        ? jsonMap.requirements
        : (jsonMap.requirements = {});

    for (const k of Object.keys(reqs)) {
      reqs[k] = !!requirements[k];
    }
    for (const k of Object.keys(requirements)) {
      if (requirements[k] === true && !(k in reqs)) reqs[k] = true;
    }
    (rec as any).requirements = writeJson(jsonMap);
    await records.patch(recordId, {
      requirements: (rec as any).requirements,
    } as any);

  }

  return { record: rec, requirements, linkedAssets };
}
