import * as React from "react";
import { useMedia } from "@/data/hooks/useMedia";
import { MediaKind, MediaResource } from "@/data/transports/Transport";
import { PDF } from "@/components/ui/util/PDF";
import { getThumb, setThumb } from "@/data/thumbStore";


type Props = React.HTMLAttributes<HTMLElement> & {
  id: string;                 // real media id
  cacheKey?: string;          // optional namespaced key, e.g. `${mode}:${id}`
  meta?: MediaResource | null;
};

export default function ImageFacade({ id, cacheKey, meta, className, ...rest }: Props) {
  const cachedMeta = useMedia(id, true);
  const m = meta ?? cachedMeta;
  const k = cacheKey ?? id;

  const [thumb, setThumbLocal] = React.useState<string | null>(() => (m ? getThumb(k) ?? null : null));
  const revokeRef = React.useRef<string | null>(null);

  React.useEffect(() => {
    if (!m) { setThumbLocal(null); return; }
    let cancelled = false;

    async function load() {
      const fromCache = getThumb(k);
      if (fromCache) { setThumbLocal(fromCache); return; }

      if (m?.kind === MediaKind.pdf) {
        const url = await PDF.thumbnail(m.url);
        if (cancelled) {/* URL.revokeObjectURL(url); */return; }
        setThumb(k, url);
        revokeRef.current = url;
        setThumbLocal(url);
        return;
      }

      if (m?.kind === MediaKind.image) {
        setThumb(k, m.url);        // unrestricted remote or embedded — direct URL is fine
        setThumbLocal(m.url);
        return;
      }

      setThumbLocal(null);
    }

    load().catch((e) => { console.error("ImageFacade load error:", e); setThumbLocal(null); });

    return () => {
      cancelled = true;
      if (revokeRef.current) {/* URL.revokeObjectURL(revokeRef.current); */revokeRef.current = null; }
    };
  }, [k, m?.id, m?.kind, m?.url]);

  // skeleton while meta/thumb not ready
  if (!m || (m.kind === MediaKind.pdf && !thumb)) {
    return <div className={`bg-gray-100 animate-pulse ${className ?? ""}`} aria-label="Loading thumbnail" {...rest} />;
  }

  if (m.kind === MediaKind.pdf)   return <img src={thumb!} alt={m.filename ?? "PDF"} className={className} {...rest} />;
  if (m.kind === MediaKind.image) return <img src={thumb ?? m.url} alt={m.filename ?? "Image"} className={className} {...rest} />;
  if (m.kind === MediaKind.video) return <video src={m.url} className={className} controls {...rest} />;

  return <a href={m.url} download className={`underline ${className ?? ""}`} {...rest}>{m.filename ?? "Download"}</a>;
}
