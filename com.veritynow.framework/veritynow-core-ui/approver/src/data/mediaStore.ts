import { LruCache } from "@/utils/LruCache";
import { DataFacade } from "@/data/facade/DataFacade";
import type { MediaResource } from "@/data/transports/Transport";

// Bounded in-memory cache for media metadata
const metaCache = new LruCache<string, MediaResource>({
  maxEntries: 500,
  ttlMs: 30 * 60 * 1000, // 30 minutes
});

const inflight = new Map<string, Promise<MediaResource>>();
const listeners = new Map<string, Set<() => void>>();

export function getMetaSync(id: string): MediaResource | null {
  return metaCache.get(id) ?? null;
}

export async function fetchMeta(id: string): Promise<MediaResource> {
  const cached = metaCache.get(id);
  if (cached) return cached;

  const ongoing = inflight.get(id);
  if (ongoing) return ongoing;

  const p = (async () => {
    const r = await DataFacade.mediaFor(String(id));
    metaCache.set(id, r);
    inflight.delete(id);
    listeners.get(id)?.forEach((fn) => fn());
    return r;
  })().catch((e) => {
    inflight.delete(id);
    throw e;
  });

  inflight.set(id, p);
  return p;
}

export function subscribe(id: string, cb: () => void) {
  if (!listeners.has(id)) listeners.set(id, new Set());
  listeners.get(id)!.add(cb);
  return () => listeners.get(id)!.delete(cb);
}

// Maintenance helpers
export function clearAllMetas() {
  metaCache.clear();
}
export function prefillMeta(id: string, meta: MediaResource) {
  metaCache.set(id, meta);
}
export function deleteMeta(id: string) {
  metaCache.delete(id);
}
