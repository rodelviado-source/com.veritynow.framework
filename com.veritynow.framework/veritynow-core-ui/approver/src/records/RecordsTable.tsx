import * as React from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

import { Button, CardContent, TextField } from "@mui/material";
import { Box, Stack, Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
import { Input } from "@mui/material";
import ComposedNameInput from "@/records/ComposedNameInput";
import Record from "@/records/Record";
import ImageGallery from "@/records/ImageGallery";
import { LabeledButton } from "@/components/ui/LabeledButton";
import { Edit as EditIcon } from '@mui/icons-material';
import { Save as SaveIcon } from '@mui/icons-material';
import { Cancel as CancelIcon } from '@mui/icons-material';

import { useSearchAndSort, SortKey } from "@/data/util/SearchAndSort";
import { ImageFacade } from "@/data/facade/ImageFacade";
import { DataFacade } from "@/data/facade/DataFacade";

import { DataModeSelect } from "@/data/control/DataModeSelect";
import { CircularIntegration } from "@/data/control/CirularIntegration";


type Status = "NEW" | "IN_REVIEW" | "APPROVED" | "REJECTED" | "CLOSED";
const STATUSES: Status[] = ["NEW", "IN_REVIEW", "APPROVED", "REJECTED", "CLOSED"];
interface PageResult<T> { items: T[]; page: number; size: number; total: number }
export interface RecordItem {
  id: number;
  agentId: string;
  agentFirstName?: string; agentMiddleName?: string; agentLastName?: string; agentSuffix?: string;
  clientId: string;
  clientFirstName?: string; clientMiddleName?: string; clientLastName?: string; clientSuffix?: string;
  title: string;
  priority?: number;
  status?: Status;
  description?: string;
  createdAt: string;
  imageIds: string[];
}

export function RecordsTable() {
  const [page, setPage] = React.useState(0);
  const [size, setSize] = React.useState(10);

  const [editingId, setEditingId] = React.useState<number | null>(null);
  const [draft, setDraft] = React.useState<Partial<RecordItem>>({});
  const [viewImagesOf, setViewImagesOf] = React.useState<RecordItem | null>(null);
  const [uploadingId, setUploadingId] = React.useState<number | null>(null);
  const qc = useQueryClient();

  // facade-based query
  const { data, isLoading, isError, error, isFetching } = useQuery<PageResult<RecordItem>>({
    queryKey: ["records", page, size, DataFacade.getMode()],
    queryFn: async () => DataFacade.list({ page, size }),
  });

  const updateMutation = useMutation({
    mutationFn: async (payload: {
      id: number;
      clientFirstName?: string;
      clientMiddleName?: string;
      clientLastName?: string;
      clientSuffix?: string;
      description?: string;
      priority?: number;
      status?: Status;
      title?: string;
    }) => DataFacade.update(payload.id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["records"] });
      setEditingId(null);
      setDraft({});
    },
  });

  const uploadMutation = useMutation({
    mutationFn: async (p: { id: number; files: File[] }) => DataFacade.uploadImages(p.id, p.files),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["records"] }); },
  });

  const searchKeys: SortKey = [
    "clientFirstName",
    "clientMiddleName",
    "clientLastName",
    "clientSuffix",
    "description",
    "title",
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

  const total = data?.total ?? 0;
  const start = total === 0 ? 0 : page * size + 1;
  const end = Math.min((page + 1) * size, total);
  const pages = Math.ceil(total / size);

  function beginEdit(row: RecordItem) {
    setEditingId(row.id);
    setDraft({ description: row.description, priority: row.priority });
  }
  function cancelEdit() { setEditingId(null); setDraft({}); }
  function saveEdit() {
    if (editingId == null) return;
    const payload: RecordItem = { id: editingId }
    if (draft.title !== undefined) payload.title = draft.title;
    if (draft.description !== undefined) payload.description = draft.description;
    if (draft.priority !== undefined) payload.priority = draft.priority;
    if (draft.status !== undefined) payload.status = draft.status;
    if (draft.clientFirstName !== undefined) payload.clientFirstName = draft.clientFirstName;
    if (draft.clientMiddleName !== undefined) payload.clientMiddleName = draft.clientMiddleName;
    if (draft.clientLastName !== undefined) payload.clientLastName = draft.clientLastName;
    if (draft.clientSuffix !== undefined) payload.clientSuffix = draft.clientSuffix;
    updateMutation.mutate(payload);
  }

  const formatName = (first?: string, middle?: string, last?: string, suffix?: string) =>
    [first, middle, last, suffix].filter(Boolean).join(" ");

  const [refreshKey, setRefreshKey] = React.useState(0);
  const refresh = () => setRefreshKey(prev => prev + 1);

  return (
    <Box sx={{ minWidth:1100 }}>

      <Stack direction="row" justifyContent="flex-start" spacing={5}>

        <DataModeSelect onModeChange={refresh} />
        {!isLoading && !isFetching && !isError && (
        <React.Fragment>
        <Record />
        <TextField
          variant="outlined"
          label="Search client, description, title, agent…"
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
            <div className="overflow-x-auto">
              <Table key={refreshKey}   >
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
                    <TableCell><div className="font-bold whitespace-nowrap">Description / Notes</div></TableCell>
                    <TableCell><div className="font-bold">Images</div></TableCell>
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
                            <textarea
                              className="border rounded-2xl px-2 py-2 cursor-pointer whitespace-nowrap"
                              value={draft.description ?? ""}
                              onChange={(e) => setDraft((d) => ({ ...d, description: e.target.value }))}
                            />
                          ) : (
                            <span className="text-gray-600">{r.description}</span>
                          )}
                        </TableCell>
                        <TableCell>
                          {r.imageIds?.length ? (
                            <div className="flex items-center gap-2 cursor-pointer" onClick={() => setViewImagesOf(r)}>
                              <ImageFacade
                                imageId={r.imageIds[0]}
                                className="h-8 w-8 rounded border object-cover"
                                alt={r.imageIds[0]} />
                              <code className="text-xs bg-gray-100 rounded px-1 py-0.5">
                                {r.imageIds.length} image{r.imageIds.length > 1 ? "s" : ""}
                              </code>
                            </div>
                          ) : (
                            <span className="text-gray-400">none</span>
                          )}
                        </TableCell>
                        <TableCell>{new Date(r.createdAt).toLocaleString()}</TableCell>
                        <TableCell>
                          {editingId === r.id ? (
                            <select
                              className="border rounded-2xl px-2 py-1"
                              value={draft.status ?? r.status ?? "NEW"}
                              onChange={(e) => setDraft((d) => ({ ...d, status: e.target.value as Status }))}
                            >
                              {STATUSES.map((s) => (
                                <option key={s} value={s}>
                                  {s.replaceAll("_", " ")}
                                </option>
                              ))}
                            </select>
                          ) : (
                            <span className="px-2 py-0.5 rounded-2xl bg白 text-xs whitespace-nowrap">
                              {(r.status ?? "NEW").replaceAll("_", " ")}
                            </span>
                          )}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2 justify-center">
                            <label className="border rounded-2xl px-3 py-2 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800">
                              {uploadingId === r.id ? "Uploading…" : "Upload"}
                              <input
                                type="file"
                                accept="image/*"
                                multiple
                                className="hidden"
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
                            </label>
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

                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                  <TableRow />
                </TableBody>
              </Table>
            </div>

            <div className="flex items-center justify-between text-sm">
              <div>Showing records {start}-{end} of {total}</div>
              <div className="flex items-center gap-2">
                <label className="mr-1">Records per page:</label>
                <select className="border rounded-2xl px-2 py-1" value={size} onChange={(e) => { setPage(0); setSize(Number(e.target.value)); }}>
                  {[5, 10, 20, 50].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
                <LabeledButton label={page > 0 ? `${page} ← Prev` : "Prev"} onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page == 0} />
                <div>{`Page ${page + 1} of ${pages}`}</div>
                <LabeledButton label={page + 1 < pages ? `Next → ${page + 2}` : "Next"} onClick={() => setPage((p) => (p + 1 < pages ? p + 1 : p))} disabled={page + 2 > pages} />
              </div>
            </div>
          </div>
        )}
      </CardContent>

      {viewImagesOf && <ImageGallery viewImagesOf={viewImagesOf} setViewImagesOf={setViewImagesOf} />}
    </Box>
  );
}
