export interface StockistModel {
  userId: string,
  name: string,
  depositId: number,
  depositName: string,
  depositAddress: string | null,
  depositPhone: string | null,
  region: string | null,
}

export interface DepositByStockist {
  depositId: number,
  depositName: string,
  depositAddress: string | null,
  depositPhone: string | null,
}

export interface ReserveDTOResponse {
  description: string,
  streets: ReserveStreetDTOResponse[]
}

export interface ReserveStreetDTOResponse {
  preMeasurementStreetId: number | null,
  directExecutionId: number | null,
  streetName: string,
  latitude: number,
  longitude: number,
  prioritized: Boolean,
  comment: string,
  assignedBy: string,
  teamId: number;
  teamName: string,
  truckDepositName: string,
  items: ItemResponseDTO[]
}

export interface ItemResponseDTO {
  itemId: number,
  description: string,
  quantity: string,
  type: string,
  linking: string,

  materials: {
    centralMaterialStockId: number | null,
    truckMaterialStockId: number | null,
    materialId: number | null,
    materialQuantity: string,
  }[]
}

export interface MaterialInStockDTO {
  materialStockId: number,
  materialId: number,
  materialName: string,
  materialPower: string,
  materialLength: string,
  materialType: string,
  deposit: string,
  availableQuantity: string,
  requestUnit: string,
  isTruck: boolean,
  plateVehicle: string | null,
}

export interface DirectExecutionDTO {
  contractId: number,
  teamId: number,
  currentUserId: string,
  stockistId: string,
  instructions: string | null;
  items: {
    contractItemId: number,
    quantity: string,
  }[],
}
