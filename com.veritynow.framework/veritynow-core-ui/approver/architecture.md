# Architecture Overview: Data Facade + Transports

This document explains how the UI talks to data in a way that supports **remote API**, **embedded (offline)**, and **auto-fallback** without changing any React components.

---

## Layers

```
UI (React components)
   │
   ▼
Data Facade (src/data/facade/DataFacade.tsx)
   │
   ├─   RemoteTransport (src/data/transports/RemoteTransport.tsx)
   │     - fetches from Spring Boot: /api/records, /api/images, etc.
   │      
   └─   EmbeddedTransport (src/data/transports/EmbeddedTransport.tsx)
          │    - fetching  from local storage for records (localStorage)
          │  
          └─ Store  (src\data\store\LocalStorageWithOPFS.tsx)
                - Records as JSON stored in localStorage
                - Record's  Documents/images via OPFS (Origin Private File System) or data URLs
```

**Separation**
facade → high-level APIs or hooks (what components use)
transport → raw fetch / Kafka / REST / event adapters
store → state handling (in-memory, cache, etc.)
control → orchestration / mode switching / refresh logic

**Key Ideas**
- UI never calls `fetch` directly or care where data comes from.
- Facade decides which transport to use based on **mode**: `"auto" | "remote" | "embedded"`.
- Image rendering uses a single component `<ImageFacaade>` for thumbnails and full view.

---

## Mode Handling

- Mode is stored in `localStorage["vn_store_mode"]`.
- Valid values: `"auto" | "remote" | "embedded"`.
- Use `DataFacade.setMode("embedded")` to force offline; `DataFacade.getMode()` to read.

**Behavior**
- `embedded` → always use EmbeddedTransport. No network calls (images & records).
- `remote` → always use RemoteTransport.
- `auto`    → try Remote first; if it fails, fallback to Embedded.

**React Query Keys**
Include mode in the query key so lists refetch when the user switches modes:
```ts
queryKey: ["records", page, size, search, sort, dir, DataFacade.getMode()]
```

---

## Data Surface (what UI should call)

From `DataFacade`:
- `list({ page, size, query?, sort?, dir? })`
- `create(values)`
- `update(id, patch)`
- `delete(id)`
- `getByKey(agentId, clientId)`
- `updateByKey(agentId, clientId, patch)`
- `deleteByKey(agentId, clientId)`
- `uploadImages(recordId, files)`
- `imageUrl(imageId) → string` (returns `/api/images/...` in remote, or `blob:/data:` in embedded)

UI components use `<ImageFacade imageId="..."/>` to render images without ever hard-coding `/api/images`.

---

## Transports

### RemoteTransport
- Is the **only** layer that uses `fetch`.
- For strict embedded guarantee, guard remote calls in embedded mode (optional):

```ts
function assertNotEmbedded() {
  if (localStorage.getItem("vn_store_mode") === "embedded") {
    throw new Error("Remote transport disabled in embedded mode");
  }
}
```

### EmbeddedTransport
- Records are stored as JSON array in `localStorage["vn_embedded_records"]`.
- Images are indexed in `localStorage["vn_embedded_images"]`:
  - `opfs:<index-file-N>` if OPFS is available (preferred).
  - `data:<mime>;base64,...` fallback if OPFS unsupported.

Sorting & pagination happen **locally**:
- List sorts by the requested field (`createdAt` default), then slices for pagination.

---

## Images

### `<ImageFacade>`
- Resolves `imageId` via `DataFacade.imageUrl(imageId)`.
- In embedded mode, returns `blob:` or `data:` URLs (never hits the network).
- In remote mode, returns `/api/images/{imageId}`.
- Revokes blob URLs on unmount to avoid memory leaks.

**Never** use raw `<img src="/api/images/...">` in UI. Always `<FacadeImage imageId="..."/>`.

---

## Adding a New Feature

### 1) Define types (if needed)
- Extend `src/data/types/types.ts` if the shape changes.

### 2) Extend `Transport` interface
- Add a new method signature to `src/data/transports/Transport.ts`.

### 3) Implement in both transports
- Implement the method in `RemoteTransport` (HTTP calls) and `EmbeddedTransport` (local logic).

### 4) Expose via `DataFacade`
- Add a method that dispatches to the current transport based on mode.

### 5) Use in UI
- Call only the Facade from components (never call transports directly).

---

## Conventions

- **No global fetch hooks**. If you need headers/auth/retry, implement helpers inside `RemoteTransport`.
- **React Query**:
  - Keys must include any parameters that change the data (page, size, filters, mode).
  - After mutating, `invalidateQueries({ queryKey: ["records"] })` to refresh lists.
- **Blob lifecycle**:
  - `<ImageFacade>` handles revocation on unmount. If you cache `imageUrl` strings, remember to revoke old blobs yourself.

---

## Troubleshooting

- **Embedded mode still hits network**: ensure there are no raw `/api/` URLs left; add `assertNotEmbedded()` guards in `RemoteTransport`.
- **Thumbnails blank but full-view OK**: ensure thumbnails use `<ImageFacade>`, not a separate thumb component.
- **Mode never changes**: make sure you read/write via `DataFacade.getMode()/setMode()`; remove `window.__VN_DATA__` remnants.

---

## Testing Checklist

- Toggle mode to `embedded` → create record → list updates without network requests.
- Upload images in embedded → thumbnails + full-view render (OPFS `blob:` URLs).
- Switch to `remote` → sorting/paging is server-driven (`/api/records?...sort=...`).
- Switch back to `auto` and simulate server down → automatic fallback to embedded without errors.

---

## Why this Design

- **Separation of concerns**: UI is transport-agnostic.
- **Offline-first**: Embedded transport provides real functionality when server is unavailable.
- **Single source of truth**: Facade centralizes decisions & mode.
- **Easy to extend**: Adding a new endpoint is a 3-step pattern (Transport → Facade → UI).
