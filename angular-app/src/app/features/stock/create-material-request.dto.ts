
export interface CreateMaterialRequest {
  materialName: string;
  materialBrand: string;
  buyUnit: string;
  requestUnit: string;
  materialType: number;
  stockQt: number | null;
  inactive: boolean;
  company: number;
  deposit: number;

}
