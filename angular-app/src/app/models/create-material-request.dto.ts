
export interface CreateMaterialRequest {
  materialName: string;
  materialBrand: string;

  materialPower: string;
  materialAmps: string;
  materialLength: string;

  buyUnit: string;
  requestUnit: string;
  materialType: number;
  inactive: boolean;
  company: number;
  deposit: number;
}
