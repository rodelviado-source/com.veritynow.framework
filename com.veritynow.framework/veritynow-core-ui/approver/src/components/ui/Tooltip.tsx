import React, { useState, useId } from "react";

interface TooltipProps {
  content: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  placement?: "top" | "bottom" | "left" | "right";
}

/**
 * Ultra-light tooltip (hover/focus). No portals/deps.
 * Uses tailwind for styling. Handles keyboard focus + Escape.
 */
export const Tooltip: React.FC<TooltipProps> = ({
  content,
  children,
  className = "",
  placement = "top",
}) => {
  const [open, setOpen] = useState(false);
  const id = useId();

  // Placement classes
  const base =
    "absolute z-50 max-w-xs rounded-md border border-gray-200 bg-white px-2.5 py-1.5 text-xs shadow-lg text-gray-800";
  const arrow =
    "absolute w-2 h-2 rotate-45 bg-white border border-gray-200";
  const pos = {
    top:    "bottom-full left-1/2 -translate-x-1/2 mb-2",
    bottom: "top-full left-1/2 -translate-x-1/2 mt-2",
    left:   "right-full top-1/2 -translate-y-1/2 mr-2",
    right:  "left-full top-1/2 -translate-y-1/2 ml-2",
  }[placement];

  const arrowPos = {
    top:    "top-full left-1/2 -translate-x-1/2 -mt-[5px]",
    bottom: "bottom-full left-1/2 -translate-x-1/2 -mb-[5px]",
    left:   "left-full top-1/2 -translate-y-1/2 -ml-[5px]",
    right:  "right-full top-1/2 -translate-y-1/2 -mr-[5px]",
  }[placement];

  return (
    <span
      className={`relative inline-flex ${className}`}
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onFocus={() => setOpen(true)}
      onBlur={() => setOpen(false)}
      onKeyDown={(e) => e.key === "Escape" && setOpen(false)}
    >
      {/* Focusable wrapper for a11y */}
      <span tabIndex={0} aria-describedby={open ? id : undefined}>
        {children}
      </span>

      {open && (
        <div id={id} role="tooltip" className={`${base} ${pos}`}>
          {content}
          <span className={arrow + " " + arrowPos} aria-hidden />
        </div>
      )}
    </span>
  );
};
