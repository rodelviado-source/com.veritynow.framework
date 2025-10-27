// src/data/bootstrapDataLayer.ts
// Intercepts window.fetch for /api/* and routes to Remote (HTTP) or Embedded (localStorage).
// Presentation layer remains unchanged.

import { IndexedFileStore } from "@/data/IndexedFileStore";

type Status = "NEW" | "IN_REVIEW" | "APPROVED" | "REJECTED" | "CLOSED";

export interface RecordItem {
  id: number;
  agentId: string;
  agentFirstName?: string; agentMiddleName?: string; agentLastName?: string; agentSuffix?: string;
  clientId: string;
  clientFirstName?: string; clientMiddleName?: string; clientLastName?: string; clientSuffix?: string;
  title: string;
  priority?: number;
  status?: Status;
  description?: string;
  createdAt: string;
  imageIds: string[];
}

export interface PageResult<T> { items: T[]; page: number; size: number; total: number; }

type Mode = "auto" | "remote" | "embedded";

const STORAGE = {
  MODE: "vn_store_mode",       // "auto" | "remote" | "embedded"
  RECS: "vn_embedded_records", // RecordItem[]
  IMGS: "vn_embedded_images",  // { [id: string]: dataURL }
} as const;

function nowISO() { return new Date().toISOString(); }
//function uid(prefix = "") { return prefix + Math.random().toString(36).slice(2) + Date.now().toString(36); }

function getMode(): Mode {
  const m = localStorage.getItem(STORAGE.MODE) as Mode | null;
  return m ?? "auto";
}
function setMode(mode: Mode) {
  localStorage.setItem(STORAGE.MODE, mode);
}
function loadRecs(): RecordItem[] {
  const raw = localStorage.getItem(STORAGE.RECS);
  if (!raw) return [];
  try { return JSON.parse(raw) as RecordItem[]; } catch { return []; }
}
function saveRecs(v: RecordItem[]) { localStorage.setItem(STORAGE.RECS, JSON.stringify(v)); }
function loadImgs(): Record<string, string> {
  const raw = localStorage.getItem(STORAGE.IMGS);
  if (!raw) return {};
  try { return JSON.parse(raw) as Record<string, string>; } catch { return {}; }
}
function saveImgs(v: Record<string, string>) { localStorage.setItem(STORAGE.IMGS, JSON.stringify(v)); }

//async function fileToDataUrl(f: File): Promise<string> {
//  const buf = await f.arrayBuffer();
//  const b64 = btoa(String.fromCharCode(...new Uint8Array(buf)));
 // const mime = f.type || "application/octet-stream";
//  return `data:${mime};base64,${b64}`;
//}

function dataUrlToParts(dataUrl: string): { mime: string; b64: string } {
  const m = /^data:([^;]+);base64,(.*)$/i.exec(dataUrl);
  if (!m) return { mime: "application/octet-stream", b64: "" };
  return { mime: m[1], b64: m[2] };
}
function b64ToBlob(b64: string, mime: string): Blob {
  const bin = atob(b64);
  const len = bin.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) bytes[i] = bin.charCodeAt(i);
  return new Blob([bytes], { type: mime });
}

