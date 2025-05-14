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
      contractReferenceItemType: string;
      contractReferenceItemPower: string;
      contractReferenceItemLength: string;
      measuredQuantity: number;
      itemStatus: string;
    }[]

  }[];
}
