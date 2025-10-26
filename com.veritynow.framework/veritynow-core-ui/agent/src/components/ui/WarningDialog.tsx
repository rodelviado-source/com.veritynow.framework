import * as React from 'react';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';

export default function WarningDialog(props) {

 const handleClickOpen = (result) => {
	props.promptHandler(result);
  };

  const handleClose = (result) => {
	props.promptHandler(result);
  };

  
  return (
    <React.Fragment>
	<Dialog>
        open={props.openState}
        onClose={() => handleClose}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
    
        <DialogTitle id="alert-dialog-title">
          {"Use Google's location service?"}
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            Clear all form values?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
			<Button onClick={() => handleClose(true)} >Continue</Button>  
			<Button onClick={() => handleClose(false)} autoFocus>Cancel</Button>
          
        </DialogActions>
      </Dialog>
    </React.Fragment>
  );
}
