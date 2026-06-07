export interface OrdersByCaseResponse {
  description: string,
  teamName: string | null,
  orders: OrderDto[]
}

export interface OrderDto {
  reserveId: number | null,
  orderId: string | null,

  materialId: number,

  requestQuantity: string | null,
  stockQuantity: number,
  materialName: string,
  description: string | null,
  status: string,
  internStatus: string | null,
}
