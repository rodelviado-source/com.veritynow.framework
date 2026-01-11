import * as React from "react";
export default function ZoomControls({ onIn, onOut, onReset, scale }: { onIn: ()=>void; onOut: ()=>void; onReset: ()=>void; scale: number; }) {
  return (
    <div className="fixed bottom-4 right-4 bg-white/80 backdrop-blur rounded-xl shadow p-2 flex items-center gap-2 z-[60]">
      <button className="px-2 py-1 border rounded" onClick={onOut}>−</button>
      <div className="min-w-[60px] text-center text-sm">{(scale*100).toFixed(0)}%</div>
      <button className="px-2 py-1 border rounded" onClick={onIn}>+</button>
      <button className="ml-2 px-2 py-1 border rounded" onClick={onReset}>Reset</button>
    </div>
  );
}
