import * as React from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { getAgent } from "@/auth/agent";
import { Input } from "@/components/ui/input";
import { DataFacade } from "@/data/facade/DataFacade";
import { Button } from "@mui/material";
import { CancelOutlined as CancelIcon } from '@mui/icons-material'
import { CreateOutlined as CreateIcon } from '@mui/icons-material'

import { Statuses, StatusValues } from "@/data/types/Record";



export default function NewRecord() {
  const qc = useQueryClient();
  const agent = getAgent();
  const [open, setOpen] = React.useState(false);
  const [form, setForm] = React.useState({
    // agent defaults from "login"
    agentId: agent?.agentId || "",
    agentFirstName: agent?.first || "",
    agentMiddleName: agent?.middle || "",
    agentLastName: agent?.last || "",
    agentSuffix: agent?.suffix || "",
    // client
    clientId: "",
    clientFirstName: "",
    clientMiddleName: "",
    clientLastName: "",
    clientSuffix: "",
    // biz
    title: "",
    priority: 0,
    status: Statuses.NEW,
    description: ""
  });
  const [files, setFiles] = React.useState<File[]>([]);
  const [agentHilightOnOff, setAgentHilightOnOff] = React.useState("");
  const [clientHilightOnOf, setClientHilightOnOf] = React.useState("");
  const [errorMsg, setErrorMsg] = React.useState("hidden");

  const clientIdInputRef = React.useRef<HTMLInputElement | null>(null);
  const agentIdInputRef = React.useRef<HTMLInputElement | null>(null);

  React.useEffect(() => {
    if (open) {
      const agentCurrent = agentIdInputRef.current;
      const clientCurrent = clientIdInputRef.current;

      if (clientCurrent) clientCurrent.value = "";
      form.clientId = "";

      if (agentCurrent) agentCurrent.value = form.agentId;

      requestAnimationFrame(() => clientIdInputRef.current?.focus());
      requestAnimationFrame(() => agentIdInputRef.current?.focus());
    }
  }, [open, clientIdInputRef, agentIdInputRef]);

  const createMutation = useMutation({
    mutationFn: async () => {
      // create record via facade
      const rec = await DataFacade.create(form);
      // upload images if provided
      if (files.length) {
        await DataFacade.uploadImages(rec.id, files);
      }
      return rec;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["records"] });
      setOpen(false);
      setFiles([]);
    },
    onError: () => {
      setErrorMsg("bg-red-500 text-yellow-500 text-center text-bold border rounded-2xl px-2 py-2 mb-2");
    }
  });

  function handleCreate() {
    setErrorMsg("hidden");
    let focusSet = false;

    if (!form.agentId || form.agentId.trim().length <= 0) {
      setAgentHilightOnOff("border-red-500 text-red-500");
      requestAnimationFrame(() => agentIdInputRef.current?.focus());
      focusSet = true;
    }

    if (!form.clientId || form.clientId.trim().length <= 0) {
      setClientHilightOnOf("border-red-500 text-red-500");
      if (!focusSet) requestAnimationFrame(() => clientIdInputRef.current?.focus());
      focusSet = true;
    }

    if (!focusSet) createMutation.mutate();
  }

  function handleCancel() {
    setAgentHilightOnOff("");
    setClientHilightOnOf("");
    setErrorMsg("hidden");
    setOpen(false);
  }

  const clientCurrent = clientIdInputRef.current;

  return (
    <>
      <Button startIcon={<CreateIcon />} variant="outlined" onClick={() => setOpen(true)} >New Records</Button>
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setOpen(false)}>
          <div className="bg-white rounded-2xl p-6 w-[95%] max-w-3xl" onClick={e => e.stopPropagation()}>
            <h2 className="text-lg font-bold mb-3">New Loan Application</h2>

            <h2 className={errorMsg}>Record already exist</h2>

            <div className="space-y-3">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="whitespace-nowrap">
                  {form.agentId != null && form.agentId.trim().length > 0 ? "✅" : ""}
                  <Input
                    ref={agentIdInputRef}
                    className={agentHilightOnOff}
                    placeholder="Agent ID (required)"
                    value={form.agentId}
                    onChange={e => setForm({ ...form, agentId: e.target.value })}
                  />
                </div>
                <div className="whitespace-nowrap">
                  {clientCurrent?.value != null && clientCurrent?.value.trim().length > 0 ? "✅" : ""}
                  <Input
                    ref={clientIdInputRef}
                    className={clientHilightOnOf}
                    placeholder="Client ID (required)"
                    value={form.clientId}
                    onChange={e => setForm({ ...form, clientId: e.target.value })}
                  />
                </div>
              </div>

              <div className="font-bold">Personal Information</div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                <Input placeholder="Agent First" value={form.agentFirstName} onChange={e => setForm({ ...form, agentFirstName: e.target.value })} />
                <Input placeholder="Agent Middle" value={form.agentMiddleName} onChange={e => setForm({ ...form, agentMiddleName: e.target.value })} />
                <Input placeholder="Agent Last" value={form.agentLastName} onChange={e => setForm({ ...form, agentLastName: e.target.value })} />
                <Input placeholder="Agent Suffix" value={form.agentSuffix} onChange={e => setForm({ ...form, agentSuffix: e.target.value })} />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                <Input placeholder="Client First" value={form.clientFirstName} onChange={e => setForm({ ...form, clientFirstName: e.target.value })} />
                <Input placeholder="Client Middle" value={form.clientMiddleName} onChange={e => setForm({ ...form, clientMiddleName: e.target.value })} />
                <Input placeholder="Client Last" value={form.clientLastName} onChange={e => setForm({ ...form, clientLastName: e.target.value })} />
                <Input placeholder="Client Suffix" value={form.clientSuffix} onChange={e => setForm({ ...form, clientSuffix: e.target.value })} />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <Input placeholder="Title" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} />
                <Input placeholder="Priority" type="number" value={form.priority} onChange={e => setForm({ ...form, priority: Number(e.target.value) })} />
                <select className="border rounded-2xl px-2 py-1" value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
                  {StatusValues.map(s => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>

              <textarea
                className="border rounded-2xl p-3 w-full min-h-[100px]"
                placeholder="Description"
                value={form.description}
                onChange={e => setForm({ ...form, description: e.target.value })}
              />

              <label className="border rounded-2xl px-3 py-2 inline-block cursor-pointer">
                Choose Images
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  className="hidden"
                  onChange={e => setFiles(e.currentTarget.files ? Array.from(e.currentTarget.files) : [])}
                />
              </label>

              {files.length != null && files.length > 0 && (
                <div className="flex items-center gap-2 cursor-pointer" >
                  {files.map((e) => (
                    <div key={e.name}  className="group relative rounded border shadow overflow-hidden bg-white">
                      <img src={URL.createObjectURL(e)}  alt={e.name} className="block w-full h-35 w-20 object-cover transition-transform group-hover:scale-[1.02]"/>
                        <span className="absolute bottom-1 right-1 text-[10px] bg-black/60 text-white px-1.5 py-0.5 rounded">
                              {files[0].name}
                         </span>

                    </div>
                  ))}


                </div>
              )
              }
              <div className="flex items-center justify-end gap-2 pt-2">
                <Button title="Cancel changes" variant="outlined" startIcon={<CancelIcon />} onClick={handleCancel} >
                  Cancel
                </Button>
                <Button title="Create new record" variant="outlined" startIcon={<CreateIcon />} onClick={handleCreate} >
                  {createMutation.isPending ? "Creating…" : "Create"}
                </Button>


              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
