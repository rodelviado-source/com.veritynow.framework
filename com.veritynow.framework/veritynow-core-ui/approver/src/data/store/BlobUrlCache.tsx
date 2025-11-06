// data/store/BlobUrlCache.ts
export type BlobUrlEntry = {
  url: string;
  expiresAt: number;
  timer: ReturnType<typeof setTimeout> | null;
};

export class BlobUrlCache {
  private map = new Map<string, BlobUrlEntry>();
  constructor(private defaultTtlMs = 5 * 60_000) {}

  set(id: string, blob: Blob, ttlMs?: number): string {
    const url = URL.createObjectURL(blob);
    this.schedule(id, url, ttlMs);
    return url;
  }

  async acquire(id: string, factory: () => Promise<Blob>, ttlMs?: number): Promise<string> {
    const e = this.map.get(id);
    if (e) {
      this.schedule(id, e.url, ttlMs);
      return e.url;
    }
    const blob = await factory();
    return this.set(id, blob, ttlMs);
  }

  touch(id: string, ttlMs?: number): boolean {
    const e = this.map.get(id);
    if (!e) return false;
    this.schedule(id, e.url, ttlMs);
    return true;
  }

  revoke(id: string): void {
    const e = this.map.get(id);
    if (!e) return;
    if (e.timer) clearTimeout(e.timer);
    try { URL.revokeObjectURL(e.url); } catch {/**ignore */}
    this.map.delete(id);
  }

  revokeAll(): void {
    for (const id of this.map.keys()) this.revoke(id);
  }

  get(id: string): string | undefined {
    return this.map.get(id)?.url;
  }

  private schedule(id: string, url: string, ttlMs?: number) {
    const ttl = Math.max(1_000, ttlMs ?? this.defaultTtlMs);
    const expiresAt = Date.now() + ttl;
    const existing = this.map.get(id);
    if (existing?.timer) clearTimeout(existing.timer);
    const timer = setTimeout(() => this.revoke(id), ttl);
    
    this.map.set(id, { url, expiresAt, timer });
  }
}

export const blobUrlCache = new BlobUrlCache();