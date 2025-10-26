import React, { forwardRef, useEffect, useImperativeHandle, useRef } from "react";

export type KV = Record<string, string | number | boolean | Array<string | number | boolean>>;

export type IframeFormHandle = {
  setValues: (values: KV) => Promise<void>;
  getValues: () => Promise<KV>;
  getDocument: () => Document | null;
};

type Props = {
  src: string;
  matchAttrs?: Array<"name" | "id" | "data-field-name" | "data-field">;
  className?: string;
  style?: React.CSSProperties;
  onReady?: (doc: Document) => void;
};

const READY_STATES = new Set(["interactive", "complete"]);

export const IframeForm = forwardRef<IframeFormHandle, Props>(
  ({ src, matchAttrs = ["name", "id", "data-field-name", "data-field"], className, style, onReady }, ref) => {
    const iframeRef = useRef<HTMLIFrameElement | null>(null);

    const getDoc = (): Document | null =>
      iframeRef.current?.contentDocument ?? iframeRef.current?.contentWindow?.document ?? null;

    const waitDocReady = async (doc: Document) =>
      new Promise<void>((resolve) => {
        if (READY_STATES.has(doc.readyState)) return resolve();
        const onDone = () => resolve();
        doc.addEventListener("DOMContentLoaded", onDone, { once: true });
        (doc.defaultView ?? window).addEventListener("load", onDone, { once: true });
      });

    const dispatch = (el: Element, type: string) =>
      el.dispatchEvent(new Event(type, { bubbles: true }));

    const keyFor = (el: Element): string | null => {
      for (const a of matchAttrs) {
        const v = el.getAttribute(a);
        if (v) return v;
      }
      return null;
    };

    const setElValue = (el: Element, v: unknown) => {
      const tag = el.tagName.toLowerCase();
      if (tag === "input") {
        const input = el as HTMLInputElement;
        const t = (input.type || "text").toLowerCase();
        if (t === "checkbox") {
          if (Array.isArray(v)) input.checked = v.map(String).includes(input.value);
          else if (typeof v === "boolean") input.checked = v;
          else input.checked = String(v) === input.value || String(v) === "true";
          dispatch(input, "input"); dispatch(input, "change"); return;
        }
        if (t === "radio") {
          input.checked = String(v) === input.value;
          if (input.checked) { dispatch(input, "input"); dispatch(input, "change"); }
          return;
        }
        input.value = v == null ? "" : String(v);
        dispatch(input, "input"); dispatch(input, "change");
      } else if (tag === "textarea") {
        const ta = el as HTMLTextAreaElement;
        ta.value = v == null ? "" : String(v);
        dispatch(ta, "input"); dispatch(ta, "change");
      } else if (tag === "select") {
        const sel = el as HTMLSelectElement;
        if (sel.multiple && Array.isArray(v)) {
          const set = new Set(v.map(String));
          Array.from(sel.options).forEach((o) => (o.selected = set.has(o.value)));
        } else sel.value = v == null ? "" : String(v);
        dispatch(sel, "input"); dispatch(sel, "change");
      }
    };

    const setValues = async (values: KV) => {
      const doc = getDoc(); if (!doc) return; await waitDocReady(doc);
      const fields = doc.querySelectorAll("input, textarea, select");
      const map = new Map<string, Element[]>();
      fields.forEach((el) => {
        const key = keyFor(el); if (!key) return;
        (map.get(key) ?? (map.set(key, []), map.get(key)!)).push(el);
      });
      for (const [k, v] of Object.entries(values)) {
        const elems = map.get(k); if (!elems) continue;
        const first = elems[0] as HTMLElement;
        if (first.tagName.toLowerCase() === "input" && (first as HTMLInputElement).type === "radio") {
          const name = (first as HTMLInputElement).name || k;
          const radios = doc.querySelectorAll(`input[type='radio'][name='${CSS.escape(name)}']`);
          radios.forEach((r) => setElValue(r, v)); continue;
        }
        if (first.tagName.toLowerCase() === "input" && (first as HTMLInputElement).type === "checkbox") {
          const name = (first as HTMLInputElement).name;
          if (name) {
            const checkboxes = doc.querySelectorAll(`input[type='checkbox'][name='${CSS.escape(name)}']`);
            checkboxes.forEach((cb) => setElValue(cb, v)); continue;
          }
        }
        elems.forEach((el) => setElValue(el, v));
      }
    };

    const getValues = async (): Promise<KV> => {
      const doc = getDoc(); const out: KV = {}; if (!doc) return out; await waitDocReady(doc);
      const fields = doc.querySelectorAll("input, textarea, select");
      const groups = new Map<string, Element[]>();
      fields.forEach((el) => {
        const key = keyFor(el); if (!key) return;
        (groups.get(key) ?? (groups.set(key, []), groups.get(key)!)).push(el);
      });
      for (const [key, elems] of groups.entries()) {
        const el = elems[0];
        if (el.tagName.toLowerCase() === "select") {
          const sel = el as HTMLSelectElement;
          out[key] = sel.multiple ? Array.from(sel.selectedOptions).map((o) => o.value) : sel.value;
        } else if (el.tagName.toLowerCase() === "textarea") {
          out[key] = (el as HTMLTextAreaElement).value;
        } else if ((el as HTMLInputElement).type === "checkbox") {
          const cbs = elems as HTMLInputElement[];
          out[key] = cbs.length > 1 ? cbs.filter((cb) => cb.checked).map((cb) => cb.value) : cbs[0].checked;
        } else if ((el as HTMLInputElement).type === "radio") {
          const checked = (elems as HTMLInputElement[]).find((e) => e.checked);
          out[key] = checked ? checked.value : "";
        } else out[key] = (el as HTMLInputElement).value;
      }
      return out;
    };

    useEffect(() => {
      const frame = iframeRef.current;
      if (!frame) return;
      const onLoad = async () => { const doc = getDoc(); if (!doc) return; await waitDocReady(doc); onReady?.(doc); };
      frame.addEventListener("load", onLoad);
      return () => frame.removeEventListener("load", onLoad);
    }, [onReady]);

    useImperativeHandle(ref, () => ({ setValues, getValues, getDocument: getDoc }));
    return <iframe ref={iframeRef} src={src} className={className} style={style} />;
  }
);
IframeForm.displayName = "IframeForm";
