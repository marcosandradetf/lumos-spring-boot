import api from '@/core/auth/api';
import type { CompanyRequest, CompanyResponse } from '@/features/company/types';

const BASE = '/api/company';

export const companiesApi = {
  async getCompanies() {
    const { data } = await api.get<CompanyResponse[]>(BASE);
    return data;
  },

  async createCompany(company: CompanyRequest, logo: File) {
    const formData = new FormData();
    formData.append('logo', logo);
    formData.append('company', new Blob([JSON.stringify(company)], { type: 'application/json' }));

    const { data } = await api.post<number>(`${BASE}/v1/create`, formData);
    return data;
  },
};
