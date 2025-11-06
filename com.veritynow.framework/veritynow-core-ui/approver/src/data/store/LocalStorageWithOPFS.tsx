
const KEYS = {
  RECS: "vn_embedded_records",
  IMGS: "vn_embedded_images",
} as const;


// OPFS (Origin Private File System) utility with strict typings.
// Works in Chrome/Edge in a secure context (https or http://localhost).

export const System = {
    async clearOPFS(): Promise<void> {
    if (!LocalStorageWithOPFS.hasOPFS()) return;
      const root = await LocalStorageWithOPFS.getRoot();
      

    await rmAll(root);

    // Recursively delete all entries under the root
    async function rmAll(dir: FileSystemDirectoryHandle): Promise<void> {
      
      // Iterate entries: [name, handle]
      // We rely on `name` to call parent.removeEntry(name)
      for await (const [name, handle] of Object.entries(dir) as unknown as AsyncIterable<[string, FileSystemHandle]>) {
        
        if (handle.kind === 'file') {
          try {
            await dir.removeEntry(name);
          } catch {
            // Some browsers might still hold the file; ignore errors
          }
        } else {
          // Directory: clear children first, then remove the directory
          await rmAll(handle as FileSystemDirectoryHandle);
          try {
            await dir.removeEntry(name, { recursive: true });
          } catch {
            // Fallback if recursive isn't supported: best effort
            try { await (handle as FileSystemDirectoryHandle).removeEntry?.('dummy'); } catch { /* ignore */ }
            try { await dir.removeEntry(name); } catch {/* ignore */ }
          }
        }
      }
    }
  },

  async  clearLocalStorage(): Promise<void> {
  try {
    localStorage.clear();
  } catch {
    /* ignore */
  }
},

async  clearStorage(): Promise < void> {
  try {
    
    await System.clearOPFS();
    console.log("<====== OPFS Cleared ======> ");
    await System.clearLocalStorage();
    console.log("<====== LocalStorage Cleared ======> ");
  } catch(e) {
    console.log("ERROR Clearing storage ======> ",e);
       /* ignore */
  }
},

async  storageEstimate(): Promise < { usage: number; quota: number } | null > {
try {
  const e = await navigator.storage.estimate();
  return { usage: e.usage ?? 0, quota: e.quota ?? 0 };
} catch {
  return null;
}
},


}


export const LocalStorageWithOPFS = {

  hasOPFS(): boolean {
    return typeof navigator !== "undefined" &&
      typeof navigator.storage !== "undefined" &&
      typeof navigator.storage.getDirectory === "function";
  },

  //=========== OPFS ====================  
  async getRoot(): Promise<FileSystemDirectoryHandle> {
    if (!LocalStorageWithOPFS.hasOPFS()) {
      throw new Error("OPFS not supported in this browser/context");
    }
    return navigator.storage.getDirectory();
  },

  async write(name: string, file: File): Promise<void> {
    const root = await LocalStorageWithOPFS.getRoot();
    const handle = await root.getFileHandle(name, { create: true });
    const writable = await handle.createWritable();
    try {
      await writable.write(file);
    } finally {
      await writable.close();
    }
  },
async writeText(name: string, text: string): Promise<void> {
  const root = await LocalStorageWithOPFS.getRoot();
  const handle = await root.getFileHandle(name, { create: true });
  const writable = await handle.createWritable();
  try {
    await writable.write(new Blob([text], { type: "application/json" }));
  } finally {
    await writable.close();
  }
},

async writeJson(name: string, data: unknown): Promise<void> {
  await LocalStorageWithOPFS.writeText(name, JSON.stringify(data));
},
  async readFile(name: string): Promise<File> {
    const root = await LocalStorageWithOPFS.getRoot();
    const handle = await root.getFileHandle(name, { create: false });
    return handle.getFile();
  },

  async toObjectUrl(name: string): Promise<string> {
    const file = await LocalStorageWithOPFS.readFile(name);
    return URL.createObjectURL(file);
  },

  async exists(name: string): Promise<boolean> {
    try {
      const root = await LocalStorageWithOPFS.getRoot();
      await root.getFileHandle(name, { create: false });
      return true;
    } catch {
      return false;
    }
  },

  async remove(name: string): Promise<void> {
    const root = await LocalStorageWithOPFS.getRoot();
    try {
      await root.removeEntry(name);
    } catch {
      // ignore
    }
  },


  //================= LocalStorage ======================

  loadRecs<R>(): R[] {
    try { return JSON.parse(localStorage.getItem(KEYS.RECS) || "[]") as R[]; } catch { return []; }
  },
  saveRecs<R>(v: R[]) { localStorage.setItem(KEYS.RECS, JSON.stringify(v)); },

  loadImgs(): Record<string, string> {
    try { return JSON.parse(localStorage.getItem(KEYS.IMGS) || "{}") as Record<string, string>; } catch { return {}; }
  },
  saveImgs(v: Record<string, string>) { localStorage.setItem(KEYS.IMGS, JSON.stringify(v)); },

 

        nowISO() { return new Date().toISOString(); },
        uid() { return Math.random().toString(36).slice(2) + Date.now().toString(36); },
};






