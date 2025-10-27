// src/data/DataModeSwitch.tsx
import * as React from "react";

// Define a global type for the helper installed by bootstrapDataLayer
declare global {
  interface Window {
    __VN_DATA__?: {
      getMode: () => string;
      setMode: (mode: string) => void;
    };
  }
}

export function DataModeSwitch() {
  const [mode, setMode] = React.useState<string>(
    () => window.__VN_DATA__?.getMode?.() ?? "auto"
  );

  const cycle = React.useCallback(() => {
    const next =
      mode === "auto" ? "remote" : mode === "remote" ? "embedded" : "auto";
    window.__VN_DATA__?.setMode(next);
    setMode(next);
  }, [mode]);

  const label =
    mode === "auto" ? "Auto" : mode === "remote" ? "Remote" : "Embedded";

  return (
    <button
      type="button"
      className="border rounded-2xl px-3 py-2 text-m hover:bg-gray-100 dark:hover:bg-gray-800"
      onClick={cycle}
      title="Switch between Auto / Remote / Embedded data modes"
    >
      Store: {label}
    </button>
  );
}
