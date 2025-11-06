import { LruCache } from "@/utils/LruCache";

// Cache thumbnail URLs (data URLs or blob: URLs). Revoke blob URLs on eviction.
export const thumbCache = new LruCache<string, string>({
  maxEntries: 300,
  ttlMs: 30 * 60 * 1000,
  onEvict: (_k, url) => {
    try {
      if (url.startsWith("blob:")) URL.revokeObjectURL(url);
    } catch {/**do nothing */}
  },
});

export const getThumb = (key: string) => thumbCache.get(key);
export const setThumb = (key: string, url: string) => thumbCache.set(key, url);
export const clearAllThumbs = () => thumbCache.clear();
export const deleteThumb = (key: string) => thumbCache.delete(key);
