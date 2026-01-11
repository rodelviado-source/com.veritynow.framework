import { useEffect, useMemo, useState, useCallback } from "react";
import { DataFacade, setMode as dfSetMode, getMode as dfGetMode } from "../facade/DataFacade";
import { ModeTypes } from "@/core/transport/types";

/**
 * Initializes the VerityNow data layer and exposes the facade, mode, and setters.
 * No external dependencies. Works in any React + TS project.
 */
export function useVerityNowDataLayer() {
  const [ready, setReady] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [mode, setModeState] = useState<ModeTypes>(() => dfGetMode());

  const setMode = useCallback((m: ModeTypes) => {
    dfSetMode(m);
    setModeState(m);
  }, []);

  useEffect(() => {
    let mounted = true;
    // Simulate any async bootstrapping required in the future (e.g., OPFS warmup).
    (async () => {
      try {
        // Quick OPFS capability probe (does not throw in supported browsers)
        // If not supported, we keep facade functional; embedded will just no-op if OPFS is absent.
        if ("storage" in navigator && "getDirectory" in (navigator.storage as any)) {
          // Optionally touch the root directory to warm up OPFS
          // @ts-ignore
          await navigator.storage.getDirectory();
        }
        if (!mounted) return;
        setReady(true);
      } catch (e) {
        if (!mounted) return;
        setError(e);
      }
    })();
    return () => { mounted = false; };
  }, []);

  // stable facade reference for consumers
  const facade = useMemo(() => DataFacade, []);

  return { ready, error, mode, setMode, DataFacade: facade };
}
