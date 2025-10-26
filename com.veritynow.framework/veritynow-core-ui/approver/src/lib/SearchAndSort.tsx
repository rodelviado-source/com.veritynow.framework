import { useMemo, useState } from "react"

/* ────────────────────────────────
   Types
────────────────────────────────── */
export type SortDir = "asc" | "desc"
export type SortKey = string | string[]
export type SortState = { fields: string[]; dir: SortDir }

/* ────────────────────────────────
   Helpers
────────────────────────────────── */
const toArray = (k: SortKey) => (Array.isArray(k) ? k : [k])

export const get = (obj: string, path: string) =>
  path.split(".").reduce((o, k) => (o == null ? undefined : o[k]), obj)

const normalize = (v: string) =>
  (v ?? "")
    .toString()
    .normalize("NFKD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()

const tokenize = (q: string) =>
  normalize(q)
    .trim()
    .split(/\s+/)
    .filter(Boolean)

/* ────────────────────────────────
   Search (OR mode)
────────────────────────────────── */
export function makeSearchPredicate<T>(keys: SortKey, query: string) {
  const fields = toArray(keys)
  const tokens = tokenize(query)
  if (tokens.length === 0) return () => true

  // OR across tokens and fields
  return (row: T) =>
    tokens.some((tk) =>
      fields.some((f) => normalize(get(row as string, f)).includes(tk))
    )
}

/* Optional ranking (more token hits = higher score) */
export function makeSearchRanker<T>(keys: SortKey, query: string) {
  const fields = toArray(keys)
  const tokens = tokenize(query)
  if (tokens.length === 0) return () => 0

  return (row: T) => {
    let score = 0
    for (const f of fields) {
      const val = normalize(get(row as string, f))
      for (const tk of tokens) if (val.includes(tk)) score++
    }
    return score
  }
}

/* ────────────────────────────────
   Sorting
────────────────────────────────── */
const cmp = (a: string, b: string) => {
  if (a == null && b == null) return 0
  if (a == null) return -1
  if (b == null) return 1
  if (typeof a === "string" && typeof b === "string")
    return a.localeCompare(b, undefined, { numeric: true, sensitivity: "base" })
  return a < b ? -1 : a > b ? 1 : 0
}

export function makeComparator<T>({ fields, dir }: SortState) {
  return (x: T, y: T) => {
    for (const f of fields) {
      const c = cmp(get(x as string, f), get(y as string, f))
      if (c !== 0) return dir === "asc" ? c : -c
    }
    return 0
  }
}

/* ────────────────────────────────
   Hook: useSearchAndSort
────────────────────────────────── */
export function useSearchAndSort<T>(rows: T[], searchKeys: SortKey) {
  const [search, setSearch] = useState("")
  const [sort, setSort] = useState<SortState>({
    fields: toArray(searchKeys),
    dir: "asc",
  })

  function toggleSort(key: SortKey) {
    const fields = toArray(key)
    setSort((s) =>
      s.fields.length === fields.length &&
      s.fields.every((v, i) => v === fields[i])
        ? { fields, dir: s.dir === "asc" ? "desc" : "asc" }
        : { fields, dir: "asc" }
    )
  }

  const filtered = useMemo(() => {
    const pred = makeSearchPredicate<T>(searchKeys, search)
    return rows.filter(pred)
  }, [rows, search, searchKeys])

  const ranked = useMemo(() => {
    const rank = makeSearchRanker<T>(searchKeys, search)
    return [...filtered].sort((a, b) => rank(b) - rank(a))
  }, [filtered, search, searchKeys])

  const sorted = useMemo(() => {
    const comp = makeComparator<T>(sort)
    return [...ranked].sort(comp)
  }, [ranked, sort])

  return { data: sorted, search, setSearch, sort, toggleSort }
}
