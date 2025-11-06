
// data/store/StoreCacheManager.ts
import { prefillMeta, getMetaSync } from "@/data/mediaStore";
import { type MediaResource, MediaKind } from "@/data/transports/Transport";
import { BlobUrlCache, blobUrlCache } from "@/data/store/BlobUrlCache";

type FileLike = { name?: string; size?: number; type?: string };
type ServerMeta = { filename?: string; size?: number; contentType?: string; url?: string };

export class StoreCacheManager {
  constructor(
    private readonly blobs: BlobUrlCache = blobUrlCache,
    private readonly defaultTtlMs = 5 * 60_000
  ) {}

  /** Strict: throw if meta for id is not present. */
  getMetaRequired(id: string): { filename: string; size: number; contentType: string; meta?: unknown } {
    const m = getMetaSync(id);
    if (!m || !m.filename || !m.contentType) {
      throw new Error(`Missing meta in store for id=${id}`);
    }
    return m as  { filename: string; size: number; contentType: string; meta?: unknown } ;
  }

  /** Remote-first: get from store or fetch from server and seed, else throw. */
  async getOrFetchMetaRequired(
    id: string,
    fetcher: () => Promise<ServerMeta | null>
  ): Promise<{ filename: string; size: number; contentType: string; meta?: unknown }> {
    const local = getMetaSync(id);
    if (local && local.filename && local.contentType) {
      return local as { filename: string; size: number; contentType: string; meta?: unknown }
    }
    const srv = await fetcher();
    if (!srv || !srv.filename || !srv.contentType) {
      throw new Error(`Missing meta both locally and remotely for id=${id}`);
    }
    // seed store from server's authoritative meta
    const merged: MediaResource = {
      id,
      url: srv.url ?? "",
      kind: this.pickKind(srv.contentType),
      filename: srv.filename,
      size: srv.size ?? 0,
      contentType: srv.contentType,
    } as MediaResource;
    prefillMeta(id, merged);
    return merged as { filename: string; size: number; contentType: string; meta?: unknown; };
  }

  /** Seed authoritative meta immediately after upload. */
  persistUploadMeta(id: string, file: FileLike | null, urlHint: string, kind: MediaKind) {
    const merged: MediaResource = {
      id,
      url: urlHint,
      kind,
      filename: file?.name ?? id,
      size: file?.size ?? 0,
      contentType: file?.type ?? "application/octet-stream",
    } as MediaResource;
    prefillMeta(id, merged);
  }

  /** Build a MediaResource by combining strict meta + provided data URL. */
  buildFromMeta(id: string, url: string): MediaResource {
    const m = this.getMetaRequired(id); // throws if missing
    return {
      id,
      url,
      kind: this.pickKind(m.contentType),
      filename: m.filename,
      size: m.size,
      contentType: m.contentType,
      ...(m.meta ? { meta: m.meta } : {}),
    };
  }

  /** Blob URL helpers (for embedded/OPFS cases). */
  async acquireBlobUrl(id: string, factory: () => Promise<Blob>, ttlMs?: number) {
    return this.blobs.acquire(id, factory, ttlMs ?? this.defaultTtlMs);
  }
  touchBlobUrl(id: string, ttlMs?: number) {
    return this.blobs.touch(id, ttlMs ?? this.defaultTtlMs);
  }
  revokeBlobUrl(id: string) {
    this.blobs.revoke(id);
  }

  private pickKind(mime: string | null): MediaKind {
    if (!mime)  return MediaKind.download;
    const m = mime.toLowerCase();
    if (m === "application/pdf") return MediaKind.pdf;
    if (m.startsWith("image/")) return MediaKind.image;
    if (m.startsWith("video/")) return MediaKind.video;
    return MediaKind.download
  }
}
export const storeCacheManager = new StoreCacheManager();
