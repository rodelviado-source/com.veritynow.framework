import { Transport, ListParams, PageResult, TransportTypes, ModeTypes, MediaResource, MediaKind } from "@/data/transports/Transport";
import { RecordItem, Statuses } from "@/data/types/Record";
import { LocalStorageWithOPFS as Store } from "@/data/store/LocalStorageWithOPFS";
import { DataFacade } from "@/data/facade/DataFacade";
import { prefillMeta, getMetaSync } from "@/data/mediaStore";


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

function parseDataUrlMime(s: string): string | null {
  const m = /^data:([^;,]+)[;,]/i.exec(s);
  return m ? m[1].toLowerCase() : null;
}

async function fetchBlob(url: string): Promise<Blob> {
  const r = await fetch(url);
  if (!r.ok) throw new Error(`blob fetch failed: ${r.status}`);
  return r.blob();
}

// --- tiny magic-number sniff ---
async function sniffMime(blob: Blob): Promise<string | null> {
  const head = new Uint8Array(await blob.slice(0, 64).arrayBuffer());
  const eq = (sig: number[], off = 0) => sig.every((b, i) => head[off + i] === b);
  const ascii = (s: string, off = 0) => [...s].every((ch, i) => head[off + i] === ch.charCodeAt(0));
  if (eq([0x25, 0x50, 0x44, 0x46, 0x2D])) return CT_PDF;                       // %PDF-
  if (eq([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A])) return 'image/png';    // PNG
  if (eq([0xFF, 0xD8, 0xFF])) return 'image/jpeg';                            // JPEG
  if (ascii('GIF87a') || ascii('GIF89a')) return 'image/gif';               // GIF
  if (ascii('RIFF', 0) && ascii('WEBP', 8)) return 'image/webp';              // WEBP
  if (eq([0x49, 0x49, 0x2A, 0x00]) || eq([0x4D, 0x4D, 0x00, 0x2A])) return 'image/tiff'; // TIFF
  if (ascii('ftyp', 4)) {
    const brand = String.fromCharCode(...head.slice(8, 12));
    if (brand.startsWith('he')) return 'image/heic';
    if (brand.startsWith('avif')) return 'image/avif';
  }
  return blob.type || null;
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
    assertNotEmbedded();


    const cached = getMetaSync(imageId) || null;
    const s = await this.imageUrl(imageId);


    let base: MediaResource;


    if (!s) {
      base = {
        id: imageId,
        url: "",
        kind: MediaKind.download,
        filename: imageId,
        size: 0,
        contentType: "application/octet-stream",
      };
    } else if (s.startsWith("data:")) {
      const mime = parseDataUrlMime(s) || "application/octet-stream";
      base = {
        id: imageId,
        url: s,
        kind: MediaKind.image,
        filename: imageId,
        size: 0,
        contentType: mime,
      };
    } else if (s.startsWith("blob:")) {
      const blob = await fetchBlob(s);
      const mime = (await sniffMime(blob)) || blob.type || "application/octet-stream";
      base = {
        id: imageId,
        url: s,
        kind: MediaKind.image,
        filename: imageId,
        size: blob.size,
        contentType: mime,
      };
    } else {
      base = {
        id: imageId,
        url: s,
        kind: MediaKind.download,
        filename: imageId,
        size: 0,
        contentType: "application/octet-stream",
      };
    }


    const merged: MediaResource = {
      ...base,
      filename: cached?.filename || base.filename,
      size: (cached?.size ?? 0) > 0 ? cached!.size : base.size,
      contentType: cached?.contentType || base.contentType,
      ...(cached && ('meta' in cached) ? { meta: (cached as any).meta } : {}),
    };


    prefillMeta(imageId, merged);
    return merged;
  }







}
