import type { StoreManager, ListOptions } from "./AssetStoreManager";
import type { AssetSummary } from "@/features/asset/index";

async function rootDir() {
  
  return await navigator.storage.getDirectory();
}
async function ensureDir(dir: FileSystemDirectoryHandle, name: string) {
  return await dir.getDirectoryHandle(name, { create: true });
}
async function getOrCreateFile(dir: FileSystemDirectoryHandle, name: string) {
  return await dir.getFileHandle(name, { create: true });
}
async function writeBlob(fh: FileSystemFileHandle, blob: Blob) {
  const ws = await fh.createWritable();
  await blob.stream().pipeTo(ws);
}
function sortItems(items: AssetSummary[], opt?: ListOptions) {
  if (!opt?.sort) return items;
  const k = opt.sort; const dir = opt.dir === "desc" ? -1 : 1;
  return items.slice().sort((a: any, b: any) => (a[k] > b[k] ? dir : a[k] < b[k] ? -dir : 0));
}
function pageItems<T>(arr: T[], page = 0, size = 50) {
  const start = page * size; return arr.slice(start, start + size);
}
async function sha1(blob: Blob): Promise<string> {
  const buf = await blob.arrayBuffer();
  const h = await crypto.subtle.digest("SHA-1", buf);
  return Array.from(new Uint8Array(h)).map(b => b.toString(16).padStart(2, "0")).join("");
}

const D_ASSETS = "assets";
const D_META = "meta";
const D_OWNER = "owners";
const F_GLOBAL = "global-index.json";
const D_OWNERIDX = "owner-indexes";

export class OPFSAssetStore implements StoreManager {
  private async readJSON<T>(dir: FileSystemDirectoryHandle, name: string): Promise<T | null> {
    try { const fh = await dir.getFileHandle(name); const f = await fh.getFile(); return JSON.parse(await f.text()) as T; }
    catch { return null; }
  }
  private async writeJSON(dir: FileSystemDirectoryHandle, name: string, data: any) {
    const fh = await getOrCreateFile(dir, name);
    await writeBlob(fh, new Blob([JSON.stringify(data)], { type: "application/json" }));
  }
  private async getGlobalIndex(root: FileSystemDirectoryHandle) {
    return (await this.readJSON<Record<string, AssetSummary>>(root, F_GLOBAL)) ?? {};
  }
  private async setGlobalIndex(root: FileSystemDirectoryHandle, idx: Record<string, AssetSummary>) {
    await this.writeJSON(root, F_GLOBAL, idx);
  }
  private async getOwnerIdx(root: FileSystemDirectoryHandle, ownerId: string) {
    const d = await ensureDir(root, D_OWNERIDX);
    return (await this.readJSON<string[]>(d, `${ownerId}.json`)) ?? [];
  }
  private async setOwnerIdx(root: FileSystemDirectoryHandle, ownerId: string, ids: string[]) {
    const d = await ensureDir(root, D_OWNERIDX);
    await this.writeJSON(d, `${ownerId}.json`, ids);
  }

  async put(ownerId: string, assetId: string, file: File): Promise<AssetSummary> {
    const root = await rootDir();
    const dAssets = await ensureDir(root, D_ASSETS);
    const dMeta = await ensureDir(root, D_META);
    const dOwners = await ensureDir(root, D_OWNER);

    const fh = await getOrCreateFile(dAssets, assetId);
    await writeBlob(fh, file);

    const filename = file.name || assetId;
    const contentType = file.type || "application/octet-stream";
    const size = file.size;
    const createdAt = new Date(file.lastModified || Date.now()).toISOString();
    const checksum = await sha1(file);

    const summary: AssetSummary = { id: assetId, ownerId: String(ownerId), filename, contentType, size, checksum, createdAt };

    const mfh = await getOrCreateFile(dMeta, `${assetId}.json`);
    await writeBlob(mfh, new Blob([JSON.stringify(summary)], { type: "application/json" }));

    const g = await this.getGlobalIndex(root);
    g[assetId] = summary;
    await this.setGlobalIndex(root, g);

    const dThisOwner = await ensureDir(dOwners, ownerId);
    await getOrCreateFile(dThisOwner, assetId);

    const ids = await this.getOwnerIdx(root, ownerId);
    if (!ids.includes(assetId)) { ids.push(assetId); await this.setOwnerIdx(root, ownerId, ids); }

    return summary;
  }

  async getAssetSummary(assetId: string): Promise<AssetSummary | null> {
    const root = await rootDir();
    const dMeta = await ensureDir(root, D_META);
    try {
      const fh = await dMeta.getFileHandle(`${assetId}.json`);
      const f = await fh.getFile();
      return JSON.parse(await f.text()) as AssetSummary;
    } catch { return null; }
  }

