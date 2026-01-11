/**
 * OPFSRequirementLinkStore
 * Stores links under: links/<recordId>/<key>.json (array of links)
 */
import { CreateRequirementLinkRequest, RequirementLinkDto, StoreManager} from "../index";

async function rootDir() { return await navigator.storage.getDirectory(); }
async function ensureDir(dir: FileSystemDirectoryHandle, name: string) {
  return await dir.getDirectoryHandle(name, { create: true });
}
async function getOrCreateFile(dir: FileSystemDirectoryHandle, name: string) {
  return await dir.getFileHandle(name, { create: true });
}
async function readJSON<T>(fh: FileSystemFileHandle): Promise<T | null> {
  try { const f = await fh.getFile(); const txt = await f.text(); return txt ? JSON.parse(txt) as T : null; }
  catch { return null; }
}
async function writeJSON(fh: FileSystemFileHandle, data: unknown) {
  const ws = await fh.createWritable();
  await ws.write(new Blob([JSON.stringify(data)]));
  await ws.close();
}

const D_LINKS = "links";

export class OPFSRequirementLinkStore implements StoreManager {
async append(req: CreateRequirementLinkRequest): Promise<RequirementLinkDto> {
    const root = await rootDir();
    const dLinks = await ensureDir(root, D_LINKS);
    const dRec = await ensureDir(dLinks, req.recordId);
    const fh = await getOrCreateFile(dRec, `${req.requirementKey}.json`);
    const arr = (await readJSON<RequirementLinkDto[]>(fh)) ?? [];
    const link: RequirementLinkDto = {
      id: crypto.randomUUID(),
      recordId: req.recordId,
      requirementKey: req.requirementKey,
      assetId: req.assetId,
      state: req.state ?? "attached",
      label: req.label,
      notes: req.notes,
      metadata: req.metadata,
      createdAt: new Date().toISOString(),
    };
    arr.push(link);
    await writeJSON(fh, arr);
    return link;
  }

  async listByRecord(recordId: string): Promise<RequirementLinkDto[]> {
    const root = await rootDir();
    const dLinks = await ensureDir(root, D_LINKS);
    try {
      const dRec = await ensureDir(dLinks, recordId);
      const links: RequirementLinkDto[] = [];
      // iterate files in record dir
      // @ts-ignore: missing types for entries
      for await (const entry of dRec.values()) {
        if (entry.kind === "file" && entry.name.endsWith(".json")) {
          const fh = await dRec.getFileHandle(entry.name);
          const arr = await readJSON<RequirementLinkDto[]>(fh);
          if (arr) links.push(...arr);
        }
      }
      return links;
    } catch {
      return [];
    }
  }

  async listByKey(recordId: string, requirementKey: string): Promise<RequirementLinkDto[]> {
    const root = await rootDir();
    const dLinks = await ensureDir(root, D_LINKS);
    try {
      const dRec = await ensureDir(dLinks, recordId);
      const fh = await getOrCreateFile(dRec, `${requirementKey}.json`);
      const arr = await readJSON<RequirementLinkDto[]>(fh);
      return arr ?? [];
    } catch {
      return [];
    }
  }

  async removeByAsset(recordId: string, assetId: string): Promise<string[]> {
    const root = await rootDir();
    const dLinks = await ensureDir(root, D_LINKS);
    const removedKeys: string[] = [];
    try {
      const dRec = await ensureDir(dLinks, recordId);
      // @ts-ignore
      for await (const entry of dRec.values()) {
        if (entry.kind === "file" && entry.name.endsWith(".json")) {
          const fh = await dRec.getFileHandle(entry.name);
          const arr = (await readJSON<RequirementLinkDto[]>(fh)) ?? [];
          const next = arr.filter(l => l.assetId !== assetId);
          if (next.length !== arr.length) {
            await writeJSON(fh, next);
            removedKeys.push(entry.name.replace(/\.json$/, ""));
          }
        }
      }
    } catch {}
    return removedKeys;
  }
}
