import * as React from "react";
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, TextField, FormGroup, FormControlLabel, Checkbox,
  IconButton, Stack, Box
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import EditIcon from "@mui/icons-material/Edit";
import { Delete as DeleteIcon } from "@mui/icons-material";
import DriveFileRenameOutlineIcon from "@mui/icons-material/DriveFileRenameOutline";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";

type RequirementsMap = Record<string, boolean>;

type Structured = {
  requirements: RequirementsMap;
  Notes: string;
};

type MergeMode = "additive" | "replace";

type Props = {
  value: string;
  onChange: (nextJsonString: string) => void;
  defaultTemplate?: Structured;
  title?: string;
  label?: string;
  disabled?: boolean;
  onUploadRequirement?: (label: string) => boolean | Promise<boolean> | null;

  /** Default values to apply when opening the editor */
  defaultValue?: Structured;

  /**
   * How to apply defaultValue.
   * - "additive" (default): merge defaults into current (current wins), and if Notes is empty use default Notes.
   * - "replace": replace current with default entirely.
   */
  defaultMergeMode?: MergeMode;
};

export const DEFAULT_TEMPLATE: Structured = {
  requirements: {},
  Notes: "",
};

function safeParse(value: string, fallback: Structured): Structured {
  try {
    if (!value?.trim()) return fallback;
    const parsed = JSON.parse(value);
    const reqsInput = parsed?.requirements ?? {};
    const reqs: RequirementsMap = {};
    for (const k of Object.keys(reqsInput)) {
      const v = (reqsInput)[k];
      if (typeof v === "boolean") reqs[k] = v;
      else if (typeof v === "string") reqs[k] = v.toLowerCase() === "true";
      else reqs[k] = Boolean(v);
    }
    const notes = typeof parsed?.Notes === "string" ? parsed.Notes : "";
    return { requirements: reqs, Notes: notes };
  } catch {
    return fallback;
  }
}

function toJsonString(s: Structured): string {
  const out = {
    requirements: Object.fromEntries(
      Object.entries(s.requirements).map(([k, v]) => [k, v ? "true" : "false"])
    ),
    Notes: s.Notes ?? "",
  };
  return JSON.stringify(out, null, 2);
}

