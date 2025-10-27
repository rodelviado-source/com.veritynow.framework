// OPFS (Origin Private File System) utility with strict typings.
// Works in Chrome/Edge in a secure context (https or http://localhost).

declare global {
  interface StorageManager {
    getDirectory?: () => Promise<FileSystemDirectoryHandle>;
  }
}

function hasOPFS(): boolean {
  return typeof navigator !== "undefined" &&
         typeof navigator.storage !== "undefined" &&
         typeof navigator.storage.getDirectory === "function";
}

async function getRoot(): Promise<FileSystemDirectoryHandle> {
  if (!hasOPFS() || !navigator.storage.getDirectory) {
    throw new Error("OPFS not supported in this browser/context");
  }
  return navigator.storage.getDirectory();
}

export const IndexedFileStore = {
  isSupported(): boolean {
    return hasOPFS();
  },

  async write(name: string, blob: Blob): Promise<void> {
    const root = await getRoot();
    const handle = await root.getFileHandle(name, { create: true });
    const writable = await handle.createWritable();
    try {
      await writable.write(blob);
    } finally {
      await writable.close();
    }
  },

  async readFile(name: string): Promise<File> {
    const root = await getRoot();
    const handle = await root.getFileHandle(name, { create: false });
    return handle.getFile();
  },

  async toObjectUrl(name: string): Promise<string> {
    const file = await this.readFile(name);
    return URL.createObjectURL(file);
  },

  async exists(name: string): Promise<boolean> {
    try {
      const root = await getRoot();
      await root.getFileHandle(name, { create: false });
      return true;
    } catch {
      return false;
    }
  },

  async remove(name: string): Promise<void> {
    const root = await getRoot();
    try {
      await root.removeEntry(name);
    } catch {
      // ignore
    }
  },
};
