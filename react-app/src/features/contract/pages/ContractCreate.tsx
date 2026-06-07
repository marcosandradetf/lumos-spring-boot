import { type ChangeEvent, useEffect, useMemo, useRef, useState } from 'react';
import JSZip from 'jszip';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { Confirm } from '@/shared/components/confirm';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { AppNumberInput } from '@/shared/components/app-number-input';
import api from '@/core/auth/api';
import { contractsApi } from '@/features/contract/api/contractsApi';
import { contractKeys } from '@/features/contract/api/contractQueryKeys';
import { companiesApi } from '@/features/company/api/companiesApi';
import type {
  ContractFilters,
  ContractItemsResponseWithExecutionsSteps,
  ContractReferenceItemsDTO,
  ContractResponse,
  CreateContractDTO,
} from '@/features/contract/types';
import type { CompanyRequest, CompanyResponse } from '@/features/company/types';
import { contractFormSchema } from '@/features/contract/validations/contractSchema';
import { Toggle } from '@/shared/components/ui/toggle';
import { focusNextInputInTableRow, getStatusMeta, NOTIFICATION_GUIDE_URL } from '@/shared/utils/utils';
import { AppDatePicker } from '@/shared/components/app-date-picker';
import { ibgeApi } from '@/shared/api/ibge-api';
import { Input } from '@/shared/components/ui/input';
import { Button } from '@/shared/components/ui/button';
import { useNotificationStore } from '@/core/notifications/useNotificationStore';
import EnableNotification from '@/shared/components/enable-notification';
import { useAuthStore } from '@/core/auth/useAuthStore';

interface ContractForm {
  number: string;
  contractor: string;
  address: string;
  phone: string;
  cnpj: string;
  unifyServices: boolean;
  companyId: number;

  ibgeCode: string;
  contractionDate: Date | null;
  dueDate: Date | null;
  contractType: string;
}

interface ContractItemRow {
  draftId: string;
  contractReferenceItemId: number;
  description: string;
  type: string;
  quantity: number;
  price: number;
  factor: number;
  linking: string;
  nameForImport: string;
  itemDependency: string;
  contractItemId: number | null;
  totalExecuted: number;
  executedQuantity: { installationId: number; step: number; quantity: number }[];
  reservedQuantity: { installationId: number; step: number; quantity: number }[];
}

interface EditContractState {
  contract?: ContractResponse;
  items?: ContractReferenceItemsDTO[];
  step?: number;
}

const EMPTY_COMPANY: CompanyRequest = {
  idCompany: 0,
  socialReason: '',
  fantasyName: '',
  companyCnpj: '',
  companyContact: '',
  companyPhone: '',
  companyEmail: '',
  companyAddress: '',
};

const fmtCurrency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

const EMPTY_FORM: ContractForm = {
  number: '',
  contractor: '',
  address: '',
  phone: '',
  cnpj: '',
  unifyServices: false,
  companyId: 0,
  ibgeCode: '',
  contractionDate: null,
  dueDate: null,
  contractType: '',
};

const inputClass = [
  'flex w-full items-center justify-between gap-3 rounded-2xl border px-3 py-2 text-sm transition',
  'border-slate-200 bg-white text-slate-800',
  'dark:border-white/20 dark:bg-zinc-900 dark:text-neutral-100',
  'placeholder:text-slate-400 outline-none focus:border-indigo-400 transition-colors',
  'disabled:cursor-not-allowed disabled:opacity-60',
].join(' ');

const maxFileSize = 10 * 1024 * 1024;

function onlyDigits(value: string) {
  return value.replace(/\D/g, '');
}

function formatContractNumber(value: string) {
  return value.replace(/[^\d/.]/g, '');
}

function formatPhone(value: string) {
  const digits = onlyDigits(value).slice(0, 11);
  if (digits.length <= 2) {
    return digits.length ? `(${digits}` : '';
  }
  if (digits.length <= 6) {
    return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
  }
  if (digits.length <= 10) {
    return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
  }
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
}

