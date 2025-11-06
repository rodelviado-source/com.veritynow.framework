import type {  RecordItem } from "@/data/types/Record";

import { RemoteTransport } from "@/data/transports/RemoteTransport";
import { EmbeddedTransport } from "@/data/transports/EmbeddedTransport";
import { ListParams, MediaResource, ModeTypes } from "../transports/Transport";



const MODE_KEY = "vn_store_mode";

function getMode(): ModeTypes {
  return (localStorage.getItem(MODE_KEY)) as ModeTypes ?? ModeTypes.embedded;
}
function setMode(m: ModeTypes) {
  localStorage.setItem(MODE_KEY, m);
}

const remote = new RemoteTransport();
const embedded = new EmbeddedTransport();

export const DataFacade = {
  getMode, setMode,

  async list(p: ListParams) {
	const m = getMode();
	if (m === "embedded") return embedded.list(p);
	if (m === "remote")   return remote.list(p);
	try { return await remote.list(p); } catch { return embedded.list(p); }
  },

  async create(payload: Partial<RecordItem>): Promise<RecordItem> {
    const m = getMode();
	  if (m === "embedded")  return embedded.create(payload);
    if (m === "remote")    return remote.create(payload);
    try { return await remote.create(payload); } catch { return embedded.create(payload); }
  },

  async update(id: number, patch: Partial<RecordItem>): Promise<RecordItem> {
    const m = getMode();
	  if (m === "embedded") return embedded.update(id, patch);
	if (m === "remote")   return remote.update(id, patch);
    try { return await remote.update(id, patch); }
    catch { return embedded.update(id, patch); }
  },

  async uploadImages(id: number, files: File[]): Promise<{ imageIds: string[] }> {
    const m = getMode();
	if (m === "embedded") return embedded.uploadImages(id, files);
	if (m === "remote")  return remote.uploadImages(id, files);
    try { return await remote.uploadImages(id, files); }
    catch { return embedded.uploadImages(id, files); }
  },

  async imageUrl(imageId: string): Promise<string> {
    const m = getMode();
	   if (m === "embedded") return embedded.imageUrl(imageId);
    if (m === "remote") return remote.imageUrl(imageId);
    const local = await embedded.imageUrl(imageId);
    if (local) return local;
    return remote.imageUrl(imageId);
  },

async  mediaFor(imageId: string): Promise<MediaResource> {

  const m = getMode();
	  if (m === ModeTypes.embedded) return embedded.mediaFor(imageId);
    if (m === ModeTypes.remote) return remote.mediaFor(imageId);
    const local = await embedded.mediaFor(imageId);
    if (local) return local;
    return remote.mediaFor(imageId);
},
  
};
