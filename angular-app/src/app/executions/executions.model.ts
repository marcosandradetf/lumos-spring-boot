export interface DelegateDTO {
  description: string,
  stockistId: string,
  stockistName: string,
  preMeasurementId: number,
  preMeasurementStep: number,
  teamId: number,
}

export interface StockistModel {
  userId: string,
  name: string,
  depositName: string,
  depositAddress: string,
  depositPhone: string,
  region: string,
}
