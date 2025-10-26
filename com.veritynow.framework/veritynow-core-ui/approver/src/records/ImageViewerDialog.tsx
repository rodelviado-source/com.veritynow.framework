import * as React from 'react';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';




import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';

import Slide from '@mui/material/Slide';


import { TransitionProps,  } from '@mui/material/transitions';
import "@/records/viewer.css";

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & {
    children: React.ReactElement<unknown>;
  },
  ref: React.Ref<unknown>,
) {
  return <Slide direction="up" ref={ref} {...props} />;
});


interface Images {
    open: boolean;
    setOpen: React.Dispatch<React.SetStateAction<boolean>>;
    baseUrl:string;
    imageIds:string[] | null;
    currentIndex:number;
}

export default function ImageViewrDialog( {open, setOpen, baseUrl, imageIds, currentIndex}:Images) {
  

  const handleClose = () => {
    setOpen(false);
  };

  return (
    <React.Fragment>
      <Dialog 
        fullScreen
        open={open}
        onClose={handleClose}
        slots={{ transition: Transition}}

      >
        <AppBar sx={{ position: 'relative' }}>
          <Toolbar>
            <IconButton
              edge="start"
              color="inherit"
              onClick={handleClose}
              aria-label="close"
            >
            </IconButton>
            <div className="middle">
                <Button autoFocus color="inherit" onClick={handleClose}>
                close
                </Button>
            </div>
          </Toolbar>
        </AppBar>
        {imageIds &&
            <div className="scrollable middle">
                <img src={baseUrl + "/api/images/" + imageIds[currentIndex]  } />
            </div>
        }
      </Dialog>
    </React.Fragment>
  );
}
