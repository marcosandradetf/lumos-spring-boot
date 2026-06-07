import api from '@/core/auth/api';
import type {
  MaterialFormDTO,
  MaterialStockResponse,
  PagedResponse,
} from '../types/types';

export const materialApi = {
  async getMaterials(page: number, size: number, depositId: number) {
    const { data } = await api.get<PagedResponse<MaterialStockResponse>>(
      '/api/material/find-stock-by-deposit-id',
      { params: { page, size, depositId } },
    );
    return data;
  },

  async getBySearch(page: number, size: number, depositId: number, name: string) {
    const { data } = await api.get<PagedResponse<MaterialStockResponse>>(
      '/api/material/find-stock-by-search',
      { params: { page, size, depositId, name } },
    );
    return data;
  },

  async getCatalogue(generic = false) {
    const { data } = await api.get<MaterialFormDTO[]>('/api/material/get-catalogue', {
      params: { generic },
    });
    return data;
  },

  async findByBarcode(barcode: string) {
    const { data } = await api.get<MaterialFormDTO>(
      `/api/material/find-by-barcode?barcode=${encodeURIComponent(barcode)}`,
    );
    return data;
  },

  async findByBarcodeAndDeposit(barcode: string, depositId: number) {
    const { data } = await api.get<MaterialStockResponse>(
      '/api/material/find-by-barcode-and-deposit-id',
      { params: { barcode, depositId } },
    );
    return data;
  },

  async findById(materialId: string) {
    const { data } = await api.get<MaterialFormDTO>(
      `/api/material/find-by-id?materialId=${encodeURIComponent(materialId)}`,
    );
    return data;
  },

  async getBrands() {
    const { data } = await api.get<Array<{ brandId: number; brandName: string }>>('/api/material/get-brands');
    return data;
  },

  async create(material: MaterialFormDTO) {
    const { data } = await api.post<MaterialFormDTO>('/api/material/create', material);
    return data;
  },

  async importData(materials: Array<Record<string, string>>) {
    await api.post('/api/material/import', materials);
  },
};
