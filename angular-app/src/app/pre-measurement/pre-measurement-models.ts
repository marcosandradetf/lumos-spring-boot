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
  depositName: String;
  preMeasurementType: string;
  preMeasurementStyle: string;
  teamName: string;
  totalPrice: string;
  status: string;
  step: number;

  streets: {
    number: number;
    preMeasurementStreetId: number;
    lastPower: string;
    latitude: number;
    longitude: number;
    street: string;
    hood: string;
    city: string;
    status: string;
    createdBy: string;
    createdAt: string;

    items: {
      preMeasurementStreetItemId: number;
      contractItemId: number;
      contractReferenceItemName: string;
      contractReferenceNameForImport: string;
      contractReferenceItemType: string;
      contractReferenceLinking: string;
      contractReferenceItemDependency: string;
      measuredQuantity: number;
      itemStatus: string;
    }[]

  }[];
}