  async getAssetSummaryByOwner(ownerId: string, assetId: string) {
    const m = await this.getAssetSummary(assetId);
    return m && m.ownerId === String(ownerId) ? m : null;
  }

  async getAssetFile(assetId: string): Promise<File | null> {
    const root = await rootDir();
    const dAssets = await ensureDir(root, D_ASSETS);
    try {
      const fh = await dAssets.getFileHandle(assetId);
      const f = await fh.getFile();
      return new File([f], f.name || assetId, { type: f.type, lastModified: f.lastModified });
    } catch { return null; }
  }

  async delete(assetId: string): Promise<boolean> {
    const root = await rootDir();
    const dAssets = await ensureDir(root, D_ASSETS);
    const dMeta = await ensureDir(root, D_META);
    const dOwners = await ensureDir(root, D_OWNER);

    const meta = await this.getAssetSummary(assetId);
    if (!meta) return false;

    try { await dAssets.removeEntry(assetId); } catch {}
    try { await dMeta.removeEntry(`${assetId}.json`); } catch {}

    const g = await this.getGlobalIndex(root);
    delete g[assetId];
    await this.setGlobalIndex(root, g);

    const dThisOwner = await ensureDir(dOwners, meta.ownerId);
    try { await dThisOwner.removeEntry(assetId); } catch {}

    const ids = await this.getOwnerIdx(root, meta.ownerId);
    const next = ids.filter(id => id !== assetId);
    await this.setOwnerIdx(root, meta.ownerId, next);

    return true;
  }

  async deleteByOwner(ownerId: string, assetId: string): Promise<boolean> {
    const m = await this.getAssetSummary(assetId);
    if (!m || m.ownerId !== String(ownerId)) return false;
    return this.delete(assetId);
  }

  async updateAssetSummary(assetId: string, patch: Partial<Omit<AssetSummary, "id" | "ownerId" | "filename" | "contentType" | "size">>) {
    const root = await rootDir();
    const dMeta = await ensureDir(root, D_META);
    const existing = await this.getAssetSummary(assetId);
    if (!existing) throw new Error("Not found");
    const updated: AssetSummary = { ...existing, ...patch };
    const fh = await getOrCreateFile(dMeta, `${assetId}.json`);
    await writeBlob(fh, new Blob([JSON.stringify(updated)], { type: "application/json" }));

    const g = await this.getGlobalIndex(root);
    g[assetId] = updated;
    await this.setGlobalIndex(root, g);
    return updated;
  }

  async list(ownerId: string | "*", opts?: ListOptions) {
    const root = await rootDir();
    const page = opts?.page ?? 0;
    const size = opts?.size ?? 50;

    if (ownerId === "*") {
      const g = await this.getGlobalIndex(root);
      const all = Object.values(g);
      const sorted = sortItems(all, opts);
      const items = pageItems(sorted, page, size);
      return { items, total: all.length, page, size };
    }

    const ids = await this.getOwnerIdx(root, ownerId);
    const g = await this.getGlobalIndex(root);
    const all = ids.map(id => g[id]).filter(Boolean) as AssetSummary[];
    const sorted = sortItems(all, opts);
    const items = pageItems(sorted, page, size);
    return { items, total: all.length, page, size };
  }

  async saveFiles(ownerId: string, files: File[]) {
    const out: AssetSummary[] = [];
    for (const f of files) {
      const id = await sha1(f);
      out.push(await this.put(ownerId, id, f));
    }
    return out;
  }

  async bulkDelete(assetIds: string[]) {
    const deleted: string[] = [];
    const failed: { id: string; reason: string }[] = [];
    for (const id of assetIds) {
      try { (await this.delete(id)) ? deleted.push(id) : failed.push({ id, reason: "not found" }); }
      catch (e: any) { failed.push({ id, reason: e?.message ?? "error" }); }
    }
    return { deleted, failed };
  }

  async exists(assetId: string) { return (await this.getAssetSummary(assetId)) != null; }
  async clearOwner(ownerId: string) {
    const ids = (await this.list(ownerId)).items.map(i => i.id);
    let n = 0; for (const id of ids) if (await this.delete(id)) n++; return n;
  }
  async clearAll() {
    const ids = (await this.list("*")).items.map(i => i.id);
    let n = 0; for (const id of ids) if (await this.delete(id)) n++; return n;
  }
}

export default OPFSAssetStore;

// LocalStorage cleanup (static)
export namespace OPFSAssetStore {
  /** Removes localStorage keys with a prefix (default 'vn.'). Returns number removed. */
  export function clearLocalStorage(prefix: string = "vn.asset."): number {
    if (typeof localStorage === "undefined") return 0;
    const toRemove: string[] = [];
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (k && k.startsWith(prefix)) toRemove.push(k);
    }
    toRemove.forEach(k => localStorage.removeItem(k));
    return toRemove.length;
  }
}

