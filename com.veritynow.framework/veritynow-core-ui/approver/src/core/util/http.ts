// src/core/util/http.ts (or wherever you keep small utils)
export function toQuery(params?: Record<string, unknown>): string {
  if (!params) return "";
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v === undefined || v === null) continue;
    if (Array.isArray(v)) {
      for (const item of v) q.append(k, String(item));
    } else if (v instanceof Date) {
      q.set(k, v.toISOString());
    } else {
      q.set(k, String(v));
    }
  }
  return q.toString();
}