// ---------- Embedded handlers ----------
const embedded = {
  async list(page = 0, size = 10): Promise<PageResult<RecordItem>> {
    const all = loadRecs().slice().sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt));
    const total = all.length;
    const start = page * size;
    return { items: all.slice(start, start + size), page, size, total };
  },

  async create(body: Partial<RecordItem>): Promise<RecordItem> {
    const recs = loadRecs();
    const id = recs.length ? Math.max(...recs.map(r => r.id)) + 1 : 1;
    const r: RecordItem = {
      id,
      agentId: body.agentId ?? "",
      agentFirstName: body.agentFirstName, agentMiddleName: body.agentMiddleName, agentLastName: body.agentLastName, agentSuffix: body.agentSuffix,
      clientId: body.clientId ?? "",
      clientFirstName: body.clientFirstName, clientMiddleName: body.clientMiddleName, clientLastName: body.clientLastName, clientSuffix: body.clientSuffix,
      title: body.title ?? "",
      priority: body.priority ?? 0,
      status: (body.status as Status | undefined) ?? "NEW",
      description: body.description ?? "",
      createdAt: body.createdAt ?? nowISO(),
      imageIds: [],
    };
    recs.push(r); saveRecs(recs);
    return r;
  },

  async update(id: number, body: Partial<RecordItem>): Promise<RecordItem | null> {
    const recs = loadRecs();
    const i = recs.findIndex(r => r.id === id);
    if (i < 0) return null;
    recs[i] = { ...recs[i], ...body, id };
    saveRecs(recs);
    return recs[i];
  },

  async upload(recordId: number, files: File[]): Promise<{ imageIds: string[] }> {
    const recs = loadRecs();
    const i = recs.findIndex(r => r.id === recordId);
    if (i < 0) throw new Error("record not found");

    const imgs = loadImgs();
    const newIds: string[] = [];
    const useOpfs = IndexedFileStore.isSupported();

    for (const f of files) {
      const logicalId = (Math.random().toString(36).slice(2) + Date.now().toString(36)); // unique key we put into record.imageIds

      if (useOpfs) {
        // Create a stable index name (not user-visible)
        const idx = `index-file-${Object.keys(imgs).length + 1}`;
        await IndexedFileStore.write(idx, f);
        imgs[logicalId] = `opfs:${idx}`;
      } else {
        // Fallback: data URL in localStorage (older path)
        const buf = await f.arrayBuffer();
        const b64 = btoa(String.fromCharCode(...new Uint8Array(buf)));
        const mime = f.type || "application/octet-stream";
        imgs[logicalId] = `data:${mime};base64,${b64}`;
      }

      newIds.push(logicalId);
    }

    saveImgs(imgs);
    recs[i].imageIds = [...(recs[i].imageIds || []), ...newIds];
    saveRecs(recs);
    return { imageIds: recs[i].imageIds };
  },
  async getImage(id: string): Promise<Response | null> {
    const imgs = loadImgs();
    const dataUrl = imgs[id];
    if (!dataUrl) return null;
    const { mime, b64 } = dataUrlToParts(dataUrl);
    const blob = b64ToBlob(b64, mime);
    return new Response(blob, { status: 200, headers: { "Content-Type": mime, "Cache-Control": "no-cache" } });
  },
};

// ---------- Global typings ----------
declare global {
  interface Window {
    __VN_FETCH_INSTALLED__?: boolean;
    __VN_ORIGINAL_FETCH__?: typeof fetch;
    __VN_DATA__?: {
      getMode: () => Mode;
      setMode: (m: Mode) => void;
      exportEmbedded: () => { records: RecordItem[]; images: Record<string, string> };
      importEmbedded: (data: { records?: RecordItem[]; images?: Record<string, string> }) => void;
      clearEmbedded: () => void;
    };
  }
}

// ---------- Interceptor ----------
const originalFetch: typeof fetch = (window.fetch as typeof fetch).bind(window);

function isApi(url: string): boolean {
  try {
    const u = new URL(url, window.location.origin);
    return u.pathname.startsWith("/api/");
  } catch {
    return false;
  }
}

async function tryRemote(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  return originalFetch(input, init);
}

