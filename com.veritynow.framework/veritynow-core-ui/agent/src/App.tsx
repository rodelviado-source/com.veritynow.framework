
import { useEffect, useRef, useState, useContext, createContext } from "react";
import ThreePaneLayout from "@/components/ui/ThreePaneLayout";
import { IframeForm, type IframeFormHandle } from "@/components/ui/IframeForm";
import { useFormContext, FormProvider } from "@/components/ui/FormContext";

import { Container, Typography } from '@mui/material';
import Checkbox from '@mui/material/Checkbox';

import Stack from '@mui/material/Stack';
import FileDropZone from '@/components/ui/FileDropZone';
import FormControlLabel from '@mui/material/FormControlLabel';

import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle'

 

function fetchForm() {
	
	let form = null;
	const iframeElement = document.querySelector('iframe'); 
	const iframeWindow = iframeElement.contentWindow;
	const iframeDocument = iframeWindow.document;
	form = iframeDocument.querySelector('form');
	if (form == null) {
		 form = document.getElementById('loan_form');
	}
		 
	if (form == null) {
		alert("don't know what to do");
	} else {
		alert("eureka ");
		form.reset();
	}
	
	return form;
	
}





function NavigationPane() {
   
	const { open, setOpen, form, setForm } = useFormContext();
 	
	const handleClose = (proceed) => {
		setOpen(false);
		if (proceed)  {
			if (form == null) {
				setForm(fetchForm());
			}
			form && form.reset();
	  }	
	}
	

 return (	
	
	<div>
	
	<Container maxWidth="sm" sx={{ mt: 5 }}>
	      <Typography variant="h5" >BDO Personal Loan Onboarding</Typography>
		  <FileDropZone />
	 </Container>
	  
	  <Stack direction="column" spacing={2} sx={{ mt: '10px', ml: '12px', mr: '10px' }}>
	  		<FormControlLabel control={<Checkbox />} label="AI Traning mode"  />		
	        <Button variant="outlined"  onClick={() => setOpen(true)}> Reset All Form Values</Button>
		</Stack>
		
		<Dialog  open={open}  aria-labelledby="alert-dialog-title"  aria-describedby="alert-dialog-description">
		        <DialogTitle id="alert-dialog-title">
		          {"Are you sure you want to reset all form fields?"}
		        </DialogTitle>
		        <DialogContent>
		          <DialogContentText id="alert-dialog-description">
		           	This operation is not reversible, any changes you made will be lost forever.
		          </DialogContentText>
		        </DialogContent>
		        <DialogActions>
		          <Button onClick={() => handleClose(true)}>Continue</Button>
		          <Button onClick={() => handleClose(false)}> Cancel </Button>
		        </DialogActions>
		  </Dialog> 
		
	 </div>
  )
}

function FormPane() {

  return (
	<div className="p-1 w-full h-full border-0">
	  	<IframeForm src="/ui/forms/bdo/personal/loans/form.html"  className="w-full h-full border-0"/>
	</div>  
  );
}

function ImagePane() {
  return (
    <div className="p1 w-full h-full border-0">
     	<img src="/images/sample.jpg"  />
		
    </div>
	
  );
}




export default function App() {
 
     
  return (
	<FormProvider>
    <div className="h-screen w-screen">
	      <ThreePaneLayout
        leftWidth={400}
        minRightPct={22}
        initialRightPct={[80, 20]}
        persistKey="three-pane-tailwind"
		
        left=  {<NavigationPane />}
        rightA={<FormPane  />}
        rightB={<ImagePane />} />
	  
    </div>
	</FormProvider>
	
	
	
  );
}
