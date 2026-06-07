export interface ListPreMeasurementRequest {
  preMeasurementId: number;
  contractId: number;
  city: string;
  preMeasurementType: string;
  step: number;
  completeName: string;
  createdAt: string;
  streetsSize: number;
  itemsSize: number;
}

export interface PreMeasurementStreet {
  preMeasurementStreetId: number;
  lastPower: string;
  latitude: number;
  longitude: number;
  address: string;
  status: string;
  items: PreMeasurementStreetItem[];
}

export interface PreMeasurementStreetItem {
  preMeasurementStreetItemId: number;
  contractItemId: number;
  contractReferenceItemName: string;
  contractReferenceItemType: string;
  measuredQuantity: number;
  balanceQuantity: number;
  differenceQuantity: number;
  itemStatus: string;
}

export interface PreMeasurementResponseDTO {
  preMeasurementId: number;
  contractId: number;
  city: string;
  preMeasurementType: string;
  totalPrice: string;
  status: string;
  step: number;
  completeName: string;
  createdAt: string;
  streets: PreMeasurementStreet[];
}

export interface CheckBalanceRequest {
  description: string;
  totalMeasured: string;
  totalBalance: string;
  totalContractedQuantity: string;
  totalQuantityExecuted: string;
  totalCurrentBalance: string;
}

export interface AvailableStockByStreet {
  streetId: number;
  materialsInStock: Array<{
    materialId: number;
    materialName: string;
    materialPower: string;
    materialAmp: string;
    materialLength: string;
    deposit: string;
    itemQuantity: number;
    availableQuantity: number;
  }>;
  materialsInTruck: Array<{
    materialId: number;
    materialName: string;
    materialPower: string;
    materialAmp: string;
    materialLength: string;
    deposit: string;
    itemQuantity: number;
    availableQuantity: number;
  }>;
}

export interface DelegateExecutionDTO {
  preMeasurementId: number;
  description: string;
  stockistId: string;
  stockistName: string;
  stockistPhone: string;
  stockistDepositName: string;
  stockistDepositAddress: string;
  preMeasurementStep: number;
  teamId: number;
  comment: string;
  street: Array<{
    preMeasurementStreetId: number;
    teamName: string;
    truckDepositName: string;
    prioritized: boolean;
  }>;
}

export const STATUS_MAP: Record<string, { label: string; apiValue: string }> = {
  pendente: { label: 'Aguardando Análise', apiValue: 'pending' },
  disponivel: { label: 'Disponível para OS', apiValue: 'available' },
  validando: { label: 'Em Validação', apiValue: 'validating' },
};
