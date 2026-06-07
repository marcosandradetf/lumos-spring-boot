export interface MaintenanceExecution {
  execution_id: string;
  streets: string[];
  date_of_visit: string;
  team: Array<{ name: string; last_name: string; role: string }>;
}

export interface MaintenanceContract {
  contract: { contract_id: number; contractor: string };
  executions: MaintenanceExecution[];
}

export interface InstallationStep {
  installation_id: number;
  installation_type: string;
  step: string;
  description: string;
  finished_at?: string;
  date_of_visit?: string;
  team: Array<{ name: string; last_name: string; role: string }>;
}

export interface InstallationContract {
  contract: { contract_id: number; contractor: string };
  steps: InstallationStep[];
}

export interface ReportFilters {
  startDate: Date | null;
  endDate: Date | null;
  contractId?: number | null;
  teamId?: number | null;
  viewMode?: 'LIST' | 'GROUP';
}
