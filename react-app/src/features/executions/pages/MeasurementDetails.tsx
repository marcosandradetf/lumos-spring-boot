import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { preMeasurementsApi } from '@/features/pre-measurement/api/preMeasurementsApi';
import type {
  AvailableStockByStreet,
  DelegateExecutionDTO,
  PreMeasurementResponseDTO,
} from '@/features/pre-measurement/types/types';
import { manageApi } from '@/features/manage/api/manageApi';
import type { TeamModel } from '@/features/manage/types/manageTypes';
import { stockistApi } from '../../stock/api/stockist-api';
import type { StockistModel } from '@/features/requests/types/reservation';
import { Modal, ModalBody, ModalFooter, ModalHeader } from '@/shared/ui/modal';
import { GlassListbox } from '@/shared/components/glass-list-box';

const findFirstPendingStreetId = (
  preMeasurement: PreMeasurementResponseDTO,
  completedStreetIds: number[],
) => {
  const pending = preMeasurement.streets.find(
    (street) => !completedStreetIds.includes(street.preMeasurementStreetId),
  );
  return pending?.preMeasurementStreetId ?? 0;
};

export default function MeasurementDetails() {
  const { id = '' } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const isMultiTeam = searchParams.get('multiTeam') === 'true';

  const navigate = useNavigate();
  const { notify } = useNotify();
  const setPageContext = useAppStore((state) => state.setPageContext);

  const [loading, setLoading] = useState(true);
  const [showMessage, setShowMessage] = useState(false);

  const [preMeasurement, setPreMeasurement] = useState<PreMeasurementResponseDTO | null>(null);
  const [teams, setTeams] = useState<TeamModel[]>([]);
  const [stockists, setStockists] = useState<StockistModel[]>([]);

  const [delegateDTO, setDelegateDTO] = useState<DelegateExecutionDTO>({
    preMeasurementId: 0,
    description: '',
    stockistId: '',
    stockistName: '',
    stockistPhone: '',
    stockistDepositName: '',
    stockistDepositAddress: '',
    preMeasurementStep: 0,
    teamId: 0,
    comment: '',
    street: [],
  });

  const [streetId, setStreetId] = useState(0);
  const [completedStreetIds, setCompletedStreetIds] = useState<number[]>([]);
  const [finish, setFinish] = useState(false);

  const [openTeamModal, setOpenTeamModal] = useState(false);
  const [openStockistPanel, setOpenStockistPanel] = useState(false);
  const [selectedStockistId, setSelectedStockistId] = useState('');

  const [localStockStreet, setLocalStockStreet] = useState<AvailableStockByStreet[]>([]);
  const streetsSliderRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setPageContext(['Pré-medições', 'Delegar Execução'], 'Delegar Execução');

    if (!id) return;

    setLoading(true);

    Promise.all([
      preMeasurementsApi.getPreMeasurement(id),
      manageApi.getTeams(),
      stockistApi.getStockists(),
    ])
      .then(([preMeasurementData, teamsData, stockistsData]) => {
        if (preMeasurementData.status !== 'AVAILABLE') {
          notify('Essa pré-medição não está disponível para delegação.', 'warn');
          navigate('/pre-medicao/disponivel', { replace: true });
          return;
        }

        setPreMeasurement(preMeasurementData);
        setTeams(teamsData);
        setStockists(stockistsData);
        setStreetId(preMeasurementData.streets[0]?.preMeasurementStreetId ?? 0);

        setDelegateDTO((previous) => ({
          ...previous,
          preMeasurementId: preMeasurementData.preMeasurementId,
          preMeasurementStep: preMeasurementData.step,
          description: `Etapa ${preMeasurementData.step} - ${preMeasurementData.city}`,
        }));

        if (!isMultiTeam) {
          setOpenTeamModal(true);
        }
      })
      .catch((error: unknown) => {
        const message = error instanceof Error ? error.message : 'Erro ao carregar pré-medição.';
        notify(message, 'error');
      })
      .finally(() => setLoading(false));
  }, [id, isMultiTeam, navigate, notify, setPageContext]);

  const ensureStreetReserve = (streetIdParam: number) => {
    setDelegateDTO((previous) => {
      const exists = previous.street.some((street) => street.preMeasurementStreetId === streetIdParam);
      if (exists) return previous;

      return {
        ...previous,
        street: [
          ...previous.street,
          {
            preMeasurementStreetId: streetIdParam,
            teamName: '',
            truckDepositName: '',
            prioritized: false,
          },
        ],
      };
    });
  };

  useEffect(() => {
    if (streetId > 0) {
      ensureStreetReserve(streetId);
    }
  }, [streetId]);

  const currentStreetReserve = useMemo(
    () => delegateDTO.street.find((street) => street.preMeasurementStreetId === streetId) ?? null,
    [delegateDTO.street, streetId],
  );

  const streetTeam = useMemo(
    () => teams.find((team) => team.teamName === currentStreetReserve?.teamName) ?? null,
    [currentStreetReserve?.teamName, teams],
  );

  const currentStreet = useMemo(
    () => preMeasurement?.streets.find((street) => street.preMeasurementStreetId === streetId) ?? null,
    [preMeasurement, streetId],
  );

  const loadStockByTeam = (teamId: number) => {
    if (!preMeasurement) return;

    preMeasurementsApi
      .getStockAvailable(preMeasurement.preMeasurementId, teamId)
      .then((data) => setLocalStockStreet(data))
      .catch(() => setLocalStockStreet([]));
  };

  const scrollStreets = (offset: number) => {
    streetsSliderRef.current?.scrollBy({ left: offset, behavior: 'smooth' });
  };

  const openStockistSelector = () => {
    setSelectedStockistId(delegateDTO.stockistId || '');
    setOpenStockistPanel(true);
  };

  const selectTeam = (team: TeamModel) => {
    if (!preMeasurement) return;

    if (isMultiTeam) {
      if (!streetId) return;

      setDelegateDTO((previous) => {
        const hasCurrent = previous.street.some((street) => street.preMeasurementStreetId === streetId);

        if (!hasCurrent) {
          return {
            ...previous,
            street: [
              ...previous.street,
              {
                preMeasurementStreetId: streetId,
                teamName: team.teamName,
                truckDepositName: team.depositName,
                prioritized: false,
              },
            ],
          };
        }

        return {
          ...previous,
          street: previous.street.map((street) =>
            street.preMeasurementStreetId === streetId
              ? {
                  ...street,
                  teamName: team.teamName,
                  truckDepositName: team.depositName,
                }
              : street,
          ),
        };
      });

      loadStockByTeam(Number(team.idTeam));
      notify(`Equipe ${team.teamName} definida para a rua atual.`, 'success');
      setOpenTeamModal(false);
      return;
    }

    setDelegateDTO((previous) => ({
      ...previous,
      teamId: Number(team.idTeam),
      street: preMeasurement.streets.map((street) => ({
        preMeasurementStreetId: street.preMeasurementStreetId,
        teamName: team.teamName,
        truckDepositName: team.depositName,
        prioritized: false,
      })),
    }));

    loadStockByTeam(Number(team.idTeam));
    setFinish(true);
    setOpenTeamModal(false);

    if (!delegateDTO.stockistId) {
      openStockistSelector();
    }
  };

  const selectStockist = () => {
    if (!selectedStockistId) {
      notify('A seleção do estoquista é obrigatória.', 'warn');
      return;
    }

    const stockist = stockists.find((item) => item.userId === selectedStockistId);
    if (!stockist || !preMeasurement) return;

    setDelegateDTO((previous) => ({
      ...previous,
      stockistId: stockist.userId,
      stockistName: stockist.name,
      stockistDepositName: stockist.depositName,
      stockistDepositAddress: stockist.depositAddress ?? '',
      stockistPhone: stockist.depositPhone ?? '',
      description: `Etapa ${preMeasurement.step} - ${preMeasurement.city}`,
    }));

    if (!isMultiTeam) {
      setFinish(true);
    }

    notify('Estoquista responsável definido com sucesso.', 'success');
    setOpenStockistPanel(false);
  };

  const togglePriority = () => {
    if (!streetId) return;

    setDelegateDTO((previous) => ({
      ...previous,
      street: previous.street.map((street) =>
        street.preMeasurementStreetId === streetId
          ? {
              ...street,
              prioritized: !street.prioritized,
            }
          : street,
      ),
    }));
  };

  const finishStreet = () => {
    if (!preMeasurement || !currentStreetReserve) return;

    if (!currentStreetReserve.teamName) {
      notify('Selecione a equipe da rua antes de concluir.', 'warn');
      return;
    }

    setCompletedStreetIds((previous) => {
      const next = previous.includes(streetId) ? previous : [...previous, streetId];

      const nextStreetId = findFirstPendingStreetId(preMeasurement, next);
      if (nextStreetId === 0) {
        setFinish(true);
        if (!delegateDTO.stockistId) {
          openStockistSelector();
        }
      } else {
        setStreetId(nextStreetId);
      }

      return next;
    });

    notify('Rua salva com sucesso.', 'success');
  };

  const getTruckMaterials = (streetIdParam: number) =>
    localStockStreet.find((street) => street.streetId === streetIdParam)?.materialsInTruck ?? [];

  const sendData = async () => {
    if (!delegateDTO.stockistId) {
      notify('Selecione o estoquista responsável antes de continuar.', 'warn');
      openStockistSelector();
      return;
    }

    if (delegateDTO.street.length === 0) {
      notify('Nenhuma rua foi selecionada para delegação.', 'warn');
      return;
    }

    setLoading(true);
    try {
      await preMeasurementsApi.delegateExecution(delegateDTO);
      setShowMessage(true);
      notify('Execução delegada com sucesso.', 'success');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Erro ao delegar execução.';
      notify(message, 'error');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <section className="flex min-h-64 items-center justify-center p-4 md:p-6">
        <i className="pi pi-spin pi-spinner text-2xl text-blue-500" />
      </section>
    );
  }

  if (!preMeasurement) {
    return null;
  }

  if (showMessage) {
    return (
      <section className="p-4 md:p-6">
        <div className="mx-auto max-w-4xl rounded-2xl border border-emerald-200 bg-emerald-50 p-8 text-center dark:border-emerald-900/40 dark:bg-emerald-950/20">
          <i className="pi pi-check-circle text-5xl text-emerald-500" />
          <h2 className="mt-4 text-xl font-semibold text-emerald-800 dark:text-emerald-200">
            Execução delegada com sucesso
          </h2>
          <p className="mt-2 text-sm text-emerald-700 dark:text-emerald-300">
            A pré-medição está disponível na tela de gerenciamento de estoque - pré-instalação.
          </p>
          <button
            type="button"
            onClick={() => navigate('/requisicoes/instalacoes/gerenciamento-estoque')}
            className="mt-4 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500"
          >
            Abrir gerenciamento de estoque
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="space-y-4 p-4 md:p-6">
      <header className="rounded-xl border border-slate-200 bg-white px-4 py-3 dark:border-zinc-800 dark:bg-zinc-900">
        <h1 className="text-base font-semibold text-slate-800 dark:text-zinc-100">
          Pré-medição - {preMeasurement.city}
        </h1>
        <p className="text-xs text-slate-500 dark:text-zinc-400">Etapa {preMeasurement.step}</p>
      </header>

      {openStockistPanel && (
        <div className="rounded-2xl border border-blue-200 bg-blue-50 p-4 dark:border-blue-900/40 dark:bg-blue-950/20">
          <h3 className="text-sm font-semibold text-blue-800 dark:text-blue-200">Selecionar Estoquista Responsável</h3>
          <p className="mt-1 text-xs text-blue-700 dark:text-blue-300">
            Defina quem irá gerenciar a reserva dos materiais para a execução.
          </p>

          <div className="mt-3 flex flex-wrap items-center gap-2">
            <div className="min-w-72">
              <GlassListbox
                value={selectedStockistId}
                onChange={(value) => setSelectedStockistId(value ?? '')}
                placeholder="Selecione"
                options={[
                  { value: '', label: 'Selecione' },
                  ...stockists.map((stockist) => ({
                    value: stockist.userId,
                    label: stockist.name,
                  })),
                ]}
              />
            </div>

            <button
              type="button"
              onClick={selectStockist}
              className="rounded-xl bg-blue-600 px-3 py-2 text-sm font-semibold text-white hover:bg-blue-500"
            >
              Salvar
            </button>
          </div>
        </div>
      )}

      {finish ? (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-slate-800 dark:text-zinc-100">Resumo da Delegação</h2>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => navigate('/pre-medicao/disponivel')}
                className="rounded-xl border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => setOpenTeamModal(true)}
                className="rounded-xl border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
              >
                Editar equipe
              </button>
              <button
                type="button"
                onClick={openStockistSelector}
                className="rounded-xl border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
              >
                Editar estoquista
              </button>
              <button
                type="button"
                onClick={() => void sendData()}
                className="rounded-xl bg-slate-900 px-3 py-1.5 text-sm font-semibold text-white hover:bg-slate-700 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
              >
                Continuar
              </button>
            </div>
          </div>

          {!!delegateDTO.stockistName && (
            <div className="rounded-xl border border-slate-200 bg-white p-3 text-sm dark:border-zinc-800 dark:bg-zinc-900">
              <p><strong>Estoquista:</strong> {delegateDTO.stockistName}</p>
              <p><strong>Depósito:</strong> {delegateDTO.stockistDepositName || '-'}</p>
              <p><strong>Telefone:</strong> {delegateDTO.stockistPhone || '-'}</p>
            </div>
          )}

          <div className="rounded-xl border border-slate-200 bg-white p-3 dark:border-zinc-800 dark:bg-zinc-900">
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400">
              Comentário para a equipe
            </label>
            <textarea
              value={delegateDTO.comment}
              onChange={(event) =>
                setDelegateDTO((previous) => ({
                  ...previous,
                  comment: event.target.value,
                }))
              }
              rows={3}
              placeholder="Inserir comentário para a equipe (se necessário)"
              className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm dark:border-zinc-700 dark:bg-zinc-900"
            />
          </div>

          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
            <div className="overflow-x-auto">
              <table className="min-w-[760px] w-full">
                <thead className="bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500 dark:bg-zinc-900/50 dark:text-zinc-400">
                  <tr>
                    <th className="px-4 py-3">Prioridade</th>
                    <th className="px-4 py-3">Rua</th>
                    <th className="px-4 py-3">Equipe</th>
                    <th className="px-4 py-3">Almox. Prioritário</th>
                  </tr>
                </thead>
                <tbody>
                  {preMeasurement.streets.map((street) => {
                    const reserve =
                      delegateDTO.street.find((item) => item.preMeasurementStreetId === street.preMeasurementStreetId) ??
                      null;

                    return (
                      <tr key={street.preMeasurementStreetId} className="border-t border-slate-100 dark:border-zinc-800">
                        <td className="px-4 py-3">
                          {reserve?.prioritized ? (
                            <i className="pi pi-exclamation-triangle text-amber-500" />
                          ) : (
                            <span className="text-xs text-slate-400">-</span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">{street.address}</td>
                        <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">{reserve?.teamName || '-'}</td>
                        <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">{reserve?.truckDepositName || '-'}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {isMultiTeam && (
            <div className="relative rounded-2xl border border-slate-200 bg-white p-3 dark:border-zinc-800 dark:bg-zinc-900">
              <div ref={streetsSliderRef} className="no-scrollbar flex gap-2 overflow-x-auto px-8 pb-1">
                {preMeasurement.streets.map((street) => {
                  const selected = street.preMeasurementStreetId === streetId;
                  const completed = completedStreetIds.includes(street.preMeasurementStreetId);
                  const reserve = delegateDTO.street.find(
                    (item) => item.preMeasurementStreetId === street.preMeasurementStreetId,
                  );

                  return (
                    <button
                      key={street.preMeasurementStreetId}
                      type="button"
                      onClick={() => setStreetId(street.preMeasurementStreetId)}
                      className={`min-w-[280px] rounded-xl border p-4 text-left transition ${
                        selected
                          ? 'border-orange-400 bg-slate-100 dark:bg-zinc-800'
                          : 'border-slate-200 hover:scale-[1.02] dark:border-zinc-700'
                      }`}
                    >
                      <p className="text-sm font-semibold text-slate-800 dark:text-zinc-100">{street.address}</p>
                      <p className="mt-1 text-xs text-slate-500 dark:text-zinc-400">
                        {street.items.length} itens pendentes de reserva
                      </p>
                      <p className="mt-2 text-xs font-semibold">
                        {completed ? 'RUA CONCLUÍDA' : reserve?.teamName ? 'EQUIPE DEFINIDA' : 'PENDENTE'}
                      </p>
                    </button>
                  );
                })}
              </div>
              <button
                type="button"
                onClick={() => scrollStreets(-300)}
                className="absolute left-2 top-1/2 -translate-y-1/2 text-slate-500 transition hover:text-slate-700 dark:text-zinc-400 dark:hover:text-zinc-200"
                aria-label="Voltar ruas"
              >
                <i className="pi pi-chevron-left" />
              </button>
              <button
                type="button"
                onClick={() => scrollStreets(300)}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 transition hover:text-slate-700 dark:text-zinc-400 dark:hover:text-zinc-200"
                aria-label="Avançar ruas"
              >
                <i className="pi pi-chevron-right" />
              </button>
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
            <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
              <div className="mb-3 flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3 dark:border-zinc-800">
                <h3 className="text-sm font-semibold text-slate-700 dark:text-zinc-200">
                  {currentStreet?.address ?? 'Selecione uma rua'}
                </h3>
                <button
                  type="button"
                  onClick={togglePriority}
                  className={`inline-flex items-center gap-2 rounded-lg border px-2.5 py-1 text-xs font-semibold transition ${
                    currentStreetReserve?.prioritized
                      ? 'border-amber-300 bg-amber-50 text-amber-700 dark:border-amber-800 dark:bg-amber-900/30 dark:text-amber-300'
                      : 'border-slate-300 text-slate-600 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-300 dark:hover:bg-zinc-800'
                  }`}
                >
                  <i className="pi pi-exclamation-triangle" />
                  {currentStreetReserve?.prioritized ? 'Com prioridade' : 'Sem prioridade'}
                </button>
              </div>

              <div className="space-y-3 text-sm text-slate-600 dark:text-zinc-300">
                <p>
                  <strong>Equipe:</strong>{' '}
                  {streetTeam ? `EQUIPE ${streetTeam.teamName.toUpperCase()}` : 'NENHUMA EQUIPE SELECIONADA'}
                </p>
                <p>
                  <strong>Almoxarifado do caminhão:</strong>{' '}
                  {currentStreetReserve?.truckDepositName || '-'}
                </p>

                <div className="flex flex-wrap gap-2 pt-1">
                  <button
                    type="button"
                    onClick={() => setOpenTeamModal(true)}
                    className="rounded-xl bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-blue-500"
                  >
                    Selecionar Equipe
                  </button>

                  {isMultiTeam && (
                    <button
                      type="button"
                      onClick={finishStreet}
                      className="rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white hover:bg-slate-700 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
                    >
                      Concluir Rua
                    </button>
                  )}
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
              <h3 className="mb-2 text-sm font-semibold text-slate-700 dark:text-zinc-200">Estoque no caminhão</h3>

              <div className="max-h-[330px] overflow-auto">
                {getTruckMaterials(streetId).length === 0 ? (
                  <p className="text-xs text-slate-500 dark:text-zinc-400">
                    Nenhum material em estoque para a rua/equipe selecionada.
                  </p>
                ) : (
                  <table className="w-full text-xs">
                    <thead>
                      <tr className="text-left text-slate-500 dark:text-zinc-400">
                        <th className="pb-2">Material</th>
                        <th className="pb-2 text-right">Nec.</th>
                        <th className="pb-2 text-right">Disp.</th>
                      </tr>
                    </thead>
                    <tbody>
                      {getTruckMaterials(streetId).map((material) => (
                        <tr key={`${material.materialId}-${material.deposit}`} className="border-t border-slate-100 dark:border-zinc-800">
                          <td className="py-2 pr-2 text-slate-700 dark:text-zinc-200">{material.materialName}</td>
                          <td className="py-2 text-right text-slate-600 dark:text-zinc-300">{material.itemQuantity}</td>
                          <td className="py-2 text-right">
                            <span className={material.availableQuantity >= material.itemQuantity ? 'text-emerald-600' : 'text-rose-600'}>
                              {material.availableQuantity}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      <Modal open={openTeamModal} onClose={() => setOpenTeamModal(false)} className="max-w-3xl">
        <ModalHeader title="Selecionar Equipe Operacional" onClose={() => setOpenTeamModal(false)} />
        <ModalBody>
          <div className="overflow-hidden rounded-xl border border-slate-200 dark:border-zinc-800">
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500 dark:bg-zinc-900/50 dark:text-zinc-400">
                  <tr>
                    <th className="px-4 py-3">Equipe</th>
                    <th className="px-4 py-3">Colaboradores</th>
                    <th className="px-4 py-3">Placa</th>
                    <th className="px-4 py-3">Região</th>
                  </tr>
                </thead>
                <tbody>
                  {teams.map((team) => (
                    <tr
                      key={team.idTeam}
                      onClick={() => selectTeam(team)}
                      className="cursor-pointer border-t border-slate-100 transition hover:bg-blue-50 dark:border-zinc-800 dark:hover:bg-blue-900/20"
                    >
                      <td className="px-4 py-3 text-slate-700 dark:text-zinc-200">{team.teamName}</td>
                      <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{team.memberNames.join(', ')}</td>
                      <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{team.plate}</td>
                      <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{team.regionName}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </ModalBody>
        <ModalFooter>
          <button
            type="button"
            onClick={() => setOpenTeamModal(false)}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
          >
            Fechar
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
