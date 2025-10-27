import * as React from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"

import { CardTitle, Card, CardContent, CardHeader } from "@/components/ui/card.tsx"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table.tsx"
import { Input } from "@/components/ui/input.tsx";
import ComposedNameInput from "@/records/ComposedNameInput"
import Record from "@/records/Record"
import ImageGallery from "@/records/ImageGallery"
import { LabeledButton } from "@/records/LabeledButton"
import { useSearchAndSort } from "../lib/SearchAndSort"            // ⬅️ your new hook


type Status = "NEW" | "IN_REVIEW" | "APPROVED" | "REJECTED" | "CLOSED"
const STATUSES: Status[] = ["NEW", "IN_REVIEW", "APPROVED", "REJECTED", "CLOSED"]
interface PageResult<T> { items: T[]; page: number; size: number; total: number }
const API_BASE = "http://localhost:8080"

export interface RecordItem {
  id: number
  agentId: string
  agentFirstName?: string; agentMiddleName?: string; agentLastName?: string; agentSuffix?: string
  clientId: string
  clientFirstName?: string; clientMiddleName?: string; clientLastName?: string; clientSuffix?: string
  title: string
  priority?: number
  status?: Status
  description?: string
  createdAt: string
  imageIds: string[]
}

export function RecordsTable() {
  // server paging stays as-is
  const [page, setPage] = React.useState(0)
  const [size, setSize] = React.useState(10)

  // local editing state (unchanged)
  const [editingId, setEditingId] = React.useState<number | null>(null)
  const [draft, setDraft] = React.useState<Partial<RecordItem>>({})
  const [viewImagesOf, setViewImagesOf] = React.useState<RecordItem | null>(null)
  const [uploadingId, setUploadingId] = React.useState<number | null>(null)
  const qc = useQueryClient()

  // fetch current page (no server-side search/sort here; the hook handles it client-side)
  const { data, isLoading, isError, error, isFetching } = useQuery<PageResult<RecordItem>>({
    queryKey: ["records", page, size],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), size: String(size) })
      const res = await fetch(`${API_BASE}/api/records?${params.toString()}`)
      if (!res.ok) throw new Error("Failed to fetch records")
      return res.json()
    },
  })

  const updateMutation = useMutation({
    mutationFn: async (payload: {
      id: number
      clientFirstName?: string
      clientMiddleName?: string
      clientLastName?: string
      clientSuffix?: string
      description?: string
      priority?: number
      status?: Status
      title?: string
    }) => {
      const res = await fetch(`${API_BASE}/api/records/${payload.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      })
      if (!res.ok) throw new Error("Update failed")
      return res.json()
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["records"] })
      setEditingId(null)
      setDraft({})
    },
  })

  const uploadMutation = useMutation({
    mutationFn: async (p: { id: number; files: File[] }) => {
      const form = new FormData()
      p.files.forEach((f) => form.append("files", f))
      const res = await fetch(`${API_BASE}/api/images/records/${p.id}`, { method: "POST", body: form })
      if (!res.ok) throw new Error("Upload failed")
      return res.json()
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["records"] }) },
  })

  // ─────────────────────────────────────────────────────────────
  // 🔎🔽 Plug in the new OR-search + composed-field sort hook
  // Search across client full name (composed), description, title, and agent name
  // You can add more fields (including dot-paths) anytime.
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
  ]

  const {
    data: pageSorted,     // this is the page items after OR-search + ranking + sort
    search,
    setSearch,
    sort,
    toggleSort,
  } = useSearchAndSort<RecordItem>(data?.items ?? [], searchKeys)

  // header arrow helper (works for single or composed keys)
  const isSameFields = (a: string[], b: string[]) =>
    a.length === b.length && a.every((v, i) => v === b[i])

  const arrow = (key: SortKey) =>
    isSameFields(sort.fields, Array.isArray(key) ? key : [key])
      ? sort.dir === "asc" ? "↑" : "↓"
      : ""

  // totals / paging UI based on server totals (unchanged)
  const total = data?.total ?? 0
  const start = total === 0 ? 0 : page * size + 1
  const end = Math.min((page + 1) * size, total)
  const pages = Math.ceil(total / size)

  function beginEdit(row: RecordItem) {
    setEditingId(row.id)
    setDraft({ description: row.description, priority: row.priority })
  }
  function cancelEdit() { setEditingId(null); setDraft({}) }
  function saveEdit() {
    if (editingId == null) return
    const payload: RecordItem = {
      id: editingId,
      title: draft.title,
      description: draft.description,
      priority: draft.priority as number,
      status: draft.status as Status,
    }
    if (draft.clientFirstName !== undefined) payload.clientFirstName = draft.clientFirstName
    if (draft.clientMiddleName !== undefined) payload.clientMiddleName = draft.clientMiddleName
    if (draft.clientLastName !== undefined) payload.clientLastName = draft.clientLastName
    if (draft.clientSuffix !== undefined) payload.clientSuffix = draft.clientSuffix
    updateMutation.mutate(payload)
  }

  const formatName = (first?: string, middle?: string, last?: string, suffix?: string) =>
    [first, middle, last, suffix].filter(Boolean).join(" ")

  return (
    <Card className="shadow-lg">
      <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <CardTitle>BDO Personal Loans</CardTitle>
        <div className="flex items-center gap-2">
          <Record />
          {/* hook-managed search string */}
          <Input  
            placeholder="Search client, description, title, agent…"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0) }}
            className="w-70 border rounded-2xl px-2 py-2 cursor-text whitespace-nowrap  text-sl text-left"
          />
          <div className="text-sm text-gray-600">
            {isFetching ? "Refreshing…" : "Up to date"}
          </div>
        </div>
      </CardHeader>

      <CardContent>
        {isLoading && <p>Loading records…</p>}
        {isError && <p className="text-red-600">Error: {(error as Error).message}</p>}

        {!isLoading && !isError && (
          <div className="space-y-3">
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow >
                    <TableHead onClick={() => toggleSort("id")} className="cursor-pointer">
                      <div className="font-bold whitespace-nowrap">
                        ID {arrow("id")}
                       </div> 
                    </TableHead>
                    <TableHead onClick={() => toggleSort("priority")} className="cursor-pointer">
                      <div className="font-bold whitespace-nowrap">
                          Priority {arrow("priority")}
                       </div>
                    </TableHead>

                    <TableHead 
                      onClick={() => toggleSort(["clientLastName", "clientFirstName", "clientMiddleName"])}
                      className="cursor-pointer"
                    >
                      <div className="font-bold whitespace-nowrap">
                        Client Name {arrow(["clientLastName", "clientFirstName", "clientMiddleName"])}
                      </div>
                    </TableHead>

                    <TableHead>
                      <div className="font-bold whitespace-nowrap">
                        Description / Notes
                      </div>
                     </TableHead>

                    

                    <TableHead>
                      <div className="font-bold">
                        Images
                      </div>
                    </TableHead>

                    <TableHead onClick={() => toggleSort("createdAt")} className="cursor-pointer">
                      <div className="font-bold whitespace-nowrap">
                      Created {arrow("createdAt")}
                      </div>
                    </TableHead>

                    <TableHead onClick={() => toggleSort("status")} className="cursor-pointer">
                      <div className="font-bold whitespace-nowrap">
                        Status {arrow("status")}
                      </div>
                    </TableHead>

                    <TableHead className="w-56 justify-center"><div className="font-bold">Actions</div></TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {pageSorted.map((r) => {
                    const isEditing = editingId === r.id

                    const clientFirst =
                      isEditing && draft.clientFirstName !== undefined ? (draft.clientFirstName as string) : r.clientFirstName
                    const clientMiddle =
                      isEditing && draft.clientMiddleName !== undefined ? (draft.clientMiddleName as string) : r.clientMiddleName
                    const clientLast =
                      isEditing && draft.clientLastName !== undefined ? (draft.clientLastName as string) : r.clientLastName
                    const clientSuffix =
                      isEditing && draft.clientSuffix !== undefined ? (draft.clientSuffix as string) : r.clientSuffix

                    const clientLabel = formatName(clientFirst, clientMiddle, clientLast, clientSuffix)

                    return (
                      <TableRow key={r.id} onDoubleClick={()=>{beginEdit(r)}}>
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
                          <ComposedNameInput 
                            label={clientLabel}
                            isEditing={isEditing}
                            row={r}
                            setDraft={setDraft}
                          />
                        </TableCell>

                        <TableCell>
                          {isEditing ? (
                            <textarea className="border rounded-2xl px-2 py-2 cursor-pointer whitespace-nowrap cursor-pointer"
                              value={draft.description ?? ""}
                              onChange={(e) => setDraft((d) => ({ ...d, description: e.target.value }))}
                            />
                          ) : (
                            <span className="text-gray-600">{r.description}</span>
                          )}
                        </TableCell>

                        

                        <TableCell>
                          {r.imageIds?.length ? (
                            <div
                              className="flex items-center gap-2 cursor-pointer"
                              onClick={() => setViewImagesOf(r)}
                            >
                              <img
                                src={`${API_BASE}/api/images/${r.imageIds[0]}?t=${Date.now()}`}
                                className="h-8 w-8 rounded border object-cover"
                              />
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
                              onChange={(e) =>
                                setDraft((d) => ({ ...d, status: e.target.value as Status }))
                              }
                            >
                              {STATUSES.map((s) => (
                                <option key={s} value={s}>
                                  {s.replaceAll("_", " ")}
                                </option>
                              ))}
                            </select>
                          ) : (
                            <span className="px-2 py-0.5 rounded-2xl bg-white text-xs whitespace-nowrap">
                              {(r.status ?? "NEW").replaceAll("_", " ")}
                            </span>
                          )}
                        </TableCell>

                        <TableCell>
                          <div className="flex items-center gap-2 justify-center">
                            <label className="border rounded-2xl px-3 py-2 cursor-pointer">
                              {uploadingId === r.id ? "Uploading…" : "Upload"}
                              <input
                                type="file"
                                accept="image/*"
                                multiple
                                className="hidden"
                                onChange={(e) => {
                                  const files = e.currentTarget.files ? Array.from(e.currentTarget.files) : []
                                  if (files.length === 0) return
                                  setUploadingId(r.id)
                                  uploadMutation.mutate(
                                    { id: r.id, files },
                                    {
                                      onSettled: () => {
                                        setUploadingId(null)
                                        e.currentTarget.value = ""
                                      },
                                    }
                                  )
                                }}
                              />
                            </label>

                            {!isEditing ? (
                              <LabeledButton label="Edit" onClick={() => beginEdit(r)} />
                            ) : (
                              <>
                                <LabeledButton label="Save" onClick={saveEdit} />
                                <LabeledButton label="Cancel" onClick={cancelEdit} />
                              </>
                            )}
                          </div>
                        </TableCell>
                        
                      </TableRow>
                    )
                  })}
                  <TableRow/>
                </TableBody>
              </Table>
            </div>

            <div className="flex items-center justify-between text-sm">
              <div>
                Showing records {start}-{end} of {total}
              </div>
              <div className="flex items-center gap-2">
                <label className="mr-1">Records per page:</label>
                <select
                  className="border rounded-2xl px-2 py-1"
                  value={size}
                  onChange={(e) => { setPage(0); setSize(Number(e.target.value)) }}
                >
                  {[5, 10, 20, 50].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>

                <LabeledButton
                  label={page > 0 ? `${page} ← Prev` : "Prev"}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                />
                <div>{`Page ${page + 1} of ${pages}`}</div>
                <LabeledButton
                  label={page + 1 < pages ? `Next → ${page + 2}` : "Next"}
                  onClick={() => setPage((p) => (p + 1 < pages ? p + 1 : p))}
                  disabled={ page + 2  > pages}
                />
              </div>
            </div>
          </div>
        )}
      </CardContent>

      {viewImagesOf && (
        <ImageGallery viewImagesOf={viewImagesOf} setViewImagesOf={setViewImagesOf} />
      )}
    </Card>
  )
}
