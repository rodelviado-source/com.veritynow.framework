import type {  RecordItem } from "./types";

import { RemoteTransport } from "./transports/RemoteTransport";
import { EmbeddedTransport } from "./transports/EmbeddedTransport";

export type Mode = "auto" | "remote" | "embedded";
const MODE_KEY = "vn_store_mode";

function getMode(): Mode {
  return (localStorage.getItem(MODE_KEY) as Mode) ?? "auto";
}
function setMode(m: Mode) {
  localStorage.setItem(MODE_KEY, m);
}

const remote = new RemoteTransport();
const embedded = new EmbeddedTransport();

export const DataFacade = {
  getMode, setMode,

  async list(p) {
	const m = getMode();
	console.log("MODE=========>", m);
	if (m === "embedded") return embedded.list(p);
	if (m === "remote")   return remote.list(p);
	try { return await remote.list(p); } catch { return embedded.list(p); }
  },

  async create(payload: Partial<RecordItem>): Promise<RecordItem> {
    const m = getMode();
	console.log("MODE=========>", m);
    if (m === "embedded")  return embedded.create(payload);
    if (m === "remote")    return remote.create(payload);
    try { return await remote.create(payload); } catch { return embedded.create(payload); }
  },

  async update(id: number, patch: Partial<RecordItem>): Promise<RecordItem> {
    const m = getMode();
	console.log("MODE=========>", m);
    if (m === "embedded") return embedded.update(id, patch);
	if (m === "remote")   return remote.update(id, patch);
    try { return await remote.update(id, patch); }
    catch { return embedded.update(id, patch); }
  },

  async uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }> {
    const m = getMode();
	console.log("MODE=========>", m);
    if (m === "embedded") return embedded.uploadImages(id, files);
	if (m === "remote")  return remote.uploadImages(id, files);
    try { return await remote.uploadImages(id, files); }
    catch { return embedded.uploadImages(id, files); }
  },

  async imageUrl(imageId: string): Promise<string> {
    const m = getMode();
	console.log("MODE=========>", m);
    if (m === "embedded") return embedded.imageUrl(imageId);
    if (m === "remote") return remote.imageUrl(imageId);
    const local = await embedded.imageUrl(imageId);
    if (local) return local;
    return remote.imageUrl(imageId);
  },
};
