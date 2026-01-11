

export type AssetRecord = {
  id: string;           // globally unique asset id
  ownerId: string;      // globally unique owner id
  asset: File;          // FILE ONLY (no Blob for now)
  assetSummary: AssetSummary;
};

export type AssetSummary = {
  id: string;                 // global asset id
  ownerId: string;            // global owner id
  filename: string;
  contentType: string;
  size: number;
  kind?: string;
  checksum?: string | null;
  createdAt?: string;
};


