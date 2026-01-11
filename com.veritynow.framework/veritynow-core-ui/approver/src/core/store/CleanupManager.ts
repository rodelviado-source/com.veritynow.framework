
/**
 * CleanupManager: isolated + scoped cleanup utilities for VerityNow embedded stores.
 * - LocalStorage cleanups (scoped by namespace)
 * - OPFS cleanups (all assets / records, or assets by ownerId)
 *
 * Note: UI can call these directly. Safe to import anywhere (no React deps).
 */

import { OPFSAssetStore } from "@/features/asset/store/OPFSAssetStore";
import { OPFSRecordStore } from "@/features/record/store/OPFSRecordStore";

export const VN_MODE_KEY = "vn.mode";
export const VN_ASSET_PREFIX = "vn.asset.";
export const VN_RECORD_PREFIX = "vn.record.";

/** Delete only asset-related localStorage entries */
export function clearAssetsLocalStorage(prefix: string = VN_ASSET_PREFIX): number {
  if (typeof localStorage === "undefined") return 0;
  const keys: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const k = localStorage.key(i);
    if (k && k.startsWith(prefix)) keys.push(k);
  }
  keys.forEach(k => localStorage.removeItem(k));
  return keys.length;
}

/** Delete only record-related localStorage entries */
export function clearRecordsLocalStorage(prefix: string = VN_RECORD_PREFIX): number {
  if (typeof localStorage === "undefined") return 0;
  const keys: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const k = localStorage.key(i);
    if (k && k.startsWith(prefix)) keys.push(k);
  }
  keys.forEach(k => localStorage.removeItem(k));
  return keys.length;
}

/** Delete EVERYTHING under our VN_* keys (assets, records, mode flag) */
export function clearAllVNLocalStorage(): number {
  if (typeof localStorage === "undefined") return 0;
  const keys: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const k = localStorage.key(i);
    if (!k) continue;
    if (k.startsWith(VN_ASSET_PREFIX) || k.startsWith(VN_RECORD_PREFIX) || k === VN_MODE_KEY) {
      keys.push(k);
    }
  }
  keys.forEach(k => localStorage.removeItem(k));
  return keys.length;
}

/** OPFS: delete ALL assets (list('*') then delete). Returns count deleted. */
export async function clearAllAssetsOPFS(): Promise<number> {
  const store = new OPFSAssetStore();
  const res = await store.list("*", { page: 0, size: 1_000_000 });
  let n = 0;
  for (const a of res.items) {
    if (await store.delete(a.id)) n++;
  }
  return n;
}

/** OPFS: delete assets by ownerId (scoped). Returns count deleted. */
export async function clearAssetsByOwnerOPFS(ownerId: string): Promise<number> {
  const store = new OPFSAssetStore();
  const res = await store.list(ownerId, { page: 0, size: 1_000_000 });
  let n = 0;
  for (const a of res.items) {
    if (await store.delete(a.id)) n++;
  }
  return n;
}

/** OPFS: delete ALL records (if needed). Returns count deleted. */
export async function clearAllRecordsOPFS(): Promise<number> {
  const store = new OPFSRecordStore();
  const res = await store.list({ page: 0, size: 1_000_000 });
  let n = 0;
  for (const r of res.items) {
    if (await store.delete(r.id)) n++;
  }
  return n;
}
