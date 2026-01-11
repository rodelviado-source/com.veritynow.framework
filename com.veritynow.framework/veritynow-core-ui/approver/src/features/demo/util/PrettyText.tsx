import * as React from "react";
export default function PrettyText({
  value,
  dense = false,
  asList,
  prefix,
  className = "",
}: {
  value: string | string[];
  dense?: boolean;
  asList?: boolean;
  prefix?: string;
  className?: string;
}) {
  const lines = React.useMemo(() => {
    if (Array.isArray(value)) return value.filter(Boolean);
    if (typeof value === "string") {
      return value.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
    }
    return [];
  }, [value]);

  const isList = asList ?? lines.length > 1;
  const base = dense ? "text-xs leading-4" : "text-sm leading-5";

  if (!lines.length) return <span className={`${base} text-gray-400 ${className}`}>—</span>;

  if (!isList) return <span className={`${base} ${className}`}>{lines[0]}</span>;

  return (
    <ul className={`${base} ${className} list-disc ml-4 space-y-0.5`}>
      {lines.map((l, i) => (
        <li key={i}>
          {prefix ? <span className="opacity-70 mr-1">{prefix}</span> : null}
          {l}
        </li>
      ))}
    </ul>
  );
}
