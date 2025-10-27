import type { Transport, ListParams } from "./Transport";
import type { PageResult, RecordItem } from "../types";
import { IndexedFileStore } from "@/data/IndexedFileStore";

const KEYS = {
  RECS: "vn_embedded_records",
  IMGS: "vn_embedded_images",
} as const;

function nowISO() { return new Date().toISOString(); }

function loadRecs(): RecordItem[] {
  try { return JSON.parse(localStorage.getItem(KEYS.RECS) || "[]") as RecordItem[]; } catch { return []; }
}
function saveRecs(v: RecordItem[]) { localStorage.setItem(KEYS.RECS, JSON.stringify(v)); }

function loadImgs(): Record<string, string> {
  try { return JSON.parse(localStorage.getItem(KEYS.IMGS) || "{}") as Record<string, string>; } catch { return {}; }
}
function saveImgs(v: Record<string, string>) { localStorage.setItem(KEYS.IMGS, JSON.stringify(v)); }

function uid() { return Math.random().toString(36).slice(2) + Date.now().toString(36); }

export class EmbeddedTransport implements Transport {
  label: "Embedded";

  async list({ page, size }: ListParams): Promise<PageResult<RecordItem>> {
    const all = loadRecs().slice().sort((a,b) => +new Date(b.createdAt) - +new Date(a.createdAt));
    const total = all.length;
    const start = page * size;
    return { items: all.slice(start, start + size), page, size, total };
  }

  async create(payload: Partial<RecordItem>): Promise<RecordItem> {
    const recs = loadRecs();
    const id = recs.length ? Math.max(...recs.map(r => r.id)) + 1 : 1;
    const r: RecordItem = {
      id,
      agentId: payload.agentId ?? "",
      agentFirstName: payload.agentFirstName,
      agentMiddleName: payload.agentMiddleName,
      agentLastName: payload.agentLastName,
      agentSuffix: payload.agentSuffix,
      clientId: payload.clientId ?? "",
      clientFirstName: payload.clientFirstName,
      clientMiddleName: payload.clientMiddleName,
      clientLastName: payload.clientLastName,
      clientSuffix: payload.clientSuffix,
      title: payload.title ?? "",
      priority: payload.priority ?? 0,
      status: payload.status ?? "NEW",
      description: payload.description ?? "",
      createdAt: payload.createdAt ?? nowISO(),
      imageIds: [],
    };
    recs.push(r); saveRecs(recs);
    return r;
  }

  async update(id: number, patch: Partial<RecordItem>): Promise<RecordItem> {
    const recs = loadRecs();
    const i = recs.findIndex(r => r.id === id);
    if (i < 0) throw new Error("record not found");
    recs[i] = { ...recs[i], ...patch, id };
    saveRecs(recs);
    return recs[i];
  }

  async uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }> {
    const recs = loadRecs();
    const i = recs.findIndex(r => r.id === id);
    if (i < 0) throw new Error("record not found");

    const imgs = loadImgs();
    const useOpfs = IndexedFileStore.isSupported();
    const newIds: string[] = [];

    for (const f of files) {
      const logicalId = uid();
      if (useOpfs) {
        const indexName = `index-file-${Object.keys(imgs).length + 1}`;
        await IndexedFileStore.write(indexName, f);
        imgs[logicalId] = `opfs:${indexName}`;
      } else {
        const buf = await f.arrayBuffer();
        const b64 = btoa(String.fromCharCode(...new Uint8Array(buf)));
        const mime = f.type || "application/octet-stream";
        imgs[logicalId] = `data:${mime};base64,${b64}`;
      }
      newIds.push(logicalId);
    }

    saveImgs(imgs);
    recs[i].imageIds = [...(recs[i].imageIds || []), ...newIds];
    saveRecs(recs);
    return { imageIds: recs[i].imageIds };
  }

  async imageUrl(imageId: string): Promise<string> {
    const imgs = loadImgs();
    const mapping = imgs[imageId];
    if (!mapping) return "";
    if (mapping.startsWith("opfs:") && IndexedFileStore.isSupported()) {
      const name = mapping.slice("opfs:".length);
      if (await IndexedFileStore.exists(name)) {
        const file = await IndexedFileStore.readFile(name);
        return URL.createObjectURL(file);
      }
      return "";
    }
    return mapping;
  }
}
