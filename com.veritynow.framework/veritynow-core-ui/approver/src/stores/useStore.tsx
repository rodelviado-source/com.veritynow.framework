import * as React from "react";
import { EmbeddedStore } from "./embedded";
import { RemoteStore } from "./remote";
import { Store, STORAGE_KEY } from "./types";

export function useStore() {
	// src/stores/useStore.tsx
	const [mode, setMode] = React.useState<"remote"|"embedded">(
	  (localStorage.getItem("vn_store_mode") as any) || "embedded" // default to embedded
	);

  const store: Store = mode === "embedded" ? EmbeddedStore : RemoteStore;

  function toggle() {
    const next = mode === "embedded" ? "remote" : "embedded";
    setMode(next);
    localStorage.setItem(STORAGE_KEY, next);
  }

  return { store, mode, toggle };
}
