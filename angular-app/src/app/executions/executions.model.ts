export interface StockistModel {
  userId: string,
  name: string,
  depositName: string,
  depositAddress: string,
  depositPhone: string,
  region: string,
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
  quantity: number,
  type: string,
  linking: string,

  materials: {
    centralMaterialStockId: number | null,
    truckMaterialStockId: number | null,
    materialId: number | null,
    materialQuantity: number,
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
  availableQuantity: number,
  requestUnit: string,
}

export interface DirectExecutionDTO {
  contractId: number,
  teamId: number,
  stockistId: string,
  currentUserUUID: string,
  instructions: string | null;
  items: {
    contractItemId: number,
    quantity: number,
  }[],
}
