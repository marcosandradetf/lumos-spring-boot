import { Fragment, useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { getGenericObject } from '@/shared/api/generic-api';
import { executionApi } from '@/features/requests/api/execution-api';
import { requestApi } from '@/features/requests/api/requests-api';
import { teamApi } from '@/features/requests/api/team-api';
import { requestsKeys } from '@/features/requests/api/query-keys';
import type {
  MaterialInStockDTO,
  OrderHistoryItem,
  ReserveItemRequest,
  ReserveMaterialSelection,
  ReserveRequest,
  TeamMemberContact,
} from '@/features/requests/types/reservation';
import { Toggle } from '@/shared/components/ui/toggle';
import { Package } from 'lucide-react';
import { CellEdit } from '@/shared/components/ui/cell-edit';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/shared/components/ui/tooltip';
import { fmtDateToBrazilian, formatDecimalForDisplay, formatPhone, normalizeFloatInput, normalizePhoneDigits } from '@/shared/utils/formatters';
import { Modal, ModalBody, ModalHeader } from '@/shared/ui/modal';

interface NavigationState {
  reserve?: ReserveRequest;
}



const sortMaterials = (materials: MaterialInStockDTO[]) =>
  [...materials].sort((a, b) => {
    const depositOrder = a.depositName.localeCompare(b.depositName);
    if (depositOrder !== 0) return depositOrder;
    return Number(b.isTruck) - Number(a.isTruck);
  });

const MATERIALS_CACHE_TIME_MS = 30_000;
const ORDER_HISTORY_CACHE_TIME_MS = 30_000;
const ITEM_TABLE_COLUMN_COUNT = 6;

export default function ReservationManagementSelect() {
  const navigate = useNavigate();
  const location = useLocation();
  const { notify } = useNotify();
  const setPageContext = useAppStore((state) => state.setPageContext);
  const queryClient = useQueryClient();

  const state = location.state as NavigationState | null;
  const initialReserve = state?.reserve ?? null;
  const [reserve, setReserve] = useState<ReserveRequest | null>(() => initialReserve);

  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [teamModalLoading, setTeamModalLoading] = useState(false);
  const [commentModal, setCommentModal] = useState(false);

  const [currentItemId, setCurrentItemId] = useState(0);
  const [truckStockControl, setTruckStockControl] = useState(false);

  const [rawFilteredMaterials, setRawFilteredMaterials] = useState<MaterialInStockDTO[]>([]);
  const [filteredMaterialHistory, setFilteredMaterialHistory] = useState<OrderHistoryItem[]>([]);

  const [showOnlyInStock, setShowOnlyInStock] = useState(true);
  const [selectedBrand, setSelectedBrand] = useState<string | null>(null);

  const [editingMaterialStockId, setEditingMaterialStockId] = useState(0);
  const [quantity, setQuantity] = useState('0');

  const [allowMeasuredMessage, setAllowMeasuredMessage] = useState(true);

  const [showTeamModal, setShowTeamModal] = useState(false);
  const [users, setUsers] = useState<TeamMemberContact[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<TeamMemberContact[]>([]);

  const [showWhatsApp, setShowWhatsApp] = useState(false);
  const [phone, setPhone] = useState('');
  const [text, setText] = useState('');
  const [formSubmitted, setFormSubmitted] = useState(false);
  const fetchRequestMaterials = (contractReferenceItemId: number, teamId: number) =>
    queryClient.fetchQuery({
      queryKey: requestsKeys.materialsByContractReference(contractReferenceItemId, teamId),
      queryFn: () => executionApi.findMaterialsByContractReference(contractReferenceItemId, teamId),
      staleTime: MATERIALS_CACHE_TIME_MS,
    });

  const fetchRequestOrderHistory = (teamId: number, status: string, contractReferenceItemId: number) =>
    queryClient.fetchQuery({
      queryKey: requestsKeys.orderHistoryByStatus(teamId, status, contractReferenceItemId),
      queryFn: () => requestApi.getOrderHistoryByStatus(teamId, status, contractReferenceItemId),
      staleTime: ORDER_HISTORY_CACHE_TIME_MS,
    });

  const reserveMaterialsForExecution = useMutation({
    mutationFn: (reserveRequest: ReserveRequest) => requestApi.reserveMaterialsForExecution(reserveRequest),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: requestsKeys.pendingReservesForStockist() });
    },
  });

  const sendStockNotification = useMutation({
    mutationFn: ({ description, notificationCode, material }: { description: string; notificationCode: string; material: string }) =>
      teamApi.sendStockNotification(description, notificationCode, material),
  });

  useEffect(() => {
    setPageContext(
      ['Solicitações ao Estoquista', 'Gerenciar Estoque de OS'],
      'Gerenciar Estoque de OS',
    );

    if (!initialReserve) {
      navigate('/requisicoes/instalacoes/gerenciamento-estoque', { replace: true });
    }
  }, [initialReserve, navigate, setPageContext]);

  const currentItem = useMemo(() => {
    if (!reserve || currentItemId === 0) return null;
    return reserve.items.find((item) => item.contractItemId === currentItemId) ?? null;
  }, [currentItemId, reserve]);

  const availableBrands = useMemo(() => {
    const brands = Array.from(
      new Set(
        rawFilteredMaterials
          .map((item) => item.materialBrand)
          .filter((brand): brand is string => Boolean(brand)),
      ),
    ).map((brand) => ({ label: brand.toUpperCase(), value: brand }));

    return brands;
  }, [rawFilteredMaterials]);

  const filteredMaterials = useMemo(
    () =>
      rawFilteredMaterials.filter((material) => {
        const inStock = !showOnlyInStock || Number(material.stockAvailable) > 0;
        const byBrand = !selectedBrand || material.materialBrand === selectedBrand;
        return inStock && byBrand;
      }),
    [rawFilteredMaterials, selectedBrand, showOnlyInStock],
  );

  const setItemMaterials = (itemId: number, updater: (materials: ReserveMaterialSelection[]) => ReserveMaterialSelection[]) => {
    setReserve((previous) => {
      if (!previous) return previous;

      return {
        ...previous,
        items: previous.items.map((item) => {
          if (item.contractItemId !== itemId) return item;
          const current = item.materials ?? [];
          return {
            ...item,
            materials: updater(current),
          };
        }),
      };
    });
  };

  const getQuantity = (material: MaterialInStockDTO) => {
    if (!reserve || !currentItem) return null;

    const currentMaterials = currentItem.materials ?? [];
    if (material.isTruck) {
      return (
        currentMaterials.find((item) => item.truckMaterialStockId === material.materialStockId)
          ?.materialQuantity ?? null
      );
    }

    return (
      currentMaterials.find((item) => item.centralMaterialStockId === material.materialStockId)
        ?.materialQuantity ?? null
    );
  };

  const existsMaterial = (material: MaterialInStockDTO) => {
    if (!currentItem) return false;
    const currentMaterials = currentItem.materials ?? [];

    if (material.isTruck) {
      return currentMaterials.some((item) => item.truckMaterialStockId === material.materialStockId);
    }

    return currentMaterials.some((item) => item.centralMaterialStockId === material.materialStockId);
  };

  const fetchMaterials = async (item: ReserveItemRequest) => {
    if (!reserve) return;

    setLoading(true);
    try {
      const response = await fetchRequestMaterials(
        item.contractReferenceItemId,
        reserve.teamId,
      );

      const sorted = sortMaterials(response);
      setRawFilteredMaterials(sorted);
      if (sorted.length === 1 && sorted[0].materialBrand) {
        setSelectedBrand(sorted[0].materialBrand);
      }
    } catch (error: unknown) {
      const messageText = error instanceof Error ? error.message : 'Erro ao carregar materiais.';
      notify(messageText, 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchHistory = async (item: ReserveItemRequest) => {
    if (!reserve) return;

    setLoading(true);
    try {
      const response = await fetchRequestOrderHistory(
        reserve.teamId,
        'COLLECTED',
        item.contractReferenceItemId,
      );

      setFilteredMaterialHistory(response);
    } catch (error: unknown) {
      const messageText = error instanceof Error ? error.message : 'Erro ao carregar histórico.';
      notify(messageText, 'error');
    } finally {
      setLoading(false);
    }
  };

  const openItem = (item: ReserveItemRequest) => {
    if (loading) {
      notify('Aguarde o carregamento atual finalizar.', 'info');
      return;
    }

    setCurrentItemId(item.contractItemId);
    setTruckStockControl(item.truckStockControl);
    setEditingMaterialStockId(0);
    setQuantity('0');
    setSelectedBrand(null);
    setRawFilteredMaterials([]);
    setFilteredMaterialHistory([]);

    if (item.truckStockControl) {
      void fetchMaterials(item);
    } else {
      void fetchHistory(item);
    }
  };

  const closeItem = () => {
    setCurrentItemId(0);
    setEditingMaterialStockId(0);
    setQuantity('0');
    setSelectedBrand(null);
    setRawFilteredMaterials([]);
    setFilteredMaterialHistory([]);
    setShowWhatsApp(false);
    setFormSubmitted(false);
  };

  useEffect(() => {
    if (currentItemId === 0) return;

    const timeout = window.setTimeout(() => {
      const expandedPanel = document.querySelector(
        `[data-reserve-item-panel="${currentItemId}"]`,
      ) as HTMLElement | null;

      if (expandedPanel) {
        expandedPanel.scrollIntoView({
          behavior: 'smooth',
          block: 'end',
          inline: 'nearest',
        });
      }

      // Ajuste final: força o fim absoluto do container com scroll
      const requestScrollContainer = document.querySelector('.js-request-scroll') as HTMLElement | null;
      const mainScrollContainer = document.querySelector('main.overflow-y-auto') as HTMLElement | null;
      const activeContainer = mainScrollContainer ?? requestScrollContainer;

      if (activeContainer) {
        activeContainer.scrollTo({
          top: activeContainer.scrollHeight,
          behavior: 'smooth',
        });
      } else {
        window.scrollTo({
          top: document.documentElement.scrollHeight,
          behavior: 'smooth',
        });
      }
    }, 120);

    return () => window.clearTimeout(timeout);
  }, [currentItemId, filteredMaterials.length, filteredMaterialHistory.length]);

  const confirmAllocation = (material: MaterialInStockDTO) => {
    if (!reserve || !currentItem) {
      notify('Erro ao reservar material, tente novamente.', 'error');
      return;
    }

    const quantityNumber = Number(quantity);
    if (quantityNumber === 0) {
      notify('Informe a quantidade desejada.', 'warn');
      return;
    }

    if (Number(material.stockAvailable) < quantityNumber) {
      notify('O material informado não possui estoque disponível.', 'error');
      return;
    }

    const truckMaterialStockId = filteredMaterials.find(
      (item) => item.materialId === material.materialId && item.depositName === reserve.truckDepositName,
    )?.materialStockId;

    if (!truckMaterialStockId) {
      notify('Referência do material do caminhão não encontrada.', 'error');
      return;
    }

    const currentBalance = Number(currentItem.currentBalance ?? 0);
    if (quantityNumber > currentBalance) {
      notify('A quantidade solicitada é maior que o saldo contratual.', 'error');
      return;
    }

    const measuredQuantity = Number(currentItem.quantity ?? 0);
    if (quantityNumber < measuredQuantity && allowMeasuredMessage) {
      notify(
        `Verifique ${currentItem.description}. A quantidade é menor que a alocada na OS.`,
        'info',
      );
      setAllowMeasuredMessage(false);
    }

    const newMaterial: ReserveMaterialSelection = material.isTruck
      ? {
        centralMaterialStockId: null,
        truckMaterialStockId: material.materialStockId,
        materialQuantity: quantity,
        materialId: material.materialId,
        truckStockControl: true,
      }
      : {
        centralMaterialStockId: material.materialStockId,
        truckMaterialStockId: null,
        materialQuantity: quantity,
        materialId: material.materialId,
        truckStockControl: true,
      };

    const materialStockId = material.isTruck
      ? newMaterial.truckMaterialStockId
      : newMaterial.centralMaterialStockId;

    if (materialStockId == null) {
      notify('Id do material não encontrado.', 'error');
      return;
    }

    if (!existsMaterial(material)) {
      setItemMaterials(currentItem.contractItemId, (previous) => [...previous, newMaterial]);
      notify(`Quantidade alocada: ${quantity}.`, 'success');
    } else {
      const propToCompare = material.isTruck ? 'truckMaterialStockId' : 'centralMaterialStockId';

      setItemMaterials(currentItem.contractItemId, (previous) =>
        previous.map((item) =>
          item[propToCompare] === materialStockId
            ? {
              ...item,
              materialQuantity: newMaterial.materialQuantity,
            }
            : item,
        ),
      );

      notify(`Quantidade alterada para: ${quantity}.`, 'success');
    }

    setEditingMaterialStockId(0);
    setQuantity('0');
  };

  const removeAllocation = (material: MaterialInStockDTO) => {
    if (!currentItem) return;

    setItemMaterials(currentItem.contractItemId, (previous) => {
      if (material.isTruck) {
        return previous.filter((item) => item.truckMaterialStockId !== material.materialStockId);
      }

      return previous.filter((item) => item.centralMaterialStockId !== material.materialStockId);
    });

    notify(`Item removido com sucesso.`, 'success');

    setEditingMaterialStockId(0);
    setQuantity('0');
  };

  const ignore = (closeAfter: boolean) => {
    if (!currentItem) return;

    const existsMarked = (currentItem.materials ?? []).some((item) => !item.truckStockControl);

    if (!existsMarked) {
      setItemMaterials(currentItem.contractItemId, (previous) => [
        ...previous,
        {
          centralMaterialStockId: null,
          truckMaterialStockId: null,
          materialQuantity: '0',
          materialId: null,
          truckStockControl: false,
        },
      ]);
    }

    if (closeAfter) {
      closeItem();
    }
  };

  const notifyTeam = async () => {
    if (!reserve || !currentItem) return;

    const existsNotified = (currentItem.materials ?? []).some((item) => !item.truckStockControl);
    if (existsNotified) {
      notify('Ação já realizada anteriormente.', 'info');
      return;
    }

    try {
      await sendStockNotification.mutateAsync({
        description: reserve.description,
        notificationCode: reserve.teamNotificationCode,
        material: currentItem.description,
      });
      notify('Notificação enviada com sucesso.', 'success');
      ignore(false);
    } catch (error: unknown) {
      const messageText = error instanceof Error ? error.message : 'Não foi possível notificar a equipe.';
      notify(messageText, 'error');
    }
  };

  const verifyTeamData = async () => {
    if (!reserve) return;

    setShowTeamModal(true);
    setTeamModalLoading(true);

    try {
      const existingUsers = users.filter((user) => user.team_id === reserve.teamId);
      if (existingUsers.length > 0) {
        setFilteredUsers(existingUsers);
        const withPhone = existingUsers.find((user) => user.phone_number);
        if (withPhone) setPhone(normalizePhoneDigits(withPhone.phone_number));
        return;
      }

      const teamData = await getGenericObject<Array<{ user_id: string }>>({
        fields: ['user_id'],
        table: 'app_user',
        where: 'team_id',
        equal: [reserve.teamId],
      });

      const uuid = teamData.map((userData) => userData.user_id);
      if (uuid.length === 0) {
        setFilteredUsers([]);
        return;
      }

      const userData = await getGenericObject<
        Array<{ name: string; last_name: string; phone_number: string }>
      >({
        fields: ['name', 'last_name', 'phone_number'],
        table: 'app_user',
        where: 'user_id',
        equal: uuid,
      });

      const nextUsers = userData.map((entry) => ({
        ...entry,
        team_id: reserve.teamId,
      }));

      setUsers((previous) => [...previous, ...nextUsers]);
      setFilteredUsers(nextUsers);

      const withPhone = nextUsers.find((entry) => entry.phone_number);
      if (withPhone) setPhone(normalizePhoneDigits(withPhone.phone_number));
    } catch (error: unknown) {
      const messageText = error instanceof Error ? error.message : 'Erro ao buscar dados da equipe.';
      notify(messageText, 'error');
    } finally {
      setTeamModalLoading(false);
    }
  };

  const openWhatsApp = async () => {
    if (!reserve || !currentItem) return;

    await verifyTeamData();

    setText(`Equipe,\n\nSerá feita a instalação:\n${reserve.description}\n\nMaterial necessário:\n${currentItem.description}\n\nConfiram se têm esse material.\nSe não tiverem, solicitem pelo aplicativo.\n\nObrigado.`);
    setShowWhatsApp(true);
  };

  const onSubmitWhatsapp = () => {
    setFormSubmitted(true);

    const normalizedPhone = normalizePhoneDigits(phone);
    if (normalizedPhone.length < 10 || !text.trim()) {
      notify('Preencha telefone e mensagem para continuar.', 'warn');
      return;
    }

    const url = `https://wa.me/55${normalizedPhone}?text=${encodeURIComponent(text)}`;
    window.open(url, '_blank');

    setShowWhatsApp(false);
    setFormSubmitted(false);
    setPhone('');
    setText('');
    ignore(true);
  };

  const sendData = async () => {
    if (!reserve) return;
    if (reserveMaterialsForExecution.isPending) return;

    const hasUndefinedItems = reserve.items.some((item) => item.materials === undefined);
    if (hasUndefinedItems) {
      notify('Existem itens pendentes.', 'warn');
      return;
    }

    const hasPendingItems = reserve.items.some((item) => (item.materials ?? []).length === 0);
    if (hasPendingItems) {
      notify('Existem itens pendentes.', 'warn');
      return;
    }

    setLoading(true);
    try {
      const response = await reserveMaterialsForExecution.mutateAsync(reserve);
      setMessage(response.message);
    } catch (error: unknown) {
      const messageText = error instanceof Error ? error.message : 'Não foi possível salvar.';
      notify(messageText, 'error');
    } finally {
      setLoading(false);
    }
  };

  const getTotalQuantity = (materials: ReserveMaterialSelection[] | undefined) =>
    (materials ?? []).reduce((total, material) => total + Number(material.materialQuantity), 0);

  const onMaterialRowClick = (material: MaterialInStockDTO) => {
    if (editingMaterialStockId !== 0 && editingMaterialStockId !== material.materialStockId) {
      notify(
        'Conclua ou cancele a edição atual antes de editar outro material.',
        'warn',
      );
      return;
    }

    setEditingMaterialStockId(material.materialStockId);
    setQuantity(getQuantity(material) ?? '0');
  };

  if (!reserve) {
    return null;
  }

  return (
    <section className="js-request-scroll relative p-4 md:p-6 space-y-4">
      <header className="rounded-2xl border border-slate-200 bg-white px-4 py-4 dark:border-zinc-800 dark:bg-zinc-900">
        <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
          <div>
            <div className="flex items-center gap-3">
              <i className="pi pi-list-check text-sm text-slate-400 dark:text-zinc-500" />
              <p className="text-xs font-bold uppercase tracking-[0.16em] text-slate-700 dark:text-zinc-300">
                {reserve.description}
              </p>
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] text-slate-600 dark:text-zinc-400">
              <span className="inline-flex items-center gap-1 rounded-full border border-sky-200 bg-sky-50 px-2 py-0.5 font-semibold text-sky-700 dark:border-sky-900/60 dark:bg-sky-950/30 dark:text-sky-300">
                <i className="pi pi-compass text-[10px]" />
                Guia rápido
              </span>
              <span>1. Verifique o estoque no caminhão</span>
              <span className="text-slate-300 dark:text-zinc-700">•</span>
              <span>2. Aloque somente a diferença</span>
              <span className="text-slate-300 dark:text-zinc-700">•</span>
              <span>3. Valide saldo contratual antes de concluir</span>
            </div>
          </div>
        </div>
      </header>

      {loading && (
        <div className="flex min-h-64 items-center justify-center rounded-2xl border border-slate-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
          <i className="pi pi-spin pi-spinner text-2xl text-blue-500" />
        </div>
      )}

      {message && (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-8 text-center dark:border-emerald-900/40 dark:bg-emerald-950/20">
          <i className="pi pi-check-circle text-5xl text-emerald-500" />
          <h2 className="mt-3 text-xl font-semibold text-emerald-800 dark:text-emerald-200">{message}</h2>
          <button
            type="button"
            onClick={() => navigate('/requisicoes/instalacoes/gerenciamento-estoque')}
            className="mt-5 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500"
          >
            Voltar para solicitações
          </button>
        </div>
      )}

      {!message && (
        <>
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
            <div className="overflow-x-auto">
              <table className="min-w-[980px] w-full">
                <thead className={`${currentItem ? 'hidden' : ''} bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500 dark:bg-zinc-900/50 dark:text-zinc-400`}>
                  <tr>
                    <th className="w-16 px-4 py-3" />
                    <th className="px-4 py-3 font-semibold">Descrição</th>
                    <th className="px-4 py-3 font-semibold">Solicitado</th>
                    <th className="px-4 py-3 font-semibold">Alocado</th>
                    <th className="px-4 py-3 font-semibold">Saldo Contratual</th>
                    <th className="px-4 py-3 font-semibold">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {reserve.items.map((item) => {
                    const isActive = currentItemId === item.contractItemId;
                    const dimmed = currentItemId !== 0 && !isActive;
                    const allocationDone = (item.materials ?? []).length > 0;

                    return (
                      <Fragment key={item.contractItemId}>
                        <tr
                          onClick={() => openItem(item)}
                          className={`border-b border-slate-100 transition dark:border-zinc-800 ${currentItem ? 'hidden' : ''
                            } ${dimmed ? 'opacity-35' : 'opacity-100'
                            } ${isActive
                              ? 'bg-blue-50/60 dark:bg-blue-900/20'
                              : 'bg-white hover:bg-slate-50 dark:bg-zinc-900 dark:hover:bg-zinc-800/70'
                            } cursor-pointer`}
                        >
                          <td className="w-16 px-4 py-3">
                            <span className={`inline-flex h-9 w-9 items-center justify-center rounded-lg border ${isActive
                              ? 'border-blue-300 bg-blue-100 text-blue-700 dark:border-blue-700 dark:bg-blue-900/40 dark:text-blue-300'
                              : 'border-slate-200 bg-white text-slate-500 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-400'
                              }`}>
                              <i className="pi pi-external-link text-sm" />
                            </span>
                          </td>
                          <td className="px-4 py-3 text-sm font-medium text-slate-800 dark:text-zinc-100">
                            {item.description}
                          </td>
                          <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">
                            {formatDecimalForDisplay(item.quantity)}
                          </td>
                          <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">
                            {formatDecimalForDisplay(getTotalQuantity(item.materials))}
                          </td>
                          <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">
                            {formatDecimalForDisplay(item.currentBalance)}
                          </td>
                          <td className="px-4 py-3">
                            <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${allocationDone
                              ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                              : 'bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300'
                              }`}>
                              {allocationDone ? 'CONCLUÍDO' : 'PENDENTE'}
                            </span>
                          </td>
                        </tr>

                        {/* Expanded details row spans all summary columns */}
                        {isActive && (
                          <tr>
                            <td colSpan={ITEM_TABLE_COLUMN_COUNT} className="p-0">
                              <div>
                                <div
                                  data-reserve-item-panel={item.contractItemId}
                                  className="border rounded-2xl border-blue-300 bg-white p-5 shadow-sm dark:border-blue-700 dark:bg-zinc-900"
                                >
                                  <div className="mb-5 flex flex-col justify-between gap-4 border-b border-slate-100 pb-4 dark:border-zinc-800 sm:flex-row sm:items-center">
                                    <div className="flex items-center gap-3 text-slate-800 dark:text-zinc-200">
                                      <span className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-50 text-blue-600 dark:bg-blue-900/40 dark:text-blue-300">
                                        <i className="pi pi-cog text-sm" />
                                      </span>
                                      <span className="text-base font-bold">{currentItem?.description}</span>
                                    </div>

                                    <div className="flex items-center gap-5">
                                      <div className="flex flex-col">
                                        <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-400 dark:text-zinc-500">
                                          Qtd Solicitada
                                        </span>
                                        <span className="text-sm font-bold text-slate-700 dark:text-zinc-200">
                                          {formatDecimalForDisplay(currentItem?.quantity)}
                                        </span>
                                      </div>

                                      <span className="hidden h-8 w-px bg-slate-200 dark:bg-zinc-700 sm:block" />

                                      <div className="flex flex-col">
                                        <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-400 dark:text-zinc-500">
                                          Saldo Contratual
                                        </span>
                                        <span className="text-sm font-bold text-blue-600 dark:text-blue-300">
                                          {formatDecimalForDisplay(currentItem?.currentBalance)}
                                        </span>
                                      </div>

                                      <button
                                        type="button"
                                        onClick={closeItem}
                                        className="ml-1 inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-100"
                                        title="Fechar"
                                      >
                                        <i className="pi pi-times text-sm" />
                                      </button>
                                    </div>
                                  </div>

                                  {truckStockControl ? (
                                    <>
                                      {rawFilteredMaterials.length > 0 && (
                                        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-zinc-800 dark:bg-zinc-900/40">

                                          <Toggle
                                            variant="outline"
                                            aria-label="Filtro: apenas com estoque disponível"
                                            pressed={showOnlyInStock}
                                            onPressedChange={(pressed) => setShowOnlyInStock(pressed)}
                                            className="h-9 gap-2 border-slate-300 bg-white px-3 text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200 dark:hover:bg-zinc-800 data-[state=on]:border-blue-300 data-[state=on]:bg-blue-50 data-[state=on]:text-blue-700 dark:data-[state=on]:border-blue-700 dark:data-[state=on]:bg-blue-900/30 dark:data-[state=on]:text-blue-300"
                                          >
                                            <Package className="size-4" />
                                            <span className="text-xs font-semibold uppercase tracking-wide">Filtro</span>
                                            <span className="h-4 w-px bg-slate-200 dark:bg-zinc-700" />
                                            <span className="text-sm font-medium">
                                              Apenas com estoque
                                            </span>
                                            <span
                                              className={`ml-1 inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold ${showOnlyInStock
                                                ? 'bg-blue-100 text-blue-700 dark:bg-blue-800/40 dark:text-blue-200'
                                                : 'bg-slate-100 text-slate-500 dark:bg-zinc-800 dark:text-zinc-400'
                                                }`}
                                            >
                                              {showOnlyInStock ? 'ATIVO' : 'INATIVO'}
                                            </span>
                                          </Toggle>

                                          <div className="min-w-[180px]">
                                            <GlassListbox
                                              value={selectedBrand}
                                              onChange={(value) => setSelectedBrand(value === null ? null : String(value))}
                                              placeholder="Filtrar por marca"
                                              disabled={availableBrands.length <= 1}
                                              disabledTooltip={availableBrands.length <= 1 ? 'Não há marcas diferentes para filtrar' : undefined}
                                              options={[
                                                { value: null, label: 'Filtrar por marca' },
                                                ...availableBrands.map((brand) => ({
                                                  value: brand.value,
                                                  label: brand.label,
                                                })),
                                              ]}
                                            />
                                          </div>
                                        </div>
                                      )}

                                      <div className="overflow-x-auto">
                                        <table className="min-w-[760px] w-full">
                                          <thead className={`${filteredMaterials.length === 0 ? 'hidden' : ''} bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500 dark:bg-zinc-900/40 dark:text-zinc-400`}>
                                            <tr>
                                              <th className="px-3 py-2 font-semibold w-[45%]">Descrição</th>
                                              <th className="px-3 py-2 font-semibold">Estoque</th>
                                              <th className="px-3 py-2 font-semibold">Almoxarifado</th>
                                              <th className="px-3 py-2 font-semibold w-[20%]">Quantidade</th>
                                            </tr>
                                          </thead>
                                          <tbody>
                                            {filteredMaterials.map((material) => {
                                              const inEdit = editingMaterialStockId === material.materialStockId;
                                              const hasStock = Number(material.stockAvailable) > 0;
                                              return (
                                                <tr
                                                  key={material.materialStockId}
                                                  onClick={(event) => {
                                                    const target = event.target as HTMLElement;
                                                    if (target.closest('button')) return;
                                                    onMaterialRowClick(material);
                                                  }}
                                                  className="cursor-pointer border-t border-slate-100 dark:border-zinc-800"
                                                >
                                                  <td className="px-3 py-2 text-sm font-medium text-slate-700 dark:text-zinc-200 truncate">
                                                    <Tooltip>
                                                      <TooltipTrigger>
                                                        {material.materialName}
                                                      </TooltipTrigger>
                                                      <TooltipContent>
                                                        {material.materialName}
                                                      </TooltipContent>
                                                    </Tooltip>
                                                  </td>
                                                  <td className="px-3 py-2">
                                                    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${hasStock
                                                      ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                                                      : 'bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300'
                                                      }`}>
                                                      {hasStock ? `${material.stockAvailable} ${material.requestUnit}` : 'Sem Estoque'}
                                                    </span>
                                                  </td>
                                                  <td className="px-3 py-2 truncate">
                                                    <Tooltip>
                                                      <TooltipTrigger>
                                                        <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${material.isTruck
                                                          ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300'
                                                          : 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300'
                                                          }`}>
                                                          {material.isTruck ? `CAMINHÃO PLACA - ${material.plateVehicle ?? '-'}` : material.depositName}
                                                        </span>
                                                      </TooltipTrigger>
                                                      <TooltipContent>
                                                        {material.isTruck ? `CAMINHÃO PLACA - ${material.plateVehicle ?? '-'}` : material.depositName}
                                                      </TooltipContent>

                                                    </Tooltip>
                                                  </td>
                                                  <td className="px-3 py-2">
                                                    {inEdit ? (
                                                      <div className="flex item-center gap-2">
                                                        <div className='relative'>
                                                          <input
                                                            autoFocus
                                                            value={quantity}
                                                            onChange={(event) => setQuantity(normalizeFloatInput(event.target.value))}
                                                            className="w-full max-w-[120px] rounded-lg border border-slate-200 px-2 py-1 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-white"
                                                            placeholder="Qtd"
                                                            onKeyDown={(event) => {
                                                              if (event.key === 'Enter') {
                                                                event.preventDefault();
                                                                confirmAllocation(material);
                                                              } else if (event.key === 'Escape') {
                                                                event.preventDefault();
                                                                setEditingMaterialStockId(0);
                                                                setQuantity('0');
                                                              }
                                                            }}
                                                          />

                                                          <b
                                                            onClick={() => confirmAllocation(material)}
                                                            className="text-xs text-zinc-700 dark:text-zinc-300 absolute right-0 mr-2 top-1/2 transform -translate-y-1/2">↵</b>
                                                        </div>

                                                        <button
                                                          type="button"
                                                          onClick={() => removeAllocation(material)}
                                                          className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-red-600 transition hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-900/20"
                                                          title={existsMaterial(material) ? 'Remover' : 'Cancelar'}
                                                        >
                                                          <i className={`pi ${existsMaterial(material) ? 'pi-trash' : 'pi-times'} text-sm`} />
                                                        </button>
                                                        <button
                                                          type="button"
                                                          onClick={() => confirmAllocation(material)}
                                                          className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-blue-500 transition hover:bg-blue-400 hover:text-white"
                                                          title="Confirmar"
                                                        >
                                                          <i className="pi pi-check text-sm" />
                                                        </button>
                                                      </div>
                                                    ) : (
                                                      <CellEdit
                                                        onClick={() => { }}
                                                        children={<span className="text-sm text-slate-700 dark:text-zinc-200">
                                                          {formatDecimalForDisplay(getQuantity(material) ?? '0')}</span>
                                                        }
                                                      />
                                                    )}
                                                  </td>
                                                </tr>
                                              );
                                            })}
                                          </tbody>
                                        </table>
                                      </div>

                                      {filteredMaterials.length === 0 && (
                                        <div className="rounded-xl border border-slate-200 bg-slate-50 p-6 text-center text-sm text-slate-600 dark:border-zinc-800 dark:bg-zinc-900/40 dark:text-zinc-300">
                                          <i className="pi pi-box mb-3 text-3xl text-blue-300 dark:text-blue-700" />
                                          <p className="font-semibold">Nenhum material encontrado</p>
                                          <p className="mt-1 leading-relaxed">
                                            {rawFilteredMaterials.length === 0
                                              ? 'Nenhum material foi encontrado para este item contratual. Vincule o material para continuar.'
                                              : 'Existem materiais vinculados, mas nenhum possui saldo disponível em estoque.'}
                                          </p>
                                          <div className="mt-3">
                                            <button
                                              type="button"
                                              onClick={() =>
                                                navigate(
                                                  rawFilteredMaterials.length === 0
                                                    ? '/contratos/itens-contratuais/vinculos?operation=material'
                                                    : '/estoque/movimentar-estoque?operation=material',
                                                )
                                              }
                                              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-blue-500"
                                            >
                                              <i className={`pi ${rawFilteredMaterials.length === 0 ? 'pi-link' : 'pi-arrow-right-arrow-left'}`} />
                                              {rawFilteredMaterials.length === 0 ? 'Vincular Material' : 'Movimentar Estoque'}
                                            </button>
                                          </div>
                                        </div>
                                      )}
                                    </>
                                  ) : showWhatsApp ? (
                                    <form
                                      onSubmit={(event) => {
                                        event.preventDefault();
                                        onSubmitWhatsapp();
                                      }}
                                      className="space-y-4 rounded-xl border border-slate-200 bg-slate-50 p-4 dark:border-zinc-800 dark:bg-zinc-900/40"
                                    >
                                      <h4 className="text-sm font-semibold text-slate-700 dark:text-zinc-200">Contato via WhatsApp</h4>

                                      <div>
                                        <label className="mb-1 block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400">
                                          Telefone da equipe
                                        </label>
                                        <input
                                          value={formatPhone(phone)}
                                          onChange={(event) => setPhone(normalizePhoneDigits(event.target.value))}
                                          className="w-full rounded-lg border border-slate-200 px-2.5 py-2 text-sm dark:border-zinc-700 dark:bg-zinc-900"
                                          placeholder="(00) 00000-0000"
                                        />
                                        {formSubmitted && normalizePhoneDigits(phone).length < 10 && (
                                          <p className="mt-1 text-xs text-red-500">Telefone obrigatório (DDD + número)</p>
                                        )}
                                      </div>

                                      <div>
                                        <label className="mb-1 block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400">
                                          Mensagem
                                        </label>
                                        <textarea
                                          value={text}
                                          onChange={(event) => setText(event.target.value)}
                                          rows={5}
                                          className="w-full rounded-lg border border-slate-200 px-2.5 py-2 text-sm dark:border-zinc-700 dark:bg-zinc-900"
                                        />
                                        {formSubmitted && !text.trim() && (
                                          <p className="mt-1 text-xs text-red-500">Mensagem obrigatória</p>
                                        )}
                                      </div>

                                      <div className="flex justify-end gap-2">
                                        <button
                                          type="button"
                                          onClick={() => setShowWhatsApp(false)}
                                          className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
                                        >
                                          Voltar
                                        </button>
                                        <button
                                          type="submit"
                                          className="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-emerald-500"
                                        >
                                          Enviar via WhatsApp
                                        </button>
                                      </div>
                                    </form>
                                  ) : (
                                    <>
                                      <div>
                                        <h4 className="text-sm font-semibold text-slate-800 dark:text-zinc-200">Movimentações do material</h4>
                                        <p className="text-xs text-slate-500 dark:text-zinc-400">Registros coletados vinculados a equipes.</p>
                                      </div>

                                      <div className="overflow-x-auto">
                                        <table className="min-w-[680px] w-full">
                                          <thead className="bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500 dark:bg-zinc-900/40 dark:text-zinc-400">
                                            <tr>
                                              <th className="px-3 py-2 font-semibold">OS/Req.</th>
                                              <th className="px-3 py-2 font-semibold">Material</th>
                                              <th className="px-3 py-2 font-semibold">Equipe</th>
                                              <th className="px-3 py-2 font-semibold">Qtd</th>
                                              <th className="px-3 py-2 font-semibold">Data</th>
                                            </tr>
                                          </thead>
                                          <tbody>
                                            {filteredMaterialHistory.map((historyItem, index) => (
                                              <tr key={`${historyItem.orderCode}-${index}`} className="border-t border-slate-100 dark:border-zinc-800">
                                                <td className="px-3 py-2 text-sm text-slate-700 dark:text-zinc-200">{historyItem.orderCode}</td>
                                                <td className="px-3 py-2 text-sm text-slate-700 dark:text-zinc-200">{historyItem.materialName}</td>
                                                <td className="px-3 py-2 text-sm text-slate-700 dark:text-zinc-200">{historyItem.teamName}</td>
                                                <td className="px-3 py-2 text-sm text-slate-700 dark:text-zinc-200">{formatDecimalForDisplay(historyItem.quantityReleased)}</td>
                                                <td className="px-3 py-2 text-sm text-slate-700 dark:text-zinc-200">
                                                  {new Date(historyItem.createdAt).toLocaleDateString('pt-BR')}
                                                </td>
                                              </tr>
                                            ))}
                                          </tbody>
                                        </table>
                                      </div>

                                      {filteredMaterialHistory.length === 0 && (
                                        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/20 dark:text-amber-300">
                                          Nenhuma movimentação registrada. Confirme com a equipe se há material disponível em campo.
                                        </div>
                                      )}

                                      <div className="flex flex-wrap justify-end gap-2 pt-2">
                                        <button
                                          type="button"
                                          onClick={() => ignore(true)}
                                          className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
                                        >
                                          Possui estoque no caminhão
                                        </button>
                                        <button
                                          type="button"
                                          onClick={() => void notifyTeam()}
                                          className="rounded-lg bg-slate-900 px-3 py-1.5 text-sm font-semibold text-white hover:bg-slate-700 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
                                        >
                                          Notificar equipe
                                        </button>
                                        <button
                                          type="button"
                                          onClick={() => void openWhatsApp()}
                                          className="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-emerald-500"
                                        >
                                          WhatsApp
                                        </button>
                                      </div>
                                    </>
                                  )}
                                </div>
                              </div>
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          <div
            className={`sticky bottom-0 z-10 px-1 pb-1 pt-2 ${currentItemId > 0 ? 'hidden' : 'block'
              }`}
          >
            <div className="mx-auto mt rounded-2xl border border-slate-200/80 bg-linear-to-b from-white/98 to-slate-50/92 px-3 py-3 shadow-[0_-10px_24px_-18px_rgba(15,23,42,0.45)] backdrop-blur dark:border-zinc-800/80 dark:from-zinc-950/98 dark:to-zinc-900/92">
              <div className="grid w-full gap-3 md:grid-cols-[1fr_auto] md:items-center">
                <div className="flex flex-col gap-2">
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-100 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-slate-600 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300">
                      <i className="pi pi-bolt text-[10px]" />
                      Ações rápidas
                    </span>
                    <p className="text-xs text-slate-500 dark:text-zinc-400">
                      Revise comentários e contatos antes de concluir o envio.
                    </p>
                  </div>
                  <p className="text-sm font-medium text-slate-700 dark:text-zinc-200">
                    Equipe responsável: <span className="font-semibold">{reserve.teamName}</span>
                  </p>

                  <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center">
                    <button
                      type="button"
                      onClick={() => navigate('/requisicoes/instalacoes/gerenciamento-estoque')}
                      className="inline-flex items-center gap-2 rounded-xl border border-slate-300/90 bg-white px-3.5 py-2 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-100 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200 dark:hover:bg-zinc-800"
                    >
                      <i className="pi pi-arrow-left text-xs" />
                      Voltar
                    </button>

                    <button
                      type="button"
                      onClick={() => setCommentModal(true)}
                      className="inline-flex items-center gap-2 rounded-xl border border-slate-300/90 bg-white px-3.5 py-2 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-100 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200 dark:hover:bg-zinc-800"
                    >
                      <i className="pi pi-comment text-xs" />
                      Comentários
                    </button>

                    <button
                      type="button"
                      onClick={() => void verifyTeamData()}
                      disabled={teamModalLoading}
                      className="inline-flex items-center gap-2 rounded-xl border border-blue-300/90 bg-blue-50 px-3.5 py-2 text-sm font-semibold text-blue-700 transition-colors hover:bg-blue-100 dark:border-blue-800 dark:bg-blue-950/30 dark:text-blue-300 dark:hover:bg-blue-900/40"
                    >
                      <i className={`pi text-xs ${teamModalLoading ? 'pi-spin pi-spinner' : 'pi-users'}`} />
                      Contatos da equipe
                    </button>
                  </div>
                </div>

                <div className="flex items-center justify-stretch lg:justify-end">
                  <button
                    type="button"
                    onClick={() => void sendData()}
                    disabled={reserveMaterialsForExecution.isPending}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-slate-700 lg:w-auto dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
                  >
                    <i className={`pi text-xs ${reserveMaterialsForExecution.isPending ? 'pi-spin pi-spinner' : 'pi-send'}`} />
                    Salvar e enviar solicitação
                  </button>
                </div>
              </div>
            </div>
          </div>
        </>
      )}

      <Modal
        open={commentModal}
        onClose={() => setCommentModal(false)}
        className="max-h-[80vh] max-w-lg"
      >
        <ModalHeader title="Comentários da solicitação" onClose={() => setCommentModal(false)} />
        <ModalBody className="space-y-4">
          {reserve.comment ? (
            <div className='flex flex-col'>
              <p className="text-sm font-semibold text-slate-800 dark:text-zinc-100">{reserve.assignedBy}</p>
              <p className="mt-1 text-sm text-slate-600 dark:text-zinc-300">{reserve.comment}</p>
            </div>
          ) : (
            <div className="flex h-24 items-center justify-center text-sm text-slate-500 dark:text-zinc-400">
              Nenhum comentário registrado para esta solicitação.
            </div>
          )}
        </ModalBody>
      </Modal>

      <Modal
        open={showTeamModal}
        onClose={() => setShowTeamModal(false)}
        className="max-h-[80vh] max-w-lg"
      >
        <ModalHeader title="Contatos da equipe" onClose={() => setShowTeamModal(false)} />
        <ModalBody className="space-y-4">
          {teamModalLoading ? (
              <div className="flex h-32 items-center justify-center">
                <i className="pi pi-spin pi-spinner text-xl text-blue-500" />
              </div>
            ) : filteredUsers.length > 0 ? (
              <ul className="space-y-3">
                {filteredUsers.map((teamUser, index) => (
                  <li key={`${teamUser.name}-${index}`} className="rounded-xl border border-slate-200 p-3 dark:border-zinc-800">
                    <p className="text-sm font-semibold text-slate-800 dark:text-zinc-100">
                      {teamUser.name} {teamUser.last_name}
                    </p>
                    <div className="mt-1 flex items-center justify-between gap-2">
                      <span className="text-sm text-slate-600 dark:text-zinc-300">
                        {teamUser.phone_number ? formatPhone(teamUser.phone_number) : 'Telefone não cadastrado'}
                      </span>

                      {teamUser.phone_number && (
                        <button
                          type="button"
                          onClick={() => {
                            navigator.clipboard
                              .writeText(teamUser.phone_number)
                              .then(() => notify('Número copiado.', 'success'))
                              .catch(() => notify('Não foi possível copiar o número.', 'error'));
                          }}
                          className="rounded-md border border-slate-300 px-2 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
                        >
                          Copiar
                        </button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <div className="flex h-24 items-center justify-center text-sm text-slate-500 dark:text-zinc-400">
                Contato não cadastrado para essa equipe.
              </div>
            )}
        </ModalBody>
      </Modal>

      
    </section>
  );
}
