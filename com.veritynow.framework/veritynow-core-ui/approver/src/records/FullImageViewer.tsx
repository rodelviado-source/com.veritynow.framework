import * as React from "react";

import Dialog from "@mui/material/Dialog";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";

import Slide from "@mui/material/Slide";
import { TransitionProps } from "@mui/material/transitions";
import { ImageFacade } from "@/data/facade/ImageFacade";
import { Button, DialogContent } from "@mui/material";
import {CloseFullscreenSharp as CloseIcon} from "@mui/icons-material"


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
  const handleClose = () => setOpen(false);

  return (
    <React.Fragment>
      <Dialog   fullScreen open={open} onClose={handleClose} slots={{transition: Transition}}>
        <AppBar sx={{ position: "sticky" }} >
          <Toolbar sx={{ justifyContent: 'center'}}>
               <Button title="Close" variant="contained" startIcon={<CloseIcon/>} onClick={handleClose} >
                  Close
              </Button>
          </Toolbar>
        </AppBar>
        <DialogContent>
          {imageIds && imageIds.length > 0 && (<ImageFacade imageId={imageIds[currentIndex]} /> )}
        </DialogContent>
       
      </Dialog>
    </React.Fragment>
  );
}
