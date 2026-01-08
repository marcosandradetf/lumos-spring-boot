export interface MaterialFormDTO {
  materialId: number | null;
  materialBaseName: string;
  materialName: string;
  materialType: number | null;
  materialSubtype: number | null;
  materialFunction: number | null;
  materialModel: string | null;
  materialBrand: number | null;
  materialAmps: number | null;
  materialLength: number | null;
  materialWidth: number | null;
  materialPower: number | null;
  materialGauge: number | null;
  materialWeight: number | null;
  barcode: string;
  inactive: boolean;
  buyUnit: string;
  requestUnit: string;
  truckStockControl: string;
  contractItems: any[];
}

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
