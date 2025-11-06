import { MediaKind, MediaResource } from "@/data/transports/Transport";
import React, {
  useEffect,
  useState,
  
} from "react";
import PDF from "@/components/ui/util/PDF";
import { Button, CircularProgress, Stack, Typography } from "@mui/material";



export function PDFViewer( m:MediaResource ) {
  const [pages, setPages] = useState<string[]>([]);
  const [pageNumber, setPageNumber] = useState(0);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [prevUrl, setPrevUrl] = useState("");
  
  useEffect(() => {
    // cleanup from prior run happens in returned function below
    if (!m?.url) {
      pages.forEach((p) => { if (p.startsWith("blob:")) URL.revokeObjectURL(p); });
      setPages([]); setPageNumber(0); setErr(null); setLoading(false);
      return;
    }

    // URL changed → reset visible state
    if (prevUrl !== m.url) {
      pages.forEach((p) => { if (p.startsWith("blob:")) URL.revokeObjectURL(p); });
      setPages([]); setPageNumber(0); setErr(null); setLoading(true); // ← set true
      setPrevUrl(m.url);
    } else {
      setLoading(true); // new render of same URL (e.g., version bump)
    }

    const controller = new AbortController();
    let cancelled = false;

    (async () => {
      try {
        if (m.kind === MediaKind.pdf) {
          for await (const { index, src, total } of PDF.getPagesAsImageStream(m.url, { signal: controller.signal })) {
            if (cancelled) return;
            setPages((prev) => {
              if (prev.length !== total) {
                const next = Array.from({ length: total }, (_, i) => prev[i] ?? "");
                next[index] = src;
                return next;
              }
              const next = [...prev];
              next[index] = src;
              return next;
            });
          }
        } else {
          if (!cancelled) setPages([m.url]);
        }
      } catch (e) {
        if (!cancelled) setErr(e instanceof Error ? e.message : String(e));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
      controller.abort();
      pages.forEach((p) => { if (p.startsWith("blob:")) URL.revokeObjectURL(p); });
    };
  }, [m.kind, m.url]);// keep url/kind for safety, but version drives reloads
      
  if (loading && pages.length === 0) return <p>Loading, please wait…  <CircularProgress/>  </p>;
  if (err) return <p style={{ color: "red" }}>{err}</p>;

  if (m.kind === MediaKind.pdf) {

    const src = pages[pageNumber];
    
    return src ? (
      <div>
        
        <Stack direction="row" style={{ justifyContent:"left", display: "flex", gap: 8, marginTop: 8 }}>
          <Button
          variant="outlined"
            onClick={() => setPageNumber((n) => Math.max(0, n - 1))}
            disabled={pageNumber <= 0}
          >
            Previous
          </Button>
          <Button
            onClick={() => setPageNumber((n) => Math.min(pages.length - 1, n + 1))}
            disabled={pageNumber >= pages.length - 1}
            variant="outlined"
          >
            Next
          </Button>
           <Typography variant="h6"  justifyContent="center" sx={{ display: 'block' }}>
                 {pages.length > 0 ? `${pageNumber + 1} / ${pages.length}` : ""}
            </Typography>
          
        </Stack>
        <img key={src} src={src} alt={`Page ${pageNumber + 1}`} />
      </div>

    ) : (

      <CircularProgress/>
    );
  }

  // Non-PDF: render directly
  return pages[0] ? <img key={pages[0]} src={pages[0]} alt="media" /> : <CircularProgress/>
};

PDFViewer.displayName = "PDFViewer";
export default PDFViewer;
