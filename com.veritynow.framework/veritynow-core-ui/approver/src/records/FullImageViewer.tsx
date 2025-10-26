import * as React from "react";
import { Button } from "@/components/ui/button";

export default function FullImageViewer({
  ids,
  index,
  setIndex,
  srcBase,
  onClose,
}: {
  ids: string[];
  index: number;
  setIndex: (n: number) => void;
  srcBase: string;
  onClose: () => void;
}) {
  const [fit, setFit] = React.useState<"original" | "contain">("contain");

  const id = ids[index] ?? "";
  const src = id ? `${srcBase}${id}` : "";

  React.useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "ArrowLeft") setIndex(index - 1);
      if (e.key === "ArrowRight") setIndex(index + 1);
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [index, setIndex, onClose]);

  const toggleFit = () => setFit((f) => (f === "contain" ? "original" : "contain"));

  if (!ids.length) {
    return (
      <div className="w-full h-[60vh] flex items-center justify-center text-sm text-muted-foreground">
        No images
      </div>
    );
  }

  return (
    <div className="space-y-3 w-full text-white">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Button
            className="border bg-transparent text-white hover:bg-white/10"
            onClick={() => setIndex(index - 1)}
          >
            ← Prev
          </Button>
          <Button
            className="border bg-transparent text-white hover:bg-white/10"
            onClick={() => setIndex(index + 1)}
          >
            Next →
          </Button>
        </div>
        <div className="text-xs opacity-80">
          {index + 1} / {ids.length} · {id}
        </div>
      </div>

      <div
        className="relative border rounded-2xl bg-black shadow-lg overflow-auto resize mx-auto"
        style={{
          width: "100vw",
          height: "100vh",
          maxWidth: "100vw",
          maxHeight: "100vh",
          minWidth: 360,
          minHeight: 240,
        }}
        title="Drag bottom-right corner to resize (clamped to viewport)"
      >
        <div className="min-w-max min-h-max p-2">
          <img
            src={src}
            alt={id}
            onDoubleClick={toggleFit}
            className="select-none mx-auto cursor-zoom-in"
            style={
              fit === "contain"
                ? { maxWidth: "100%", maxHeight: "100%", objectFit: "contain" }
                : { maxWidth: "none", maxHeight: "none" }
            }
            draggable={false}
          />
        </div>
      </div>

      <div className="flex items-center justify-center gap-2">
        <Button
          className="border bg-transparent text-white hover:bg-white/10"
          onClick={toggleFit}
        >
          {fit === "contain" ? "Original Size" : "Fit Inside Panel"}
        </Button>
        <a
          href={src}
          target="_blank"
          rel="noreferrer"
          className="text-sm underline text-white/70 hover:text-white"
          title="Open original"
        >
          Open original
        </a>
      </div>

      <div className="text-center text-xs opacity-70">
        Tips: drag the bottom-right corner to resize · ←/→ to navigate · Esc to close · double-click to toggle fit
      </div>
    </div>
  );
}
