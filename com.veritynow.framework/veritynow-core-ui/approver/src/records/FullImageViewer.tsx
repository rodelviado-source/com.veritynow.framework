import * as React from "react";

import Dialog from "@mui/material/Dialog";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";

import Slide from "@mui/material/Slide";
import { TransitionProps } from "@mui/material/transitions";
import { Button, DialogContent } from "@mui/material";
import { CloseFullscreenSharp as CloseIcon } from "@mui/icons-material"
import { PDFViewer, CleanupHandle } from "@/components/ui/util/PDFViewer";
import { MediaKind, MediaResource } from "@/data/transports/Transport";
import { useRef, useState } from "react";
import { DataFacade } from "@/data/facade/DataFacade";

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & { children: React.ReactElement<unknown>; },
  ref: React.Ref<unknown>,
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

interface Props {
  open: boolean;
  setOpen: React.Dispatch<React.SetStateAction<boolean>>;
  imageIds: string[] | null;
  currentIndex: number;
}

export default function FullImageViewer({ open, setOpen, imageIds, currentIndex }: Props) {
  const imageId:string = imageIds![currentIndex]  ;
  const [media, setMedia] = useState<MediaResource>({ id:imageId, url: "", kind: MediaKind.image });
  const viewerRef = useRef<CleanupHandle>(null);

  const handleClose = () => { /* viewerRef.current?.cleanup(); */ setOpen(false) };

  React.useEffect(() => {
    let alive = true;
    (async () => {
      if (!imageIds) return;
      const next = await DataFacade.mediaFor(imageIds[currentIndex]);
      if (!alive) return;
      setMedia(prev =>
        prev.url === next.url && prev.kind === next.kind ? prev : next
      );
    })();
    return () => { alive = false; };
  }, [imageIds, currentIndex]);


  return (
    <React.Fragment>
      <Dialog fullScreen open={open} onClose={handleClose} slots={{ transition: Transition }}>
        <AppBar sx={{ position: "sticky" }} >
          <Toolbar sx={{ justifyContent: 'center' }}>
            <Button title="Close" variant="contained" startIcon={<CloseIcon />} onClick={handleClose} >
              Close
            </Button>
          </Toolbar>
        </AppBar>
        <DialogContent sx={{ justifyItems: 'center' }}>
          <PDFViewer ref={viewerRef} m={media} />
        </DialogContent>
      </Dialog>
    </React.Fragment>
  );
}
