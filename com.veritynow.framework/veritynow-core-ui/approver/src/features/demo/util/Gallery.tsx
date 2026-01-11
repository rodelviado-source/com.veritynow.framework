// Gallery.tsx
import * as React from "react";
import {
  Dialog, DialogContent, DialogTitle, IconButton, Box, Stack,
  List, ListItemButton, ListItemText, Tooltip, CircularProgress
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";


import { DataFacade } from "@/core/facade/DataFacade";
import PDF from "@/features/demo/util/PDF";
import { PDFViewer } from "@/features/demo/util/PDFViewer";
import { useEffect } from "react";
import { AssetRecord, AssetSummary } from "@/features/asset";

type Props = {
  open: boolean;
  imageIds: string[];
  onClose: () => void;
  initialIndex?: number;
  thumbScale?: number;
  leftPaneWidth?: number;
};


type ThumbEntry = {
  id: string;
  media: AssetSummary | null;
  thumbUrl: string | null;
  error?: string | null;
};

/** safely pick common version-ish fields from MediaResource without `any` */
function pickVersion(media: AssetRecord | null | undefined): string {
  if (!media) return "";
  const raw: Record<string, unknown> = media as unknown as Record<string, unknown>;

  const candidates: string[] = [];

  const addIfString = (k: string) => {
    if (Object.prototype.hasOwnProperty.call(raw, k)) {
      const v = raw[k];
      if (typeof v === "string" && v.length) candidates.push(`${k}=${v}`);
    }
  };
  const addIfNumber = (k: string) => {
    if (Object.prototype.hasOwnProperty.call(raw, k)) {
      const v = raw[k];
      if (typeof v === "number" && Number.isFinite(v)) candidates.push(`${k}=${v}`);
    }
  };

  addIfString("hash");
  addIfString("etag");
  addIfString("revision");
  addIfNumber("lastModified");

  return candidates.join("&");
}

/** compact meta formatter; returns string or null */
function formatMediaMeta(media: AssetSummary | null | undefined): string | null {
  if (!media) return null;
  const base: Record<string, unknown> = media as unknown as Record<string, unknown>;
  const parts: string[] = [];

  const pushIf = (label: string, v: unknown) => {
    if (v === undefined || v === null) return;
    if (typeof v === "string" || typeof v === "number" || typeof v === "boolean") {
      parts.push(`${label}: ${String(v)}`);
    }
  };

  pushIf("name", (base["filename"]));
  pushIf("type", (base["contentType"]));
  pushIf("size", (base["size"]));
  if ("width" in base || "height" in base) {
    const w = typeof base["width"] === "number" ? base["width"] : "?";
    const h = typeof base["height"] === "number" ? base["height"] : "?";
    parts.push(`dim: ${w}×${h}`);
  }
  pushIf("pages", (base["pages"]));
  pushIf("kind", (base["kind"]));
  pushIf("id", (base["id"]));
  if (typeof base["lastModified"] === "number") {
    parts.push(`modified: ${new Date(Number(base["lastModified"])).toLocaleString()}`);
  }
  if (typeof base["createdAt"] === "number" || typeof base["createdAt"] === "string") {
    parts.push(`created: ${new Date(String(base["createdAt"])).toLocaleString()}`);
  }

  // optional nested meta / metadata sample
  const metaSource = ((): Record<string, unknown> | null => {
    if (Object.prototype.hasOwnProperty.call(base, "meta") && typeof base["meta"] === "object" && base["meta"] !== null) {
      return base["meta"] as Record<string, unknown>;
    }
    if (Object.prototype.hasOwnProperty.call(base, "metadata") && typeof base["metadata"] === "object" && base["metadata"] !== null) {
      return base["metadata"] as Record<string, unknown>;
    }
    return null;
  })();

  if (metaSource) {
    const keys = Object.keys(metaSource).slice(0, 8);
    if (keys.length) {
      const extra = keys
        .map((k) => {
          const v = metaSource[k];
          if (v === null || v === undefined) return `${k}: null`;
          if (typeof v === "string" || typeof v === "number" || typeof v === "boolean") return `${k}: ${String(v)}`;
          try { return `${k}: ${JSON.stringify(v)}`; } catch { return `${k}: [object]`; }
        })
        .join("\n");
      parts.push("\nmeta:\n" + extra);
    }
  }

  return parts.length ? parts.join("\n") : null;
}

export default function Gallery({
  open,
  imageIds,
  onClose,
  initialIndex = 0,
  thumbScale = 0.6,
  leftPaneWidth = 240,
}: Props) {
  const [thumbs, setThumbs] = React.useState<ThumbEntry[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [selected, setSelected] = React.useState<number>(Math.min(initialIndex, Math.max(0, imageIds.length - 1)));

  
  // load media + thumbnails
  React.useEffect(() => {
    if (!open || imageIds.length === 0) {
      setThumbs([]);
      return;
    }

    let cancelled = false;

    (async () => {
      setLoading(true);
      const initialized: ThumbEntry[] = imageIds.map((id) => ({ id, media: null, thumbUrl: null }));
      setThumbs(initialized);

      await Promise.all(
        imageIds.map(async (id, idx) => {
          try {
            const media = await DataFacade.assets.getSummary(id);
          

            console.log("from mediaFor===>", media.);
                      //check if there is already a cached url
            const url = DataFacade.;
            console.log("from touchANdGet===>", url);
            //make sure we use the same url
            if (!url) {
                media.url = url as string;
            } else {
                media.url = await DataFacade.imageUrl(id);
            }

            if (cancelled) return;

            const thumbUrl =
              media.kind === MediaKind.pdf
                ? await PDF.thumbnail(media.url, 1, thumbScale)
                : media.url;

            if (cancelled) return;
            

            setThumbs((prev) => {
              const copy = prev.slice();
              copy[idx] = { id, media, thumbUrl };
              return copy;
            });
          } catch (e) {
            if (cancelled) return;
            const message = e instanceof Error ? e.message : "Failed to load";
            setThumbs((prev) => {
              const copy = prev.slice();
              copy[idx] = { id, media: null, thumbUrl: null, error: message };
              return copy;
            });
          }
        })
      );

      if (!cancelled) {
        setSelected((s) => Math.min(Math.max(0, s), imageIds.length - 1));
        setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
      //thumbs.forEach((t) => {if (t.thumbUrl) DataFacade.revokeObjectUrl(t.thumbUrl)});
      setThumbs([]);
    };
  }, [open, thumbScale, imageIds]);

  // keyboard nav
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "ArrowLeft") { e.preventDefault(); setSelected((s) => Math.max(0, s - 1)); }
      if (e.key === "ArrowRight") { e.preventDefault(); setSelected((s) => Math.min(thumbs.length - 1, s + 1)); }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, thumbs.length]);

  const current = thumbs[selected];


  // kind + version-aware key (forces full remount on image⇄pdf)
  const viewerKey = React.useMemo(() => {
    if (!current || !current.media) return "none";
    const v = pickVersion(current.media);
    return `${current.id}|${current.media.kind}|${current.media.url}|${v}`;
  }, [current]);


 
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xl">
      <DialogTitle sx={{ pr: 6 }}>
        Documents
        <IconButton onClick={onClose} sx={{ position: "absolute", right: 8, top: 8 }}>
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers sx={{ p: 0 }}>
        <Stack direction="row" sx={{ height: "80vh", width: "100%" }}>
          {/* Left rail */}
          <Box
            sx={{
              width: leftPaneWidth,
              borderRight: (t) => `1px solid ${t.palette.divider}`,
              overflowY: "auto",
            }}
          >
            <List dense disablePadding>
              {thumbs.map((t, i) => {
                const isActive = i === selected;
                const tip = t.media ? formatMediaMeta(t.media) : null;
                const tipNode: React.ReactNode = tip
                  ? <Box component="pre" sx={{ m: 0, fontFamily: "monospace", whiteSpace: "pre-wrap" }}>{tip}</Box>
                  : undefined;

                return (
                  <Tooltip
                    key={t.id}
                    title={tipNode}
                    placement="right"
                    arrow
                    enterDelay={400}
                  >
                    <ListItemButton
                      selected={isActive}
                      onClick={() => setSelected(i)}
                      sx={{ py: 1, px: 1.5, alignItems: "flex-start" }}
                    >
                      <Box sx={{ width: "100%", display: "flex", gap: 1, alignItems: "center" }}>
                        <Box
                          sx={{
                            width: 72, height: 72, borderRadius: 1, overflow: "hidden",
                            border: (theme) => `1px solid ${isActive ? theme.palette.primary.main : theme.palette.divider}`,
                            flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center",
                            bgcolor: "background.paper",
                          }}
                        >
                          {t.thumbUrl ? (
                            <img src={t.thumbUrl} alt={current.media?.filename} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                          ) : t.error ? (
                            <Box sx={{ fontSize: 11, color: "error.main", p: 1, textAlign: "center" }}>err</Box>
                          ) : (
                            <CircularProgress size={18} />
                          )}
                        </Box>
                        <ListItemText
                          slotProps={{ primary: { noWrap: true, fontSize: 13 }, secondary: { fontSize: 11 } }}
                          primary={t.media?.filename}
                          secondary={
                            t.media
                              ? (t.media.kind === MediaKind.pdf ? "PDF" : (t.media.contentType ?? "image"))
                              : t.error ?? (t.thumbUrl ? "" : "Loading…")
                          }
                        />
                      </Box>
                    </ListItemButton>
                  </Tooltip>
                );
              })}
              {thumbs.length === 0 && (
                <Box sx={{ p: 2, color: "text.secondary", fontSize: 13 }}>
                  {loading ? "Loading…" : "No items"}
                </Box>
              )}
            </List>
          </Box>

          {/* Right pane */}

          <Box
            sx={{
              flex: 1,
              minHeight: 0,
              overflow: "hidden",      // important: parent scroll happens in inner layers
              position: "relative",
              bgcolor: "background.default",
            }}
          >
            {/* Stacked layers: keep both mounted; toggle visibility only */}
            <Box
              sx={{
                position: "absolute",
                inset: 0,
                display: current?.media && current.media.kind !== MediaKind.pdf ? "flex" : "none",
                alignItems: "center",
                justifyContent: "center",
                overflow: "auto",
                p: 2,
              }}
            >
              {current?.media ? (
                current.media.kind !== MediaKind.pdf ? (
                  <img
                    key={`img|${current.id}|${current.media.url}`}
                    src={current.media.url}
                    alt={current.media.filename}
                    style={{ maxWidth: "100%", maxHeight: "100%", objectFit: "contain" }}
                  />
                ) : (
                  // keep structure consistent; never return {} or an object
                  <span style={{ opacity: 0.7 }}>Loading image…</span>
                )
              ) : loading ? (
                <CircularProgress />
              ) : (
                <span style={{ opacity: 0.7 }}>No item</span>
              )}
            </Box>

            <Box
              sx={{
                position: "absolute",
                inset: 0,
                display: current?.media && current.media.kind === MediaKind.pdf ? "flex" : "none",
                alignItems: "center",
                justifyContent: "center",
                overflow: "auto",
                p: 2,
              }}
            >
              {current?.media && current.media.kind === MediaKind.pdf ? (
                 <PDFViewer key={viewerKey} m={current.media} />
              ) : loading ? (
                <CircularProgress />
              ) : (
                // Keep a stable, valid node (never `{}`)
                <span style={{ opacity: 0.7 }}>Select a PDF…</span>
              )}
            </Box>
          </Box>

        </Stack>
      </DialogContent>
    </Dialog>
  );
}