async function handleEmbedded(url: URL, init?: RequestInit): Promise<Response> {
  const method = (init?.method || "GET").toUpperCase();

  // /api/records
  if (url.pathname === "/api/records") {
    if (method === "GET") {
      const page = Number(url.searchParams.get("page") ?? "0");
      const size = Number(url.searchParams.get("size") ?? "10");
      const data = await embedded.list(page, size);
      return new Response(JSON.stringify(data), { status: 200, headers: { "Content-Type": "application/json" } });
    }
    if (method === "POST") {
      const bodyText = init?.body ? await readBodyAsText(init.body) : "{}";
      const body = (bodyText ? JSON.parse(bodyText) : {}) as Partial<RecordItem>;
      const data = await embedded.create(body);
      return new Response(JSON.stringify(data), { status: 200, headers: { "Content-Type": "application/json" } });
    }
  }

  // /api/records/{id}
  const recIdMatch = url.pathname.match(/^\/api\/records\/(\d+)$/);
  if (recIdMatch && method === "PUT") {
    const id = Number(recIdMatch[1]);
    const bodyText = init?.body ? await readBodyAsText(init.body) : "{}";
    const body = (bodyText ? JSON.parse(bodyText) : {}) as Partial<RecordItem>;
    const data = await embedded.update(id, body);
    if (!data) return new Response(null, { status: 404 });
    return new Response(JSON.stringify(data), { status: 200, headers: { "Content-Type": "application/json" } });
  }

  // POST /api/images/records/{id}
  const uploadMatch = url.pathname.match(/^\/api\/images\/records\/(\d+)$/);
  if (uploadMatch && method === "POST") {
    const id = Number(uploadMatch[1]);
    const files = await readFilesFromFormData(init?.body);
    const data = await embedded.upload(id, files);
    return new Response(JSON.stringify(data), { status: 200, headers: { "Content-Type": "application/json" } });
  }

  // GET /api/images/{imgId}
  const imgMatch = url.pathname.match(/^\/api\/images\/(.+)$/);
  if (imgMatch && method === "GET") {
    const resp = await embedded.getImage(imgMatch[1]);
    if (resp) return resp;
    return new Response(null, { status: 404 });
  }

  // Not implemented in embedded => 404
  return new Response(null, { status: 404 });
}

// ---------- Body helpers (no `any`) ----------
function hasTextMethod(x: unknown): x is { text: () => Promise<string> } {
  if (typeof x !== "object" || x === null) return false;
  const rec = x as Record<string, unknown>;
  return typeof rec["text"] === "function";
}

async function readBodyAsText(body: BodyInit): Promise<string> {
  if (typeof body === "string") return body;
  if (body instanceof Blob) return await body.text();
  if (body instanceof FormData) {
    // Convert form fields to JSON text (useful if someone posts form to JSON endpoint)
    const obj: Record<string, FormDataEntryValue> = {};
    body.forEach((v, k) => { obj[k] = v; });
    return JSON.stringify(obj);
  }
  if (hasTextMethod(body)) {
    return await body.text();
  }
  return "";
}

async function readFilesFromFormData(body?: BodyInit): Promise<File[]> {
  if (!(body instanceof FormData)) return [];
  const files: File[] = [];
  for (const [key, val] of body.entries()) {
    if (key === "files" && val instanceof File) {
      files.push(val);
    }
  }
  return files;
}

async function interceptedFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  try {
    const url = new URL(typeof input === "string" ? input : (input as Request).url, window.location.origin);

    if (!isApi(url.href)) {
      return originalFetch(input, init); // non-API passthrough
    }

    const mode = getMode(); // "auto" | "remote" | "embedded"

    if (mode === "embedded") {
      return handleEmbedded(url, init);
    }

    if (mode === "remote") {
      return tryRemote(input, init);
    }

    // auto: prefer remote; if it throws (offline/CORS), fall back embedded
    try {
      const res = await tryRemote(input, init);
      return res;
    } catch {
      return handleEmbedded(url, init);
    }
  } catch {
    // If URL parsing fails, just fall back to original
    return originalFetch(input, init);
  }
}

// Install only once
if (!window.__VN_FETCH_INSTALLED__) {
  window.__VN_FETCH_INSTALLED__ = true;
  window.__VN_ORIGINAL_FETCH__ = originalFetch;
  window.fetch = interceptedFetch as typeof fetch;

  // Utilities for manual control
  window.__VN_DATA__ = {
    getMode,
    setMode,
    exportEmbedded() {
      return {
        records: loadRecs(),
        images: loadImgs(),
      };
    },
    importEmbedded(data) {
      if (data.records) saveRecs(data.records);
      if (data.images) saveImgs(data.images);
    },
    clearEmbedded() {
      localStorage.removeItem(STORAGE.RECS);
      localStorage.removeItem(STORAGE.IMGS);
    },
  };

  // default to "auto" on first run
  if (!localStorage.getItem(STORAGE.MODE)) setMode("auto");
}
