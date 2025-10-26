import * as React from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import FullImageViewer from "./FullImageViewer";

type Props = {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  ids: string[];
  index: number;
  setIndex: (n: number) => void;
  srcBase: string;
};

export default function FullImageModal({ open, onOpenChange, ids, index, setIndex, srcBase }: Props) {
  const len = ids.length;

  function go(n: number) {
    if (!len) return;
    const next = (n + len) % len;
    setIndex(next);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="fixed inset-0 bg-black/90 text-white p-4 flex flex-col gap-3"
        style={{ zIndex: 60 }}
      >
        <DialogHeader className="flex flex-row items-center justify-between">
          <DialogTitle className="text-white">Full Image</DialogTitle>
          <div className="flex items-center gap-2">
            <Button
              className="border bg-transparent text-white hover:bg-white/10"
              onClick={() => go(index - 1)}
              disabled={!len}
            >
              ← Prev
            </Button>
            <Button
              className="border bg-transparent text-white hover:bg-white/10"
              onClick={() => go(index + 1)}
              disabled={!len}
            >
              Next →
            </Button>
            <Button
              className="border bg-transparent text-white hover:bg-white/10"
              onClick={() => onOpenChange(false)}
            >
              ✕
            </Button>
          </div>
        </DialogHeader>

        <div className="flex-1 overflow-auto flex items-center justify-center">
          <div className="w-full">
            <FullImageViewer
              ids={ids}
              index={index}
              setIndex={(n) => go(n)}
              srcBase={srcBase}
              onClose={() => onOpenChange(false)}
            />
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
