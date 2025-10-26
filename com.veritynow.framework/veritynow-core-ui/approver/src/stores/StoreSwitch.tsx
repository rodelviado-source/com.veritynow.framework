import * as React from "react";
import { Button } from "@/components/ui/button";
import { useStore } from "./useStore";

export function StoreSwitch() {
  const { mode, toggle } = useStore();
  return (
	<div>
	<div>Just an ordinary text</div>
    <Button
      variant="secondary"
      className="border"
      onClick={toggle}
      title="Switch data store"
    >
      {mode === "embedded" ? "Embedded (offline)" : "Remote (API)"}
    </Button>
	</div>
  );
}
