
export class CreateMaterialRequest {
  materialName: string;
  materialBrand: string;
  buyUnit: string;
  requestUnit: string;
  materialType: number;
  stockQt: number | null;
  inactive: boolean;
  company: number;
  deposit: number;

  constructor() {
    this.materialName = '';
    this.materialBrand = '';
    this.buyUnit = '';
    this.requestUnit = '';
    this.materialType = 0;
    this.stockQt = null;
    this.inactive = false;
    this.company = 0;
    this.deposit = 0;
  }
}
