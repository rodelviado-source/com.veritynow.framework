import * as React from "react";
import { Button } from "@/components/ui/button";
import { FacadeImage } from "@/data/core/FacadeImage";

export default function FullImageViewer({
  ids,
  index,
  setIndex,
  onClose,
}: {
  ids: string[];
  index: number;
  setIndex: (n: number) => void;
  onClose: () => void;
}) {
  const [fit, setFit] = React.useState<"original" | "contain">("contain");
  const id = ids[index] ?? "";

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
    return <div className="w-full h-[60vh] flex items-center justify-center text-sm text-muted-foreground">No images</div>;
  }

  return (
    <div className="space-y-3 w-full text-white">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Button className="border bg-transparent text-white hover:bg-white/10" onClick={() => setIndex(index - 1)}>← Prev</Button>
          <Button className="border bg-transparent text-white hover:bg-white/10" onClick={() => setIndex(index + 1)}>Next →</Button>
        </div>
        <div className="text-xs opacity-80">{index + 1} / {ids.length} · {id}</div>
      </div>

      <div
        className="relative border rounded-2xl bg-black shadow-lg overflow-auto resize mx-auto flex justify-center items-center"
        style={{ width: "100vw", height: "100vh", maxWidth: "100vw", maxHeight: "100vh", minWidth: 360, minHeight: 240 }}
      >
        {id and (
          <FacadeImage
            imageId={id}
            className={`max-w-full max-h-full object-${fit === "contain" ? "contain" : "none"}`}
            alt={id}
          />
        )}
      </div>

      <div className="flex items-center justify-center gap-2">
        <Button className="border bg-transparent text-white hover:bg-white/10" onClick={toggleFit}>
          {fit === "contain" ? "Original Size" : "Fit Inside Panel"}
        </Button>
      </div>

      <div className="text-center text-xs opacity-70">
        Tips: drag to resize · ←/→ to navigate · Esc to close · double-click toggles fit
      </div>
    </div>
  );
}
