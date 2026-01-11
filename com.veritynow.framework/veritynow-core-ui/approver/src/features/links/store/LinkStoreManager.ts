import { CreateRequirementLinkRequest, RequirementLinkDto } from "../index";

export interface StoreManager {
    append(req: CreateRequirementLinkRequest): Promise<RequirementLinkDto>;
    listByRecord(recordId: string): Promise<RequirementLinkDto[]>;
    listByKey(recordId: string, requirementKey: string): Promise<RequirementLinkDto[]>;
    removeByAsset(recordId: string, assetId: string): Promise<string[]>;
}