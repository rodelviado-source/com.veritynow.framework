import { Transport, ListParams, PageResult, TransportTypes, ModeTypes, MediaResource, MediaKind } from "@/data/transports/Transport";
import { RecordItem, Statuses } from "@/data/types/Record";
import { LocalStorageWithOPFS as Store } from "@/data/store/LocalStorageWithOPFS";
import { DataFacade } from "@/data/facade/DataFacade";
import { prefillMeta } from "@/data/mediaStore";
import { storeCacheManager } from "@/data/store/StoreCacheManager";


const CT_PDF = 'application/pdf';

function isModeRemote(): boolean {
  return DataFacade.getMode() === ModeTypes.remote;
}

function assertNotEmbedded() {
  if (isModeRemote()) {
    // Prevent accidental embedded calls when user expects offline
    throw new Error("Embedded transport disabled in remote mode");
  }
}

function pickKind(mime: string | null): MediaKind {
  if (!mime) return MediaKind.download;
  const m = mime.toLowerCase();
  if (m === CT_PDF) return MediaKind.pdf;
  if (m.startsWith('image/')) return MediaKind.image;
  if (m.startsWith('video/')) return MediaKind.video;
  return MediaKind.download;
}

export class EmbeddedTransport implements Transport {
  mode = ModeTypes.embedded;
  label = TransportTypes.Embedded;

  async list({ page, size }: ListParams): Promise<PageResult<RecordItem>> {
    assertNotEmbedded();
    const all = Store.loadRecs<RecordItem>().slice().sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt));
    const total = all.length;
    const start = page * size;
    return { items: all.slice(start, start + size), page, size, total };
  }

  async create(payload: Partial<RecordItem>): Promise<RecordItem> {
    assertNotEmbedded();
    const recs = Store.loadRecs<RecordItem>();
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
      status: payload.status ?? Statuses.NEW,
      description: payload.description ?? "",
      createdAt: payload.createdAt ?? Store.nowISO(),
      imageIds: [],
    };
    recs.push(r); Store.saveRecs(recs);
    return r;
  }

  async update(id: number, patch: Partial<RecordItem>): Promise<RecordItem> {
    assertNotEmbedded();
    const recs = Store.loadRecs<RecordItem>();
    const i = recs.findIndex(r => r.id === id);
    if (i < 0) throw new Error("record not found");
    recs[i] = { ...recs[i], ...patch, id };
    Store.saveRecs(recs);
    return recs[i];
  }

  async uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }> {
    assertNotEmbedded();
    const recs = Store.loadRecs<RecordItem>();
    const i = recs.findIndex(r => r.id === id);
    if (i < 0) throw new Error("record not found");

    const imgs = Store.loadImgs();
    const useOpfs = Store.hasOPFS();
    const newIds: string[] = [];

    for (const f of files) {
      const logicalId = Store.uid();

      if (useOpfs) {
        const indexName = `index-file-${Object.keys(imgs).length + 1}`;
        await Store.write(indexName, f);

        const contentType = f.type || "application/octet-stream";
        const url = await Store.toObjectUrl(indexName);

        prefillMeta(logicalId, {
          id: logicalId,
          url,
          kind: pickKind(contentType),
          filename: f.name ?? "",
          size: f.size ?? 0,
          contentType,
        });

        imgs[logicalId] = `opfs:${indexName}`;
        newIds.push(logicalId);
      } else {
        const buf = await f.arrayBuffer();
        const b64 = btoa(String.fromCharCode(...new Uint8Array(buf)));
        const mime = f.type || "application/octet-stream";
        const dataUrl = `data:${mime};base64,${b64}`;

        prefillMeta(logicalId, {
          id: logicalId,
          url: dataUrl,
          kind: pickKind(mime),
          filename: f.name ?? "",
          size: f.size ?? 0,
          contentType: mime,
        });

        imgs[logicalId] = dataUrl;
        newIds.push(logicalId);
      }
    }

    Store.saveImgs(imgs);
    recs[i].imageIds = [...(recs[i].imageIds || []), ...newIds];
    Store.saveRecs(recs);
    return { imageIds: recs[i].imageIds };
  }




  async imageUrl(imageId: string): Promise<string> {
    assertNotEmbedded();
    const imgs = Store.loadImgs();
    const mapping = imgs[imageId];
    if (!mapping) return "";
    if (mapping.startsWith("opfs:") && Store.hasOPFS()) {
      const name = mapping.slice("opfs:".length);
      if (await Store.exists(name)) {
        return Store.toObjectUrl(name);
      }
      return "";
    }
    return mapping;
  }

  async mediaFor(imageId: string): Promise<MediaResource> {
    const meta = storeCacheManager.getMetaRequired(imageId);
    const s = await this.imageUrl(imageId);
    if (!s) throw new Error(`No data source for id=${imageId}`);
    let url: string = s;
    if (s.startsWith("blob:")) {
      url = await storeCacheManager.acquireBlobUrl(imageId, async () => await fetch(s).then(r => r.blob()));
    }
    return {
      id: imageId,
      url,
      kind: ((): MediaKind => {
        const ct = meta.contentType.toLowerCase();
        if (ct === "application/pdf") return MediaKind.pdf;
        if (ct.startsWith("image/")) return MediaKind.image;
        if (ct.startsWith("video/")) return MediaKind.video;
        return MediaKind.download;
      })(),
      filename: meta.filename,
      size: meta.size,
      contentType: meta.contentType,
      ...(meta.meta ? { meta: meta.meta } : {}),
    };
  }
}