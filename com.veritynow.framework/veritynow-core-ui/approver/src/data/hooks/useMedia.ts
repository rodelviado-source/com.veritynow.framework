import * as React from "react";
import { fetchMeta, getMetaSync, subscribe } from "@/data/mediaStore";
import type { MediaResource } from "@/data/transports/Transport";

export function useMedia(id: string, eager = true): MediaResource | null {
  const [meta, setMeta] = React.useState<MediaResource | null>(() => getMetaSync(id));

  React.useEffect(() => {
    let cancel = false;
    const unsub = subscribe(id, () => {
      if (!cancel) setMeta(getMetaSync(id));
    });
    if (eager && !meta) {
      fetchMeta(id)
        .then((r) => {
          if (!cancel) setMeta(r);
        })
        .catch(console.error);
    }
    return () => {
      cancel = true;
      unsub();
    };
  }, [id]); // keep deps minimal

  return meta;
}
