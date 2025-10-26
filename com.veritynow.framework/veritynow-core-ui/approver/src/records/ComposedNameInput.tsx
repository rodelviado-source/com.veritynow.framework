import * as React from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";

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

function composeName(first?: string, middle?: string, last?: string, suffix?: string) {
  return [first, middle, last, suffix].filter(Boolean).join(" ");
}

export default function ComposedNameInput({
  label,
  isEditing,
  row,
  draftSetter,
}: {
  label: string;
  isEditing: boolean;
  row: RecordItemNameLike;
  draftSetter: React.Dispatch<React.SetStateAction<Partial<NameDraft>>>;
}) {
  const [open, setOpen] = React.useState(false);

  const [first, setFirst] = React.useState(row.clientFirstName ?? "");
  const [middle, setMiddle] = React.useState(row.clientMiddleName ?? "");
  const [last, setLast] = React.useState(row.clientLastName ?? "");
  const [suffix, setSuffix] = React.useState(row.clientSuffix ?? "");
  const firstRef = React.useRef<HTMLInputElement | null>(null);

  React.useEffect(() => {
    if (open) {
      setFirst(row.clientFirstName ?? "");
      setMiddle(row.clientMiddleName ?? "");
      setLast(row.clientLastName ?? "");
      setSuffix(row.clientSuffix ?? "");
      requestAnimationFrame(() => firstRef.current?.focus());
    }
  }, [open, row.clientFirstName, row.clientMiddleName, row.clientLastName, row.clientSuffix]);

  const preview = composeName(first, middle, last, suffix);

  function applyChanges() {
    draftSetter((d) => ({
      ...d,
      clientFirstName: first || "",
      clientMiddleName: middle || "",
      clientLastName: last || "",
      clientSuffix: suffix || "",
    }));
    setOpen(false);
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
      applyChanges();
    }
  }

  return (
    <>
      {isEditing ? (
        <div className="flex items-start gap-2">
          <button
            type="button"
            className="text-left text-sm underline decoration-dotted hover:decoration-solid"
            onClick={() => setOpen(true)}
            title="Edit client full name"
          >
            {label || "(set client name)"}
          </button>
          <button
            type="button"
            aria-label="Edit"
            onClick={() => setOpen(true)}
            className="mt-[2px] inline-flex h-6 w-6 items-center justify-center rounded-full border text-xs"
            title="Edit client full name"
          >
            ✏️
          </button>
        </div>
      ) : (
        <span className="text-sm">{label || "—"}</span>
      )}

      <Dialog open={open} onOpenChange={(v) => setOpen(v)}>
        <DialogHeader>
          <DialogTitle>Edit Client Full Name</DialogTitle>
        </DialogHeader>

        <DialogContent onKeyDown={onKeyDown}>
          <div className="mb-2 rounded-2xl border bg-white px-3 py-2 text-sm">
            <span className="text-gray-500 mr-2">Preview:</span>
            <span className="font-medium">{preview || "(empty)"}</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <Input ref={firstRef} placeholder="First name" value={first} onChange={(e) => setFirst(e.target.value)} />
            <Input placeholder="Middle name" value={middle} onChange={(e) => setMiddle(e.target.value)} />
            <Input placeholder="Last name" value={last} onChange={(e) => setLast(e.target.value)} />
            <Input placeholder="Suffix (e.g., Jr., Sr.)" value={suffix} onChange={(e) => setSuffix(e.target.value)} />
          </div>
          <p className="text-xs text-gray-500 pt-1">Tip: <kbd>Ctrl/⌘ + Enter</kbd> to apply.</p>
        </DialogContent>

        <DialogFooter>
          <Button className="border" onClick={() => setOpen(false)}>Cancel</Button>
          <Button onClick={applyChanges}>Apply</Button>
        </DialogFooter>
      </Dialog>
    </>
  );
}
