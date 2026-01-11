import * as React from "react";
import { Input } from "@mui/material";
import { Button } from "@mui/material";
import { saveAgent } from "./agent";

export default function Login({ onDone }: { onDone: () => void }) {
  const [form, setForm] = React.useState({
    agentId: "", first: "", middle: "", last: "", suffix: ""
  });
  function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.agentId.trim()) return;
    saveAgent(form);
    onDone();
  }
  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <form onSubmit={submit} className="w-full max-w-md space-y-3 border rounded-2xl p-6 bg-white shadow">
        <h1 className="text-2xl font-bold">Agent Login</h1>
        <Input placeholder="Agent ID (required)" value={form.agentId}
               onChange={e=>setForm({...form, agentId: e.target.value})} required />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <Input placeholder="First name" value={form.first} onChange={e=>setForm({...form, first: e.target.value})}/>
          <Input placeholder="Middle name" value={form.middle} onChange={e=>setForm({...form, middle: e.target.value})}/>
          <Input placeholder="Last name" value={form.last} onChange={e=>setForm({...form, last: e.target.value})}/>
          <Input placeholder="Suffix" value={form.suffix} onChange={e=>setForm({...form, suffix: e.target.value})}/>
        </div>
        
          <Button type="submit" className="w-full border rounded-2xl px-2 py-2 cursor-pointer whitespace-nowrap cursor-pointer">Continue</Button>
        
      </form>
    </div>
  );
}
