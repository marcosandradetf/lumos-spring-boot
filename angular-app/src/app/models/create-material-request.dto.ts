
export interface CreateMaterialRequest {
  materialName: string;
  materialBrand: string;

  materialPower: string;
  materialAmps: string;
  materialLength: string;

  buyUnit: string;
  requestUnit: string;
  materialType: string;
  inactive: boolean;
  allDeposits: boolean;
  deposit: string;
}