function formatCnpj(value: string) {
  const digits = onlyDigits(value).slice(0, 14);
  if (digits.length <= 2) return digits;
  if (digits.length <= 5) return `${digits.slice(0, 2)}.${digits.slice(2)}`;
  if (digits.length <= 8) return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5)}`;
  if (digits.length <= 12) return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8)}`;
  return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8, 12)}-${digits.slice(12)}`;
}

function defaultEditFilters(): ContractFilters {
  return {
    contractor: null,
    startDate: new Date('2000-01-01T00:00:00'),
    endDate: new Date('2100-01-01T00:00:00'),
    status: null,
  };
}

function createDraftId() {
  return `draft-${Date.now()}-${Math.random()}`;
}

function mapReferenceItemToRow(item: ContractReferenceItemsDTO): ContractItemRow {
  return {
    draftId: createDraftId(),
    contractReferenceItemId: item.contractReferenceItemId,
    description: item.description,
    type: item.type,
    quantity: Number(item.quantity) || 0,
    price: Number(item.price) || 0,
    factor: Number(item.factor) || 1,
    linking: item.linking ?? '',
    nameForImport: item.nameForImport ?? '',
    itemDependency: item.itemDependency ?? '',
    contractItemId: item.contractItemId ?? null,
    totalExecuted: Number(item.totalExecuted) || 0,
    executedQuantity: item.executedQuantity ?? [],
    reservedQuantity: item.reservedQuantity ?? [],
  };
}

function mapContractItemToRow(item: ContractItemsResponseWithExecutionsSteps): ContractItemRow {
  return {
    draftId: createDraftId(),
    contractReferenceItemId: item.contractReferenceItemId,
    description: item.description,
    type: item.type,
    quantity: Number(item.contractedQuantity) || 0,
    price: Number(item.unitPrice) || 0,
    factor: Number(item.factor) || 1,
    linking: item.linking ?? '',
    nameForImport: item.nameForImport ?? '',
    itemDependency: '',
    contractItemId: item.contractItemId ?? null,
    totalExecuted: Number(item.totalExecuted) || 0,
    executedQuantity: item.executedQuantity ?? [],
    reservedQuantity: item.reservedQuantity ?? [],
  };
}

function mapCatalogToDraft(item: ContractReferenceItemsDTO): ContractItemRow {
  return {
    draftId: createDraftId(),
    contractReferenceItemId: item.contractReferenceItemId,
    description: item.description,
    type: item.type,
    quantity: 0,
    price: 0,
    factor: Number(item.factor) || 1,
    linking: item.linking ?? '',
    nameForImport: item.nameForImport ?? '',
    itemDependency: item.itemDependency ?? '',
    contractItemId: null,
    totalExecuted: 0,
    executedQuantity: [],
    reservedQuantity: [],
  };
}

function getTotalReserved(item: Pick<ContractItemRow, 'reservedQuantity'>): number {
  return (item.reservedQuantity ?? []).reduce((sum, row) => sum + (Number(row.quantity) || 0), 0);
}

function getMinimumQuantity(item: Pick<ContractItemRow, 'totalExecuted' | 'reservedQuantity'>, defaultValue = 0) {
  const min = Number(item.totalExecuted || 0) + getTotalReserved(item);
  return min === 0 ? defaultValue : min;
}

function isRowValid(item: ContractItemRow, unifyServices: boolean) {
  const hasPrice = Number(item.price || 0) > 0;
  const hasQuantity = Number(item.quantity || 0) > 0;
  const hasFactor = !unifyServices || Number(item.factor || 0) > 0;
  return hasPrice && hasQuantity && hasFactor;
}

function normalizeFactorsInRows(rows: ContractItemRow[], unifyServices: boolean) {
  return rows.map((row) => {
    if (!unifyServices) {
      return { ...row, factor: 1 };
    }

    const factor = Number(row.factor || 1);
    return { ...row, factor: factor > 0 ? factor : 1 };
  });
}

function applyDependentQuantities(rows: ContractItemRow[]) {
  const nextRows = [...rows];

  // Regra serviço: itens cujo itemDependency = tipo recebem a soma das quantidades do mesmo tipo (exceto descrições "SERVIÇO")
  nextRows.forEach((sourceRow) => {
    const typeQuantity = nextRows
      .filter((row) => row.type === sourceRow.type && !row.description.toUpperCase().includes('SERVIÇO'))
      .reduce((sum, row) => sum + Number(row.quantity || 0), 0);

    nextRows
      .filter((row) => row.itemDependency === sourceRow.type)
      .forEach((row) => {
        row.quantity = typeQuantity;
      });
  });

  // Regra cabo: soma conforme linking dos itens BRAÇO
  let cableQuantity = 0;
  nextRows
    .filter((row) => row.type === 'BRAÇO')
    .forEach((row) => {
      if (!row.linking) return;
      if (row.linking.startsWith('1')) cableQuantity += Number(row.quantity || 0) * 2.5;
      else if (row.linking.startsWith('2')) cableQuantity += Number(row.quantity || 0) * 8.5;
      else if (row.linking.startsWith('3')) cableQuantity += Number(row.quantity || 0) * 12.5;
    });

  const cableRow = nextRows.find((row) => row.type === 'CABO');
  if (cableRow) {
    cableRow.quantity = cableQuantity;
  }

  return nextRows;
}

export default function ContractCreate() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();

  const { setPageContext, setGuideUrl } = useAppStore();
  const { notify } = useNotify();
  const createCompanyMutation = useMutation({
    mutationFn: ({ company, logo }: { company: CompanyRequest; logo: File }) => companiesApi.createCompany(company, logo),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['companies', 'list'] });
    },
  });

  const createContractMutation = useMutation({
    mutationFn: (payload: CreateContractDTO) => contractsApi.createContract(payload),
    onSuccess: async (response: any) => {
      await queryClient.invalidateQueries({ queryKey: contractKeys.all });
      editId = response.contractId;
    },
  });

  const deleteContractMutation = useMutation({
    mutationFn: (id: number) => contractsApi.deleteById(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: contractKeys.all });
    },
  });

  let editId = searchParams.get('contractId');
  const isEdit = !!editId;

  const [cancelled, setCancelled] = useState(false);
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [form, setForm] = useState<ContractForm>(EMPTY_FORM);

  const [catalogDraftValues, setCatalogDraftValues] = useState<Record<number, { quantity: number; price: number; factor: number }>>({});
  const [contractItems, setContractItems] = useState<ContractItemRow[]>([]);

  const [loading, setLoading] = useState(false);
  const [loadingEditData, setLoadingEditData] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [openConfirm, setOpenConfirm] = useState(false);
  const [companyModalOpen, setCompanyModalOpen] = useState(false);
  const [companyForm, setCompanyForm] = useState<CompanyRequest>(EMPTY_COMPANY);
  const [companyLogo, setCompanyLogo] = useState<File | null>(null);
  const [creatingCompany, setCreatingCompany] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const contractFileInputRef = useRef<HTMLInputElement | null>(null);

  const [existingContractFile, setExistingContractFile] = useState<string | null>(null);
  const [existingNoticeFile, setExistingNoticeFile] = useState<string | null>(null);
  const priceInputRef = useRef<HTMLInputElement>(null);

  const hasReserved = contractItems.some((item) => Number(item.reservedQuantity || 0) > 0);
  const hasExecuted = contractItems.some((item) => Number(item.totalExecuted || 0) > 0);

  const [uf, setUf] = useState<string>('');
  const [cityName, setCityName] = useState<string>('');
  const [autoCalculateItems, setAutoCalculateItems] = useState<boolean>(localStorage.getItem('autoCalculateItems') === 'true');
  const [responseDate, setResponseDate] = useState<Date>(new Date());

  useEffect(() => {
    setPageContext(['Contratos', isEdit ? 'Editar Contrato' : 'Novo Contrato'], isEdit ? 'Editar Contrato' : 'Novo Contrato');
  }, [isEdit, setPageContext]);

  const { data: catalogItems = [] } = useQuery({
    queryKey: contractKeys.referenceItems(),
    queryFn: contractsApi.getContractReferenceItems,
  });
  const { data: companies = [], isLoading: loadingCompanies } = useQuery({
    queryKey: ['companies', 'list'],
    queryFn: companiesApi.getCompanies,
  });

  const {
    data: statesData,
    isLoading: isStatesLoading,
    error: statesError,
  } = useQuery({
    queryKey: ['ibge-states'],
    queryFn: ibgeApi.getUfs,
  });

  const {
    data: citiesData,
    isLoading: isCitiesLoading,
    error: citiesError,
  } = useQuery({
    queryKey: ['ibge-cities', uf],
    queryFn: () => ibgeApi.getCities(uf),
    enabled: uf !== ''
  });


  const { user } = useAuthStore();

  const {
    status,
    requestPermission,
    syncStatus,
  } = useNotificationStore();

  useEffect(() => {
    syncStatus();

    const handleFocus = () => syncStatus();

    window.addEventListener('focus', handleFocus);
    document.addEventListener('visibilitychange', handleFocus);

    return () => {
      window.removeEventListener('focus', handleFocus);
      document.removeEventListener('visibilitychange', handleFocus);
    };
  }, [syncStatus]);

  const statusMeta = useMemo(() => {
    if (status !== 'default' && status !== 'denied') return null;
    return getStatusMeta(status);
  }, [status]);

  useEffect(() => {
    if (!isEdit || !editId) {
      return;
    }

    let cancelled = false;

    const hydrateFromState = (state: EditContractState): boolean => {
      if (!state.contract || !state.items) {
        return false;
      }

      const selectedItems = state.items.map(mapReferenceItemToRow);
      setContractItems(selectedItems);
      setForm({
        number: state.contract.number ?? '',
        contractor: state.contract.contractor ?? '',
        address: state.contract.address ?? '',
        phone: state.contract.phone ?? '',
        cnpj: state.contract.cnpj ?? '',
        unifyServices: selectedItems.some((item) => Number(item.factor) !== 1),
        companyId: state.contract.companyId ?? null,
        ibgeCode: state.contract.ibgeCode ?? '',
        contractionDate: state.contract.contractionDate ?? new Date(),
        dueDate: state.contract.dueDate ?? new Date(),
        contractType: state.contract.contractType ?? '',
      });
      setExistingContractFile(state.contract.contractFile ?? null);
      setExistingNoticeFile(state.contract.noticeFile ?? null);
      setStep(state.step === 2 || state.step === 3 ? state.step : 1);
      return true;
    };

    const loadEditData = async () => {
      const routeState = location.state as EditContractState;
      if (hydrateFromState(routeState)) {
        return;
      }

      setLoadingEditData(true);
      try {
        const editFilters = defaultEditFilters();
        const [contracts, items] = await Promise.all([
          queryClient.fetchQuery({
            queryKey: contractKeys.list(editFilters),
            queryFn: () => contractsApi.getAllContracts(editFilters),
          }),
          queryClient.fetchQuery({
            queryKey: contractKeys.items(Number(editId)),
            queryFn: () => contractsApi.getContractItemsWithExecutionsSteps(Number(editId)),
          }),
        ]);

        if (cancelled) {
          return;
        }

        const contract = contracts.find((item) => item.contractId === Number(editId));
        if (!contract) {
          notify('Não foi possível encontrar o contrato para edição.', 'error');
          navigate('/contratos/listar?for=view', { replace: true });
          return;
        }

        const selectedItems = items.map(mapContractItemToRow);
        setContractItems(selectedItems);
        setForm({
          number: contract.number ?? '',
          contractor: contract.contractor ?? '',
          address: contract.address ?? '',
          phone: contract.phone ?? '',
          cnpj: contract.cnpj ?? '',
          unifyServices: selectedItems.some((item) => Number(item.factor) !== 1),
          companyId: contract.companyId ?? null,
          ibgeCode: contract.ibgeCode ?? '',
          contractionDate: contract.contractionDate ?? new Date(),
          dueDate: contract.dueDate ?? new Date(),
          contractType: contract.contractType ?? '',
        });
        setExistingContractFile(contract.contractFile ?? null);
        setExistingNoticeFile(contract.noticeFile ?? null);
      } catch {
        if (!cancelled) {
          notify('Erro ao carregar dados do contrato para edição.', 'error');
          navigate('/contratos/listar?for=view', { replace: true });
        }
      } finally {
        if (!cancelled) {
          setLoadingEditData(false);
        }
      }
    };

    void loadEditData();

    return () => {
      cancelled = true;
    };
  }, [editId, isEdit, location.state, navigate, notify, queryClient]);

  const catalogRows = useMemo(() => {
    const selectedIds = new Set(contractItems.map((item) => item.contractReferenceItemId));

    return (catalogItems as ContractReferenceItemsDTO[])
      .filter((item) => !selectedIds.has(item.contractReferenceItemId))
      .map((item) => {
        const base = mapCatalogToDraft(item);
        const draft = catalogDraftValues[item.contractReferenceItemId];
        if (!draft) return base;

        return {
          ...base,
          quantity: draft.quantity,
          price: draft.price,
          factor: draft.factor,
        };
      });
  }, [catalogDraftValues, catalogItems, contractItems]);

  const totalValue = useMemo(
    () => contractItems.reduce((sum, item) => {
      const factor = form.unifyServices ? (Number(item.factor || 1) > 0 ? Number(item.factor || 1) : 1) : 1;
      return sum + Number(item.price || 0) * (Number(item.quantity || 0) * factor);
    }, 0),
    [contractItems, form.unifyServices],
  );

  const step2Count = contractItems.length;

  const updateCatalogRow = (contractReferenceItemId: number, field: 'quantity' | 'price' | 'factor', value: number) => {
    setCatalogDraftValues((previous) => {
      const selectedIds = new Set(contractItems.map((item) => item.contractReferenceItemId));
      const currentVisibleRows = (catalogItems as ContractReferenceItemsDTO[])
        .filter((item) => !selectedIds.has(item.contractReferenceItemId))
        .map((item) => {
          const base = mapCatalogToDraft(item);
          const draft = previous[item.contractReferenceItemId];
          if (!draft) return base;
          return {
            ...base,
            quantity: draft.quantity,
            price: draft.price,
            factor: draft.factor,
          };
        });

      const updatedRows = currentVisibleRows.map((row) => {
        if (row.contractReferenceItemId !== contractReferenceItemId) return row;
        return {
          ...row,
          [field]: Number.isNaN(value) ? 0 : value,
        };
      });

      const adjustedRows = autoCalculateItems ? applyDependentQuantities(updatedRows) : updatedRows;
      const next = { ...previous };
      adjustedRows.forEach((row) => {
        next[row.contractReferenceItemId] = {
          quantity: Number(row.quantity || 0),
          price: Number(row.price || 0),
          factor: Number(row.factor || 1),
        };
      });
      return next;
    });
  };

  const addItemToContract = (row: ContractItemRow, target: HTMLInputElement | null = null) => {
    if (!isRowValid(row, form.unifyServices)) {
      notify(
        form.unifyServices
          ? 'Preencha quantidade, fator e valor unitário válidos para adicionar o item.'
          : 'Preencha quantidade e valor unitário válidos para adicionar o item.',
        'warn',
      );
      return;
    }

    setContractItems((previous) => [
      ...previous,
      {
        ...row,
        draftId: createDraftId(),
        factor: form.unifyServices ? row.factor : 1,
      },
    ]);

    notify('Item adicionado ao contrato.', 'success');

    if (target) {
      focusNextInputInTableRow(target);
    }
  };

  const updateContractItem = (draftId: string, field: 'quantity' | 'price' | 'factor', value: number) => {
    setContractItems((previous) => previous.map((row) => {
      if (row.draftId !== draftId) {
        return row;
      }

      return {
        ...row,
        [field]: Number.isNaN(value) ? 0 : value,
      };
    }));
  };

  const removeContractItem = (draftId: string) => {
    const target = contractItems.find((item) => item.draftId === draftId);
    if (!target) {
      return;
    }

    const minimum = getMinimumQuantity(target);
    if (minimum > 0) {
      notify('Não é permitido remover item com execução/reserva registrada.', 'warn');
      return;
    }

    setContractItems((previous) => previous.filter((item) => item.draftId !== draftId));
    setCatalogDraftValues((previous) => ({
      ...previous,
      [target.contractReferenceItemId]: {
        quantity: Number(target.quantity || 0),
        price: Number(target.price || 0),
        factor: Number(target.factor || 1),
      },
    }));
  };

  const resetCompanyForm = () => {
    setCompanyForm(EMPTY_COMPANY);
    setCompanyLogo(null);
  };

  const createCompany = async () => {
    if (
      !companyForm.socialReason
      || !companyForm.fantasyName
      || !companyForm.companyCnpj
      || !companyForm.companyContact
      || !companyForm.companyPhone
      || !companyForm.companyEmail
      || !companyForm.companyAddress
      || !companyLogo
    ) {
      notify('Preencha todos os campos da empresa e selecione o logo.', 'warn');
      return;
    }

    setCreatingCompany(true);
    try {
      const idCompany = await createCompanyMutation.mutateAsync({ company: companyForm, logo: companyLogo });
      setForm((previous) => ({ ...previous, companyId: idCompany }));
      setCompanyModalOpen(false);
      resetCompanyForm();
      notify('Empresa cadastrada com sucesso.', 'success');
    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string; error?: string } } })?.response?.data?.message
        ?? (error as { response?: { data?: { error?: string } } })?.response?.data?.error
        ?? 'Erro ao cadastrar empresa.';
      notify(message, 'error');
    } finally {
      setCreatingCompany(false);
    }
  };

  const validateStep1 = () => {
    const result = contractFormSchema.safeParse({
      number: form.number,
      contractor: form.contractor,
      address: form.address,
      phone: form.phone,
      cnpj: form.cnpj,
      companyId: form.companyId,

      ibgeCode: form.ibgeCode,
      contractionDate: form.contractionDate,
      dueDate: form.dueDate,
      contractType: form.contractType,
    });

    if (!result.success) {
      notify(result.error.issues[0]?.message ?? 'Preencha os campos obrigatórios do contrato antes de continuar.', 'warn');
      return false;
    }

    return true;
  };

  const onFilesSelected = (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    if (!files.length) {
      setSelectedFiles([]);
      return;
    }

    const allowedTypes = ['application/pdf', 'application/zip'];
    const accepted = files.filter((file) => allowedTypes.includes(file.type) && file.size <= maxFileSize);

    setSelectedFiles(accepted);

    if (accepted.length < files.length) {
      notify('Alguns arquivos foram ignorados por tipo/tamanho (aceitos: PDF/ZIP até 10MB).', 'warn');
    }
  };

  const uploadContractFiles = async () => {
    if (!selectedFiles.length || existingContractFile) return existingContractFile;

    const formData = new FormData();
    if (selectedFiles.length === 1) {
      formData.append('file', selectedFiles[0]);
    } else {
      const zip = new JSZip();
      selectedFiles.forEach((file) => {
        zip.file(file.name, file);
      });

      const zipBlob = await zip.generateAsync({
        type: 'blob',
        compression: 'DEFLATE',
        compressionOptions: { level: 6 },
      });

      formData.append('file', zipBlob, 'contract.zip');
      notify('Arquivos compactados automaticamente em ZIP para envio.', 'info');
    }

    const { data } = await api.post<string | { fileName?: string; url?: string }>(
      '/api/s3/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      },
    );

    if (typeof data === 'string') {
      setExistingContractFile(data);
      return data;
    }

    setExistingContractFile(data.fileName ?? data.url ?? null);
    return data.fileName ?? data.url ?? null;
  };

  const validateStep3 = () => {
    if (contractItems.length === 0 && form.contractType !== 'MAINTENANCE') {
      notify('Adicione pelo menos um item ao contrato.', 'warn');
      return false;
    }

    const invalid = contractItems.find((row) => {
      const minQuantity = getMinimumQuantity(row, 1);
      const qtyInvalid = Number(row.quantity || 0) < minQuantity;
      return qtyInvalid || !isRowValid(row, form.unifyServices);
    });

    if (invalid) {
      notify('Existem itens com quantidade mínima inválida ou valor/fator incompleto.', 'warn');
      return false;
    }

    return true;
  };

  const cancelContract = async () => {
    setLoading(true);

    try {
      if (!editId) {
        notify('Não foi possível cancelar o cadastro. Solicite apoio ao suporte.', 'error');
        return;
      }

      await deleteContractMutation.mutateAsync(Number(editId));
      setCancelled(true);
    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Erro ao salvar contrato.';
      notify(message, 'error');
    } finally {
      setLoading(false);
    }
  }

  const submitContract = async () => {
    if (!validateStep1() || !validateStep3()) {
      setOpenConfirm(false);
      return;
    }

    setLoading(true);
    setOpenConfirm(false);

    if (form.contractType === 'MAINTENANCE') setContractItems([]);

    try {
      const uploadedContractFile = await uploadContractFiles();
      await createContractMutation.mutateAsync({
        contractId: editId ? Number(editId) : null,
        number: form.number,
        contractor: form.contractor,
        address: form.address,
        phone: form.phone,
        cnpj: form.cnpj,
        unifyServices: form.unifyServices,
        noticeFile: existingNoticeFile,
        contractFile: uploadedContractFile,
        companyId: form.companyId,
        ibgeCode: form.ibgeCode,
        contractionDate: form.contractionDate,
        dueDate: form.dueDate,
        contractType: form.contractType,

        items: contractItems.map((row) => ({
          contractReferenceItemId: row.contractReferenceItemId,
          description: row.description,
          nameForImport: row.nameForImport,
          type: row.type,
          linking: row.linking,
          itemDependency: row.itemDependency,
          quantity: Number(row.quantity || 0),
          price: Number(row.price || 0),
          factor: form.unifyServices ? (Number(row.factor || 1) > 0 ? Number(row.factor || 1) : 1) : 1,
          contractItemId: row.contractItemId,
          totalExecuted: row.totalExecuted,
          executedQuantity: row.executedQuantity,
          reservedQuantity: row.reservedQuantity,
        })),
      });

      const responseDate = new Date();
      responseDate.setHours(responseDate.getHours() + 48);
      setSubmitted(true);
      setResponseDate(responseDate);
      setSelectedFiles([]);

    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Erro ao salvar contrato.';
      notify(message, 'error');

    } finally {
      setLoading(false);

    }
  };

  const responseDateLabel = responseDate
    ? new Intl.DateTimeFormat('pt-BR', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit',
    }).format(responseDate)
    : '48 horas';

  const getSpanDescription = () => {
    if (isEdit) return 'Atualização concluída';
    if (cancelled) return 'Cancelamento concluído';

    return 'Solicitação enviada';
  };

  const spanDescription = getSpanDescription();

  const getTitleDescription = () => {
    if (isEdit) return 'Contrato atualizado com sucesso';
    if (cancelled) return 'Cadastro cancelado com sucesso';

    return 'Recebemos seu cadastro';
  }

  const titleDescription = getTitleDescription();

  const getDescription = () => {
    if (isEdit) return 'As alterações já foram salvas e você pode acompanhar o contrato na listagem.';
    if (cancelled) return 'O cadastro está cancelado. Caso necessário, você pode cadastrar outro contrato.';

    return 'Agora vamos conferir os dados do contrato e os arquivos enviados. Avisaremos pelo sistema quando a análise terminar.';
  }

  const description = getDescription();

  if (submitted) {
    return (
      <div className="flex min-h-[70vh] items-center justify-center px-4 py-8">
        <div className="w-full max-w-2xl overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-[0_24px_70px_-45px_rgba(15,23,42,0.55)] dark:border-zinc-800 dark:bg-zinc-950">
          <div className="border-b border-slate-100 bg-slate-50/70 px-6 py-5 dark:border-zinc-800 dark:bg-zinc-900/60 sm:px-8">
            <div className="flex items-start gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700 ring-8 ring-white dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-zinc-950">
                <i className="pi pi-check text-lg" />
              </div>
              <div className="min-w-0 text-left">
                <span className="inline-flex rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300">
                  {spanDescription}
                </span>
                <h2 className="mt-3 text-2xl font-semibold tracking-tight text-slate-950 dark:text-zinc-50">
                  {titleDescription}
                </h2>
                <p className="mt-2 max-w-xl text-sm leading-6 text-slate-600 dark:text-zinc-400">
                  {description}
                </p>
              </div>
            </div>
          </div>

          {!isEdit && !cancelled && (
            <>
              <div className="px-6 py-6 sm:px-8">
                <div className="rounded-2xl border border-indigo-100 bg-indigo-50/60 p-4 text-left dark:border-indigo-900/40 dark:bg-indigo-950/20">
                  <div className="flex items-start gap-3">
                    <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white text-indigo-700 shadow-sm dark:bg-zinc-900 dark:text-indigo-300">
                      <i className="pi pi-clock text-sm" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-slate-900 dark:text-zinc-100">
                        Retorno previsto no máximo até {responseDateLabel}
                      </p>
                      <p className="mt-1 text-sm leading-6 text-slate-600 dark:text-zinc-400">
                        Se precisarmos de alguma correção ou documento extra, você recebe uma notificação por aqui.
                      </p>
                    </div>
                  </div>
                </div>

                {statusMeta && (
                  <EnableNotification
                    statusMeta={statusMeta}
                    clickAction={async () => {
                      if (status === 'denied') {
                        setGuideUrl(NOTIFICATION_GUIDE_URL);
                        return;
                      }

                      try {
                        const permission = await requestPermission(user?.roles ?? []);
                        console.log(permission)
                        if (permission === 'granted') {
                          notify('Notificações ativadas com sucesso.', 'success');
                        } else {
                          notify('Permissão de notificações não concedida.', 'warn');
                        }
                      } catch (error) {
                        console.error('Erro ao solicitar notificações', error);
                        notify('Não foi possível ativar as notificações.', 'error');
                      }

                    }}
                    detail={
                      status !== 'denied' ? 'Ative as notificações para receber um alerta após o fim da análise.' : undefined
                    }
                    className='mt-3'

                  />
                )}

                <div className="mt-5 grid gap-3 text-left sm:grid-cols-3">
                  <div className="rounded-2xl border border-slate-200 p-4 dark:border-zinc-800">
                    <i className="pi pi-file-check text-sm text-slate-500 dark:text-zinc-400" />
                    <p className="mt-3 text-sm font-semibold text-slate-900 dark:text-zinc-100">Documentos</p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-zinc-400">Arquivos recebidos para conferência.</p>
                  </div>
                  <div className="rounded-2xl border border-slate-200 p-4 dark:border-zinc-800">
                    <i className="pi pi-search text-sm text-slate-500 dark:text-zinc-400" />
                    <p className="mt-3 text-sm font-semibold text-slate-900 dark:text-zinc-100">Análise</p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-zinc-400">Nossa equipe valida dados e itens.</p>
                  </div>
                  <div className="rounded-2xl border border-slate-200 p-4 dark:border-zinc-800">
                    <i className="pi pi-bell text-sm text-slate-500 dark:text-zinc-400" />
                    <p className="mt-3 text-sm font-semibold text-slate-900 dark:text-zinc-100">Resposta</p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-zinc-400">Você será avisado assim que houver retorno.</p>
                  </div>
                </div>
              </div>

            </>


          )}

          <div className="flex flex-col gap-2 border-t border-slate-100 bg-white px-6 py-5 dark:border-zinc-800 dark:bg-zinc-950 sm:px-8">
            <Button
              className="h-11"
              onClick={() => navigate('/contratos/listar')}
            >
              Ver contratos
            </Button>

            {!cancelled && !isEdit && (
              <Button
                variant="outline"
                className="h-11"
                type="button"
                onClick={() => cancelContract()}
              >
                Cancelar cadastro
              </Button>
            )}

            <Button
              variant="outline"
              className="h-11"
              type="button"
              onClick={() => {
                setForm(EMPTY_FORM);
                setCatalogDraftValues({});
                setContractItems([]);
                setStep(1);
                setSubmitted(false);
                setExistingContractFile(null);
                setExistingNoticeFile(null);
                setCancelled(false);
              }}
            >
              Criar outro contrato
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <section className="p-4 md:p-6">
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="hidden md:block rounded-2xl border border-slate-200/80 bg-linear-to-r from-white to-slate-50 px-4 py-3 dark:border-zinc-800/80 dark:from-zinc-900 dark:to-zinc-900/60">
          <div className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-zinc-400">
            Fluxo de cadastro
          </div>
          <div className="flex items-center gap-3 overflow-x-auto pb-1">
            {([1, 2, 3] as const).map((currentStep) => (
              <div key={currentStep} className="flex items-center gap-2 min-w-max">
                <div onClick={() => { setStep(currentStep); }} className={`cursor-pointer flex h-9 w-9 items-center justify-center rounded-full text-sm font-bold transition-all ${step >= currentStep ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/30' : 'bg-slate-100 dark:bg-zinc-800 text-slate-400 dark:text-zinc-500'}`}>
                  {step > currentStep ? <i className="pi pi-check text-xs" /> : currentStep}
                </div>
                <span className={`text-sm font-semibold ${step === currentStep ? 'text-slate-800 dark:text-zinc-100' : 'text-slate-400 dark:text-zinc-500'}`}>
                  {currentStep === 1 ? 'Dados' : currentStep === 2 ? 'Selecionar Itens' : 'Revisão'}
                </span>
                {currentStep < 3 && <i className="pi pi-chevron-right text-xs text-slate-300 dark:text-zinc-600 ml-1" />}
              </div>
            ))}
          </div>
        </div>

        <div className="relative sm:rounded-2xl sm:border border-slate-200 dark:border-zinc-800 sm:bg-white sm:dark:bg-zinc-900 sm:p-6 sm:shadow-[0_10px_30px_-20px_rgba(15,23,42,0.35)] space-y-5">
          <LoadingOverlay loading={loading || loadingEditData} />

          {step === 1 && (
            <>
              <h2 className="text-center text-base font-semibold text-slate-800 dark:text-zinc-100">
                {isEdit ? 'Editar os dados do contrato' : 'Digite os dados do contrato'}
              </h2>
              <div className="gap-4 flex flex-col">
                <div className='flex gap-4 flex-wrap md:flex-nowrap'>
                  <div className='w-full md:w-[20%]'>
                    <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">N.º Contrato*</label>
                    <input
                      type="text"
                      value={form.number}
                      onChange={(event) => setForm((previous) => ({ ...previous, number: formatContractNumber(event.target.value) }))}
                      placeholder="Digite o número"
                      className={inputClass}
                    />
                  </div>
                  <div className='w-full md:w-[80%]'>
                    <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Contratante*</label>
                    <input
                      type="text"
                      value={form.contractor}
                      onChange={(event) => setForm((previous) => ({ ...previous, contractor: event.target.value }))}
                      placeholder="Prefeitura / Contratante"
                      className={inputClass}
                    />
                  </div>
                </div>

                <div className='flex gap-4 flex-wrap md:flex-nowrap'>
                  <div className='w-full md:w-[25%]'>
                    <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Data de início do Contrato*</label>
                    <AppDatePicker
                      value={form.contractionDate}
                      onChange={(value) => setForm((previous) => ({ ...previous, contractionDate: value }))}
                    />
                  </div>

                  <div className='w-full md:w-[25%] md:flex-nowrap'>
                    <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Data de término do Contrato*</label>
                    <AppDatePicker
                      value={form.dueDate}
                      onChange={(value) => setForm((previous) => ({ ...previous, dueDate: value }))}
                    />
                  </div>

                  <div className='w-full md:w-[50%] md:flex-nowrap'>
                    <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Tipo de Contrato*</label>
                    <GlassListbox
                      value={form.contractType}
                      onChange={(value) => setForm((previous) => ({ ...previous, contractType: value }))}
                      placeholder="Selecione o tipo de contrato"
                      options={[
                        { label: 'Somente Instalação/Modernização', value: 'INSTALLATION' },
                        { label: 'Somente Manutenção', value: 'MAINTENANCE' },
                        { label: 'Instalação e Manutenção', value: 'ALL' },
                      ]}
                    />
                  </div>

                </div>

                <div className='flex gap-4 flex-wrap md:flex-nowrap'>

                  <div className='w-full md:w-[25%]'>
                    <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Telefone do Contratante*</label>
                    <input
                      type="text"
                      value={form.phone}
                      onChange={(event) => setForm((previous) => ({ ...previous, phone: formatPhone(event.target.value) }))}
                      placeholder="(31) 99999-9999"
                      className={inputClass}
                    />
                  </div>
                  <div className='w-full md:w-[25%]'>
                    <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">CNPJ do Contratante*</label>
                    <input
                      type="text"
                      value={form.cnpj}
                      onChange={(event) => setForm((previous) => ({ ...previous, cnpj: formatCnpj(event.target.value) }))}
                      placeholder="00.000.000/0001-00"
                      className={inputClass}
                    />
                  </div>


                  <div className='w-full md:w-[25%]'>
                    <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Estado refente ao contrato*</label>
                    <GlassListbox
                      options={(statesData ?? []).map((state) => ({ label: `${state.nome} (${state.sigla})`, value: state.sigla }))}
                      value={uf}
                      onChange={(value) => {
                        setUf(value);
                        setCityName('');
                        setForm((previous) => ({ ...previous, address: '', ibgeCode: '' }));
                      }}
                      placeholder="Estado (UF)"
                      searchable
                      isLoading={isStatesLoading}
                    />
                  </div>

                  <div className='w-full md:w-[25%] md:flex-nowrap'>
                    <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Cidade refente ao contrato*</label>
                    <GlassListbox
                      options={(citiesData ?? []).map((city) => ({ label: city.nome, value: city.nome }))}
                      value={cityName}
                      onChange={(value) => {
                        setCityName(value);

                        const ibgeCode = citiesData?.find((city) => city.nome === value)?.id;
                        setForm((previous) => ({ ...previous, address: `${value} - ${uf}`, ibgeCode: String(ibgeCode ?? '-1') }));
                      }}
                      placeholder="Cidade"
                      searchable
                      isLoading={isCitiesLoading}
                      disabled={uf === '' || !citiesData}
                      disabledTooltip={uf === '' ? 'Selecione um estado primeiro' : undefined}
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">
                    Endereço do contratante*
                  </label>
                  <Input
                    type="text"
                    value={form.address || ''}
                    onChange={(event) => {
                      const place = event.target.value;

                      setForm((previous) => ({
                        ...previous,
                        address: place,
                      }));

                    }}
                    placeholder="Digite o endereço"
                    disabled={form.ibgeCode === ''}
                    disabledTooltip={form.ibgeCode === '' ? 'Selecione a cidade primeiro' : undefined}

                  />
                </div>

                <div className='relative'>
                  <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Empresa contratada*</label>
                  <GlassListbox
                    disabled={companyModalOpen}
                    disabledTooltip={companyModalOpen ? 'Cadastrando nova empresa' : undefined}
                    value={form.companyId}
                    onChange={(value) => setForm((previous) => ({ ...previous, companyId: value }))}
                    placeholder={loadingCompanies ? 'Carregando empresas...' : 'Selecione a Empresa Prestadora'}
                    options={(companies as CompanyResponse[]).map((company) => ({
                      value: company.idCompany,
                      label: company.socialReason,
                    }))}
                    footerActionLabel="Adicionar Nova Empresa"
                    footerActionIcon="pi-plus"
                    footerActionClassName="font-semibold"
                    onFooterAction={() => setCompanyModalOpen(true)}
                    emptyText="Nenhuma empresa cadastrada"
                  />

                  {companyModalOpen && (
                    <div className="absolute left-0 top-full z-20 mt-3 w-full">
                      <div className="w-full rounded-lg border border-slate-200 bg-linear-to-b from-white to-slate-50 p-5 text-slate-800 shadow-xl dark:border-zinc-700 dark:from-zinc-900 dark:to-zinc-900/80 dark:text-zinc-100">
                        <div className="mb-4 flex items-center justify-between border-b border-slate-200 pb-3 dark:border-zinc-700">
                          <div>
                            <h3 className="flex items-center gap-2 text-base font-semibold">
                              <i className="pi pi-building text-sm text-indigo-500" />
                              Cadastrar Empresa
                            </h3>
                            <p className="text-xs text-slate-500 dark:text-zinc-400">Preencha as informações abaixo para criar e já vincular ao contrato.</p>
                          </div>
                          <button
                            type="button"
                            onClick={() => {
                              setCompanyModalOpen(false);
                              resetCompanyForm();
                            }}
                            className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
                          >
                            <i className="pi pi-times text-xs" />
                          </button>
                        </div>

                        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                          <input className={inputClass} placeholder="Razão social*" value={companyForm.socialReason} onChange={(event) => setCompanyForm((previous) => ({ ...previous, socialReason: event.target.value.toUpperCase() }))} />
                          <input className={inputClass} placeholder="Nome fantasia*" value={companyForm.fantasyName} onChange={(event) => setCompanyForm((previous) => ({ ...previous, fantasyName: event.target.value.toUpperCase() }))} />
                          <input className={inputClass} placeholder="CNPJ*" value={companyForm.companyCnpj} onChange={(event) => setCompanyForm((previous) => ({ ...previous, companyCnpj: formatCnpj(event.target.value) }))} />
                          <input className={inputClass} placeholder="Telefone*" value={companyForm.companyPhone} onChange={(event) => setCompanyForm((previous) => ({ ...previous, companyPhone: formatPhone(event.target.value) }))} />
                          <input className={inputClass} placeholder="Contato*" value={companyForm.companyContact} onChange={(event) => setCompanyForm((previous) => ({ ...previous, companyContact: event.target.value }))} />
                          <input className={inputClass} placeholder="E-mail*" value={companyForm.companyEmail} onChange={(event) => setCompanyForm((previous) => ({ ...previous, companyEmail: event.target.value }))} />
                          <input className={`${inputClass} sm:col-span-2`} placeholder="Endereço*" value={companyForm.companyAddress} onChange={(event) => setCompanyForm((previous) => ({ ...previous, companyAddress: event.target.value }))} />
                          <div className="sm:col-span-2 rounded-xl border border-slate-200 bg-white/70 p-3 dark:border-zinc-700 dark:bg-zinc-900/50">
                            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-zinc-400">Logo da empresa*</label>
                            <input
                              id="company-logo-input"
                              type="file"
                              accept="image/*"
                              onChange={(event) => setCompanyLogo(event.target.files?.[0] ?? null)}
                              className="hidden"
                            />
                            <div>
                              <button
                                type="button"
                                onClick={() => document.getElementById('company-logo-input')?.click()}
                                className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-indigo-600 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-indigo-500"
                              >
                                <i className="pi pi-image text-xs" />
                                Selecionar logo
                              </button>
                              <p className="mt-2 text-[11px] text-center text-slate-500 dark:text-zinc-400">
                                {companyLogo ? `Selecionado: ${companyLogo.name}` : 'Aceita imagens (PNG, JPG, SVG...)'}
                              </p>
                            </div>
                          </div>
                        </div>

                        <div className="mt-4 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                          <button
                            type="button"
                            onClick={() => {
                              setCompanyModalOpen(false);
                              resetCompanyForm();
                            }}
                            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-300 dark:hover:bg-zinc-800"
                          >
                            Cancelar
                          </button>
                          <button
                            type="button"
                            disabled={creatingCompany}
                            onClick={() => void createCompany()}
                            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-indigo-500 disabled:opacity-50"
                          >
                            {creatingCompany && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
                            Criar e vincular
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Arquivos do contrato</label>
                  <input
                    ref={contractFileInputRef}
                    type="file"
                    accept=".pdf,.zip,application/pdf,application/zip"
                    onChange={onFilesSelected}
                    multiple
                    className="hidden"
                  />
                  <div className="rounded-xl border border-dashed border-slate-300 dark:border-zinc-700 bg-slate-50/60 dark:bg-zinc-900/50 p-3">
                    <button
                      type="button"
                      onClick={() => contractFileInputRef.current?.click()}
                      className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-600 px-3 py-2 text-sm font-semibold text-white hover:bg-emerald-500 transition-colors"
                    >
                      <i className="pi pi-paperclip text-xs" />
                      Selecionar arquivos
                    </button>
                    {selectedFiles.length > 0 ? (
                      <div className='flex flex-col'>
                        {selectedFiles.map((file) => (
                          <p className="mt-2 text-[11px] text-slate-500 dark:text-zinc-400">
                            {file.name} ({(file.size / (1024 * 1024)).toFixed(2)} MB)
                          </p>
                        ))}
                      </div>
                    ) : (
                      <p className="mt-2 text-[11px] text-center text-slate-500 dark:text-zinc-400">
                        Aceita PDF • até 25MB / ZIP • até 100MB
                      </p>
                    )}
                  </div>
                </div>
                <div className="col-span-4">
                  <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">O Contrato utiliza fator US?</label>

                  <Toggle
                    variant="outline"
                    aria-label="Botão para ativar ou desativar o fator US no contrato"
                    pressed={form.unifyServices}
                    onPressedChange={(pressed) => {
                      setForm((previous) => ({ ...previous, unifyServices: pressed }));
                      setCatalogDraftValues((previous) => {
                        const next = { ...previous };
                        Object.keys(next).forEach((key) => {
                          const itemId = Number(key);
                          const current = next[itemId];
                          if (!current) return;
                          if (!pressed) {
                            next[itemId] = { ...current, factor: 1 };
                            return;
                          }
                          const factor = Number(current.factor || 1);
                          next[itemId] = { ...current, factor: factor > 0 ? factor : 1 };
                        });
                        return next;
                      });
                      setContractItems((previous) => normalizeFactorsInRows(previous, pressed));
                    }}
                    className="rounded-2xl w-full h-9 gap-2 border-slate-300 bg-white px-3 text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200 dark:hover:bg-zinc-800 data-[state=on]:border-blue-300 data-[state=on]:bg-blue-50 data-[state=on]:text-blue-700 dark:data-[state=on]:border-blue-700 dark:data-[state=on]:bg-blue-900/30 dark:data-[state=on]:text-blue-300"
                  >
                    <span className="text-sm font-medium">
                      {form.unifyServices ? 'Desativar Fator US' : 'Ativar Fator US'}
                    </span>
                  </Toggle>
                  <p className="mt-1 text-xs text-slate-600 dark:text-zinc-300 border rounded-2xl p-2 border-green-500/20 bg-green-500/5">
                    {form.unifyServices ? (
                      <>
                        O valor de cada item será calculado como: <strong>valor unitário x (quantidade x fator)</strong>.
                      </>
                    ) : (
                      <>
                        Com o fator US desativado, o cálculo usa apenas: <strong>valor unitário x quantidade</strong>.
                      </>
                    )}
                  </p>
                </div>
              </div>

              <div className="flex justify-end pt-2">
                <button
                  type="button"
                  onClick={() => {
                    if (!validateStep1()) {
                      return;
                    }
                    setStep(2);
                  }}
                  className="rounded-2xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors"
                >
                  Continuar <i className="pi pi-arrow-right ml-1.5 text-xs" />
                </button>
              </div>
            </>
          )}

          {step === 2 && (
            <>
              {form.unifyServices && (
                <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900 dark:border-emerald-900/40 dark:bg-emerald-950/20 dark:text-emerald-200">
                  O fator US está ativo. O total por item será calculado com: <strong>valor unitário x (quantidade x fator)</strong>.
                </div>
              )}
              <div className="flex items-start justify-between gap-3">
                {['INSTALLATION', 'ALL', ''].includes(form.contractType) && (
                  <div>
                    <h2 className="text-base font-semibold text-slate-800 dark:text-zinc-100">Selecionar Itens</h2>
                    <p className="text-sm text-slate-500 dark:text-zinc-400">Preencha quantidade e valor unitário e adicione os itens ao contrato.</p>
                  </div>
                )}

                <div className='space-y-2'>
                  {/* <Tooltip>
                    <TooltipTrigger>
                      <div className="flex items-center space-x-2">
                        <Switch
                          id="btn-auto"
                          checked={autoCalculateItems}
                          onCheckedChange={(checked) => {
                            setAutoCalculateItems(checked);
                            localStorage.setItem('autoCalculateItems', String(checked));
                          }}
                        />
                        <Label htmlFor="btn-auto" className='text-xs text-foreground'>Cálculo automático de itens</Label>
                      </div>
                    </TooltipTrigger>
                    <TooltipContent>
                      Com o cálculo automático ativado, ao preencher a quantidade de um item, os itens relacionados serão calculados automaticamente.
                    </TooltipContent>
                  </Tooltip> */}

                  <div className="rounded-2xl border border-indigo-200 dark:border-indigo-900/40 px-3 py-1 text-xs font-semibold text-indigo-700 dark:text-indigo-300">
                    {step2Count} selecionado(s)
                  </div>

                </div>
              </div>

              <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-zinc-700">
                {['INSTALLATION', 'ALL', ''].includes(form.contractType) ? (
                  <table className="min-w-[900px] w-full text-sm">
                    <thead className="bg-slate-50 dark:bg-zinc-800 text-xs uppercase tracking-wide text-slate-500 dark:text-zinc-400">
                      <tr>
                        <th className="px-3 py-2 text-left">Descrição</th>
                        <th className="px-3 py-2 text-right">Quantidade</th>
                        {form.unifyServices && <th className="px-3 py-2 text-right">Fator US</th>}
                        <th className="px-3 py-2 text-right">Valor unitário</th>
                        <th className="px-3 py-2 text-right w-[20%]">Total item</th>
                        <th className="px-3 py-2 text-left" />
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-zinc-800">
                      {catalogRows.map((row) => {
                        const effectiveFactor = form.unifyServices ? (Number(row.factor || 1) > 0 ? Number(row.factor || 1) : 1) : 1;
                        const total = Number(row.price || 0) * (Number(row.quantity || 0) * effectiveFactor);

                        return (
                          <tr key={row.contractReferenceItemId} className="hover:bg-emerald-50/40 dark:hover:bg-emerald-900/10 transition-colors">
                            <td className="px-3 py-2 text-slate-700 dark:text-zinc-200">{row.description}</td>
                            <td className="px-3 py-2 text-right">
                              <AppNumberInput
                                value={row.quantity}
                                onChange={(next) => updateCatalogRow(row.contractReferenceItemId, 'quantity', next)}
                                min={0}
                                minFractionDigits={0}
                                maxFractionDigits={2}
                                mode="decimal"
                                inputClassName="w-24"
                                onKeyDown={(event) => {
                                  if (event.key === 'Enter') {
                                    event.preventDefault();
                                    if (!row.quantity || Number(row.quantity) <= 0) {
                                      return;
                                    }

                                    focusNextInputInTableRow(event.currentTarget);
                                  }

                                }}
                              />
                            </td>

                            {/* FATOR US */}
                            {form.unifyServices && (
                              <td className="px-3 py-2 text-right">
                                <AppNumberInput
                                  value={row.factor}
                                  onChange={(next) => updateCatalogRow(row.contractReferenceItemId, 'factor', next)}
                                  min={0.1}
                                  minFractionDigits={0}
                                  maxFractionDigits={2}
                                  disabled={!row.quantity || Number(row.quantity) <= 0}
                                  disabledTooltip="Insira a quantidade primeiro"
                                  mode="decimal"
                                  inputClassName="w-24"
                                  onKeyDown={(event) => {
                                    if (event.key === 'Enter') {
                                      event.preventDefault();
                                      if (!row.quantity || Number(row.quantity) <= 0) {
                                        return;
                                      }

                                      focusNextInputInTableRow(event.currentTarget);
                                    }

                                  }}
                                />
                              </td>
                            )}


                            <td className="px-3 py-2 text-right">
                              <AppNumberInput
                                ref={priceInputRef}
                                value={row.price}
                                onChange={(next) => updateCatalogRow(row.contractReferenceItemId, 'price', next)}
                                min={0}
                                minFractionDigits={2}
                                maxFractionDigits={2}
                                mode="currency"
                                inputClassName="w-28"
                                disabled={!row.quantity || Number(row.quantity) <= 0}
                                disabledTooltip="Insira a quantidade primeiro"
                                onKeyDown={(event) => {
                                  if (event.key === 'Enter') {
                                    event.preventDefault();
                                    if (!row.quantity || Number(row.quantity) <= 0) {
                                      return;
                                    }
                                    addItemToContract(row, event.currentTarget);
                                  }

                                }}
                              />
                            </td>
                            <td className="px-3 py-2 text-right font-semibold text-slate-700 dark:text-zinc-200">
                              <div className='flex flex-col items-end'>
                                <p>{fmtCurrency.format(total)}</p>
                                {form.unifyServices && (
                                  <p className='text-[11px] font-light'>
                                    {fmtCurrency.format(row.price)} * <strong>({row.quantity} * {row.factor}) </strong> = {fmtCurrency.format(total)}
                                  </p>
                                )}
                              </div>
                            </td>
                            <td className="px-3 py-2 text-center">
                              <button
                                type="button"
                                onClick={() => addItemToContract(row, null)}
                                className="h-8 w-8 rounded-full bg-blue-600 text-white hover:bg-blue-500"
                                title="Adicionar item"
                              >
                                <i className="pi pi-plus text-[10px]" />
                              </button>
                            </td>
                          </tr>
                        );
                      })}

                      {catalogRows.length === 0 && (
                        <tr>
                          <td colSpan={form.unifyServices ? 7 : 6} className="px-3 py-6 text-center text-sm text-slate-400 dark:text-zinc-500">
                            Todos os itens referenciais já foram adicionados ao contrato.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                ) : (
                  <div className='flex flex-col items-center gap-3 py-10 text-center text-foreground'>
                    <i className='pi pi-info-circle text-2xl text-indigo-500'></i>
                    <h4>
                      Itens referenciais não necessários
                    </h4>
                    <p className='text-xs'>
                      Este contrato é do tipo <strong>Somente Manutenção</strong>, portanto, não possui itens referenciais para adicionar.
                    </p>
                  </div>
                )}
              </div>

              <div className="flex items-center justify-between pt-2">
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="rounded-2xl border border-slate-200 dark:border-zinc-700 px-4 py-2.5 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
                >
                  <i className="pi pi-arrow-left mr-1.5 text-xs" /> Voltar
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (contractItems.length === 0 && form.contractType !== 'MAINTENANCE') {
                      notify('Adicione ao menos um item antes de revisar.', 'warn');
                      return;
                    }
                    setStep(3);
                  }}
                  className="rounded-2xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors"
                >
                  Revisar {['ALL', '', 'INSTALLATION'].includes(form.contractType) ? 'Itens' : 'Contrato'} <i className="pi pi-arrow-right ml-1.5 text-xs" />
                </button>
              </div>
            </>
          )}

          {step === 3 && (
            <>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="text-base font-semibold text-slate-800 dark:text-zinc-100">Revisão do Contrato</h2>
                  <p className="text-sm text-slate-500 dark:text-zinc-400">Revise {['ALL', '', 'INSTALLATION'].includes(form.contractType) ? 'os itens adicionados' : 'o contrato'} antes de finalizar.</p>
                </div>
                {['INSTALLATION', 'ALL', ''].includes(form.contractType) && (
                  <div className="rounded-xl bg-indigo-50 dark:bg-indigo-900/20 border border-indigo-200 dark:border-indigo-900/40 px-3 py-2 text-right">
                    <p className="text-[11px] uppercase tracking-wide text-indigo-600 dark:text-indigo-300">Valor total</p>
                    <p className="text-lg font-extrabold text-indigo-700 dark:text-indigo-200">{fmtCurrency.format(totalValue)}</p>
                  </div>
                )}
              </div>

              <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-zinc-700">
                {['INSTALLATION', 'ALL', ''].includes(form.contractType) ? (
                  <table className="min-w-[980px] w-full text-sm">
                    <thead className="bg-slate-50 dark:bg-zinc-800 text-xs uppercase tracking-wide text-slate-500 dark:text-zinc-400">
                      <tr>
                        <th className="px-3 py-2 text-left">Descrição</th>
                        <th className="px-3 py-2 text-right">Qtde</th>
                        {form.unifyServices && <th className="px-3 py-2 text-right">Fator US</th>}
                        {hasExecuted && <th className="px-3 py-2 text-right">Executada</th>}
                        {hasReserved && <th className="px-3 py-2 text-right">Reservada</th>}
                        <th className="px-3 py-2 text-right">Valor unitário</th>
                        <th className="px-3 py-2 text-right">Total item</th>
                        <th className="px-3 py-2 text-left" />
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-zinc-800">


                      {contractItems.map((row) => {
                        const minQty = getMinimumQuantity(row, 1);
                        const reserved = getTotalReserved(row);
                        const factor = form.unifyServices ? (Number(row.factor || 1) > 0 ? Number(row.factor || 1) : 1) : 1;
                        const total = Number(row.price || 0) * (Number(row.quantity || 0) * factor);

                        return (
                          <tr key={row.draftId} className="hover:bg-slate-50 dark:hover:bg-zinc-800/50 transition-colors">
                            <td className="px-3 py-2 text-slate-700 dark:text-zinc-200">{row.description}</td>
                            <td className="px-3 py-2 text-right">
                              <AppNumberInput
                                value={row.quantity}
                                onChange={(next) => updateContractItem(row.draftId, 'quantity', next)}
                                min={minQty}
                                minFractionDigits={0}
                                maxFractionDigits={2}
                                mode="decimal"
                                inputClassName="w-24"
                                onKeyDown={(event) => {
                                  if (event.key === 'Enter') {
                                    event.preventDefault();
                                    if (!row.quantity || Number(row.quantity) <= 0) {
                                      return;
                                    }
                                    focusNextInputInTableRow(event.currentTarget);
                                  }
                                }}
                              />
                            </td>
                            {form.unifyServices && (
                              <td className="px-3 py-2 text-right">
                                <AppNumberInput
                                  value={row.factor}
                                  onChange={(next) => updateContractItem(row.draftId, 'factor', next)}
                                  min={1}
                                  minFractionDigits={0}
                                  maxFractionDigits={2}
                                  mode="decimal"
                                  inputClassName="w-24"
                                  onKeyDown={(event) => {
                                    if (event.key === 'Enter') {
                                      event.preventDefault();
                                      if (!row.quantity || Number(row.quantity) <= 0) {
                                        return;
                                      }
                                      focusNextInputInTableRow(event.currentTarget);
                                    }
                                  }}
                                />
                              </td>
                            )}
                            {hasExecuted && (
                              <td className="px-3 py-2 text-right">{row.totalExecuted}</td>
                            )}
                            {hasReserved && (
                              <td className="px-3 py-2 text-right">{reserved}</td>
                            )}
                            <td className="px-3 py-2 text-right">
                              <AppNumberInput
                                value={row.price}
                                onChange={(next) => updateContractItem(row.draftId, 'price', next)}
                                min={0}
                                minFractionDigits={2}
                                maxFractionDigits={2}
                                mode="currency"
                                inputClassName="w-28"
                              />
                            </td>
                            <td className="px-3 py-2 text-right font-semibold text-slate-700 dark:text-zinc-200">{fmtCurrency.format(total)}</td>
                            <td className="px-3 py-2 text-center">
                              <button
                                type="button"
                                onClick={() => removeContractItem(row.draftId)}
                                className="h-8 w-8 rounded-full bg-red-600 text-white hover:bg-red-500"
                                title="Remover item"
                              >
                                <i className="pi pi-trash text-[10px]" />
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                ) : (
                  <div className='flex flex-col items-center gap-3 py-10 text-center text-foreground'>
                    <i className='pi pi-info-circle text-2xl text-indigo-500'></i>
                    <h4>
                      Revisão do contrato
                    </h4>
                    <p className='text-xs'>
                      Este contrato é do tipo <strong>Somente Manutenção</strong>, portanto, não possui itens para revisar. Se necessário revise os dados do contrato e finalize o cadastro.
                    </p>
                  </div>
                )}

              </div>

              <div className="flex items-center justify-between pt-2">
                <button
                  type="button"
                  onClick={() => setStep(2)}
                  className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2.5 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
                >
                  <i className="pi pi-arrow-left mr-1.5 text-xs" /> Voltar
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (!validateStep3()) {
                      return;
                    }

                    void submitContract();
                  }}
                  className="rounded-2xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-emerald-500 transition-colors"
                >
                  {isEdit ? 'Salvar contrato' : 'Finalizar cadastro'}
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      <Confirm
        open={openConfirm}
        onClose={() => setOpenConfirm(false)}
        onConfirm={() => void submitContract()}
        title={isEdit ? 'Salvar alterações do contrato?' : 'Finalizar cadastro do contrato?'}
        description="Confirme para enviar os dados e itens do contrato."
        confirmLabel={isEdit ? 'Salvar' : 'Finalizar'}
        loading={loading}
      />
    </section >
  );
}