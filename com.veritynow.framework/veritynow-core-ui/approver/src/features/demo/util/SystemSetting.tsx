import React from "react";
import SystemFacade from "@/core/facade/SystemFacade";
import { Button, Stack } from "@mui/material";

function fmt(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  const units = ["KB","MB","GB","TB"];
  let i = -1, v = bytes;
  do { v /= 1024; i++; } while (v >= 1024 && i < units.length-1);
  return `${v.toFixed(1)} ${units[i]}`;
}

export default function ClearStorageCard() {
  const [busy, setBusy] = React.useState(false);
  const [msg, setMsg] = React.useState<string>("");
  const [usage, setUsage] = React.useState<{usage:number; quota:number} | null>(null);

  
  
  const onClear = async () => {
    if (!confirm("Clear local data (localStorage + OPFS)? This cannot be undone.")) return;
    setBusy(true);
    setMsg("");
    try {
      await SystemFacade.clearStorage();
    
   
      setMsg("Local data cleared.");
    } catch (e) {
      setMsg("Failed to clear some local data (see console).");
      console.error(e);
    } finally {
      setBusy(false);
    }
  };

  const onClearInMemory = async () => {
    if (!confirm("Clear In memory meta cache, it is usually safe to do so but it might slow down image rendering until it builds up again. If you are experiencing memory starvation it might help clearing the in memory cache")) return;
    setBusy(true);
    setMsg("");
    try {
      
      setMsg("In-Memory Cache cleared.");
    } catch (e) {
      setMsg("Failed to clear some local data (see console).");
      console.error(e);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="rounded-2xl border p-4 shadow-sm space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Storage & Cache</h3>
        <Stack direction="column" spacing="10px">
        <Button
          variant="outlined"
          onClick={onClear}
          disabled={busy}
          className="px-3 py-1.5 rounded-xl border hover:bg-gray-50 disabled:opacity-50"
        >
          {busy ? "Clearing..." : "Clear local data"}
        </Button>

        <Button
         variant="outlined"
          onClick={onClearInMemory}
          disabled={busy}
          className="px-3 py-1.5 rounded-xl border hover:bg-gray-50 disabled:opacity-50"
        >
          {busy ? "Clearing..." : "Clear In-Memory Meta cache"}
        </Button>
        </Stack>
      </div>

      <p className="text-sm text-gray-600">
        Clears <code>localStorage</code> and Origin Private File System (OPFS) data for this app.
      </p>

      <div className="text-sm">
        {usage ? (
          <div className="flex gap-4">
            <span>Usage: <strong>{fmt(usage.usage)}</strong></span>
            <span>Quota: <strong>{fmt(usage.quota)}</strong></span>
          </div>
        ) : (
          <span className="text-gray-500">Storage estimate unavailable.</span>
        )}
      </div>

      {msg && <div className="text-sm text-emerald-700">{msg}</div>}
    </div>
  );
}
