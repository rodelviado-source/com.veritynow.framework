import type { StoreManager, RecordListOptions } from "./RecordStoreManager";
import type { RecordItem } from "@/features/record/types";

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
async function writeJSONFile(dir: FileSystemDirectoryHandle, name: string, data: any) {
  const fh = await getOrCreateFile(dir, name);
  await writeBlob(fh, new Blob([JSON.stringify(data)], { type: "application/json" }));
}
async function readJSONFile<T>(dir: FileSystemDirectoryHandle, name: string): Promise<T | null> {
  try { const fh = await dir.getFileHandle(name); const f = await fh.getFile(); return JSON.parse(await f.text()); }
  catch { return null; }
}
function sortItems<T>(items: T[], key?: keyof T, dir: "asc" | "desc" = "asc") {
  if (!key) return items;
  const mult = dir === "desc" ? -1 : 1;
  return items.slice().sort((a: any, b: any) => (a[key] > b[key] ? mult : a[key] < b[key] ? -mult : 0));
}
function pageItems<T>(items: T[], page = 0, size = 50) {
  const start = page * size; return items.slice(start, start + size);
}

const D_RECORDS = "records";
const F_INDEX = "records-index.json";

export class OPFSRecordStore implements StoreManager {
  private async idx(root: FileSystemDirectoryHandle) {
    return (await readJSONFile<string[]>(root, F_INDEX)) ?? [];
  }
  private async setIdx(root: FileSystemDirectoryHandle, ids: string[]) {
    await writeJSONFile(root, F_INDEX, ids);
  }
  private async putDoc(root: FileSystemDirectoryHandle, rec: RecordItem) {
    const dir = await ensureDir(root, D_RECORDS);
    await writeJSONFile(dir, `${rec.id}.json`, rec);
  }
  private async getDoc(root: FileSystemDirectoryHandle, id: string): Promise<RecordItem | null> {
    const dir = await ensureDir(root, D_RECORDS);
    return await readJSONFile<RecordItem>(dir, `${id}.json`);
  }
  private async delDoc(root: FileSystemDirectoryHandle, id: string) {
    const dir = await ensureDir(root, D_RECORDS);
    try { await dir.removeEntry(`${id}.json`); } catch {}
  }

  async create(input: Omit<RecordItem, "id" | "createdAt" | "updatedAt">): Promise<RecordItem> {
    const root = await rootDir();
    const id = crypto.randomUUID();
    const now = new Date().toISOString();
    const rec: RecordItem = { ...input, id, createdAt: now, updatedAt: undefined, assetIds: input.assetIds ?? [] };
    await this.putDoc(root, rec);
    const ids = await this.idx(root);
    ids.push(id);
    await this.setIdx(root, ids);
    return rec;
  }

  async get(id: string) {
    const root = await rootDir();
    return this.getDoc(root, id);
  }

  async update(id: string, record: Omit<RecordItem, "createdAt">): Promise<RecordItem> {
    const root = await rootDir();
    const existing = await this.getDoc(root, id);
    if (!existing) throw new Error("Not found");
    const updated: RecordItem = { ...record, createdAt: existing.createdAt, updatedAt: new Date().toISOString() };
    await this.putDoc(root, updated);
    return updated;
  }

  async patch(id: string, patch: Partial<Omit<RecordItem, "id" | "createdAt">>) {
    const root = await rootDir();
    const existing = await this.getDoc(root, id);
    if (!existing) throw new Error("Not found");
    const updated: RecordItem = { ...existing, ...patch, id: existing.id, createdAt: existing.createdAt, updatedAt: new Date().toISOString() };
    await this.putDoc(root, updated);
    return updated;
  }

  async delete(id: string) {
    const root = await rootDir();
    const existing = await this.getDoc(root, id);
    if (!existing) return false;
    await this.delDoc(root, id);
    const ids = (await this.idx(root)).filter(x => x !== id);
    await this.setIdx(root, ids);
    return true;
  }

  async list(opts?: RecordListOptions) {
    const root = await rootDir();
    const ids = await this.idx(root);
    const dir = await ensureDir(root, D_RECORDS);
    const items: RecordItem[] = [];
    for (const id of ids) {
      const r = await readJSONFile<RecordItem>(dir, `${id}.json`);
      if (r) items.push(r);
    }

    let filtered = items;
    if (opts?.status) filtered = filtered.filter(r => r.status === opts.status);
    if (opts?.agentId) filtered = filtered.filter(r => r.agentId === opts.agentId);
    if (opts?.clientId) filtered = filtered.filter(r => r.clientId === opts.clientId);
    if (opts?.q) {
      const q = opts.q.toLowerCase();
      filtered = filtered.filter(r =>
        (r.requirements ?? "").toLowerCase().includes(q) ||
        (r.agentFirstName ?? "").toLowerCase().includes(q) ||
        (r.agentLastName ?? "").toLowerCase().includes(q) ||
        (r.clientFirstName ?? "").toLowerCase().includes(q) ||
        (r.clientLastName ?? "").toLowerCase().includes(q)
      );
    }

    const sorted = sortItems(filtered, opts?.sort, opts?.dir ?? "asc");
    const page = opts?.page ?? 0;
    const size = opts?.size ?? 50;
    const paged = pageItems(sorted, page, size);
    return { items: paged, total: filtered.length, page, size };
  }

  async addImages(id: string, assetIds: string[]) {
    const root = await rootDir();
    const existing = await this.getDoc(root, id);
    if (!existing) throw new Error("Not found");
    const set = new Set([...(existing.assetIds ?? []), ...assetIds]);
    const updated = { ...existing, assetIds: Array.from(set), updatedAt: new Date().toISOString() };
    await this.putDoc(root, updated);
    return updated;
  }

  async removeImages(id: string, assetIds: string[]) {
    const root = await rootDir();
    const existing = await this.getDoc(root, id);
    if (!existing) throw new Error("Not found");
    const set = new Set(existing.assetIds ?? []);
    assetIds.forEach(x => set.delete(x));
    const updated = { ...existing, assetIds: Array.from(set), updatedAt: new Date().toISOString() };
    await this.putDoc(root, updated);
    return updated;
  }

  async clearAll() {
    const root = await rootDir();
    const ids = await this.idx(root);
    for (const id of ids) await this.delDoc(root, id);
    await this.setIdx(root, []);
    return ids.length;
  }
}

export default OPFSRecordStore;

// LocalStorage cleanup (static)
export namespace OPFSRecordStore {
  /** Removes localStorage keys with a prefix (default 'vn.'). Returns number removed. */
  export function clearLocalStorage(prefix: string = "vn.record."): number {
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

