// import { RecordsTable } from "@/records/RecordsTable";
import Login from "@/auth/Login";
import { getAgent, clearAgent } from "@/auth/agent";
import * as React from "react";
import { Button } from "@/components/ui/button";
import BDODemo from "./records/BDODemo";

export default function App(){
  const [agent, setAgent] = React.useState(() => getAgent());
  if (!agent) return <Login onDone={() => setAgent(getAgent())} />;

  const name = [agent.first, agent.middle, agent.last, agent.suffix].filter(Boolean).join(" ");

  return (
    <div className="min-h-screen p-6 max-w-6xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">VerityNow Core API + UI DEMO</h1>
        <div className="text-sm flex items-center gap-3">
          <span className="px-3 py-1 rounded-2xl border bg-white shadow">
            Agent: <b>{agent.agentId}</b> {name && <span className="text-gray-600">({name})</span>}
          </span>
          <label className="border rounded-2xl px-3 py-2 cursor-pointer">
            Logout
            <Button className="hidden" onClick={()=>{ clearAgent(); location.reload(); }}/>
          </label>
        </div>
      </div>

      <BDODemo></BDODemo>

    {/* <RecordsTable/> */}
    </div>
  );
}
