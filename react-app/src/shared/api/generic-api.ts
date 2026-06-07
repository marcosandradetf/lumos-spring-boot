import api from '../../core/auth/api';

export interface GetObjectRequest {
  fields: string[];
  table: string;
  where: string;
  equal: string[] | number[];
}

export const getGenericObject = async <T>(body: GetObjectRequest): Promise<T> => {
  const { data } = await api.post('/api/util/generic/get-object', body);
  return data as T;
};
