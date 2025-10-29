import {Transport, ListParams, PageResult, TransportTypes} from "@/data/transports/Transport";
import {RecordItem} from "@/data/types/Record";
import {IndexedFileStore, loadRecs, nowISO, saveRecs, loadImgs, hasOPFS, uid, saveImgs  } from "@/data/store/LocalStorageWithOPFS";



export class EmbeddedTransport implements Transport {
  
	label = TransportTypes.Embedded;

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
    const useOpfs = hasOPFS();
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
    if (mapping.startsWith("opfs:") && hasOPFS()) {
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
