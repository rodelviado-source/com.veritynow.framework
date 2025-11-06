export type OnEvict<K, V> = (key: K, value: V) => void;

export class LruCache<K, V> {
  private maxEntries: number;
  private onEvict?: OnEvict<K, V>;
  private ttlMs?: number;

  private map = new Map<K, { v: V; expiresAt?: number }>();

  constructor(opts: { maxEntries: number; onEvict?: OnEvict<K, V>; ttlMs?: number }) {
    this.maxEntries = opts.maxEntries;
    this.onEvict = opts.onEvict;
    this.ttlMs = opts.ttlMs;
  }

  get size() { return this.map.size; }

  get(key: K): V | undefined {
    const hit = this.map.get(key);
    if (!hit) return undefined;
    if (hit.expiresAt && Date.now() > hit.expiresAt) {
      this.delete(key);
      return undefined;
    }
    this.map.delete(key);
    this.map.set(key, hit);
    return hit.v;
  }

  set(key: K, value: V) {
    const expiresAt = this.ttlMs ? Date.now() + this.ttlMs : undefined;
    if (this.map.has(key)) this.map.delete(key);
    this.map.set(key, { v: value, expiresAt });
    this.evictIfNeeded();
  }

  has(key: K) { return this.get(key) !== undefined; }

  delete(key: K) {
    const hit = this.map.get(key);
    if (!hit) return false;
    this.map.delete(key);
    this.onEvict?.(key, hit.v);
    return true;
  }

  clear() {
    if (this.onEvict) {
      for (const [k, { v }] of this.map) this.onEvict(k, v);
    }
    this.map.clear();
  }

  private evictIfNeeded() {
    while (this.map.size > this.maxEntries) {
      const firstKey = this.map.keys().next().value as K | undefined;
      if (firstKey === undefined) break;
      this.delete(firstKey);
    }
  }
}
