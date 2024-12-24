
export interface MaterialResponse {
  selected: boolean;
  idMaterial: number;
  materialName: string;
  materialBrand: string;
  buyUnit: string;
  requestUnit: string;
  materialType: string;
  materialGroup: string;
  stockQt: number | null;
  inactive: boolean;
  company: string;
  deposit: string;
}
