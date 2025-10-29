import React, { useState } from "react";
import { Dialog, DialogContent, DialogTitle, DialogActions, Toolbar, AppBar, Slide, Button } from "@mui/material";

import ImageFacade from "@/data/facade/ImageFacade";
import { type RecordItem } from "./RecordsTable";
import {CloseSharp as CloseIcon} from  "@mui/icons-material"
import FullImageViewer from "./FullImageViewer";

import { TransitionProps } from "@mui/material/transitions";

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & { children: React.ReactElement<unknown>; },
  ref: React.Ref<unknown>,
) {
  return <Slide direction="up" ref={ref} {...props} />;
});


interface GalleryProps {
    viewImagesOf: RecordItem | null;
    setViewImagesOf: React.Dispatch<React.SetStateAction<RecordItem | null>>;
}

export default function ImageGallery({ viewImagesOf, setViewImagesOf }: GalleryProps) {

    const [open, setOpen] = useState<boolean>(false);
    const [imageIds, setImageIds] = useState<string[] | null>([]);

    const [currentIndex, setCurrentIndex] = useState<number>(0);


    function openViewer(n: number) {
        if (viewImagesOf != null) {
            setImageIds(viewImagesOf.imageIds);
        }
        setCurrentIndex(n);
        setOpen(true);
    }
    return (
        <React.Fragment>
            <Dialog  fullWidth open={!!viewImagesOf} slots={{transition: Transition}} >
                <AppBar sx={{ position: "sticky" }}>
                    <Toolbar sx={{ justifyContent: 'center' }}>
                        <DialogActions sx={{ justifyContent: 'center' }} >
                            <Button title="Close" variant="contained" startIcon={<CloseIcon/>} onClick={() => setViewImagesOf(null)} >
                                Close
                            </Button>
                        </DialogActions>
                    </Toolbar>
                </AppBar>
                <DialogTitle>Images</DialogTitle>
                <DialogContent >
                    <FullImageViewer open={open} setOpen={setOpen} imageIds={imageIds} currentIndex={currentIndex} />
                    {viewImagesOf && (
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                            {viewImagesOf.imageIds?.map((id, i) => (
                                <button
                                    key={id}
                                    type="button"
                                    onClick={() => openViewer(i)}
                                    title="Click to view full size"
                                    className="group relative rounded-2xl border shadow overflow-hidden bg-white"
                                >
                                    <ImageFacade
                                        imageId={id}
                                        className="block w-full h-35 w-20 object-cover transition-transform group-hover:scale-[1.02]"
                                        alt={id}
                                    />
                                    <span className="absolute bottom-1 right-1 text-[10px] bg-black/60 text-white px-1.5 py-0.5 rounded">
                                        View
                                    </span>
                                </button>
                            ))}

                        </div>

                    )

                    }

                </DialogContent>


            </Dialog>


        </React.Fragment>
    )
}
