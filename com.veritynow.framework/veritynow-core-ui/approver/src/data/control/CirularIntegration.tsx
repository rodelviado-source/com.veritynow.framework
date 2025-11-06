import * as React from 'react';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import { green } from '@mui/material/colors';
import { red } from '@mui/material/colors';
import { grey } from '@mui/material/colors';
import Button from '@mui/material/Button';
import Fab from '@mui/material/Fab';
import {Check as CheckIcon} from '@mui/icons-material';
import {CachedSharp  as CachedIcon} from '@mui/icons-material'
import {ErrorSharp as ErrorIcon} from '@mui/icons-material'
import {ArrowRightAltSharp as RightArrowIcon} from '@mui/icons-material'

import { DataFacade } from '../facade/DataFacade';
import { ModeTypes } from '../transports/Transport';
import { Stack } from '@mui/material';


interface DataModeSelectProps {
   isLoading: boolean;
   isFetching?: boolean;
   isError?: boolean;
   errMsg?:string;
   refresh?: () => void;
}

export const CircularIntegration: React.FC<DataModeSelectProps> = ({ errMsg, isFetching, isLoading, isError, refresh}) =>  {

  const buttonSx = {
    ...(isError ?  {
      bgcolor: red[500],
      '&:hover': {
        bgcolor: red[700],
      },
    } : isLoading || isFetching ? {  
      bgcolor: grey[500],
      '&:hover': {
        bgcolor: grey[700],
      } 
    } : {  
      bgcolor: green[500],
      '&:hover': {
        bgcolor: green[700],
      }
    }),
  };

  const timer = React.useRef<ReturnType<typeof setTimeout>>(undefined);

  const success = ():boolean => {
    return !isLoading && !isFetching && !isError;
  }

const [prevMode, setPrevMode] =  React.useState(DataFacade.getMode);
const [open, setOpen] = React.useState(true);

React.useEffect(() => {
      clearTimeout(timer.current);
      console.log(prevMode, " === ", DataFacade.getMode());
      if (prevMode !== DataFacade.getMode()) {
        setOpen(true);
      }
 },[open, DataFacade.getMode() ])

const handleOnClick = () => {
   setOpen(false);
   setPrevMode(DataFacade.getMode());
   if (isError) {
    DataFacade.setMode(ModeTypes.embedded);
    refresh?.();
   }
}

if (success() ) {
  timer.current = setTimeout(() => {
      handleOnClick();
    }, prevMode === DataFacade.getMode() ? 1 : 1000);
}
return (
   
open &&
    <Box  sx={ { display: 'flex', alignItems: 'center', m:1 }}>
      <Box sx={{ m: 1, position: 'relative' }}>
        <Fab
          sx={buttonSx}
        >
          {success() ? <CheckIcon onClick={handleOnClick}/> : !isError ? <CachedIcon /> : <ErrorIcon onClick={handleOnClick}/>}
        </Fab>
        {!isError && (isLoading || isFetching) && (
          <CircularProgress
            size={68}
            sx={{
              color: green[500],
              position: 'absolute',
              top: -6,
              left: -6,
              zIndex: 1,
            }}
          />
        )}
      </Box>

      <Box sx={{ m: 1, position: 'relative' } }>
        <Button
          variant="contained"
          sx={buttonSx}
          onClick={handleOnClick}
          disabled={ isLoading || isFetching}
        >
          {isError ? 
          <Stack>
              <div>Please Click to try again...</div> 
              <div>Error  <RightArrowIcon/> {errMsg}</div>
           </Stack>   
            :
            isLoading || isFetching ?
              'Fetching data remotely...'  
            :
               `Setting mode to ${DataFacade.getMode()}`
           }
        </Button>
        
      </Box>
    </Box>
);
}