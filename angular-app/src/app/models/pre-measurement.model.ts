export interface PreMeasurementModel {
  preMeasurementId: number;
  contractId: number;
  city: string;
  createdBy: string;
  createdAt: string;
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
      materialId: number;
      contractItemId: number;
      materialName: string;
      materialType: string;
      materialPower: string;
      materialLength: string;
      materialQuantity: number;
      status: string;
    }[]

  }[];
}
