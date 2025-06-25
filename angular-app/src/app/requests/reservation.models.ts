export interface ReservationsByCaseDtoResponse {
 description: string,
 reservations: ReservationDtoResponse[]
}

export interface ReservationDtoResponse {
  reserveId: number,
  reserveQuantity: number,
  materialName: string,
  description: string | null,
  teamId: number | null,
  teamName: string| null
}
