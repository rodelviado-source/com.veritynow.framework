export type RequirementLinkState = "pending" | "attached" | "rejected" | "superseded";

export interface CreateRequirementLinkRequest {
  recordId: string;
  requirementKey: string;
  assetId: string;
  state?: RequirementLinkState;
  label?: string;
  notes?: string;
  metadata?: Record<string, unknown>;
}
