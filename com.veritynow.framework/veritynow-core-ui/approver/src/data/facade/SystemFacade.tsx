import { System   } from "@/data/store/LocalStorageWithOPFS";

const clearStorage = System.clearStorage;
const storageEstimate = System.storageEstimate;

export const SystemFacade = {
 clearStorage,
 storageEstimate,
}

export default SystemFacade;