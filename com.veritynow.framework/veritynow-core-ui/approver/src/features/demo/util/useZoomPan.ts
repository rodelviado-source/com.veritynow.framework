import * as React from "react";
export function useZoomPan(opts: { min?: number; max?: number; step?: number } = {}) {
  const { min = 0.25, max = 6, step = 0.1 } = opts;
  const [scale, setScale] = React.useState(1);
  const [offset, setOffset] = React.useState({ x: 0, y: 0 });
  const containerRef = React.useRef<HTMLDivElement | null>(null);
  const isPanning = React.useRef(false);
  const last = React.useRef({ x: 0, y: 0 });
  const clamp = (v: number) => Math.min(max, Math.max(min, v));
  const setScaleAround = React.useCallback((nextScale: number, cx: number, cy: number) => {
    nextScale = clamp(nextScale);
    const c = containerRef.current;
    if (!c) { setScale(nextScale); return; }
    const rect = c.getBoundingClientRect();
    const dx = (cx - rect.left - offset.x) / scale;
    const dy = (cy - rect.top - offset.y) / scale;
    const nx = cx - rect.left - dx * nextScale;
    const ny = cy - rect.top - dy * nextScale;
    setScale(nextScale);
    setOffset({ x: nx, y: ny });
  }, [scale, offset.x, offset.y]);
  const onWheel = React.useCallback((e: WheelEvent) => {
    if (!(e.ctrlKey || e.metaKey)) return;
    e.preventDefault();
    const delta = e.deltaY < 0 ? step : -step;
    const next = scale + delta;
    setScaleAround(next, e.clientX, e.clientY);
  }, [scale, step, setScaleAround]);
  const onMouseDown = React.useCallback((e: MouseEvent) => {
    if (!(e.button === 1 || e.buttons === 4 || e.getModifierState("Space"))) return;
    e.preventDefault();
    isPanning.current = true;
    last.current = { x: e.clientX, y: e.clientY };
  }, []);
  const onMouseMove = React.useCallback((e: MouseEvent) => {
    if (!isPanning.current) return;
    e.preventDefault();
    const dx = e.clientX - last.current.x;
    const dy = e.clientY - last.current.y;
    last.current = { x: e.clientX, y: e.clientY };
    setOffset(o => ({ x: o.x + dx, y: o.y + dy }));
  }, []);
  const onMouseUp = React.useCallback(() => { isPanning.current = false; }, []);
  React.useEffect(() => {
    const c = containerRef.current;
    if (!c) return;
    c.addEventListener("wheel", onWheel, { passive: false });
    c.addEventListener("mousedown", onMouseDown);
    window.addEventListener("mousemove", onMouseMove);
    window.addEventListener("mouseup", onMouseUp);
    return () => {
      c.removeEventListener("wheel", onWheel as any);
      c.removeEventListener("mousedown", onMouseDown as any);
      window.removeEventListener("mousemove", onMouseMove as any);
      window.removeEventListener("mouseup", onMouseUp as any);
    };
  }, [onWheel, onMouseDown, onMouseMove, onMouseUp]);
  const zoomIn = React.useCallback((cx?: number, cy?: number) => {
    const rect = containerRef.current?.getBoundingClientRect();
    setScaleAround(scale + step, cx ?? (rect?.left ?? 0) + (rect?.width ?? 0)/2, cy ?? (rect?.top ?? 0) + (rect?.height ?? 0)/2);
  }, [scale, step, setScaleAround]);
  const zoomOut = React.useCallback((cx?: number, cy?: number) => {
    const rect = containerRef.current?.getBoundingClientRect();
    setScaleAround(scale - step, cx ?? (rect?.left ?? 0) + (rect?.width ?? 0)/2, cy ?? (rect?.top ?? 0) + (rect?.height ?? 0)/2);
  }, [scale, step, setScaleAround]);
  const reset = React.useCallback(() => { setScale(1); setOffset({ x: 0, y: 0 }); }, []);
  const style: React.CSSProperties = { transform: `translate(${offset.x}px, ${offset.y}px) scale(${scale})`, transformOrigin: "0 0", willChange: "transform" };
  return { containerRef, style, scale, offset, zoomIn, zoomOut, reset };
}
export default useZoomPan;
