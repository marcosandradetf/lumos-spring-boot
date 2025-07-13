export interface ReservationsByCaseDtoResponse {
  description: string,
  teamName: string | null,
  reservations: ReservationDtoResponse[]
}

export interface ReservationDtoResponse {
  reserveId: number,
  materialId: number,
  orderId: string,
  reserveQuantity: number,
  stockQuantity: number | null,
  materialName: string,
  description: string | null,
  status: string,
  internStatus: string | null,
}
