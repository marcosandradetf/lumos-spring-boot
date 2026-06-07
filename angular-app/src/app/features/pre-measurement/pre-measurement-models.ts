export interface PreMeasurementDTO {
  contractId: Number,
  streets: PreMeasurementStreetItemsDTO[],
}

export interface PreMeasurementStreetItemsDTO {
  street: PreMeasurementStreetDTO,
  items: PreMeasurementStreetItemDTO[]
}

export interface PreMeasurementStreetDTO {
  lastPower: string | null,
  latitude: number | null,
  longitude: number | null,
  street: string | null,
  number: string | null,
  neighborhood: string | null,
  city: string | null,
  state: string | null,
}

export interface PreMeasurementStreetItemDTO {
  itemContractId: Number,
  itemContractQuantity: Number,
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

  streets: {
    number: number;
    preMeasurementStreetId: number;
    lastPower: string;
    latitude: number;
    longitude: number;
    address: string;
    status: string;

    items: {
      preMeasurementStreetItemId: number;
      contractItemId: number;
      contractReferenceItemName: string;
      contractReferenceNameForImport: string;
      contractReferenceItemType: string;
      contractReferenceLinking: string;
      contractReferenceItemDependency: string;
      measuredQuantity: number;
      balanceQuantity: number;
      differenceQuantity: number;
      itemStatus: string;
    }[]

  }[];
}

export interface CheckBalanceRequest {
  description: string;
  totalMeasured: string;
  totalBalance: string;
  totalContractedQuantity: string;
  totalQuantityExecuted: string;
  totalCurrentBalance: string;
}

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
