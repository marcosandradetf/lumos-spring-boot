export type ExecutionStatusSlug =
  | 'analise-estoque'
  | 'aguardando-coleta'
  | 'prontas-para-execucao'
  | 'em-execucao'
  | 'concluidas';

export const EXECUTION_STATUS_MAP: Record<ExecutionStatusSlug, { title: string; value: string }> = {
  'analise-estoque': { title: 'Em Análise de Estoque', value: 'WAITING_STOCKIST' },
  'aguardando-coleta': { title: 'Aguardando Coleta', value: 'WAITING_COLLECT' },
  'prontas-para-execucao': { title: 'Prontas para Execução', value: 'AVAILABLE_EXECUTION' },
  'em-execucao': { title: 'Em Execução', value: 'IN_PROGRESS' },
  'concluidas': { title: 'Concluídas', value: 'FINISHED' },
};

export interface Execution {
  reservationManagementId: number;
  description: string;
  userId: string | null;
  teamId: number | null;
  status: string;
  createdAt: string;
  availableAt: string | null;
  contractId: number | null;
  contractNumber: string | null;
  preMeasurementId: number | null;
}

export interface DirectExecutionDTO {
  contractId: number;
  teamId: number;
  currentUserId: string;
  stockistId: string;
  instructions: string | null;
  items: Array<{
    contractItemId: number;
    quantity: string;
  }>;
}
