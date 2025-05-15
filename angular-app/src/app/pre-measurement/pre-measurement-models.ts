export interface PreMeasurementDTO {
  contractId: Number,
  streets: PreMeasurementStreetItemsDTO[],
}

export interface PreMeasurementStreetItemsDTO {
  street: PreMeasurementStreetDTO,
  items: PreMeasurementStreetItemDTO[]
}

export interface PreMeasurementStreetDTO {
  lastPower: string,
  latitude: string,
  longitude: string,
  street: string,
  number: string,
  neighborhood: string,
  city: string,
  state: string,
}

export interface PreMeasurementStreetItemDTO {
  itemContractId: Number,
  itemContractQuantity: Number,
}

export interface PreMeasurementResponseDTO {
  preMeasurementId: number;
  contractId: number;
  city: string;
  createdBy: string;
  createdAt: string;
  depositName: String;
  preMeasurementType: string;
  preMeasurementStyle: string;
  teamName: string;
  totalPrice: string;
  status: string;

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
