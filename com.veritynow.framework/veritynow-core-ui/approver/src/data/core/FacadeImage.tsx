import React from "react";
import { DataFacade } from "./DataFacade";

type Props = React.ImgHTMLAttributes<HTMLImageElement> & { imageId: string };

export function FacadeImage({ imageId, ...rest }: Props) {
  const [src, setSrc] = React.useState<string>("");

  React.useEffect(() => {
    let cancelled = false;
    let revoke: string | null = null;

    async function go() {
      const url = await DataFacade.imageUrl(imageId);
      if (cancelled) return;
      setSrc(url);
      if (url.startsWith("blob:")) revoke = url;
    }

    void go();
    return () => {
      cancelled = true;
      if (revoke) URL.revokeObjectURL(revoke);
    };
  }, [imageId]);

  if (!src) return null;
  return <img src={src} {...rest} />;
}

export default FacadeImage;
