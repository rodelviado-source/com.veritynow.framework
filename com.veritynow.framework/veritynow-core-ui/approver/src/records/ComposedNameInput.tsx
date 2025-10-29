// src/records/ComposedNameInput.tsx
import * as React from "react";

import {CancelSharp  as CancelIcon} from '@mui/icons-material'
import {Save  as ApplyIcon} from '@mui/icons-material'
import {Edit  as EditIcon} from '@mui/icons-material'

import { Input } from "@/components/ui/input";

import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import { Button } from "@mui/material";

type NameDraft = {
  clientFirstName?: string;
  clientMiddleName?: string;
  clientLastName?: string;
  clientSuffix?: string;
};

export type RecordItemNameLike = {
  id: number;
  clientId: string;
  clientFirstName?: string;
  clientMiddleName?: string;
  clientLastName?: string;
  clientSuffix?: string;
};

function composeName(
  first?: string,
  middle?: string,
  last?: string,
  suffix?: string
) {
  return [first, middle, last, suffix].filter(Boolean).join(" ");
}



export default function ComposedNameInput({
  label,                 // string shown in the table cell
  isEditing,             // row is in edit mode?
  row,                   // current row (for initial values)
  setDraft,           // setDraft from parent (Partial<RecordItem>)
  

}: {
  label: string;
  isEditing: boolean;
  row: RecordItemNameLike;
  setDraft: React.Dispatch<React.SetStateAction<Partial<NameDraft>>>;
}) {
  const [open, setOpen] = React.useState(false);

  // Local, controlled state for the popup fields
  const [first, setFirst] = React.useState(row.clientFirstName ?? "");
  const [middle, setMiddle] = React.useState(row.clientMiddleName ?? "");
  const [last, setLast] = React.useState(row.clientLastName ?? "");
  const [suffix, setSuffix] = React.useState(row.clientSuffix ?? "");
  const firstRef = React.useRef<HTMLInputElement | null>(null);

  // When dialog opens, (re)initialize from the row and focus the first field
  React.useEffect(() => {
    if (open) {
      setFirst(row.clientFirstName ?? "");
      setMiddle(row.clientMiddleName ?? "");
      setLast(row.clientLastName ?? "");
      setSuffix(row.clientSuffix ?? "");
      // next tick to ensure element is mounted
      requestAnimationFrame(() => firstRef.current?.focus());
    }
  }, [open, row.clientFirstName, row.clientMiddleName, row.clientLastName, row.clientSuffix]);

  const preview = composeName(first, middle, last, suffix);

  function applyChanges() {
    // Write to parent draft only — persist when the user clicks the row Save
    setDraft((d) => ({
      ...d,
      clientFirstName: first || "",
      clientMiddleName: middle || "",
      clientLastName: last || "",
      clientSuffix: suffix || "",
    }));
    setOpen(false);
  }

  // function onKeyDown(e: React.KeyboardEvent) {
  //   if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
  //     applyChanges();
  //   }
  // }

  return (
    <>
      {/* Table cell trigger */}
      {isEditing ? (
        <div className="flex items-start gap-2">
          
          <Button title="Edit client full name" endIcon={<EditIcon/>} variant="text" onClick = {() => setOpen(true)} >
                {label ?  `${label}` : "set client name"}
          </Button>
        </div>
      ) : (
        <span className="text-sm">{label || "—"}</span>
      )}

      {/* Popup dialog */}
      <Dialog open={open} onOpenChange={(v) => setOpen(v)} >
        
        <DialogHeader>
          <DialogTitle>Edit Client Full Name</DialogTitle>
        </DialogHeader>
        

        <DialogContent>
          {/* Live preview */}
          <div className="mb-2 rounded-2xl border bg-white px-3 py-2 text-sm">
            <span className="text-gray-500 mr-2">Preview:</span>
            <span className="font-medium">{preview || "(empty)"}</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <Input title= "First Name" 
              ref={firstRef}
              placeholder="First name"
              value={first}
              onChange={(e) => setFirst(e.target.value)}
            />
            <Input
              placeholder="Middle name"
              title="Middle name"
              value={middle}
              onChange={(e) => setMiddle(e.target.value)}
            />
            <Input
              placeholder="Last name"
              title="Last name"
              value={last}
              onChange={(e) => setLast(e.target.value)}
            />
            <Input
              placeholder="Suffix (e.g., Jr., Sr.)"
              title="Suffix"
              value={suffix}
              onChange={(e) => setSuffix(e.target.value)}
            />
          </div>
          <p className="text-xs text-gray-500 pt-1">
            Tip: <kbd>Ctrl/⌘ + Enter</kbd> to apply.
          </p>
        </DialogContent>

         <DialogFooter>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-2 mr-2 ml-2">
            <Button title="Cancel changes" variant="outlined" startIcon={<CancelIcon/>} onClick={() => setOpen(false)} >
                Cancel
            </Button>
            <Button title="Apply changes" variant="outlined" startIcon={<ApplyIcon/>} onClick ={applyChanges} >
                Apply
            </Button>
          </div>
        </DialogFooter>
      </Dialog>
    </>
  );
}
