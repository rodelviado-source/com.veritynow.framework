import * as React from "react";
import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable";
import { cn } from "@/lib/utils";

type Props = {
  left: React.ReactNode;
  rightA: React.ReactNode;
  rightB: React.ReactNode;
  leftWidth?: number;
  minRightPct?: number;
  initialRightPct?: [number, number];
  persistKey?: string;
  className?: string;
};

export default function ThreePaneLayout({
  left,
  rightA,
  rightB,
  leftWidth = 280,
  minRightPct = 20,
  initialRightPct = [50, 50],
  persistKey,
  className,
}: Props) {
  const [layout, setLayout] = React.useState<number[] | undefined>(() => {
    if (!persistKey) return undefined;
    try {
      const raw = localStorage.getItem(persistKey);
      const parsed = raw ? JSON.parse(raw) : null;
      if (Array.isArray(parsed) && parsed.length === 2) return parsed as number[];
    } catch {}
    return undefined;
  });

  const handleLayout = (sizes: number[]) => {
    setLayout(sizes);
    if (persistKey) localStorage.setItem(persistKey, JSON.stringify(sizes));
  };

  const reset = () => handleLayout(initialRightPct);

  return (
    <div className={cn("relative flex h-full w-full min-h-0 min-w-0", className)}>
      {/* Left fixed panel */}
      <aside
        className="shrink-0 border-r bg-white/60 dark:bg-neutral-900/60"
        style={{ width: leftWidth }}
      >
        <div className="h-full overflow-auto">{left}</div>
      </aside>

      {/* Right resizable panels */}
      <div className="flex h-full min-w-0 flex-1">
        <ResizablePanelGroup
          direction="horizontal"
          {...(!layout && { initial: initialRightPct })}
          onLayout={handleLayout}
          className="min-w-0"
        >
          <ResizablePanel
            minSize={minRightPct}
            defaultSize={(layout?.[0] ?? initialRightPct[0]) as number}
            className="min-w-0"
          >
            <div className="h-full overflow-auto border-r bg-white dark:bg-neutral-900">
              {rightA}
            </div>
          </ResizablePanel>

          <ResizableHandle
            withHandle
            className="mx-0.5 bg-transparent data-[orientation=vertical]:w-2"
          />

          <ResizablePanel
            minSize={minRightPct}
            defaultSize={(layout?.[1] ?? initialRightPct[1]) as number}
            className="min-w-0"
          >
            <div className="h-full overflow-auto bg-white dark:bg-neutral-900">
              {rightB}
            </div>
          </ResizablePanel>
        </ResizablePanelGroup>
      </div>

 
    </div>
  );
}
