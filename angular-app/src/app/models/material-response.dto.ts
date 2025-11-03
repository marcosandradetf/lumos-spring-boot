
export interface MaterialResponse {
  selected: boolean;
  idMaterial: number;
  materialName: string;
  materialBrand: string;

  materialPower: string;
  materialAmps: string;
  materialLength: string;

  buyUnit: string;
  requestUnit: string;
  materialType: string;
  materialGroup: string;
  stockQt: number | null;
  inactive: boolean;
  deposit: string;
}
