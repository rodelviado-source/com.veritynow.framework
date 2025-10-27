// OPFS (Origin Private File System) utility.
// Stores and retrieves blobs by a simple "index-file-N" name.
// Works in Chrome/Edge (secure context: https or http://localhost).

export const IndexedFileStore = {
  isSupported(): boolean {
    return typeof navigator !== "undefined" && !!(navigator).storage?.getDirectory;
  },

  async write(name: string, blob: Blob): Promise<void> {
    const root = await (navigator).storage.getDirectory();
    const handle = await root.getFileHandle(name, { create: true });
    const writable = await handle.createWritable();
    try { await writable.write(blob); } finally { await writable.close(); }
  },

  async readFile(name: string): Promise<File> {
    const root = await (navigator).storage.getDirectory();
    const handle = await root.getFileHandle(name, { create: false });
    return handle.getFile();
  },

  async toObjectUrl(name: string): Promise<string> {
    const file = await this.readFile(name);
    return URL.createObjectURL(file);
  },

  async exists(name: string): Promise<boolean> {
    try {
      const root = await (navigator).storage.getDirectory();
      await root.getFileHandle(name, { create: false });
      return true;
    } catch {
      return false;
    }
  },

  async remove(name: string): Promise<void> {
    const root = await (navigator).storage.getDirectory();
    try {
      await root.removeEntry(name);
    } catch { /* ignore */ }
  },
};
