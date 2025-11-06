import { pdfjs } from "react-pdf";
import worker from "pdfjs-dist/build/pdf.worker.min.mjs?url";
import dummy from "pdfjs-dist/build/pdf.worker.min.mjs?url";

if (!dummy) console.log("GOT NULL");
pdfjs.GlobalWorkerOptions.workerSrc = worker.toString();

async function ensurePdfJs() {
  pdfjs.GlobalWorkerOptions.workerSrc = worker.toString();
  return pdfjs;
}

export type PageChunk = {
  index: number;        // 0-based page index
  src: string;          // data: URL (PNG)
  total: number;        // total pages
};

export type GetPagesStreamOptions = {
  /** Abortable / cooperative cancellation */
  signal?: AbortSignal;
  /** Render scale; 1.0 = native size (defaults to 1) */
  scale?: number;
  /** 1-based inclusive range (optional) */
  startPage?: number;
  /** 1-based inclusive range (optional) */
  endPage?: number;

  refresh?:() => void;
};

/**
 * Async generator that yields each page as an image as soon as it’s rendered.
 * Usage:
 *   for await (const { index, src, total } of PDF.getPagesAsImageStream(url, {signal})) { ... }
 */
async function* getPagesAsImageStream(
  pdfUrl: string,
  opts: GetPagesStreamOptions = {},

): AsyncGenerator<PageChunk, void, unknown> {
  const pdfjs = await ensurePdfJs();
  const { signal, scale = 1.0, startPage, endPage } = opts;

  // NOTE: using { url } form keeps parity with pdfjs recommendations
  const loadingTask = pdfjs.getDocument({ url: pdfUrl });
  const pdf = await loadingTask.promise;
  const total = pdf.numPages;

  const first = Math.max(1, startPage ?? 1);
  const last = Math.min(total, endPage ?? total);

  try {
    for (let i = first; i <= last; i++) {
      if (signal?.aborted) throw new DOMException("Aborted", "AbortError");

      const page = await pdf.getPage(i);
      const viewport = page.getViewport({ scale });

      const canvas = document.createElement("canvas");
      const canvasContext = canvas.getContext("2d")!;
      canvas.width = Math.ceil(viewport.width);
      canvas.height = Math.ceil(viewport.height);

      await page.render({ canvasContext, canvas, viewport }).promise;

      const src = canvas.toDataURL("image/png"); // PNG ignores quality param

      // clean up page resources immediately after producing image
      try {
        page.cleanup?.();
         
      } catch {
        /* ignore */
      }
      opts.refresh?.();
      yield { index: i - 1, src, total };
      
    }
  } finally {
    // finalize pdf / worker resources
    try {
      pdf.cleanup?.();
    } catch {
      /* ignore */
    }
    try {
      loadingTask.destroy?.();
    } catch {
      /* ignore */
    }

    
  }
}

export const PDF = {
  /**
   * Collects all pages (non-streaming). Uses the streaming generator internally.
   * Optional single-page support via pageNumber (1-based; 0 or omitted = all pages).
   */
  async getPagesAsImage(
    pdfUrl: string,
    pageNumber: number = 0,
    scale: number = 1.0
  ): Promise<string[]> {
    const opts: GetPagesStreamOptions =
      pageNumber > 0
        ? { scale, startPage: pageNumber, endPage: pageNumber }
        : { scale };

    const chunks: string[] = [];
    for await (const { index, src, total } of getPagesAsImageStream(pdfUrl, opts)) {
      // allocate once for stable length (helps pagers)
      if (chunks.length !== total) {
        chunks.length = total;
      }
      chunks[index] = src;
    }
    // filter if a single page was requested
    return pageNumber > 0 ? [chunks[(pageNumber - 1) | 0]] : chunks.filter(Boolean);
  },

  async thumbnail(pdfUrl: string, pageNumber: number = 1, scale: number = 1.0): Promise<string> {
    const pages = await this.getPagesAsImage(pdfUrl, pageNumber, scale);
    return pages[0];
  },

  // expose the stream for callers that want progressive consumption
  getPagesAsImageStream,
};

export default PDF;
