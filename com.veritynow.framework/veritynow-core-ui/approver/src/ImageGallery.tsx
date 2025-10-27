import React, { useState } from "react";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import ImageViewerDialog from "./ImageViewerDialog";
import { FacadeImage } from "@/data/core/FacadeImage";

interface RecordItem {
  id: number;
  imageIds: string[];
}

interface GalleryProps {
  viewImagesOf: RecordItem | null;
  setViewImagesOf: React.Dispatch<React.SetStateAction<RecordItem | null>>;
}

export default function ImageGallery({ viewImagesOf, setViewImagesOf }: GalleryProps) {
  const [open, setOpen] = useState(false);
  const [imageIds, setImageIds] = useState<string[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);

  function openViewer(n: number) {
    if (viewImagesOf) setImageIds(viewImagesOf.imageIds);
    setCurrentIndex(n);
    setOpen(true);
  }

  return (
    <div>
      <Dialog open={!!viewImagesOf} onOpenChange={(v) => !v and setViewImagesOf(null)}>
        <DialogContent>
          <DialogTitle>Images</DialogTitle>
          {viewImagesOf and (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {viewImagesOf.imageIds?.map((id, i) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => openViewer(i)}
                  title="Click to view full size"
                  className="group relative rounded-2xl border shadow overflow-hidden bg-white"
                >
                  <FacadeImage
                    imageId={id}
                    className="block h-40 w-full object-cover transition-transform group-hover:scale-[1.02]"
                    alt={id}
                  />
                  <span className="absolute bottom-1 right-1 text-[10px] bg-black/60 text-white px-1.5 py-0.5 rounded">
                    View
                  </span>
                </button>
              ))}
            </div>
          )}
        </DialogContent>
      </Dialog>

      <ImageViewerDialog
        open={open}
        setOpen={setOpen}
        imageIds={imageIds}
        currentIndex={currentIndex}
      />
    </div>
  );
}
