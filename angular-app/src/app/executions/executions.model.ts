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
  items: ItemResponseDTO[]
}

export interface ItemResponseDTO {
  description: string,
  quantity: number,
  type: string,
  linking: string,
}
