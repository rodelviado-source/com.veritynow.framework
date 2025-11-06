// src/sync/uploadRecordByLocalId.ts
import { LocalStorageWithOPFS as Store } from "@/data/store/LocalStorageWithOPFS";
import type { RecordItem } from "@/data/types/Record";

const API_BASE = import.meta.env.VITE_API_BASE ?? "";

/** Fetch a Blob for an imageId using LocalStorageWithOPFS + imgs map. */
async function resolveBlobForImageId(imageId: string): Promise<{ blob: Blob; filename: string; contentType: string }> {
  const imgs = Store.loadImgs();                 // { [imageId]: "opfs:/... | blob:... | data:... | http... | <opfs-name>" }
  const src = imgs[imageId];
  if (!src) throw new Error(`No local image source for ${imageId}`);

  const fallbackType = "application/octet-stream";
  const filenameFrom = (s: string) => {
    try { return decodeURIComponent(s.split("/").pop()!.split("?")[0] || imageId); } catch { return imageId; }
  };

  // OPFS pointer "opfs:<name>"
  if (src.startsWith("opfs:")) {
    const name = src.slice("opfs:".length);
    const file = await Store.readFile(name);     // File (Blob subtype)
    return { blob: file, filename: file.name || name || imageId, contentType: file.type || fallbackType };
  }

  // A plain OPFS name (no prefix)
  if (!/^https?:|^blob:|^data:/i.test(src)) {
    try {
      const file = await Store.readFile(src);
      return { blob: file, filename: file.name || src || imageId, contentType: file.type || fallbackType };
    } catch {
      // fall through to fetch below (in case src is actually a path/URL)
    }
  }

  // blob:/data:/http(s): → fetch then .blob()
  const resp = await fetch(src);
  if (!resp.ok) throw new Error(`Failed to read ${src} (${resp.status})`);
  const blob = await resp.blob();
  return { blob, filename: filenameFrom(src), contentType: blob.type || fallbackType };
}

/** Build the JSON that your backend expects. Adjust mapping to your fields as needed. */
function buildCreateJson(rec: RecordItem) {
  // Keep it explicit; include only fields your backend accepts.
  // If your /api/records accepts the same fields, mirror them here.
  return {
    localId: (rec as any).localId,
    agentId: (rec as any).agentId,
    agentFirstName: (rec as any).agentFirstName,
    agentMiddleName: (rec as any).agentMiddleName,
    agentLastName: (rec as any).agentLastName,
    agentSuffix: (rec as any).agentSuffix,
    clientId: (rec as any).clientId,
    clientFirstName: (rec as any).clientFirstName,
    clientMiddleName: (rec as any).clientMiddleName,
    clientLastName: (rec as any).clientLastName,
    clientSuffix: (rec as any).clientSuffix,
    title: (rec as any).title,
    priority: (rec as any).priority,
    status: (rec as any).status,
    description: (rec as any).description,
    createdAt: (rec as any).createdAt ?? new Date().toISOString(),
  };
}

/** Upload a single record by its localId: reads from Store, posts to /api/records-with-assets. */
export async function uploadRecordByLocalId(localId: string): Promise<{ id: number; imageIds: string[] }> {
  // 1) Load the record from LocalStorageWithOPFS
  const recs = Store.loadRecs<RecordItem>();            // stored under "vn_embedded_records"
  const rec = recs.find(r => (r as any).localId === localId);
  if (!rec) throw new Error(`Local record not found: ${localId}`);

  // 2) Resolve images -> Files (preserve filename/type when possible)
  const ids: string[] = Array.isArray((rec as any).imageIds) ? (rec as any).imageIds : [];
  const files: File[] = [];
  for (const imgId of ids) {
    const { blob, filename, contentType } = await resolveBlobForImageId(imgId);
    files.push(new File([blob], filename || `${imgId}`, { type: contentType || "application/octet-stream" }));
  }

  // 3) Build FormData: JSON part "record" + N file parts "files"
  const fd = new FormData();
  fd.append("record", new Blob([JSON.stringify(buildCreateJson(rec))], { type: "application/json" }));
  files.forEach(f => fd.append("files", f, f.name));    // matches @RequestPart("files")

  // 4) POST to backend
  const r = await fetch(`${API_BASE}/api/records-with-assets`, { method: "POST", body: fd });
  if (!r.ok) {
    const text = await r.text().catch(() => "");
    throw new Error(`Upload failed (${r.status}) ${text}`);
  }
  const resp = await r.json();

  // 5) Mark as synced locally (optional)
  try {
    (rec as any).id = resp.id;               // remote PK
    (rec as any).syncStatus = "synced";
    (rec as any).lastSyncError = undefined;
    const idx = recs.findIndex(r0 => (r0 as any).localId === localId);
    if (idx >= 0) recs[idx] = rec;
    Store.saveRecs(recs);
  } catch { /* non-fatal */ }

  return resp;
}
