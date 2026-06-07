import {useEffect, useMemo, useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {useAppStore} from '@/store/use-app-store';
import {useNotify} from '@/shared/hooks/use-notify';
import {useAuthStore} from '@/core/auth/useAuthStore';
import {contractsApi} from '../../contract/api/contractsApi';
import type {ContractItemsResponse} from '../../contract/types';
import {manageApi} from '@/features/manage/api/manageApi';
import type {TeamModel} from '@/features/manage/types/manageTypes';
import {stockistApi} from '../../stock/api/stockist-api';
import type {StockistModel} from '@/features/requests/types/reservation';
import {GlassListbox} from '@/shared/components/glass-list-box';
import {executionsApi} from '@/features/executions/api/executionsApi';
import type {DirectExecutionDTO} from '../types';

const normalizeFloatInput = (value: string) => {
    const digits = value.replace(/[^0-9.]/g, '');
    const parts = digits.split('.');
    if (parts.length <= 2) return digits;
    return `${parts[0]}.${parts.slice(1).join('')}`;
};

export default function ExecutionNoPreMeasurement() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const {notify} = useNotify();
    const setPageContext = useAppStore((state) => state.setPageContext);
    const userUuid = useAuthStore((state) => state.user?.uuid ?? '');

    const [loading, setLoading] = useState(true);
    const [nextStep, setNextStep] = useState(false);
    const [finish, setFinish] = useState(false);

    const [contractId, setContractId] = useState(0);
    const [contractor, setContractor] = useState<string>('');

    const [referenceItems, setReferenceItems] = useState<ContractItemsResponse[]>([]);
    const [teams, setTeams] = useState<TeamModel[]>([]);
    const [stockists, setStockists] = useState<StockistModel[]>([]);

    const [execution, setExecution] = useState<DirectExecutionDTO>({
        contractId: 0,
        teamId: 0,
        currentUserId: '',
        stockistId: '',
        instructions: null,
        items: [],
    });

    const [currentItemId, setCurrentItemId] = useState(0);
    const [quantity, setQuantity] = useState<string | null>(null);

    useEffect(() => {
        setPageContext(['Instalação', 'Gerar Ordem de Serviço'], 'Gerar Ordem de Serviço');

        const rawContractId = searchParams.get('codigo');
        const rawContractor = searchParams.get('nome') ?? '';

        if (!rawContractId) {
            navigate('/', {replace: true});
            return;
        }

        const parsedContractId = Number(rawContractId);
        setContractId(parsedContractId);
        setContractor(rawContractor);

        setExecution((previous) => ({
            ...previous,
            contractId: parsedContractId,
            currentUserId: userUuid,
        }));

        setLoading(true);

        Promise.all([
            contractsApi.getContractItems(parsedContractId),
            manageApi.getTeams(),
            stockistApi.getStockists(),
        ])
            .then(([items, teamsData, stockistsData]) => {
                setReferenceItems(items);
                setTeams(teamsData);
                setStockists(stockistsData);
            })
            .catch((error: unknown) => {
                const message = error instanceof Error ? error.message : 'Erro ao carregar dados da execução.';
                notify(message, 'error');
            })
            .finally(() => setLoading(false));
    }, [navigate, notify, searchParams, setPageContext, userUuid]);

    const selectedTeamName = useMemo(
        () => teams.find((team) => Number(team.idTeam) === execution.teamId)?.teamName ?? null,
        [execution.teamId, teams],
    );

    const getQuantity = (itemId: number) => {
        const selected = execution.items.find((item) => item.contractItemId === itemId);
        return selected?.quantity ?? null;
    };

    const existsItem = (itemId: number) => execution.items.some((item) => item.contractItemId === itemId);

    const onRowClick = (item: ContractItemsResponse) => {
        if (currentItemId !== 0 && currentItemId !== item.contractItemId) {
            cancelItem();
        }

        setCurrentItemId(item.contractItemId);
        setQuantity(getQuantity(item.contractItemId));
    };

    const cancelItem = () => {
        if (currentItemId === 0) return;

        const alreadySelected = existsItem(currentItemId);
        if (alreadySelected) {
            setExecution((previous) => ({
                ...previous,
                items: previous.items.filter((item) => item.contractItemId !== currentItemId),
            }));
            notify('Item removido com sucesso.', 'success');
        }

        setCurrentItemId(0);
        setQuantity(null);
    };

    const confirmItem = (item: ContractItemsResponse): boolean => {
        const numericQuantity = Number(quantity ?? '0');

        if (numericQuantity < 0) {
            notify('A quantidade não pode ser menor que 0.', 'warn');
            return false;
        }

        if (numericQuantity < 1) {
            return true;
        }

        const contractualBalance = item.contractedQuantity - item.executedQuantity;
        if (numericQuantity > contractualBalance) {
            notify(`O saldo atual desse item é ${contractualBalance}.`, 'info');
            return false;
        }

        const realBalance = item.contractedQuantity - item.executedQuantity - item.reservedQuantity;
        if (numericQuantity > realBalance) {
            notify(
                `Existem ${item.reservedQuantity} itens reservados em execuções em andamento. Apenas ${realBalance} itens podem ser utilizados agora.`,
                'warn',
            );
            return false;
        }

        setExecution((previous) => {
            const index = previous.items.findIndex((selected) => selected.contractItemId === currentItemId);

            if (index === -1) {
                return {
                    ...previous,
                    items: [
                        ...previous.items,
                        {
                            contractItemId: currentItemId,
                            quantity: String(numericQuantity),
                        },
                    ],
                };
            }

            const nextItems = [...previous.items];
            nextItems[index] = {
                ...nextItems[index],
                quantity: String(numericQuantity),
            };

            return {
                ...previous,
                items: nextItems,
            };
        });

        notify(existsItem(currentItemId) ? 'Item alterado com sucesso.' : 'Item adicionado com sucesso.', 'success');

        setCurrentItemId(0);
        setQuantity(null);
        return true;
    };

    const goToNextStep = () => {
        if (execution.items.length === 0) {
            notify('Para continuar, selecione ao menos um item.', 'warn');
            return;
        }

        setNextStep(true);
    };

    const sendData = async () => {
        if (!execution.stockistId) {
            notify('Para concluir, selecione o estoquista.', 'warn');
            return;
        }

        setLoading(true);
        try {
            await executionsApi.delegateDirectExecution(execution);
            setFinish(true);
            notify('Ordem gerada com sucesso.', 'success');
        } catch (error: unknown) {
            const message = error instanceof Error ? error.message : 'Etapa não criada.';
            notify(message, 'error');
        } finally {
            setLoading(false);
        }
    };

    const hasMissingSetup = !loading && (teams.length === 0 || stockists.length === 0);

    return (
        <section className="relative space-y-4 p-4 md:p-6">
            {loading && (
                <div
                    className="flex min-h-64 items-center justify-center rounded-2xl border border-slate-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
                    <i className="pi pi-spin pi-spinner text-2xl text-blue-500"/>
                </div>
            )}

            {!loading && hasMissingSetup && (
                <div
                    className="rounded-2xl border border-amber-200 bg-amber-50 p-6 text-amber-800 dark:border-amber-900/40 dark:bg-amber-950/20 dark:text-amber-300">
                    <h2 className="text-base font-semibold">Antes de criar ordens de serviços</h2>
                    <ul className="mt-3 list-disc space-y-1 pl-5 text-sm">
                        {stockists.length === 0 && <li>Cadastre ao menos um estoquista.</li>}
                        {teams.length === 0 && <li>Cadastre ao menos uma equipe operacional.</li>}
                    </ul>
                </div>
            )}

            {!loading && !hasMissingSetup && (
                <>
                    <header
                        className="rounded-xl border border-slate-200 bg-white px-4 py-3 dark:border-zinc-800 dark:bg-zinc-900">
                        <h1 className="text-base font-semibold text-slate-800 dark:text-zinc-100">
                            {contractor || 'Nova Ordem de Serviço'}
                        </h1>
                    </header>

                    {nextStep ? (
                        finish ? (
                            <div
                                className="rounded-2xl border border-emerald-200 bg-emerald-50 p-8 text-center dark:border-emerald-900/40 dark:bg-emerald-950/20">
                                <i className="pi pi-check-circle text-5xl text-emerald-500"/>
                                <p className="mt-4 text-base text-emerald-800 dark:text-emerald-200">
                                    Ordem gerada com sucesso. O estoquista foi notificado para gerenciamento de estoque.
                                </p>
                                <button
                                    type="button"
                                    onClick={() => navigate('/contratos/listar?for=execution')}
                                    className="mt-4 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500"
                                >
                                    Nova Execução
                                </button>
                            </div>
                        ) : (
                            <div className="space-y-4">
                                <div
                                    className="rounded-2xl border border-slate-200 bg-white p-6 text-center dark:border-zinc-800 dark:bg-zinc-900">
                                    <h2 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Revisão</h2>
                                    <div className="mt-2 text-sm text-slate-600 dark:text-zinc-300 space-y-1">
                                        <p>Será gerada uma OS com os itens selecionados.</p>
                                        <p>
                                            A Ordem estará disponível no menu
                                            <span
                                                className="mx-2 text-blue-500 p-1 bg-blue-100 rounded-lg text-xs font-bold">
                                            Solicitações ao Estoquista - Gerenciar Ordens de Serviço
                                        </span>
                                        </p>
                                        <p>O Estoquista selecionado deverá fazer o gerenciamento junto à equipe
                                            responsável.</p>
                                        <p>A Equipe deverá seguir todas as instruções do estoquista antes de inicar a
                                            execução.</p>
                                    </div>

                                    <div className="mx-auto mt-5 max-w-sm text-left">
                                        <label
                                            className={'mb-1 block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400'}>Estoquista
                                            Selecionado</label>
                                        <GlassListbox
                                            value={execution.stockistId}
                                            onChange={(value) =>
                                                setExecution((previous) => ({
                                                    ...previous,
                                                    stockistId: value ?? '',
                                                }))
                                            }
                                            placeholder="Selecione o estoquista"
                                            options={[
                                                {value: '', label: 'Selecione o estoquista'},
                                                ...stockists.map((stockist) => ({
                                                    value: stockist.userId,
                                                    label: stockist.name,
                                                })),
                                            ]}
                                        />
                                    </div>
                                </div>

                                <div
                                    className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
                                    <label
                                        className="mb-2 block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400">
                                        Instruções para a equipe
                                    </label>
                                    <textarea
                                        value={execution.instructions ?? ''}
                                        onChange={(event) =>
                                            setExecution((previous) => ({
                                                ...previous,
                                                instructions: event.target.value,
                                            }))
                                        }
                                        rows={4}
                                        className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm dark:border-zinc-700 dark:bg-zinc-900"
                                    />
                                </div>

                                <div className="flex flex-wrap justify-center gap-3">
                                    <button
                                        type="button"
                                        onClick={() => setNextStep(false)}
                                        className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
                                    >
                                        Revisar itens selecionados
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => void sendData()}
                                        className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
                                    >
                                        Concluir
                                    </button>
                                </div>
                            </div>
                        )
                    ) : (
                        <>
                            <div
                                className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
                                <label className="mb-2 block text-sm font-medium text-slate-700 dark:text-zinc-200">
                                    Selecione a equipe/caminhão responsável
                                </label>
                                <GlassListbox
                                    value={execution.teamId || null}
                                    onChange={(value) =>
                                        setExecution((previous) => ({
                                            ...previous,
                                            teamId: value === null ? 0 : Number(value),
                                        }))
                                    }
                                    placeholder="Selecione"
                                    options={[
                                        {value: null, label: 'Selecione'},
                                        ...teams.map((team) => ({
                                            value: Number(team.idTeam),
                                            label: team.teamName,
                                        })),
                                    ]}
                                />
                            </div>

                            <div
                                className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
                                <div className="overflow-x-auto">
                                    <table className="min-w-[860px] w-full">
                                        <thead
                                            className="bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500 dark:bg-zinc-900/50 dark:text-zinc-400">
                                        <tr>
                                            <th className="px-4 py-3 font-semibold">Descrição</th>
                                            <th className="px-4 py-3 font-semibold">Equipe Responsável</th>
                                            <th className="px-4 py-3 font-semibold">Quantidade</th>
                                            <th className="px-4 py-3 font-semibold">Status</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {referenceItems.map((item) => {
                                            const isEditing = currentItemId === item.contractItemId;
                                            const selected = existsItem(item.contractItemId);

                                            const currentIndex = referenceItems.findIndex(
                                                (selected) => selected.contractItemId === item.contractItemId,
                                            );

                                            return (
                                                <tr
                                                    key={item.contractItemId}
                                                    onClick={(event) => {
                                                        const target = event.target as HTMLElement;
                                                        if (target.closest('button')) return;
                                                        onRowClick(item);
                                                    }}
                                                    className="cursor-pointer border-t border-slate-100 transition hover:bg-slate-50 dark:border-zinc-800 dark:hover:bg-zinc-800/50"
                                                >
                                                    <td className="px-4 py-3 text-sm font-medium text-slate-700 dark:text-zinc-200">{item.description}</td>
                                                    <td className="px-4 py-3 text-xs text-slate-600 dark:text-zinc-300">{selectedTeamName ?? 'Sem equipe'}</td>
                                                    <td
                                                        className="relative px-4 py-3 cursor-pointer group transition-all duration-200"
                                                    >
                                                        {isEditing ? (
                                                            <input
                                                                id={`input-quantity-${currentIndex}`}
                                                                value={quantity ?? 0}
                                                                autoFocus
                                                                onKeyDown={(event) => {
                                                                    if(!['Enter', 'Escape', 'ArrowDown', 'ArrowUp'].includes(event.key)) return;

                                                                    const direction = ['Enter', 'ArrowDown'].includes(event.key) ? 1 : -1;
                                                                    const nextIndex = currentIndex + direction;
                                                                    const nextItem = referenceItems[nextIndex];

                                                                    if (event.key === 'Enter') {
                                                                        if (confirmItem(item) && nextItem) onRowClick(nextItem);
                                                                        if(!nextItem) {
                                                                            setCurrentItemId(0);
                                                                            setQuantity(null);
                                                                        }
                                                                    } else if (event.key === 'Escape') {
                                                                        cancelItem();
                                                                    } else if(event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                                                                        event.preventDefault(); // Impede o cursor de pular pro início/fim do texto

                                                                        if (confirmItem(item) && nextItem) {
                                                                            onRowClick(nextItem);
                                                                        }
                                                                    }
                                                                }}
                                                                onChange={(event) => setQuantity(normalizeFloatInput(event.target.value))}
                                                                // Estilo do input focado em edição
                                                                className="w-32 rounded-lg border border-blue-400 bg-white px-2 py-1 text-sm shadow-inner outline-none ring-2 ring-blue-100 focus:border-blue-500 dark:border-zinc-700 dark:bg-zinc-900 dark:ring-zinc-800"
                                                            />
                                                        ) : (
                                                            // ESTADO DE VISUALIZAÇÃO (com a "dica" no hover)
                                                            <div className="flex items-center justify-between gap-2">
                                                              <span
                                                                  className="text-sm font-medium text-slate-700 dark:text-zinc-200">
                                                                {getQuantity(item.contractItemId) ?? 0}
                                                              </span>

                                                                {/* Ícone de lápis sutil que só aparece no hover da linha ou célula */}
                                                                <i className="pi pi-pencil text-xs text-blue-400 opacity-0 transition-opacity duration-200 group-hover:opacity-100"/>

                                                                {/* Opcional: Borda de destaque sutil no hover da célula */}
                                                                <div
                                                                    className="absolute inset-0 border border-dashed border-blue-300 rounded-lg opacity-0 transition-opacity duration-200 group-hover:opacity-100 pointer-events-none mx-1 my-1"/>
                                                            </div>
                                                        )}
                                                    </td>
                                                    <td className="px-4 py-3">
                                                        {isEditing ? (
                                                            <div className="flex items-center gap-2">
                                                                <button
                                                                    type="button"
                                                                    onClick={cancelItem}
                                                                    className="rounded-lg border border-slate-300 px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
                                                                >
                                                                    {selected ? <i className="pi pi-trash"></i> :
                                                                        <i className="pi pi-times"></i>}
                                                                </button>
                                                                <button
                                                                    type="button"
                                                                    onClick={() => confirmItem(item)}
                                                                    className="rounded-lg bg-blue-600 px-2.5 py-1 text-xs font-semibold text-white hover:bg-blue-500"
                                                                >
                                                                    <i className="pi pi-check"></i>
                                                                </button>
                                                            </div>
                                                        ) : (
                                                            <i className={`pi inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                                                                selected
                                                                    ? 'pi-check bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                                                                    : 'pi-times bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300'
                                                            }`}></i>
                                                        )}
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            {execution.teamId > 0 && (
                                <div
                                    className="sticky bottom-0 z-10 flex justify-end bg-slate-50/95 py-4 backdrop-blur dark:bg-zinc-950/95">
                                    <button
                                        type="button"
                                        onClick={goToNextStep}
                                        className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
                                    >
                                        Continuar
                                    </button>
                                </div>
                            )}
                        </>
                    )}
                </>
            )}

            <footer className="text-xs text-slate-400 dark:text-zinc-500">
                Contrato #{contractId || '-'}
            </footer>
        </section>
    );
}
