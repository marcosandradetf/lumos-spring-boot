export interface StockMovementResponse {
    id: number;
    description: string | null;
    materialName: string;
    inputQuantity: string;
    buyUnit: string;
    requestUnit: string;
    pricePerItem: string;
    priceTotal: string;
    deposit: string;
    responsible: string;
    dateOf: string;
    totalQuantity: string;
    quantityPackage: string | null;
}
