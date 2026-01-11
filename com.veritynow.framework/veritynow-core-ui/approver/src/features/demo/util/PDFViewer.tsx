// PDFViewer.tsx
import * as React from "react";
import { Button, Stack, Typography, CircularProgress } from "@mui/material";
import { DataFacade } from "@/core/facade/DataFacade";
import { PDF } from "@/features/demo/util/PDF"; // your PDF.tsx module
import { useZoomPan } from "@/features/demo/util/useZoomPan";
import { RestorePageOutlined, ZoomIn, ZoomOut } from "@mui/icons-material";


 

// If you pass it like <PDFViewer m={media} />
export function PDFViewer({ m }: { m: MediaResource }) {
  const { scale, zoomIn, zoomOut, resetZoom } = useZoom(1.0);
  // If you pass it like <PDFViewer {...media} />, use: export function PDFViewer(m: MediaResource) {

  const [pages, setPages] = React.useState<string[]>([]);
  const [pageNumber, setPageNumber] = React.useState(0);
  const [loading, setLoading] = React.useState(false);
  const [err, setErr] = React.useState<string | null>(null);
  const prevUrlRef = React.useRef<string>("");

  // 1) stable refresh callback (created once)
  const refresh = React.useCallback(() => {
    console.log("GOT called");
    // light nudge; you can do a more meaningful “progress” update here if you like
    // setPages(p => p.slice());
    // Helpful debug:
    // console.debug("PDFViewer.refresh()");
  }, []);

 

  React.useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();

    (async () => {
      setLoading(true);
      setErr(null);

      try {
        // 2) resolve a usable URL (prefer cached)
        let url = DataFacade.touchAndGetObjectUrl?.(m.id);
        if (!url) url = await DataFacade.imageUrl(m.id);
        if (!url) throw new Error("No URL available for PDF");

        const prevUrl = prevUrlRef.current;
        if (prevUrl !== url) {
          prevUrlRef.current = url;
          setPages([]);
          setPageNumber(0);
        }

        // 3) stream pages; pass stable refresh
        if (m.kind === MediaKind.pdf) {
          for await (const { index, src, total } of PDF.getPagesAsImageStream(url, {
            signal: controller.signal,
            refresh, // <-- will not be undefined now
          })) {
            if (cancelled) return;

            setPages(prev => {
              if (prev.length !== total) {
                const next = Array.from({ length: total }, (_, i) => prev[i] ?? "");
                next[index] = src;
                return next;
              }
              const next = prev.slice();
              next[index] = src;
              return next;
            });
          }
        } else {
          // non-PDF fallback
          if (!cancelled) setPages([m.url]);
        }
      } catch (e) {
        if (!cancelled) setErr("Error in useEffect :" + e);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
      controller.abort();
      // If you’re using lease-based cache:
      DataFacade.releaseObjectUrl?.(m.id);
      // If not using leases, and you want hard cleanup:
      // DataFacade.revokeObjectUrl?.(m.id);
    };
  }, [m.id, m.kind, m.url, refresh]);




  if (loading && pages.length === 0) {
    return <p>Loading, please wait… <CircularProgress /></p>;
  }
  if (err) {
    return <p style={{ color: "red" }}>{err}</p>;
  }

  if (m.kind === MediaKind.pdf) {
    const src = pages[pageNumber];
    return src ? (
      <div>
        <Stack direction="row" sx={{ justifyContent: "left", gap: 1, mt: 1, flex:"right" }}>
          <Button variant="outlined" onClick={() => setPageNumber(n => Math.max(0, n - 1))} disabled={pageNumber <= 0}>Previous</Button>
          <Button variant="outlined" onClick={() => setPageNumber(n => Math.min(pages.length - 1, n + 1))} disabled={pageNumber >= pages.length - 1}>Next</Button>
          <Typography variant="h6">{pages.length > 0 ? `${pageNumber + 1} / ${pages.length}` : ""}</Typography>
            <Button  variant="outlined" startIcon={<ZoomOut/>}  onClick={zoomOut}>-</Button>
          <Button  variant="outlined" startIcon={<RestorePageOutlined/>} onClick={resetZoom}>Reset</Button>
          <Button  variant="outlined" endIcon={<ZoomIn/>} onClick={zoomIn}>+</Button>

        </Stack >
        <div>
          <img key={src} src={src} alt={`Page ${pageNumber + 1}`} style={{
            transform: `scale(${scale})`,
            transformOrigin: "center center",
            transition: "transform 0.2s ease",
          }} />
        </div>
      </div>
    ) : (
      <CircularProgress />
    );
  }

  return pages[0] ? <img key={pages[0]} src={pages[0]} alt="media" /> : <CircularProgress />;
}
