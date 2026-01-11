import { RequirementLinkState } from "@/features/links/types";
export type RequirementLinkDto = {
  id: string; 
  recordId: string; 
  requirementKey: string; 
  assetId: string;
  state: RequirementLinkState; 
  label?: string; 
  notes?: string; 
  metadata?: Record<string, unknown>;
  createdAt: string; 
  createdBy?: string;
};


