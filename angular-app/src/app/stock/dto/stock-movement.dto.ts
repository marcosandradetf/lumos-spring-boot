export interface StockMovementDTO {
    materialStockId: number;
    materialName: string;
    barcode: string | null;
    description: string;
    inputQuantity: string;
    buyUnit: string;
    requestUnit: string;
    quantityPackage: string;
    priceTotal: string;
    totalQuantity: string;
    hidden: boolean;
    invalid: boolean;
}
