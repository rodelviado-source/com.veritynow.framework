import { MediaKind, MediaResource } from "@/data/transports/Transport";
import React, { useEffect, useImperativeHandle, useRef, useState, forwardRef } from "react";
import PDF from "@/components/ui/util/PDF";




export type CleanupHandle = { cleanup: () => void };
type PDFViewerProps = { m: MediaResource };

// tiny helper: only revoke blob: URLs
const revokeIfBlob = (u: string) => {
  if (typeof u === "string" && u.startsWith("blob:")) {
    try { URL.revokeObjectURL(u); } catch {/* ignore */}
  }
};

export const PDFViewer = forwardRef<CleanupHandle, PDFViewerProps>(({ m }, ref) => {
  const [pages, setPages] = useState<string[]>([]);
  const [pageNumber, setPageNumber] = useState(0);

  // keep latest pages for imperative cleanup
  const pagesRef = useRef<string[]>([]);
  pagesRef.current = pages;

  useImperativeHandle(ref, () => ({
    cleanup() {
      pagesRef.current.forEach(revokeIfBlob);
      setPages([]);
      setPageNumber(0);
      console.log("PDFViwer cleanup called");
    },
  }));

  PDFViewer.displayName = "PDFViewer"; 

  useEffect(() => {
    let cancelled = false;

     // optional: ignore if nothing changed
    if (!m?.url) return;
  

    // load current media
    (async () => {
        // clean up previous media when m changes
        const prev = pagesRef.current.slice();
        if (prev.length) prev.forEach(revokeIfBlob);

      if (m.kind === MediaKind.pdf) {
        const p = await PDF.getPagesAsImage(m.url); // should return data- or blob-URLs
        if (!cancelled) {
          setPages(p);
          setPageNumber(0);
        }
      } else {
        // non-pdf: single page/image
        if (!cancelled) {
          setPages([m.url]);
          setPageNumber(0);
        }
      }
    })();

    // on unmount or when m changes again
    return () => {
      cancelled = true;
      
    };
   
  }, [m.url, m.kind]); // m is the only dependency

  if (m.kind === MediaKind.pdf) {
    const src = pages[pageNumber];
    return src ? (
      <div>
        <img src={src} alt={`Page ${pageNumber + 1}`} />
        <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
          <button
            onClick={() => setPageNumber((n) => Math.max(0, n - 1))}
            disabled={pageNumber <= 0}
          >
            Previous
          </button>
          <button
            onClick={() => setPageNumber((n) => Math.min(pages.length - 1, n + 1))}
            disabled={pageNumber >= pages.length - 1}
          >
            Next
          </button>
        </div>
      </div>
    ) : (
      <p>Loading PDF, please wait…</p>
    );
  }

  // Non-PDF: render directly (no state mutation in render)
  return <img src={pages[0]} alt="media" />;
});
