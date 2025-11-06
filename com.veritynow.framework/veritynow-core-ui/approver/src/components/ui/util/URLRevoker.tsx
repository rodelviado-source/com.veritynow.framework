import React from "react";

// keep per-index pending revokes and a global unmount cleanup
const pendingRevokesRef = React.useRef<Map<number, string[]>>(new Map());

export function queueRevoke(index: number, url: string) {
  if (!url || !url.startsWith("blob:")) return;
  const m = pendingRevokesRef.current;
  const arr = m.get(index) ?? [];
  arr.push(url);
  m.set(index, arr);
}

export function revokeAllFor(index: number, keep: string) {
  const arr = pendingRevokesRef.current.get(index) ?? [];
  for (const u of arr) {
    if (u && u !== keep && u.startsWith("blob:")) {
      try { URL.revokeObjectURL(u); } catch {}
    }
  }
  pendingRevokesRef.current.delete(index);
}