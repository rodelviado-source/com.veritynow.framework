// src/data/DataModeSwitch.tsx
import * as React from "react";
import {DataFacade} from "@/data/facade/DataFacade";

import { MenuItem, SelectChangeEvent, Select, InputLabel, FormControl } from "@mui/material";
import { ModeTypes } from "../transports/Transport";

interface DataModeSelectProps{
   onModeChange?: () => void;
}


export const DataModeSelect: React.FC<DataModeSelectProps> = ({ onModeChange }) =>  {
  const [mode, setMode] = React.useState(DataFacade.getMode());
  const [selectEvent, setSelectEvent] = React.useState<SelectChangeEvent>();

  React.useEffect(() => {
  
      //Always synch select value against the source of truth
      //since it may change without onModeChange being calledd
      if (selectEvent != null && selectEvent.target.value !== DataFacade.getMode()) {
          selectEvent.target.value = DataFacade.getMode();
          setMode(DataFacade.getMode());
      }
      setMode(DataFacade.getMode());
    
    
   },[mode, DataFacade.getMode(), selectEvent])

const handleChange = (event: SelectChangeEvent) => {
    DataFacade.setMode(event.target.value as ModeTypes);
    setMode(event.target.value as ModeTypes);
    setSelectEvent(event);
    onModeChange?.();
  };
    
  return (
    <div>
   
   <FormControl sx={{ width: '12ch' }} variant="outlined">
   <InputLabel htmlFor='mode-select'>mode</InputLabel>
   <Select
    id="mode-select"
    value={mode}
    label="mode"
    onChange={handleChange}
  > <MenuItem value="embedded">Offline</MenuItem>
    <MenuItem value="remote">Online</MenuItem>
    <MenuItem value="auto">Auto</MenuItem>
  </Select>
 
  </FormControl>
</div>

  );
}
