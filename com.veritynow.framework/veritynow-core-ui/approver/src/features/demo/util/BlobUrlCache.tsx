// src/data/store/BlobUrlCache.ts
type RevokeReason = "explicit" | "ttl" | "renew" | "release";

export class BlobUrlCache {
  private map = new Map<
    string,
    { url: string; leases: number; timeout?: number; expiresAt?: number }
  >();
  private defaultTtlMs = 2 * 60 * 60 * 1000; // 2h default

  setDefaultTtl(ms: number) {
    this.defaultTtlMs = Math.max(10_000, ms);
  }

  get(id: string) { return this.map.get(id)?.url; }
  has(id: string) { return this.map.has(id); }

  async acquire(
    id: string,
    factory: () => Promise<Blob>,
    opts?: { ttlMs?: number; touch?: boolean }
  ): Promise<string> {
    const ttlMs = opts?.ttlMs ?? this.defaultTtlMs;
    const touch = opts?.touch ?? true;

    let e = this.map.get(id);
    if (e) {
      e.leases++;
      if (touch) this.scheduleTtl(id, ttlMs);
      return e.url;
    }

    const blob = await factory();
    const url = URL.createObjectURL(blob);
    e = { url, leases: 1 };
    this.map.set(id, e);
    this.scheduleTtl(id, ttlMs);
    return url;
  }


  async set(id:string, blob:Blob, ttlMs:number): Promise<string> {
    const url = URL.createObjectURL(blob);
    const e = { url, leases: 1 };
    this.map.set(id, e);
    this.scheduleTtl(id, ttlMs);
    return url;

  }


  /** New: release one lease; revoke only if no leases and TTL already elapsed. */
  release(id: string) {
    const e = this.map.get(id);
    if (!e) return;
    e.leases = Math.max(0, e.leases - 1);
    if (e.leases === 0 && e.expiresAt && Date.now() >= e.expiresAt) {
      this._revoke(id, "release");
    }
  }


  /** Optional: force a fresh URL (revokes old, ignoring leases). */
  async renew(id: string, factory: () => Promise<Blob>, ttlMs?: number) {
    this._revoke(id, "renew");
    return this.acquire(id, factory, { ttlMs });
  }

  /** Hard revoke now (admin/explicit cleanup). */
  revoke(id: string) { this._revoke(id, "explicit"); }

  revokeAll() {
    for (const k of Array.from(this.map.keys())) this._revoke(k, "explicit");
  }

  touch(id: string, ttlMs?: number) {
    if (!this.map.has(id)) return;
    this.scheduleTtl(id, ttlMs ?? this.defaultTtlMs);
  }

// inside BlobUrlCache.ts (after touch())
touchAndGet(id: string, ttlMs?: number): string | undefined {
  const e = this.map.get(id);
  if (!e) return undefined;
  // refresh its TTL if it exists
  this.scheduleTtl(id, ttlMs ?? this.defaultTtlMs);
  return e.url;
}

  // ---- internals ----
  private scheduleTtl(id: string, ttlMs: number) {
    const e = this.map.get(id);
    if (!e) return;
    if (e.timeout) clearTimeout(e.timeout);
    e.expiresAt = Date.now() + ttlMs;
    e.timeout = window.setTimeout(() => {
      const cur = this.map.get(id);
      if (!cur) return;
      if (cur.leases > 0) { // still in use → check again later
        cur.timeout = window.setTimeout(() => this.scheduleTtl(id, 30_000), 30_000);
        return;
      }
      this._revoke(id, "ttl");
    }, ttlMs) as unknown as number;
  }

  private _revoke(id: string, reason: RevokeReason) {
    const e = this.map.get(id);
    if (!e) return;
    if (e.timeout) clearTimeout(e.timeout);
    try { URL.revokeObjectURL(e.url); } catch {/** ignore */}
    this.map.delete(id);
    
    console.log("REVOKED====>", e.url, reason);
  }
}


declare global {
  /**
   * This is a HMR-safe singleton for the Blob URL cache.
   * We use `var` and `declare global` to ensure it attaches to the global scope
   * and maintains state across Hot Module Replacement updates.
   */
  var __vn_blob_cache: BlobUrlCache | undefined;
}

/* HMR-safe singleton */
export const blobUrlCache: BlobUrlCache =
  globalThis.__vn_blob_cache ??
  (globalThis.__vn_blob_cache = new BlobUrlCache());
