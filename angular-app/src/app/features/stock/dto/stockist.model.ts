export interface Stockist {
    stockistId: number | null;
    depositIdDeposit: number;
    userIdUser: string;
    notificationCode: string;
}

export interface StockistManagement extends Stockist {
    userName: string;
    userEmail: string;
    userRoles: string[];
    depositName: string;
    depositAddress: string | null;
    depositPhone: string | null;
    depositRegion: string | null;
}

export interface CreateStockistRequest {
    depositIdDeposit: number;
    userIdUser: string;
}

export interface UpdateStockistRequest {
    depositIdDeposit: number;
    userIdUser: string;
}
