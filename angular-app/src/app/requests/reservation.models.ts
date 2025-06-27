export interface ReservationsByCaseDtoResponse {
  description: string,
  teamName: string | null,
  reservations: ReservationDtoResponse[]
}

export interface ReservationDtoResponse {
  reserveId: number,
  reserveQuantity: number,
  stockQuantity: number,
  materialName: string,
  description: string | null,
  status: string,
  internStatus: string | null,
}
