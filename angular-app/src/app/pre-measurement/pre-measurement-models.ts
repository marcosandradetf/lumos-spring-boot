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
