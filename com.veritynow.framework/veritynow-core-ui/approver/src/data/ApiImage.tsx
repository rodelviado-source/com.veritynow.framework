import * as React from "react";
import { IndexedFileStore } from "@/data/IndexedFileStore";

type Mode = "auto" | "remote" | "embedded";

function getMode(): Mode {
  return (window).__VN_DATA__?.getMode?.() ?? "auto";
}
function loadImgs(): Record<string, string> {
  try { return JSON.parse(localStorage.getItem("vn_embedded_images") || "{}"); }
  catch { return {}; }
}
function apiBase(): string {
  return import.meta.env.VITE_API_BASE ?? "";
}

type Props = React.ImgHTMLAttributes<HTMLImageElement> & {
  imageId: string;
};

// Shows nothing until src is ready; you can pass className/alt/etc.
export function ApiImage({ imageId, ...rest }: Props) {
  const [src, setSrc] = React.useState<string>("");

  React.useEffect(() => {
    let revoked: string | null = null;
    let abort = false;

    async function go() {
      const mode = getMode();
      if (mode === "embedded") {
        const mapVal = loadImgs()[imageId];
        if (mapVal?.startsWith("opfs:") && IndexedFileStore.isSupported()) {
          const name = mapVal.slice("opfs:".length);
          if (await IndexedFileStore.exists(name)) {
            const url = await IndexedFileStore.toObjectUrl(name);
            if (!abort) { setSrc(url); revoked = url; }
            return;
          }
        }
        // fallback to stored data URL (older records)
        if (mapVal?.startsWith("data:")) {
          if (!abort) setSrc(mapVal);
          return;
        }
        // if no local copy, fall back to network URL
        if (!abort) setSrc(`${apiBase()}/api/images/${imageId}`);
        return;
      }
      // remote/auto -> normal API URL
      if (!abort) setSrc(`${apiBase()}/api/images/${imageId}`);
    }

    go();
    return () => {
      abort = true;
      if (revoked) URL.revokeObjectURL(revoked);
    };
  }, [imageId]);

  if (!src) return null;
  return <img src={src} {...rest} />;
}
