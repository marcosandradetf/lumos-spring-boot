export interface Deposit {
  idDeposit: number;
  depositName: string;
  depositAddress: string;
  depositDistrict: string;
  depositCity: string;
  depositState: string;
  depositRegion: string;
  depositPhone: string;
  isTruck: boolean;
  teamName: string;
  plateVehicle: string;
}

export interface DepositByStockist {
  depositId: number;
  depositName: string;
  depositAddress: string | null;
  depositPhone: string | null;
}

export interface Group {
  idGroup: number;
  groupName: string;
}

export interface MaterialType {
  idType: number;
  typeName: string;
  group: Group;
}

export interface MaterialTypeSubtype {
  typeId: number;
  typeName: string;
  subtypes: Array<{ subtypeId: number; subtypeName: string }>;
}

export interface Supplier {
  id: number;
  name: string;
}

export interface Stockist {
  stockistId: number | null;
  depositIdDeposit: number;
  userIdUser: string;
  notificationCode: string;
}

export interface StockistManagement extends Stockist {
  userName: string;
  userEmail: string;
  userRoles: string[];
  depositName: string;
  depositAddress: string | null;
  depositPhone: string | null;
  depositRegion: string | null;
}

export interface StockistModel {
  stockistId: number;
  userId: string;
  name: string;
  depositId: number;
  depositName: string;
}

export interface CreateStockistRequest {
  depositIdDeposit: number;
  userIdUser: string;
}

export interface UpdateStockistRequest {
  depositIdDeposit: number;
  userIdUser: string;
}

export interface StockMovementDTO {
  materialStockId: number;
  materialName: string;
  barcode: string | null;
  description: string;
  inputQuantity: string;
  buyUnit: string;
  requestUnit: string;
  quantityPackage: string;
  priceTotal: string;
  totalQuantity: string;
  hidden: boolean;
  invalid: boolean;
}

export interface StockMovementResponse {
  id: number;
  description: string | null;
  materialName: string;
  inputQuantity: string;
  buyUnit: string;
  requestUnit: string;
  pricePerItem: string;
  priceTotal: string;
  deposit: string;
  responsible: string;
  dateOf: string;
  totalQuantity: string;
  quantityPackage: string | null;
}

export interface MaterialFormDTO {
  materialId: number | null;
  materialBaseName: string;
  materialName: string;
  materialType: number | null;
  materialSubtype: number | null;
  materialFunction: string | null;
  materialModel: string | null;
  materialBrand: string | null;
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
  contractItems: unknown[];
}

export interface MaterialStockResponse {
  materialStockId: number;
  materialId: number;
  materialName: string;
  barcode: string | null;
  buyUnit: string;
  requestUnit: string;
  stockQuantity: number;
  depositName: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  last: boolean;
  number: number;
  size: number;
}

export interface UnitOption {
  code: string;
  truckStockControl?: boolean;
}

export interface UnitsResponse {
  buyUnits: UnitOption[];
  requestUnits: Array<UnitOption & { truckStockControl: boolean }>;
}
