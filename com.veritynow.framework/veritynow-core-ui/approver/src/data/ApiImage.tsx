import React from "react";
// src/data/ApiImage.tsx
const TRANSPARENT_PX =
  "data:image/gif;base64,R0lGODlhAQABAAAAACwAAAAAAQABAAA=";

export function ApiImage({ imageId, ...rest }: Props) {
  const [src, setSrc] = React.useState<string>("");

  React.useEffect(() => {
    let revoked: string | null = null;
    let cancelled = false;

    async function resolve() {
      const mode = getMode();
      if (mode === "embedded") {
        const mapping = loadImgs()[imageId];

        if (mapping?.startsWith("opfs:") && IndexedFileStore.isSupported()) {
          const name = mapping.slice("opfs:".length);
          if (await IndexedFileStore.exists(name)) {
            const url = await IndexedFileStore.toObjectUrl(name);
            if (!cancelled) { setSrc(url); revoked = url; }
            return;
          }
        }

        if (mapping?.startsWith("data:")) {
          if (!cancelled) setSrc(mapping);
          return;
        }

        // STRICT: embedded mode with no local mapping -> show placeholder, never network
        if (!cancelled) setSrc(TRANSPARENT_PX);
        return;
      }

      // remote/auto
      if (!cancelled) setSrc(`${apiBase()}/api/images/${imageId}`);
    }

    void resolve();
    return () => { cancelled = true; if (revoked) URL.revokeObjectURL(revoked); };
  }, [imageId]);

  if (!src) return null;
  return <img src={src} {...rest} />;
}
