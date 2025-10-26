import * as React from "react";
import { Panel, PanelGroup, PanelResizeHandle } from "react-resizable-panels";

export const ResizablePanelGroup = PanelGroup;
export const ResizablePanel = Panel;

export const ResizableHandle = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement> & { withHandle?: boolean }
>(({ className = "", withHandle = false, ...props }, ref) => (
  <PanelResizeHandle
    ref={ref}
    className={`relative flex w-px bg-neutral-200 transition-colors hover:bg-neutral-300 focus-visible:bg-neutral-400 dark:bg-neutral-700 dark:hover:bg-neutral-600 ${withHandle ? "cursor-col-resize" : ""} ${className}`}
    {...props}
  >
    {withHandle && (
      <div className="absolute left-1/2 top-1/2 h-6 w-1 -translate-x-1/2 -translate-y-1/2 rounded bg-neutral-300 dark:bg-neutral-500" />
    )}
  </PanelResizeHandle>
));
ResizableHandle.displayName = "ResizableHandle";
