import { RecordItem } from "@/data/types/Record";
import { ModeTypes, PageResult, TransportTypes, Store } from "@/data/transports/Transport";

const REC_KEY = "vn_embedded_records";
const IMG_KEY = "vn_embedded_images";

function loadRecords(): RecordItem[] {
  try { return JSON.parse(localStorage.getItem(REC_KEY) || "[]"); } catch { return []; }
}
function saveRecords(recs: RecordItem[]) {
  localStorage.setItem(REC_KEY, JSON.stringify(recs));
}
function loadImages(): Record<string, string> {
  try { return JSON.parse(localStorage.getItem(IMG_KEY) || "{}"); } catch { return {}; }
}
function saveImages(map: Record<string, string>) {
  localStorage.setItem(IMG_KEY, JSON.stringify(map));
}
function uid(prefix: string = "") {
  return prefix + Math.random().toString(36).slice(2) + Date.now().toString(36);
}
async function fileToDataUrl(f: File): Promise<string> {
  const buf = await f.arrayBuffer();
  const b64 = btoa(String.fromCharCode(...new Uint8Array(buf)));
  const mime = f.type || "application/octet-stream";
  return `data:${mime};base64,${b64}`;
}

export const EmbeddedStore: Store = {
  mode: ModeTypes.embedded,
  label: TransportTypes.Embedded,
  async list(page, size) {
    const all = loadRecords().sort((a, b) => (new Date(b.createdAt).getTime()) - (new Date(a.createdAt).getTime()));
    const total = all.length;
    const start = page * size;
    const items = all.slice(start, start + size);
    return { items, page, size, total } as PageResult<RecordItem>;
  },
  async create(payload) {
    const recs = loadRecords();
    const id = recs.length ? Math.max(...recs.map(r => r.id)) + 1 : 1;
    const now = new Date().toISOString();
    const r: RecordItem = {
      id, imageIds: [],
      agentId: payload.agentId || "",
      clientId: payload.clientId || "",
      title: payload.title || "",
      status: payload.status || "NEW",
      priority: payload.priority ?? 0,
      description: payload.description || "",
      createdAt: payload.createdAt || now,
      agentFirstName: payload.agentFirstName, agentMiddleName: payload.agentMiddleName, agentLastName: payload.agentLastName, agentSuffix: payload.agentSuffix,
      clientFirstName: payload.clientFirstName, clientMiddleName: payload.clientMiddleName, clientLastName: payload.clientLastName, clientSuffix: payload.clientSuffix,
    };
    recs.push(r); saveRecords(recs); return r;
  },
  async update(payload) {
    const recs = loadRecords();
    const idx = recs.findIndex(r => r.id === payload.id);
    if (idx < 0) throw new Error("Not found");
    recs[idx] = { ...recs[idx], ...payload };
    saveRecords(recs);
    return recs[idx];
  },
  async uploadImages(id, files) {
    const recs = loadRecords();
    const idx = recs.findIndex(r => r.id === id);
    if (idx < 0) throw new Error("Not found");
    const images = loadImages();
    const newIds: string[] = [];
    for (const f of files) {
      const dataUrl = await fileToDataUrl(f);
      const imageId = uid("img_");
      images[imageId] = dataUrl;
      newIds.push(imageId);
    }
    saveImages(images);
    recs[idx].imageIds = [...(recs[idx].imageIds || []), ...newIds];
    saveRecords(recs);
    return { imageIds: recs[idx].imageIds };
  },
  imageSrc(id) {
    const images = loadImages();
    return images[id] || null;
  },
};
