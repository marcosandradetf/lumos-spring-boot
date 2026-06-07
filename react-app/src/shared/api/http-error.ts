import type { AxiosError } from 'axios';

export interface ApiErrorData {
  message?: string;
  error?: string;
  code?: string;
}

export type ApiHttpError = AxiosError<ApiErrorData | string>;

export function getApiErrorMessage(error: unknown): string | undefined {
  const apiError = error as ApiHttpError;
  const data = apiError.response?.data;

  if (typeof data === 'string') return data;
  const errorMessage =  data?.message ?? data?.error ?? data?.code;
  return !['Internal Server Error', undefined, '']
    .includes(errorMessage) ? errorMessage : undefined;
}