export function EditRequirements({
  value,
  onChange,
  defaultTemplate = DEFAULT_TEMPLATE,
  title = "Edit Requirements",
  label = "Details (JSON)",
  disabled,
  onUploadRequirement,
  defaultValue,
  defaultMergeMode = "additive",
}: Props) {
  const [open, setOpen] = React.useState(false);
  const [text, setText] = React.useState(value ?? "");

  const [model, setModel] = React.useState<Structured>(() =>
    safeParse(value, defaultTemplate)
  );
  const [newReqName, setNewReqName] = React.useState("");

  // Inline rename
  const [editingKey, setEditingKey] = React.useState<string | null>(null);
  const [editingValue, setEditingValue] = React.useState<string>("");

  // Confirmations
  const [confirmDelete, setConfirmDelete] = React.useState<string | null>(null);
  const [confirmUpload, setConfirmUpload] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (open) setText(value);
  }, [value, open]);

  const applyDefaults = (parsed: Structured): Structured => {
    if (!defaultValue) return parsed;

    if (defaultMergeMode === "replace") {
      // Full replacement
      return {
        requirements: { ...(defaultValue.requirements || {}) },
        Notes: defaultValue.Notes ?? "",
      };
    }

    // Additive: defaults fill missing keys; parsed values win on overlap
    const mergedReqs: RequirementsMap = {
      ...(defaultValue.requirements || {}),
      ...(parsed.requirements || {}),
    };

    const mergedNotes =
      parsed.Notes && parsed.Notes.trim().length > 0
        ? parsed.Notes
        : (defaultValue.Notes ?? "");

    return {
      requirements: mergedReqs,
      Notes: mergedNotes,
    };
  };

  const openDialog = () => {
    // Parse fresh from current value, then apply defaultValue per mode
    const parsed = safeParse(value, defaultTemplate);
    const withDefaults = applyDefaults(parsed);

    setModel(withDefaults);
    setNewReqName("");
    setEditingKey(null);
    setEditingValue("");
    setOpen(true);
  };

  const handleDeleteRequirement = (key: string) => setConfirmDelete(key);
  const confirmDeleteAction = () => {
    if (!confirmDelete) return;
    setModel((prev) => {
      const next = { ...prev.requirements };
      delete next[confirmDelete];
      return { ...prev, requirements: next };
    });
    setConfirmDelete(null);
  };

  const handleToggle = (key: string) => {
    setModel((prev) => ({
      ...prev,
      requirements: { ...prev.requirements, [key]: !prev.requirements[key] },
    }));
  };

  const handleAddRequirement = () => {
    const key = newReqName.trim();
    if (!key) return;
    setModel((prev) => {
      if (prev.requirements[key] !== undefined) return prev; // ignore dup
      return { ...prev, requirements: { ...prev.requirements, [key]: true } };
    });
    setNewReqName("");
  };

  const handleNotesChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const v = e.target.value;
    setModel((prev) => ({ ...prev, Notes: v }));
  };

  const handleApply = () => {
    const sorted: Structured = {
      Notes: model.Notes,
      requirements: Object.fromEntries(
        Object.entries(model.requirements).sort(([a], [b]) =>
          a.localeCompare(b)
        )
      ),
    };
    const nextString = toJsonString(sorted);
    onChange(nextString);
    setText(nextString);
    setOpen(false);
  };

  const handleCancel = () => setOpen(false);

  // Inline rename handlers
  const beginRename = (key: string) => {
    setEditingKey(key);
    setEditingValue(key);
  };
  const commitRename = () => {
    if (!editingKey) return;
    const nextKey = editingValue.trim();
    if (!nextKey || nextKey === editingKey) {
      setEditingKey(null);
      return;
    }
    setModel((prev) => {
      if (prev.requirements[nextKey] !== undefined) return prev; // duplicate guard
      const nextReqs = { ...prev.requirements };
      const val = nextReqs[editingKey];
      delete nextReqs[editingKey];
      nextReqs[nextKey] = val;
      return { ...prev, requirements: nextReqs };
    });
    setEditingKey(null);
  };
  const cancelRename = () => {
    setEditingKey(null);
    setEditingValue("");
  };

  // Upload handlers
  const handleUpload = (key: string) => setConfirmUpload(key);

  const confirmUploadAction = async () => {
    if (confirmUpload && onUploadRequirement) {
      const result = await onUploadRequirement(confirmUpload);
      if (typeof result === "boolean") {
        setModel((prev) => ({
          ...prev,
          requirements: {
            ...prev.requirements,
            [confirmUpload]: result,
          },
        }));
      }
    }
    setConfirmUpload(null);
  };

  return (
    <>
      <Stack direction="row" spacing={1} alignItems="flex-start" onClick={openDialog}>
        <TextField
          hidden
          label={label}
          value={text}
          onChange={(e) => setText(e.target.value)}
          fullWidth
          multiline
          minRows={6}
          disabled={disabled}
          placeholder={`{\n  "requirements": { "requirement_one": "true" },\n  "Notes": ""\n}`}
        />
        <Button
          variant="outlined"
          onClick={openDialog}
          disabled={disabled}
          sx={{ alignSelf: "stretch" }}
          endIcon={<EditIcon />}
        >
          Requirements
        </Button>
      </Stack>

      {/* Main Editor */}
      <Dialog open={open} onClose={handleCancel} fullWidth maxWidth="sm">
        <DialogTitle>{title}</DialogTitle>
        <DialogContent dividers>
          <FormGroup sx={{ mt: 1, mb: 2 }}>
            {Object.keys(model.requirements).length === 0 && (
              <div style={{ opacity: 0.7, fontStyle: "italic", marginBottom: 8 }}>
                No requirements yet — add one below.
              </div>
            )}
            {Object.entries(model.requirements).map(([key, val]) => {
              const isEditing = editingKey === key;
              return (
                <Stack
                  key={key}
                  direction="row"
                  alignItems="center"
                  spacing={1}
                  sx={{ mb: 0.5 }}
                  onDoubleClick={() => beginRename(key)}
                >
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={!!val}
                        onChange={() => handleToggle(key)}
                      />
                    }
                    label={
                      isEditing ? (
                        <Box sx={{ display: "flex", alignItems: "center", gap: 1, width: "100%" }}>
                          <TextField
                            autoFocus
                            size="small"
                            value={editingValue}
                            onChange={(e) => setEditingValue(e.target.value)}
                            onBlur={cancelRename}
                            onKeyDown={(e) => {
                              if (e.key === "Enter") commitRename();
                              if (e.key === "Escape") cancelRename();
                            }}
                            placeholder="key"
                            sx={{ flex: 1 }}
                          />
                          <Button
                            variant="outlined"
                            size="small"
                            onMouseDown={(e) => e.preventDefault()}
                            onClick={commitRename}
                          >
                            Save
                          </Button>
                        </Box>
                      ) : (
                        key
                      )
                    }
                    sx={{ flex: 1, m: 0 }}
                  />

                  {!isEditing && (
                    <IconButton
                      aria-label={`Rename ${key}`}
                      size="small"
                      onClick={() => beginRename(key)}
                      title="Rename"
                      edge="end"
                    >
                      <DriveFileRenameOutlineIcon fontSize="small" />
                    </IconButton>
                  )}

                  <IconButton
                    aria-label={`Upload for ${key}`}
                    size="small"
                    onClick={() => handleUpload(key)}
                    title="Upload for this requirement"
                    edge="end"
                    disabled={disabled}
                  >
                    <CloudUploadIcon fontSize="small" />
                  </IconButton>

                  <IconButton
                    aria-label={`Delete ${key}`}
                    size="small"
                    onClick={() => handleDeleteRequirement(key)}
                    title="Delete requirement"
                    edge="end"
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Stack>
              );
            })}
          </FormGroup>

          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
            <TextField
              label="New requirement name"
              value={newReqName}
              onChange={(e) => setNewReqName(e.target.value)}
              fullWidth
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  handleAddRequirement();
                }
              }}
            />
            <IconButton color="primary" onClick={handleAddRequirement} title="Add requirement">
              <AddIcon />
            </IconButton>
          </Stack>

          <TextField
            label="Notes"
            value={model.Notes}
            onChange={handleNotesChange}
            fullWidth
            multiline
            minRows={4}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCancel} variant="outlined">Cancel</Button>
          <Button onClick={handleApply} variant="outlined">Apply</Button>
        </DialogActions>
      </Dialog>

      {/* Confirmation */}
      <Dialog
        open={!!confirmDelete || !!confirmUpload}
        onClose={() => {
          setConfirmDelete(null);
          setConfirmUpload(null);
        }}
      >
        <DialogTitle>
          {confirmDelete
            ? "Delete Requirement"
            : confirmUpload
            ? "Upload Requirement"
            : ""}
        </DialogTitle>
        <DialogContent dividers>
          {confirmDelete && (
            <div>
              Are you sure you want to delete the requirement{" "}
              <strong>{confirmDelete}</strong>?
            </div>
          )}
          {confirmUpload && (
            <div>
              Upload for requirement <strong>{confirmUpload}</strong>?
            </div>
          )}
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setConfirmDelete(null);
              setConfirmUpload(null);
            }}
          >
            Cancel
          </Button>
          {confirmDelete && (
            <Button color="error" onClick={confirmDeleteAction}>
              Delete
            </Button>
          )}
          {confirmUpload && (
            <Button color="primary" onClick={confirmUploadAction}>
              Upload
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </>
  );
}

export default EditRequirements;
