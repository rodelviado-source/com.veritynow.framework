import React, { useEffect, useMemo, useState } from "react";
import {
    Dialog, DialogContent, DialogTitle, DialogActions, Toolbar, AppBar, Slide, Button
} from "@mui/material";
import { CloseSharp as CloseIcon } from "@mui/icons-material";
import ImageFacade from "@/data/facade/ImageFacade";
import { RecordItem } from "@/data/types/Record";
import FullImageViewer from "@/records/FullImageViewer";
import { TransitionProps } from "@mui/material/transitions";
import { DataFacade } from "@/data/facade/DataFacade";
import { MediaResource } from "@/data/transports/Transport";

const Transition = React.forwardRef(function Transition(
    props: TransitionProps & { children: React.ReactElement<unknown> },
    ref: React.Ref<unknown>
) {
    return <Slide direction="up" ref={ref} {...props} />;
});
Transition.displayName = "ImageGalleryTransition";

interface GalleryProps {
    viewImagesOf: RecordItem | null;
    setViewImagesOf: React.Dispatch<React.SetStateAction<RecordItem | null>>;
}

// ...imports stay the same

export default function ImageGallery({ viewImagesOf, setViewImagesOf }: GalleryProps) {
    const [open, setOpen] = useState(false);
    const [imageIds, setImageIds] = useState<string[] | null>(null);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [metas, setMetas] = useState<{ [key: string]: MediaResource }>({});

    useEffect(() => {
        if (!viewImagesOf) {
            setOpen(false);
            setImageIds(null);
            setMetas({});
            setCurrentIndex(0);
            return;
        }

        const ids = viewImagesOf.imageIds ?? [];
        setImageIds(ids);
        setCurrentIndex(0);

        let cancelled = false;
        (async () => {
            try {
                const resources = await Promise.all(ids.map((id) => DataFacade.mediaFor(id)));
                if (cancelled) return;
                const nextMetas: { [key: string]: MediaResource } = {};
                ids.forEach((id, i) => { nextMetas[id] = resources[i]; });
                setMetas(nextMetas);
            } catch (e) {
                if (!cancelled) console.error(e);
                setMetas({});
            }
        })();

        return () => { cancelled = true; };
    }, [viewImagesOf]);

    const memoImageIds = useMemo(() => imageIds ?? [], [imageIds?.join("|")]);

    const handleClose = () => {
        setOpen(false);
        setViewImagesOf(null);
        setImageIds(null);
        setCurrentIndex(0);
    };

    const openViewer = (n: number) => {
        setCurrentIndex(n);
        setOpen(true);
    };

    // nothing selected
    if (!viewImagesOf) return null;

    const total = imageIds?.length ?? 0;
    const ready = total > 0 && Object.keys(metas).length === total;

    // shared dialog shell
    const Shell: React.FC<{ children: React.ReactNode; title?: React.ReactNode }> = ({ children }) => (
        <Dialog fullWidth open={!!viewImagesOf} onClose={handleClose} slots={{ transition: Transition }}>
            <AppBar sx={{ position: "sticky" }}>
                <Toolbar sx={{ justifyContent: "center" }}>
                    <DialogActions sx={{ justifyContent: "center" }}>
                        <Button title="Close" variant="contained" startIcon={<CloseIcon />} onClick={handleClose}>
                            Close
                        </Button>
                    </DialogActions>
                </Toolbar>
            </AppBar>
            <DialogTitle>Documents</DialogTitle>
            <DialogContent>{children}</DialogContent>
        </Dialog>
    );

    // RENDER: loading (no touching m until present)
    if (!ready) {
        return (
            <Shell>
                <FullImageViewer open={open} setOpen={setOpen} imageIds={memoImageIds} currentIndex={currentIndex} />
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mt-3">
                    {imageIds?.map((id) => (
                        <div
                            key={id}
                            className="h-36 rounded-2xl border shadow overflow-hidden bg-gray-100 animate-pulse"
                            aria-label="Loading thumbnail"
                        />
                    ))}
                </div>
            </Shell>
        );
    }

    // RENDER: ready (safe to pass m)
    return (
        <Shell>
            <FullImageViewer open={open} setOpen={setOpen} imageIds={memoImageIds} currentIndex={currentIndex} />
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mt-3">
              
                {imageIds!.map((id, i) => {
                    const meta = metas[id] ?? null;  /** optional; speeds up first paint **/
                    return (
                        <button key={id} onClick={() => openViewer(i)} className="group relative rounded-2xl border shadow overflow-hidden bg-white">
                            <ImageFacade
                                id={id}                         // real id
                                cacheKey={`${DataFacade.getMode()}:${id}`}      // namespaced cache key (prevents cross-mode collisions)
                                meta={meta}
                                className="block w-full h-35 object-cover transition-transform group-hover:scale-[1.02]"
                            />
                            <span className="absolute bottom-1 right-1 text-[10px] bg-black/60 text-white px-1.5 py-0.5 rounded">View</span>
                        </button>
                    );
                })}
            </div>
        </Shell>
    );
}
