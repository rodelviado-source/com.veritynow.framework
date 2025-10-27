import * as React from "react";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import IconButton from "@mui/material/IconButton";
import Slide from "@mui/material/Slide";
import { TransitionProps } from "@mui/material/transitions";
import { FacadeImage } from "@/data/core/FacadeImage";
import "@/records/viewer.css";

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

export default function ImageViewerDialog({ open, setOpen, imageIds, currentIndex }: Props) {
  const handleClose = () => setOpen(false);

  return (
    <React.Fragment>
      <Dialog fullScreen open={open} onClose={handleClose} TransitionComponent={Transition}>
        <AppBar sx={{ position: "relative" }}>
          <Toolbar>
            <IconButton edge="start" color="inherit" onClick={handleClose} aria-label="close" />
            <div className="middle">
              <Button autoFocus color="inherit" onClick={handleClose}>
                close
              </Button>
            </div>
          </Toolbar>
        </AppBar>
        {imageIds && imageIds.length > 0 && (
          <div className="scrollable middle">
            <FacadeImage imageId={imageIds[currentIndex]} className="max-w-full max-h-full object-contain" />
          </div>
        )}
      </Dialog>
    </React.Fragment>
  );
}
