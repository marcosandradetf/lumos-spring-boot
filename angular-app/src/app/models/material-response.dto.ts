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
  truckStockControl: boolean;
  contractItems: any[];
}

export interface MaterialStockResponse {
  materialStockId: number;
  materialName: string;
  barcode: string;
  buyUnit: string;
  requestUnit: string;
  stockQt: number;
  depositName: string;
}
