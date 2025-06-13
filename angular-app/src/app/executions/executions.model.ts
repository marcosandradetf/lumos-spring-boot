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
  preMeasurementStreetId: number,
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
    materialId: number,
    materialQuantity: number,
  }[]
}

export interface MaterialInStockDTO{
   materialId: number,
   materialName: string,
   materialPower: string,
   materialLength: string,
   materialType: string,
   deposit: string,
   availableQuantity: number,
   requestUnit: string,
}

export interface executionWithoutPreMeasurement {
  contractId: number,
  teamId: number,
  items: {
    contractReferenceItemId: number,
  }[],
}
