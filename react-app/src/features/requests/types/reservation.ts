export type ReservationStatus = 'PENDING' | 'APPROVED';

export interface OrderDto {
  reserveId: number | null;
  orderId: string | null;
  materialId: number;
  requestQuantity: string | null;
  stockQuantity: number;
  materialName: string;
  description: string | null;
  status: string;
  internStatus: 'APROVADO' | 'REJEITADO' | 'COLETADO' | null;
}

export interface OrdersByCaseResponse {
  description: string;
  teamName: string | null;
  orders: OrderDto[];
}

export interface OrdersByCaseView extends OrdersByCaseResponse {
  reservations: Array<OrderDto & { uniqueId: string | number }>;
}

export interface OrderActionPayload {
  reserveId: number | null;
  order: {
    orderId: string | null;
    materialId: number;
    quantity: string | null;
  };
}

export interface ReplyPayload {
  approved: OrderActionPayload[];
  rejected: OrderActionPayload[];
}

export interface StockBalance {
  materialId: number;
  stockQuantity: number;
}

export interface LastStockUsage {
  reserveId: number | null;
  orderId: string | null;
  materialId: number;
  quantity: number;
}

export interface DepositByStockist {
  depositId: number;
  depositName: string;
  depositAddress: string | null;
  depositPhone: string | null;
}

export interface StockistModel {
  stockistId: number;
  userId: string;
  name: string;
  depositId: number;
  depositName: string;
  depositAddress: string | null;
  depositPhone: string | null;
  region: string | null;
}

export interface Deposit {
  idDeposit: number;
  depositName: string;
  depositAddress: string | null;
  depositPhone: string | null;
  isTruck: boolean;
}

export interface ReserveMaterialSelection {
  centralMaterialStockId: number | null;
  truckMaterialStockId: number | null;
  materialId: number | null;
  materialQuantity: string;
  truckStockControl: boolean;
}

export interface ReserveItemRequest {
  contractItemId: number;
  description: string;
  quantity: string;
  type: string;
  linking: string;
  currentBalance: string;
  contractReferenceItemId: number;
  truckStockControl: boolean;
  materials?: ReserveMaterialSelection[];
}

export interface ReserveRequest {
  preMeasurementId: number | null;
  directExecutionId: number | null;
  description: string;
  comment: string;
  assignedBy: string;
  teamId: number;
  teamName: string;
  teamNotificationCode: string;
  truckDepositName: string;
  reservationManagementId: number;
  items: ReserveItemRequest[];
}

export interface MaterialInStockDTO {
  materialStockId: number;
  materialId: number;
  materialName: string;
  depositName: string;
  stockAvailable: string;
  requestUnit: string;
  isTruck: boolean;
  plateVehicle: string | null;
  contractReferenceItemId: number;
  materialBrand: string | null;
}

export interface OrderHistoryItem {
  orderCode: string;
  materialName: string;
  teamName: string;
  quantityReleased: string;
  createdAt: string;
}

export interface TeamMemberContact {
  name: string;
  last_name: string;
  phone_number: string;
  team_id: number;
}
