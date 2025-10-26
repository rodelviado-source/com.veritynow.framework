# Three Pane Layout (Tailwind + shadcn-style)

A minimal React + Vite + Tailwind project that implements:
- 1 fixed-width **left** panel
- 2 **resizable** right panels with a draggable handle
- shadcn-style API via a local `@/components/ui/resizable` wrapper (no generator required)

## Quick start

```bash
npm install
npm run dev
```

Open http://localhost:5173

## Notes

- We use `react-resizable-panels` under the hood via `src/components/ui/resizable.tsx`, which mimics the shadcn/ui import path. If you later move to shadcn’s CLI, you can replace this file with the generated one.
- Tailwind is pre-configured. Adjust styles in `tailwind.config.ts` and `src/index.css`.

## Files of interest

- `src/components/ThreePaneLayout.tsx` — the layout component
- `src/components/ui/resizable.tsx` — resizable primitives (handle/panels)
- `src/App.tsx` — demo usage
