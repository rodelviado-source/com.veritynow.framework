// src/data/DataModeSwitch.tsx
import * as React from "react";
import {DataFacade} from "@/data/core/DataFacade";

export function DataModeSwitch() {
  const [mode, setMode] = React.useState(DataFacade.getMode());
  
  const cycle = () => {
    const next = mode === "auto" ? "remote" : mode === "remote" ? "embedded" : "auto";
    DataFacade.setMode(next);
    setMode(next);
  };
    
  return (
    <button
      type="button"
      className="border rounded-2xl px-3 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-800"
      onClick={cycle}
      title="Switch between Auto / Remote / Embedded data modes"
    >
      Store: {mode}
    </button>
  );
}
