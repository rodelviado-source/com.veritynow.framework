import * as React from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

import { 
  Button, CardContent, FormControl, Input, InputLabel, MenuItem, 
  Select, SelectChangeEvent, TableContainer, TextField 
} from "@mui/material";

import { Box, Stack, Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";

import ComposedNameInput from "@/features/demo/bdo/ComposedNameInput";

import { 
  ArrowCircleLeftRounded as ArrowLeftIcon, ArrowCircleRightRounded as ArrowRightIcon, 
  Edit as EditIcon, CloudUploadOutlined as CloudUploadIcon 
} from '@mui/icons-material';

import { SaveOutlined as SaveIcon } from '@mui/icons-material';
import { CancelOutlined as CancelIcon } from '@mui/icons-material';

import { useSearchAndSort, SortKey } from "@/features/demo/util/SearchAndSort";
import { DataFacade } from "@/core/facade/DataFacade";

import { DataModeSelect } from "@/core/control/DataModeSelect";
import { CircularIntegration } from "@/core/control/CirularIntegration";
import  { Statuses, StatusValues, type RecordItem ,PaginationDTO }  from "@/features/record/index";

import { NewRecord }  from "@/features/demo/bdo//NewRecord";
import { VisuallyHiddenInput } from "@/features/demo/util/VisuallyHiddenInput";
import PictureAsPdfTwoToneIcon  from '@mui/icons-material/PictureAsPdfTwoTone';
import EditRequirements, { DEFAULT_TEMPLATE } from "@/features/demo/bdo/EditRequirements";
import { Requirements } from "@/features/demo/bdo/Requirements";
import Gallery from "@/features/demo/util/Gallery";
//import PrettyText from "@/features/demo/util/PrettyText";
import { RequirementLinksClient } from "@/features/demo/bdo/RequirementLinksClient";
import { ListRecordsResponse } from "@/features/record/index";

export function RecordsTable() {

  const [page, setPage] = React.useState(0);
  const [size, setSize] = React.useState(10);

  const [editingId, setEditingId] = React.useState<string | null>(null);
  const [draft, setDraft] = React.useState<Partial<RecordItem>>({});
  const [viewImagesOf, setViewImagesOf] = React.useState<RecordItem | null>(null);
  const [uploadingId, setUploadingId] = React.useState<string | null>(null);
  const [openGallery, setOpenGallery] = React.useState(false);;
  const qc = useQueryClient();

  // facade-based query
  const { data, isLoading, isError, error, isFetching } = useQuery<ListRecordsResponse>({
    queryKey: ["records", page, size, DataFacade.getMode()],
    queryFn: async () => DataFacade.records.list({ page, size }),
  });

  const updateMutation = useMutation({
    mutationFn: async (payload: {
      id: string;
      clientFirstName?: string;
      clientMiddleName?: string;
      clientLastName?: string;
      clientSuffix?: string;
      requirements?: string;
      priority?: number;
      status?: Statuses;
      title?: string;
    }) => DataFacade.records.update(payload.id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["records"] });
      setEditingId(null);
      setDraft({});
    },
  });

  const uploadMutation = useMutation({
    mutationFn: async (p: { id: string; files: File[] }) => DataFacade.assets.upload(String(p.id), p.files),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["records"] }); },
  });

  const searchKeys: SortKey = [
    "clientFirstName",
    "clientMiddleName",
    "clientLastName",
    "clientSuffix",
    "requirements",
    "status",
    "agentFirstName",
    "agentMiddleName",
    "agentLastName",
  ];

  const {
    data: pageSorted,
    search,
    setSearch,
    sort,
    toggleSort,
  } = useSearchAndSort<RecordItem>(data?.items ?? [], searchKeys);

  const isSameFields = (a: string[], b: string[]) => a.length === b.length && a.every((v, i) => v === b[i]);
  const arrow = (key: SortKey) =>
    isSameFields(sort.fields, Array.isArray(key) ? key : [key]) ? (sort.dir === "asc" ? "↑" : "↓") : "";

  const total = data?.page.total ?? 0;
  const start = total === 0 ? 0 : page * size + 1;
  const end = Math.min((page + 1) * size, total);
  const pages = Math.ceil(total / size);

  function beginEdit(row: RecordItem) {
    setEditingId(row.id);
    setDraft({ requirements: row.requirements, priority: row.priority });
  }
  function cancelEdit() { setEditingId(null); setDraft({}); }
  function saveEdit() {
    if (editingId == null) return;
    const payload: Partial<RecordItem> = { id: editingId }
    if (draft.requirements !== undefined) payload.requirements = draft.requirements;
    if (draft.priority !== undefined) payload.priority = draft.priority;
    if (draft.status !== undefined) payload.status = draft.status;
    if (draft.clientFirstName !== undefined) payload.clientFirstName = draft.clientFirstName;
    if (draft.clientMiddleName !== undefined) payload.clientMiddleName = draft.clientMiddleName;
    if (draft.clientLastName !== undefined) payload.clientLastName = draft.clientLastName;
    if (draft.clientSuffix !== undefined) payload.clientSuffix = draft.clientSuffix;
    updateMutation.mutate(payload as RecordItem);
  }

  const formatName = (first?: string, middle?: string, last?: string, suffix?: string) =>
    [first, middle, last, suffix].filter(Boolean).join(" ");

  const [refreshKey, setRefreshKey] = React.useState(0);
  const refresh = () => setRefreshKey(prev => prev + 1);
  const uploadRef = React.useRef<HTMLInputElement>(null);
  const [linkLabel, setLinkLabel] = React.useState<string | null>(null);

  function uploadRequirement(label: string): boolean | null {
    if (label == null) return null;
    setLinkLabel(label);
   if (uploadRef && uploadRef.current) { uploadRef.current.click(); return true; }
    return null;
  }

  function onChangeFileUpload(r: RecordItem): void {
    if (!uploadRef || !uploadRef.current) return;

    const input = uploadRef.current;
    const files: File[] = input.files ? Array.from(input.files) : [];
    if (!files.length) return;

    setUploadingId(r.id);

    (async () => {
        try {
            for (const file of files) {
                const fd = new FormData();
                fd.append("file", file, file.name);

                // Upload asset
                const res = await DataFacade.fetch("/api/images", {
                    method: "POST",
                    body: fd,
                });
                const up = await res.json(); // { id, filename, contentType }

                // Link to requirement
                if (linkLabel) {
                    await RequirementLinksClient.create({
                        recordId: String(r.id),
                        requirementKey: linkLabel,
                        assetId: up.id,
                        state: "attached",
                        metadata: { primary: true },
                    });
                }
            }
        } finally {
            setUploadingId(null);
            input.value = "";
            setLinkLabel(null); // clear after linking
        }
    })();
}






  return (
    <Box sx={{ minWidth: 1000 }}>
      
      <Stack direction="row" justifyContent="flex-start" spacing={5}>

        <DataModeSelect onModeChange={refresh} />
        {!isLoading && !isFetching && !isError && (
          <React.Fragment>
            <NewRecord />
            <TextField
              variant="outlined"
              label="Search client, requirements, status, agent…"
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="w-70"
            />
          </React.Fragment>
        )}

        <CircularIntegration
          errMsg={error?.message}
          isError={isError}
          isLoading={isLoading}
          isFetching={isFetching}
          refresh={refresh}
        />

      </Stack>


      <CardContent>

        {!isLoading && !isError && (
          <div className="space-y-2">
            <TableContainer sx={{ maxHeight: 1000 }} >
              <Table key={refreshKey} stickyHeader aria-label="sticky table" >
                <TableHead>
                  <TableRow>
                    <TableCell onClick={() => toggleSort("id")} className="cursor-pointer">
                      <div className="font-bold whitespace-nowrap">ID {arrow("id")}</div>
                    </TableCell>
                    <TableCell onClick={() => toggleSort("priority")} className="cursor-pointer">
                      <div className="font-bold whitespace-nowrap">Priority {arrow("priority")}</div>
                    </TableCell>
                    <TableCell onClick={() => toggleSort(["clientLastName", "clientFirstName", "clientMiddleName"])} className="cursor-pointer">
                      <div className="font-bold whitespace-nowrap">
                        Client Name {arrow(["clientLastName", "clientFirstName", "clientMiddleName"])}
                      </div>
                    </TableCell>
                    <TableCell><div className="font-bold whitespace-nowrap">Requirements / Notes</div></TableCell>
                    <TableCell><div className="font-bold">Documents</div></TableCell>
                    <TableCell onClick={() => toggleSort("createdAt")} className="cursor-pointer">
                      <div className="font-bold whitespace-nowrap">Created {arrow("createdAt")}</div>
                    </TableCell>
                    <TableCell onClick={() => toggleSort("status")} className="cursor-pointer">
                      <div className="font-bold whitespace-nowrap">Status {arrow("status")}</div>
                    </TableCell>
                    <TableCell className="w-56 justify-center"><div className="font-bold">Actions</div></TableCell>
                   
        </TableRow>
                </TableHead>

                <TableBody>
                  {pageSorted.map((r) => {
                     

                    const isEditing = editingId === r.id;

                    const clientFirst = isEditing && draft.clientFirstName !== undefined ? (draft.clientFirstName as string) : r.clientFirstName;
                    const clientMiddle = isEditing && draft.clientMiddleName !== undefined ? (draft.clientMiddleName as string) : r.clientMiddleName;
                    const clientLast = isEditing && draft.clientLastName !== undefined ? (draft.clientLastName as string) : r.clientLastName;
                    const clientSuffix = isEditing && draft.clientSuffix !== undefined ? (draft.clientSuffix as string) : r.clientSuffix;
                    const clientLabel = formatName(clientFirst, clientMiddle, clientLast, clientSuffix);

                    return (
                      
                      /** {r.uploaded ? 'UPLOADED' : 'LOCAL'} color the row instead **/

                      <TableRow key={r.id} onDoubleClick={() => { beginEdit(r); }} >
                        
                        <TableCell>{r.id}</TableCell>
                        <TableCell>
                          {isEditing ? (
                            <Input
                              type="number"
                              value={draft.priority ?? 0}
                              onChange={(e) => setDraft((d) => ({ ...d, priority: Number(e.target.value) }))}
                            />
                          ) : (
                            r.priority
                          )}
                        </TableCell>
                        <TableCell>
                          <ComposedNameInput label={clientLabel} isEditing={isEditing} row={r} setDraft={setDraft} />
                        </TableCell>
                        <TableCell>
                          {isEditing ? (
                          <div>
                            <input ref={uploadRef} multiple type="file" hidden onChange={() =>onChangeFileUpload(r)}/>
                            <EditRequirements
                              defaultValue={DEFAULT_TEMPLATE}
                              value={r.requirements ?? JSON.stringify(DEFAULT_TEMPLATE)}
                              onChange={(value) => setDraft((d) => ({ ...d, requirements: value }))}
                              onUploadRequirement={uploadRequirement}
                            />
                            </div>
                          ) : (
                            <Requirements
                              requirement={r.requirements ?? JSON.stringify(DEFAULT_TEMPLATE)}
                              dense includeFulfilled tooltipPlacement="right"
                            />

                          )}
                        </TableCell>
                        <TableCell>
                          {isEditing && (
                            <Button
                              component="label"
                              role={undefined}
                              variant="outlined"
                              sx={{ alignContent: "flex-start" }}
                              tabIndex={-1}
                              startIcon={<CloudUploadIcon />}
                            >
                              {uploadingId === r.id ? "Uploading…" : "Upload"}
                              <VisuallyHiddenInput

                                type="file"
                                multiple
                                onChange={(e) => {
                                  const files = e.currentTarget.files ? Array.from(e.currentTarget.files) : [];
                                  if (files.length === 0) return;
                                  setUploadingId(r.id);
                                  uploadMutation.mutate(
                                    { id: r.id, files },
                                    {
                                      onSettled: () => {
                                        setUploadingId(null);
                                        e.currentTarget.value = "";
                                      },
                                    }
                                  );
                                }}

                              />
                            </Button>
                          )}
                          {!isEditing && (
                            r.assetIds?.length ? (
                              <div className="flex items-center gap-2 cursor-pointer" onClick={() => { setViewImagesOf(r); setOpenGallery(true) }}>
                                <PictureAsPdfTwoToneIcon />
                                <code className="text-xs bg-gray-100 rounded px-1 py-0.5">
                                  {r.assetIds.length}
                                </code>
                              </div>
                            ) : (
                              <span className="text-gray-400">none</span>
                            ))
                          }
                        </TableCell>
                        <TableCell>{new Date(r.createdAt).toLocaleString()}</TableCell>
                        <TableCell>
                          {editingId === r.id ? (
                            <FormControl sx={{ width: '18ch' }} variant="outlined">
                              <InputLabel htmlFor='status-select'>Status</InputLabel>
                              <Select
                                id="status-select"
                                value={draft.status ?? r.status ?? "NEW"}
                                label="Status"
                                onChange={(e: SelectChangeEvent) => setDraft((d) => ({ ...d, status: e.target.value as Statuses }))}>
                                {StatusValues.map((s) => (
                                  <MenuItem key={s} value={s}> {s.replaceAll("_", " ")}</MenuItem>
                                ))}

                              </Select>
                            </FormControl>
                          ) : (
                            <span className="px-2 py-0.5 rounded-2xl bg白 text-xs whitespace-nowrap">
                              {(r.status ?? "NEW").replaceAll("_", " ")}
                            </span>
                          )}
                        </TableCell>
                        <TableCell>
                          <Stack width={110} direction="column" spacing={1}>


                            {!isEditing && (

                              <Button title="Edit" variant="outlined" startIcon={<EditIcon />} onClick={() => beginEdit(r)} disabled={isEditing && editingId === r.id} >
                                Edit
                              </Button>

                            )}
                            {isEditing && (
                              <React.Fragment>
                                <Button title="Save" variant="outlined" startIcon={<SaveIcon />} onClick={saveEdit} disabled={!isEditing && editingId !== r.id} >
                                  Save
                                </Button>
                                <Button title="Cancel" variant="outlined" startIcon={<CancelIcon />} onClick={cancelEdit}  >
                                  Cancel
                                </Button>
                              </React.Fragment>
                            )}

                          </Stack>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                  <TableRow />
                </TableBody>
              </Table>
            </TableContainer>

            <div className="flex items-center justify-between text-sm">
              <div>Showing records {start}-{end} of {total}</div>
              <div className="flex items-center gap-2">
                <label className="mr-1">Records per page:</label>
                <select className="border rounded-2xl px-2 py-1" value={size} onChange={(e) => { setPage(0); setSize(Number(e.target.value)); }}>
                  {[5, 10, 20, 50].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
                <Button title="Previos page" startIcon={<ArrowLeftIcon />} variant="outlined" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page <= 0} >
                  Prev
                </Button>
                <div>{`Page ${page + 1} of ${pages}`}</div>
                <Button title="Next page" endIcon={<ArrowRightIcon />} variant="outlined" onClick={() => setPage((p) => (p + 1 < pages ? p + 1 : p))} disabled={page + 2 > pages}  >
                  Next
                </Button>
              </div>
            </div>
          </div>
        )}
      </CardContent>

      
      {
                 
        viewImagesOf && <Gallery open={openGallery} imageIds={viewImagesOf?.assetIds} onClose={()=>setOpenGallery(false)}/> 
       }
    </Box>
  );
}
export { RecordItem };

