import { useState, useEffect } from 'react';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { EmbeddedDocPanel } from '@/shared/components/embedded-doc-panel';
import { manageApi } from '@/features/manage/api/manageApi';
import { manageKeys } from '@/features/manage/api/queryKeys';
import { useTeams } from '@/features/manage/hooks/useTeams';
import { useUsers } from '@/features/manage/hooks/useUsers';
import type { ManagedUser, TeamModel } from '@/features/manage/types/manageTypes';
import { teamSchema } from '@/features/manage/validations/teamSchema';
import { capitalize } from '@/shared/utils/formatters';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/shared/components/ui/table';
import { ibgeApi } from '@/shared/api/ibge-api';
import { GlassListbox } from '@/shared/components/glass-list-box';

interface TeamForm {
  teamName: string;
  memberIds: string[];
  UFName: string;
  cityName: string;
  regionName: string;
  plate: string;
}

const EMPTY: TeamForm = { teamName: '', memberIds: [], UFName: '', cityName: '', regionName: '', plate: '' };

export default function Teams() {
  const navigate = useNavigate();
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const queryClient = useQueryClient();

  const [formOpen, setFormOpen] = useState(false);
  const [editTeam, setEditTeam] = useState<TeamModel | null>(null);
  const [form, setForm] = useState<TeamForm>(EMPTY);
  const [docOpen, setDocOpen] = useState(false);
  const [activeDocKey, setActiveDocKey] = useState<'setup-teams' | 'field-app' | 'day-to-day'>('setup-teams');
  const [uf, setUf] = useState('');

  useEffect(() => {
    setPageContext(['Configurações', 'Equipes Operacionais'], 'Equipes Operacionais');
  }, [setPageContext]);

  const {
    data: statesData,
    isLoading: isStatesLoading,
    error: statesError,
  } = useQuery({
    queryKey: ['ibge-states'],
    queryFn: ibgeApi.getUfs,
    enabled: formOpen, 
  });

  const {
    data: citiesData,
    isLoading: isCitiesLoading,
    error: citiesError,
  } = useQuery({
    queryKey: ['ibge-cities', uf],
    queryFn: () => ibgeApi.getCities(uf),
    enabled: formOpen && uf !== ''
  });

  useEffect(() => {
    if (statesError) {
      notify('Erro ao carregar estados. Tente novamente mais tarde.', 'error');
    }
  }, [statesError, notify]);

  useEffect(() => {
    if (citiesError) {
      notify('Erro ao carregar cidades. Tente novamente mais tarde.', 'error');
    }
  }, [citiesError, notify]);

  const { data: teams = [], isLoading } = useTeams();
  const { data: users = [] } = useUsers();
  const docs = {
    'setup-teams': {
      title: 'Como estruturar equipes de campo',
      description: 'Veja como montar equipes, vincular caminhão e preparar a operação para uso no app.',
      url: 'https://lumosip.com.br/como-usar/02-access-management/04-team-management/',
    },
    'field-app': {
      title: 'Como orientar a equipe a baixar o app',
      description: 'Abra o guia para explicar download e primeiro acesso.',
      url: 'https://lumosip.com.br/como-usar/03-operation/01-android-app-admin/',
    },
    'day-to-day': {
      title: 'Como orientar a equipe a usar o app',
      description: 'Guia prático para equipes operacionais usarem o app durante a rotina em campo.',
      url: 'https://lumosip.com.br/como-usar/03-operation/03-day-to-day-app/',
    },
  } as const;
  const saveMutation = useMutation({
    mutationFn: async ({ form, users, editTeam }: { form: TeamForm; users: ManagedUser[]; editTeam?: TeamModel | null }) => {
      const payload: Partial<TeamModel> = {
        idTeam: editTeam?.idTeam,
        teamName: form.teamName,
        memberIds: form.memberIds,
        memberNames: form.memberIds.map((id) => users.find((user) => user.userId === id)?.name ?? id),
        UFName: form.UFName,
        cityName: form.cityName,
        regionName: form.regionName,
        plate: form.plate,
        depositName: editTeam?.depositName ?? '',
      };

      return manageApi.updateTeams([payload as TeamModel]);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: manageKeys.teams() });
    },
  });

  const openCreate = () => {
    setEditTeam(null);
    setForm(EMPTY);
    setUf('');
    setFormOpen(true);
  };

  const openEdit = (team: TeamModel) => {
    setEditTeam(team);
    setForm({
      teamName: team.teamName,
      memberIds: team.memberIds ?? [],
      UFName: team.UFName,
      cityName: team.cityName,
      regionName: team.regionName,
      plate: team.plate,
    });
    setUf(team.UFName);
    setFormOpen(true);
  };

  const closeForm = () => {
    setFormOpen(false);
  };

  const toggleMember = (userId: string) => {
    setForm((previous) => ({
      ...previous,
      memberIds: previous.memberIds.includes(userId)
        ? previous.memberIds.filter((id) => id !== userId)
        : [...previous.memberIds, userId],
    }));
  };

  const handleSave = () => {
    const parseResult = teamSchema.safeParse(form);
    if (!parseResult.success) {
      notify(parseResult.error.issues[0]?.message ?? 'Formulário inválido.', 'warn');
      return;
    }

    saveMutation.mutate(
      { form, users: users as ManagedUser[], editTeam },
      {
        onSuccess: () => {
          notify(editTeam ? 'Equipe atualizada.' : 'Equipe criada.', 'success');
          setFormOpen(false);
        },
        onError: (error: unknown) => {
          const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
          notify(message ?? 'Erro ao salvar equipe.', 'error');
        },
      },
    );
  };

  const inputClass = 'w-full rounded-lg border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-2.5 py-2 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors';
  const compactLabelClass = 'mb-1 block text-xs font-semibold text-slate-600 dark:text-zinc-300';
  const compactListboxButtonClass = 'rounded-lg px-2.5 py-2';

  if (formOpen) {
    return (
      <section className="min-h-full p-4 md:p-6">
        <div className="mx-auto max-w-5xl space-y-5">

          <div className="relative overflow-hidden rounded-3xl border border-blue-200/80 bg-linear-to-br from-blue-50 via-white to-slate-50 p-5 shadow-[0_18px_60px_-35px_rgba(37,99,235,0.55)] dark:border-blue-900/50 dark:from-blue-950/30 dark:via-zinc-950 dark:to-zinc-900">
            <div className="absolute right-0 top-0 h-32 w-32 rounded-full bg-blue-400/20 blur-3xl" />
            <div className="relative flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div>
                <span className="inline-flex items-center gap-2 rounded-full border border-blue-200 bg-blue-100/80 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-blue-700 dark:border-blue-800 dark:bg-blue-950/60 dark:text-blue-300">
                  <i className={`pi ${editTeam ? 'pi-pencil' : 'pi-plus'} text-[10px]`} />
                  {editTeam ? 'Editando equipe' : 'Criando equipe'}
                </span>
                <h1 className="mt-3 text-2xl font-semibold text-slate-900 dark:text-zinc-50">
                  {editTeam ? form.teamName || 'Editar equipe' : 'Nova equipe operacional'}
                </h1>
                <p className="mt-1 max-w-2xl text-sm text-slate-600 dark:text-zinc-400">
                  Preencha os dados da equipe, selecione a localidade e marque os membros que atuarão em campo.
                </p>
              </div>

              <div className="rounded-2xl border border-white/70 bg-white/75 px-4 py-3 text-sm text-slate-600 shadow-sm backdrop-blur dark:border-white/10 dark:bg-white/5 dark:text-zinc-300">
                <p className="font-semibold text-slate-800 dark:text-zinc-100">
                  {form.memberIds.length} membro{form.memberIds.length === 1 ? '' : 's'} selecionado{form.memberIds.length === 1 ? '' : 's'}
                </p>
                <p className="text-xs text-slate-500 dark:text-zinc-400">
                  {form.cityName && form.UFName ? `${form.cityName}, ${form.UFName}` : 'Localidade pendente'}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm dark:border-zinc-800 dark:bg-zinc-950">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
              <div>
                <label className={compactLabelClass}>Nome da equipe*</label>
                <input
                  type="text"
                  autoFocus
                  value={form.teamName}
                  onChange={(event) => setForm((previous) => ({ ...previous, teamName: event.target.value }))}
                  placeholder="Ex.: Iveco ABC-1234"
                  className={inputClass}
                />
              </div>

              <div>
                <label className={compactLabelClass}>Placa do veículo</label>
                <input
                  type="text"
                  value={form.plate}
                  onChange={(event) => setForm((previous) => ({ ...previous, plate: event.target.value.toUpperCase() }))}
                  placeholder="ABC-1234"
                  className={inputClass}
                />
              </div>

              <div>
                <label className={compactLabelClass}>Estado</label>
                <GlassListbox
                  options={(statesData ?? []).map((state) => ({ label: `${state.nome} (${state.sigla})`, value: state.sigla }))}
                  value={form.UFName}
                  onChange={(value) => {
                    setUf(value);
                    setForm((previous) => ({ ...previous, UFName: value, cityName: '', regionName: '' }));
                  }}
                  placeholder="Estado (UF)"
                  searchable
                  isLoading={isStatesLoading}
                  buttonClassName={compactListboxButtonClass}
                />
              </div>

              <div>
                <label className={compactLabelClass}>Cidade</label>
                <GlassListbox
                  options={(citiesData ?? []).map((city) => ({ label: city.nome, value: city.nome }))}
                  value={form.cityName}
                  onChange={(value) => {
                    const region = citiesData?.find((city) => city.nome === value)?.microrregiao.mesorregiao.nome ?? '';
                    setForm((previous) => ({ ...previous, cityName: value, regionName: region }));
                  }}
                  placeholder="Cidade"
                  searchable
                  isLoading={isCitiesLoading}
                  disabled={!form.UFName || !citiesData}
                  disabledTooltip={!form.UFName ? 'Selecione um estado primeiro' : undefined}
                  buttonClassName={compactListboxButtonClass}
                />
              </div>

              <div>
                <label className={compactLabelClass}>Região</label>
                <input
                  disabled
                  type="text"
                  value={form.regionName}
                  onChange={(event) => setForm((previous) => ({ ...previous, regionName: event.target.value }))}
                  placeholder="Região (autopreenchida)"
                  className={`${inputClass} cursor-not-allowed bg-neutral-100 dark:bg-zinc-800/50`}
                />
              </div>
            </div>

            <div className="mt-4">
              <label className={compactLabelClass}>Membros</label>
              <div className="relative mb-2 rounded-lg border border-slate-200 dark:border-zinc-700">
                <input
                  type="text"
                  onInput={(event) => {
                    const input = event.currentTarget;
                    const filter = input.value.toLowerCase();
                    const listContainer = input.closest('.relative')?.nextElementSibling;
                    if (!listContainer) return;

                    const optionElements = listContainer.querySelectorAll<HTMLElement>('.user-item');
                    optionElements.forEach((optionEl) => {
                      const text = optionEl.textContent?.toLowerCase() || '';
                      optionEl.style.display = text.includes(filter) ? '' : 'none';
                    });
                  }}
                  placeholder="Digite para buscar..."
                  className="w-full p-2 pr-9 text-sm focus:outline-none dark:bg-zinc-900 dark:text-zinc-100"
                />
                <i className="pi pi-search absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
              </div>

              <div className="mac-scroll max-h-32 overflow-y-auto rounded-xl border border-slate-200 divide-y divide-slate-100 dark:border-zinc-700 dark:divide-zinc-800">
                {(users as ManagedUser[])
                  .filter((user) => {
                    const roles = user.role.map((role) => role.roleName);
                    return roles.includes('ELETRICISTA') || roles.includes('MOTORISTA');
                  })
                  .sort((user) => (form.memberIds.includes(user.userId) ? -1 : 1))
                  .map((user) => {
                    const role = user.role.map((item) => item.roleName).find((item) => item === 'ELETRICISTA' || item === 'MOTORISTA');

                    return (
                      <label
                        key={user.userId}
                        className="user-item flex cursor-pointer items-center gap-2.5 px-3 py-1.5 transition-colors hover:bg-slate-50 dark:hover:bg-zinc-800"
                      >
                        <input
                          type="checkbox"
                          checked={form.memberIds.includes(user.userId)}
                          onChange={() => toggleMember(user.userId)}
                          className="accent-indigo-600"
                        />
                        <div className="flex w-full items-center justify-between gap-2">
                          <span className="text-sm text-slate-700 dark:text-zinc-200">{user.name} {user.lastname}</span>
                          <span
                            className={
                              'rounded-md px-2 py-0.5 text-xs font-medium ' +
                              (role === 'ELETRICISTA' ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400' :
                                role === 'MOTORISTA' ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400' :
                                  'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400')
                            }
                          >
                            {capitalize(role ?? '')}
                          </span>
                        </div>
                      </label>
                    );
                  })}
              </div>
            </div>

            <div className="mt-4 flex flex-col-reverse gap-2 border-t border-slate-100 pt-3 sm:flex-row sm:justify-end dark:border-zinc-800">
              <button
                type="button"
                onClick={closeForm}
                className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-50 dark:border-zinc-700 dark:text-zinc-400 dark:hover:bg-zinc-800"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!form.teamName.trim() || saveMutation.isPending}
                onClick={handleSave}
                className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-indigo-500 disabled:opacity-50"
              >
                {saveMutation.isPending && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
                Salvar
              </button>
            </div>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="p-4 md:p-6 space-y-4">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Equipes Operacionais</h1>
          <p className="text-sm text-slate-500 dark:text-zinc-400">
            Estruture equipes, vincule caminhões e oriente o uso do app com contexto na própria tela.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => navigate('/estoque/caminhoes')}
            className="flex items-center gap-2 rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-700 dark:text-zinc-200 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
          >
            <i className="pi pi-truck text-sm" /> Ver caminhões
          </button>

          <button
            type="button"
            onClick={openCreate}
            className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors"
          >
            <i className="pi pi-plus text-sm" /> Nova Equipe
          </button>
        </div>
      </div>

      <div className="relative overflow-hidden rounded-2xl border border-neutral-200/80 bg-linear-to-b from-white to-neutral-50/50 p-5 shadow-[0_2px_8px_-3px_rgba(0,0,0,0.05),0_8px_24px_-12px_rgba(0,0,0,0.05)] subpixel-antialiased select-none dark:border-zinc-800/80 dark:from-zinc-950/50 dark:to-zinc-950/20 dark:backdrop-blur-md">
        {/* Detalhe estético: Uma barra lateral fina e discreta para indicar status/instrução */}
        <div className="absolute top-0 bottom-0 left-0 w-[4px] bg-blue-500/80 dark:bg-blue-400/60" />

        <div className="pl-2 flex flex-col gap-5 xl:flex-row xl:items-center xl:justify-between">
          {/* Bloco de Texto Principal */}
          <div className="max-w-2xl">
            <div className="flex items-center gap-2">
              <span className="flex h-5 w-5 items-center justify-center rounded-md bg-blue-50 text-blue-600 dark:bg-blue-950/50 dark:text-blue-400">
                <i className="pi pi-compass text-xs" />
              </span>
              <h4 className="text-sm font-semibold tracking-tight text-neutral-900 dark:text-zinc-100">
                Fluxo guiado de operação em campo
              </h4>
            </div>
            <p className="mt-2 text-xs leading-relaxed text-neutral-500 dark:text-zinc-400 max-w-xl">
              Monte a equipe, defina a região e a placa corretamente e confirme o caminhão vinculado. Depois, use a orientação integrada para preparar o time no aplicativo.
            </p>
          </div>

          {/* Bloco de Botões Lado a Lado (Ações Unificadas) */}
          <div className="flex flex-col gap-2 sm:flex-row sm:w-full xl:w-auto xl:flex-col">
            {/* Botão 1: Equipes */}
            <button
              type="button"
              onClick={() => {
                setActiveDocKey('setup-teams');
                setDocOpen(true);
              }}
              className="group flex flex-1 xl:w-64 min-h-11 items-center gap-3 rounded-xl border border-neutral-200 bg-white px-3.5 py-2 text-neutral-700 transition-all duration-200 hover:border-blue-500/30 hover:bg-neutral-50/50 hover:text-blue-600 active:scale-[0.98] dark:border-zinc-800 dark:bg-zinc-900/40 dark:text-zinc-300 dark:hover:border-blue-400/30 dark:hover:bg-zinc-900/80 dark:hover:text-blue-400 cursor-pointer"
            >
              <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-neutral-50 text-neutral-500 border border-neutral-100 transition-colors group-hover:bg-blue-50 group-hover:border-blue-100/50 group-hover:text-blue-500 dark:bg-zinc-900 dark:text-zinc-400 dark:border-zinc-800/60 dark:group-hover:bg-blue-950/40 dark:group-hover:text-blue-400">
                <i className="pi pi-users text-xs" />
              </div>
              <div className="flex flex-col items-start text-left leading-tight">
                <span className="text-xs font-semibold tracking-tight">Montar equipes</span>
                <span className="text-[10px] text-neutral-400 font-medium tracking-wide mt-0.5 group-hover:text-blue-500/70 dark:group-hover:text-blue-400/70">Acessar guia completo</span>
              </div>
            </button>

            {/* Botão 2: Instalar App */}
            <button
              type="button"
              onClick={() => {
                setActiveDocKey('field-app');
                setDocOpen(true);
              }}
              className="group flex flex-1 xl:w-64 min-h-11 items-center gap-3 rounded-xl border border-neutral-200 bg-white px-3.5 py-2 text-neutral-700 transition-all duration-200 hover:border-blue-500/30 hover:bg-neutral-50/50 hover:text-blue-600 active:scale-[0.98] dark:border-zinc-800 dark:bg-zinc-900/40 dark:text-zinc-300 dark:hover:border-blue-400/30 dark:hover:bg-zinc-900/80 dark:hover:text-blue-400 cursor-pointer"
            >
              <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-neutral-50 text-neutral-500 border border-neutral-100 transition-colors group-hover:bg-blue-50 group-hover:border-blue-100/50 group-hover:text-blue-500 dark:bg-zinc-900 dark:text-zinc-400 dark:border-zinc-800/60 dark:group-hover:bg-blue-950/40 dark:group-hover:text-blue-400">
                <i className="pi pi-download text-xs" />
              </div>
              <div className="flex flex-col items-start text-left leading-tight">
                <span className="text-xs font-semibold tracking-tight">Instalar o app</span>
                <span className="text-[10px] text-neutral-400 font-medium tracking-wide mt-0.5 group-hover:text-blue-500/70 dark:group-hover:text-blue-400/70">Como orientar o time</span>
              </div>
            </button>

            {/* Botão 3: Usar App */}
            <button
              type="button"
              onClick={() => {
                setActiveDocKey('day-to-day');
                setDocOpen(true);
              }}
              className="group flex flex-1 xl:w-64 min-h-11 items-center gap-3 rounded-xl border border-neutral-200 bg-white px-3.5 py-2 text-neutral-700 transition-all duration-200 hover:border-blue-500/30 hover:bg-neutral-50/50 hover:text-blue-600 active:scale-[0.98] dark:border-zinc-800 dark:bg-zinc-900/40 dark:text-zinc-300 dark:hover:border-blue-400/30 dark:hover:bg-zinc-900/80 dark:hover:text-blue-400 cursor-pointer"
            >
              <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-neutral-50 text-neutral-500 border border-neutral-100 transition-colors group-hover:bg-blue-50 group-hover:border-blue-100/50 group-hover:text-blue-500 dark:bg-zinc-900 dark:text-zinc-400 dark:border-zinc-800/60 dark:group-hover:bg-blue-950/40 dark:group-hover:text-blue-400">
                <i className="pi pi-mobile text-xs" />
              </div>
              <div className="flex flex-col items-start text-left leading-tight">
                <span className="text-xs font-semibold tracking-tight">Usar o app</span>
                <span className="text-[10px] text-neutral-400 font-medium tracking-wide mt-0.5 group-hover:text-blue-500/70 dark:group-hover:text-blue-400/70">Guia do dia a dia</span>
              </div>
            </button>
          </div>
        </div>
      </div>

      {users.length === 0 && !isLoading ? (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5 dark:border-amber-900 dark:bg-amber-950/30">
          <h3 className="text-base font-semibold text-amber-900 dark:text-amber-100">Antes de cadastrar equipes</h3>
          <p className="mt-1 text-sm text-amber-800 dark:text-amber-200">
            Primeiro cadastre usuários com função de eletricista e motorista para compor as equipes.
          </p>
          <button
            type="button"
            onClick={() => navigate('/configuracoes/usuarios')}
            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-amber-600 px-4 py-2 text-sm font-semibold text-white hover:bg-amber-500 transition-colors"
          >
            <i className="pi pi-plus text-sm" /> Ir para cadastro de usuários
          </button>
        </div>
      ) : null}

      <Table>
        
        <TableHeader>
            <TableRow>
              <TableHead>Nome da equipe</TableHead>
              <TableHead>Placa do veículo</TableHead>
              <TableHead>Cidade</TableHead>
              <TableHead>Ação</TableHead>
            </TableRow>
        </TableHeader>
        <TableBody>
            {teams.map((team) => {

              return (
                <TableRow key={team.idTeam}>
                  <TableCell>
                    <p className="font-medium">{team.teamName}</p>
                    <p className="text-xs text-slate-500 dark:text-zinc-400" >{
                      team.memberNames?.join(', ') || 'Nenhum membro adicionado'
                    }</p>
                  </TableCell>
                  <TableCell>
                    <p className="font-semibold">{team.plate}</p>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <p className="font-medium">{team.cityName}, {team.UFName}</p>
                      <p className="text-xs text-slate-500 dark:text-zinc-400">{team.regionName}</p>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2 justify-center">
                      <button
                        type="button"
                        onClick={() => {
                          openEdit(team);
                        }}
                        className="rounded-lg border border-slate-200 dark:border-zinc-700 p-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
                        <i className="pi pi-pencil text-sm" />
                      </button>
                      {/* <button
                        type="button"
                        onClick={() => setDeleteTeam(team)}
                        className="rounded-lg border border-neutral-200 dark:border-zinc-700 p-2 text-sm font-semibold text-red-600 hover:bg-red-50 transition-colors"
                      >
                        <i className="pi pi-trash text-sm" />
                      </button> */}
                    </div>
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
      </Table>
      
      <EmbeddedDocPanel
        open={docOpen}
        onClose={() => setDocOpen(false)}
        title={docs[activeDocKey].title}
        description={docs[activeDocKey].description}
        url={docs[activeDocKey].url}
      />
    </section>
  );
}
