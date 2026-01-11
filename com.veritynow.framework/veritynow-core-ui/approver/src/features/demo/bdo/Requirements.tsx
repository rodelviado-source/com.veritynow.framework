import React from "react";

export type RequirementsObj = Record<string, boolean | string>;
export interface RequirementPayload {
  requirements?: RequirementsObj;
  Notes?: string;
  notes?: string;
}

import { Tooltip } from "@mui/material";



interface RequirementsProps {
  requirement: string | "";
  listOnly?: boolean;
  includeFulfilled?: boolean;
  dense?: boolean;
  maxItems?: number;
  maxChars?: number;

  /** Show a hover/focus tooltip with the full expanded content */
  showTooltip?: boolean;
  tooltipPlacement?: "top" | "bottom" | "left" | "right";
}

export const Requirements: React.FC<RequirementsProps> = ({
  requirement,
  listOnly = true,
  includeFulfilled = false,
  dense = false,
  maxItems = 5,
  maxChars = 22,
  showTooltip = true,
  tooltipPlacement = "top",
}) => {
  const parsed = parseRequirement(requirement);
  if (parsed.type === "none") return <span className="text-gray-400">none</span>;

  if (listOnly) {
    const items: Array<{ icon: string; text: string; color: string; key: string }> = [];

    for (const f of parsed.failed) items.push({ icon: "❌", text: f, color: "text-red-600", key: `f-${f}` });

    if (includeFulfilled) {
      for (const m of parsed.met) items.push({ icon: "✅", text: m, color: "text-green-600", key: `m-${m}` });
    } else if (parsed.type === "fulfilled" && parsed.failed.length === 0 && parsed.met.length === 0) {
      items.push({ icon: "✅", text: "fulfilled", color: "text-green-600", key: "met-fulfilled" });
    }

    if (parsed.type === "note" && parsed.note) {
      items.push({ icon: "📝", text: parsed.note, color: "text-amber-600", key: "note" });
    }

    const sliced = items.slice(0, maxItems);
    const hasMore = items.length > sliced.length;

    const listEl = (
      <ul className={`${dense ? "space-y-0.5" : "space-y-1"} list-none m-0 p-0`}>
        {sliced.map((it) => (
          <li
            key={it.key}
            className={`flex items-start ${dense ? "text-[11px]" : "text-sm"} ${it.color}`}
            title={it.text}
          >
            <span className={`${dense ? "mr-1" : "mr-1.5"}`}>{it.icon}</span>
            <span className="truncate">{truncate(it.text, maxChars)}</span>
          </li>
        ))}
        {hasMore && <li className={`${dense ? "text-[11px]" : "text-sm"} text-gray-500`}>…</li>}
      </ul>
    );

    if (!showTooltip) return listEl;

    // Build full, expanded tooltip content
    const fullContent = (
      <div className="text-xs leading-5">
        {parsed.failed.length > 0 && (
          <div className="mb-1">
            {parsed.failed.map((f, i) => (
              <div key={`tf-${i}`} className="text-red-700">
                ❌ {f}
              </div>
            ))}
          </div>
        )}
        {includeFulfilled && parsed.met.length > 0 && (
          <div className="mb-1">
            {parsed.met.map((m, i) => (
              <div key={`tm-${i}`} className="text-green-700">
                ✅ {m}
              </div>
            ))}
          </div>
        )}
        {parsed.type === "note" && parsed.note && (
          <div className="text-amber-700">📝 {parsed.note}</div>
        )}
        {parsed.type === "fulfilled" && parsed.failed.length === 0 && parsed.met.length === 0 && (
          <div className="text-green-700">✅ fulfilled</div>
        )}
      </div>
    );

    return (
      <Tooltip placement={tooltipPlacement}  title={fullContent}>
        {listEl}
      </Tooltip>
    );
  }

  // fallback summary (non-listOnly path)
  const color =
    parsed.type === "failed" ? "text-red-600"
    : parsed.type === "note" ? "text-amber-600"
    : parsed.type === "fulfilled" ? "text-green-600"
    : "text-gray-400";

  return <pre className={`whitespace-pre-wrap font-medium ${color}`}>{parsed.display}</pre>;
};

// ...keep the same parser + helpers (parseRequirement, truncate, etc.)



/* ---------------- Parser ---------------- */

function parseRequirement(
  input: string | RequirementPayload
): {
  type: "none" | "failed" | "note" | "fulfilled";
  display: string;
  failed: string[];
  met: string[];
  note: string;
} {
  if (input == null) return base("none");

  let obj: RequirementPayload | null = null;
  if (typeof input === "string") {
    const s = input.trim();
    if (!s) return base("none");
    try {
      obj = JSON.parse(s) as RequirementPayload;
    } catch {
      return base("none");
    }
  } else if (typeof input === "object") {
    obj = input;
  } else {
    return base("none");
  }

  if (!obj || typeof obj !== "object") return base("none");

  const requirements = obj.requirements;
  const note = (obj.Notes ?? obj.notes ?? "").toString().trim();

  if (!requirements || typeof requirements !== "object" || Array.isArray(requirements)) {
    if (note) return { ...base("note"), display: `📝 ${note}`, note };
    return base("fulfilled", "✅ fulfilled", [], []);
  }

  const entries = Object.entries(requirements);
  if (entries.length === 0) {
    if (note) return { ...base("note"), display: `📝 ${note}`, note };
    return base("fulfilled", "✅ fulfilled");
  }

  const failed: string[] = [];
  const met: string[] = [];

  for (const [k, v] of entries) {
    (isFalseyFlag(v) ? failed : met).push(prettifyKey(k));
  }

  if (failed.length > 0) {
    return {
      type: "failed",
      display: `❌ Unmet Requirements:\n• ${failed.join("\n• ")}`,
      failed,
      met,
      note: "",
    };
  }

  if (note) return { type: "note", display: `📝 ${note}`, failed, met, note };

  return { type: "fulfilled", display: "✅ fulfilled", failed, met, note: "" };
}

function base(
  type: "none" | "failed" | "note" | "fulfilled",
  display = "none",
  failed: string[] = [],
  met: string[] = [],
  note = ""
) {
  return { type, display, failed, met, note };
}

function isFalseyFlag(v: unknown): boolean {
  if (typeof v === "boolean") return !v;
  if (typeof v === "string") return v.trim().toLowerCase() === "false";
  return false;
}

function prettifyKey(k: string): string {
  return k
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

/* ---------------- Utils ---------------- */

function truncate(text: string, max: number): string {
  if (text.length <= max) return text;
  return text.slice(0, Math.max(0, max - 1)).trimEnd() + "…";
}
