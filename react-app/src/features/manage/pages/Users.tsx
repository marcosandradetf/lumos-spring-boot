import { useState, useEffect, useMemo } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { Modal, ModalBody, ModalFooter, ModalHeader } from '@/shared/ui/modal';
import { SkeletonTable } from '@/shared/ui/skeleton-table';
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from '@/shared/components/ui/drawer';
import { AppDatePicker } from '@/shared/components/app-date-picker';
import { usersApi } from '@/features/manage/api/usersApi';
import { manageKeys } from '@/features/manage/api/queryKeys';
import { useUsers } from '@/features/manage/hooks/useUsers';
import type {
  ActivationCodeResponse,
  ManagedUser,
  RoleOption,
  UserActivationStatus,
  UserUpdatePayload,
} from '@/features/manage/types/manageTypes';
import { LockKeyhole, LockKeyholeOpen } from 'lucide-react';
import { GlassMultiSelect } from '@/shared/components/glass-multi-select';
import { getApiErrorMessage, type ApiHttpError } from '@/shared/api/http-error';

type EditableUser = ManagedUser & {
  clientId: string;
  year: string;
  month: string;
  day: string;
  sel: boolean;
  show: boolean;
};

type ActivationModalState = {
  user: EditableUser;
  code: ActivationCodeResponse;
  title: string;
  isOperational: boolean;
};

const STATUS_BADGE: Record<UserActivationStatus, string> = {
  ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300',
  PENDING_ACTIVATION: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  BLOCKED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
};

const STATUS_LABEL: Record<UserActivationStatus, string> = {
  ACTIVE: 'Ativo',
  PENDING_ACTIVATION: 'Pendente',
  BLOCKED: 'Bloqueado',
};

const inputClass = 'w-full rounded-full border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors';

function makeClientId() {
  return `draft-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function isActivationStatus(value: unknown): value is UserActivationStatus {
  return value === 'PENDING_ACTIVATION' || value === 'ACTIVE' || value === 'BLOCKED';
}

function normalizeStatus(value: unknown): UserActivationStatus {
  if (typeof value === 'boolean') {
    return value ? 'ACTIVE' : 'BLOCKED';
  }

  return isActivationStatus(value) ? value : 'PENDING_ACTIVATION';
}

function roleToken(role: RoleOption | string): string {
  return typeof role === 'string' ? role : role.roleName || role.roleId || role.label;
}

function normalizeRole(role: unknown, roleOptions: RoleOption[]): RoleOption | null {
  const raw = typeof role === 'string'
    ? role
    : (role as Partial<RoleOption> | undefined)?.roleName
    ?? (role as Partial<RoleOption> | undefined)?.roleId
    ?? (role as Partial<RoleOption> | undefined)?.label;

  if (!raw) {
    return null;
  }

  const token = String(raw);
  const reference = roleOptions.find((option) =>
    [option.roleName, option.roleId, option.label].some((value) => value === token),
  );
  const roleObject = typeof role === 'object' && role !== null ? role as Partial<RoleOption> : {};

  return {
    selected: roleObject.selected ?? reference?.selected ?? false,
    roleId: String(roleObject.roleId ?? reference?.roleId ?? token),
    roleName: String(roleObject.roleName ?? reference?.roleName ?? token),
    label: String(reference?.label ?? roleObject.label ?? token),
    description: String(reference?.description ?? roleObject.description ?? ''),
  };
}

function normalizeUser(user: ManagedUser, roleOptions: RoleOption[]): EditableUser {
  const dateParts = user.dateOfBirth?.split('-') ?? [];
  const roles = (Array.isArray(user.role) ? user.role : [])
    .map((role) => normalizeRole(role, roleOptions))
    .filter((role): role is RoleOption => Boolean(role));

  return {
    ...user,
    clientId: user.userId || makeClientId(),
    userId: user.userId ?? '',
    username: user.username ?? '',
    name: user.name ?? '',
    lastname: user.lastname ?? '',
    email: user.email ?? '',
    cpfCnpj: user.cpfCnpj ?? '',
    year: `${user.year ?? dateParts[0] ?? ''}`,
    month: `${user.month ?? (dateParts[1] ? Number(dateParts[1]) : '')}`,
    day: `${user.day ?? (dateParts[2] ? Number(dateParts[2]) : '')}`,
    role: roles,
    status: normalizeStatus(user.status),
    mustChangePassword: user.mustChangePassword ?? false,
    activationExpiresAt: user.activationExpiresAt ?? null,
    sel: user.sel ?? false,
    show: user.show ?? false,
  };
}

function createDraftUser(): EditableUser {
  return {
    clientId: makeClientId(),
    userId: '',
    username: '',
    name: '',
    lastname: '',
    email: '',
    cpfCnpj: '',
    year: '',
    month: '',
    day: '',
    role: [],
    status: 'PENDING_ACTIVATION',
    mustChangePassword: false,
    activationExpiresAt: null,
    sel: true,
    show: false,
  };
}

function toUpdatePayload(user: EditableUser): UserUpdatePayload {
  return {
    userId: user.userId,
    username: user.username,
    name: user.name,
    lastname: user.lastname,
    email: user.email,
    cpfCnpj: user.cpfCnpj,
    year: user.year,
    month: user.month,
    day: user.day,
    role: user.role.map(roleToken).filter(Boolean),
    status: user.status,
    sel: user.sel,
  };
}

function isOperationalUser(user: EditableUser) {
  const roles = user.role.map((role) => role.roleName);
  return roles.includes('MOTORISTA') || roles.includes('ELETRICISTA');
}

function formatActivationExpiration(value: string | null | undefined) {
  if (!value) {
    return 'Sem código ativo';
  }

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

function getBirthDate(user: EditableUser) {
  const day = Number(user.day);
  const month = Number(user.month);
  const year = Number(user.year);

  if (!day || !month || !year) {
    return null;
  }

  const date = new Date(year, month - 1, day);

  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null;
  }

  return date;
}

async function writeClipboard(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textArea = document.createElement('textarea');
  textArea.value = text;
  textArea.style.position = 'fixed';
  textArea.style.opacity = '0';
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();
  document.execCommand('copy');
  document.body.removeChild(textArea);
}

export default function Users() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [draftUsers, setDraftUsers] = useState<EditableUser[]>([]);
  const [userPatches, setUserPatches] = useState<Record<string, Partial<EditableUser>>>({});
  const [editingClientId, setEditingClientId] = useState<string | null>(null);
  const [activationModal, setActivationModal] = useState<ActivationModalState | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  useEffect(() => {
    setPageContext(['Configurações', 'Usuários'], 'Usuários');
  }, [setPageContext]);

  const { data: fetchedUsers = [], isLoading } = useUsers();
  const { data: roles = [] } = useQuery({
    queryKey: manageKeys.roles(),
    queryFn: usersApi.getRoles,
  });

  const normalizedUsers = useMemo(
    () => (fetchedUsers as ManagedUser[]).map((user) => normalizeUser(user, roles)),
    [fetchedUsers, roles],
  );

  const users = useMemo(
    () => [
      ...draftUsers,
      ...normalizedUsers.map((user) => ({
        ...user,
        ...userPatches[user.clientId],
      })),
    ],
    [draftUsers, normalizedUsers, userPatches],
  );

  const generateCodeMutation = useMutation({
    mutationFn: usersApi.generateActivationCode,
  });
  const resetActivationMutation = useMutation({
    mutationFn: usersApi.resetActivation,
  });

  const updateUsersMutation = useMutation({
    mutationFn: usersApi.updateUsers,
    onSuccess: async (response) => {
      queryClient.setQueryData(manageKeys.users(), response);
      setDraftUsers([]);
      setUserPatches({});
      await queryClient.invalidateQueries({ queryKey: manageKeys.users() });
      notify('Atualização realizada com sucesso.', 'success');ß
    },
    onError: (error) => {
      const message = getApiErrorMessage(error);
      if (message) notify(message, 'error');
    }
  });

  const filtered = useMemo(() => {
    const value = search.trim().toLowerCase();

    if (!value) {
      return users;
    }

    return users.filter((user) => {
      const roleNames = user.role.map((role) => `${role.label} ${role.roleName}`).join(' ');
      const searchableFields = [
        user.name,
        user.lastname,
        `${user.name} ${user.lastname}`,
        user.username,
        user.email,
        user.cpfCnpj,
        roleNames,
        STATUS_LABEL[user.status],
      ];

      return searchableFields.some((field) => field.toLowerCase().includes(value));
    });
  }, [search, users]);

  const draftUsersCount = users.filter((user) => !user.userId).length;
  const editingUser = useMemo(
    () => users.find((user) => user.clientId === editingClientId) ?? null,
    [editingClientId, users],
  );

  const patchUser = (clientId: string, patch: Partial<EditableUser>) => {
    if (clientId.startsWith('draft-')) {
      setDraftUsers((previous) => previous.map((user) => user.clientId === clientId ? { ...user, ...patch } : user));
      return;
    }

    setUserPatches((previous) => ({
      ...previous,
      [clientId]: {
        ...previous[clientId],
        ...patch,
      },
    }));
  };

  const handleNewUser = () => {
    const draft = createDraftUser();
    setDraftUsers((previous) => [draft, ...previous]);
    setEditingClientId(draft.clientId);
  };

  const handleRemoveDrafts = () => {
    setDraftUsers([]);
    if (editingClientId?.startsWith('draft-')) {
      setEditingClientId(null);
    }
  };

  const handleToggleEdit = (user: EditableUser) => {
    setEditingClientId(user.clientId);
    patchUser(user.clientId, { sel: true });
  };

  const handleRoleChange = (user: EditableUser, selectedRoleNames: string[]) => {
    const selectedRoles = selectedRoleNames
      .map((roleName) => roles.find((role) => role.roleName === roleName) ?? normalizeRole(roleName, roles))
      .filter((role): role is RoleOption => Boolean(role));

    patchUser(user.clientId, { role: selectedRoles });
  };

  const handleCloseUserDrawer = () => {
    if (editingClientId?.startsWith('draft-')) {
      setDraftUsers((previous) => previous.filter((user) => user.clientId !== editingClientId));
    } else if (editingClientId) {
      setUserPatches((previous) => {
        const next = { ...previous };
        delete next[editingClientId];
        return next;
      });
    }

    setEditingClientId(null);
  };

  const handleSaveUser = async () => {
    if (!editingUser) {
      return;
    }

    try {
      await updateUsersMutation.mutateAsync([toUpdatePayload(editingUser)]);
      setEditingClientId(null);
    } catch (error) {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      notify(message ?? 'Não foi possível atualizar os usuários.', 'error');
    }
  };

  const applyActivationResponse = (userId: string, expiresAt: string) => {
    setUserPatches((previous) => ({
      ...previous,
      [userId]: {
        ...previous[userId],
        status: 'PENDING_ACTIVATION',
        mustChangePassword: true,
        activationExpiresAt: expiresAt,
      },
    }));

    queryClient.setQueryData<ManagedUser[]>(manageKeys.users(), (previous) =>
      previous?.map((user) => user.userId === userId ? {
        ...user,
        status: 'PENDING_ACTIVATION',
        mustChangePassword: true,
        activationExpiresAt: expiresAt,
      } : user) ?? previous,
    );
  };

  const handleGenerateCode = async (user: EditableUser) => {
    if (!user.userId) {
      notify('Salve o usuário antes de gerar o código de ativação.', 'info');
      return;
    }

    setActionLoading(user.clientId);
    try {
      const code = await generateCodeMutation.mutateAsync(user.userId);
      applyActivationResponse(user.userId, code.expiresAt);
      setActivationModal({ user, code, title: 'Código de ativação gerado', isOperational: isOperationalUser(user) });
      notify(code.message, 'success');
    } catch (error) {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      notify(message ?? 'Erro ao gerar código.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleResetActivation = async (user: EditableUser) => {
    if (!user.userId) {
      notify('Salve o usuário antes de resetar a ativação.', 'info');
      return;
    }

    setActionLoading(`${user.clientId}-reset`);
    try {
      const code = await resetActivationMutation.mutateAsync(user.userId);
      applyActivationResponse(user.userId, code.expiresAt);
      setActivationModal({ user, code, title: 'Código de ativação redefinido', isOperational: isOperationalUser(user) });
      notify(code.message, 'success');
    } catch (error) {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      notify(message ?? 'Erro ao redefinir ativação.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleCopyActivationCode = async () => {
    if (!activationModal) {
      return;
    }

    try {
      await writeClipboard(activationModal.code.activationCode);
      notify('Código copiado!', 'success');
    } catch {
      notify('Não foi possível copiar o código.', 'error');
    }
  };

  const handleShareActivationCode = async () => {
    if (!activationModal) {
      return;
    }

    const expiration = formatActivationExpiration(activationModal.code.expiresAt);
    const text = `Olá! Você foi convidado para acessar o Lumos IP.\n\nCódigo de ativação: ${activationModal.code.activationCode}\nValidade: ${expiration}\n\n${activationModal.isOperational
      ? 'Use o app Lumos OP, toque em "Primeiro acesso", informe seu CPF, o código acima e crie sua senha.'
      : 'Acesse https://app.lumosip.com.br/primeiro-acesso, informe seu CPF, o código acima e crie sua senha.'
      }\n\nEste código é pessoal e expira automaticamente.`;

    try {
      if (navigator.share) {
        await navigator.share({ title: 'Acesso ao Lumos IP', text });
        return;
      }

      await writeClipboard(text);
      notify('Mensagem de ativação copiada.', 'success');
    } catch (error) {
      if ((error as { name?: string })?.name !== 'AbortError') {
        notify('Não foi possível compartilhar o código de ativação.', 'error');
      }
    }
  };

  return (
    <section className="p-4 md:p-6 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Usuários</h1>
        <div className="flex w-full flex-wrap items-center justify-end gap-2 md:w-auto">
          {draftUsersCount > 0 && (
            <button type="button"
              onClick={handleRemoveDrafts}
              className="rounded-full border border-amber-200 dark:border-amber-800 px-3 py-2 text-xs font-semibold text-amber-700 dark:text-amber-300 hover:bg-amber-50 dark:hover:bg-amber-900/20 transition-colors">
              <i className="pi pi-trash mr-1.5 text-xs" />
              Remover rascunhos
            </button>
          )}
          <button type="button"
            onClick={handleNewUser}
            className="rounded-full border border-indigo-200 dark:border-indigo-800 px-3 py-2 text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-900/20 transition-colors">
            <i className="pi pi-user-plus mr-1.5 text-xs" />
            Novo usuário
          </button>
          <div className="relative max-w-xs w-full">
            <i className="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm" />
            <input type="text" placeholder="Pesquisar usuário..." value={search}
              onChange={e => setSearch(e.target.value)}
              className="w-full rounded-full border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 pl-9 pr-3 py-2 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors" />
          </div>
        </div>
      </div>

      {isLoading ? (
        <SkeletonTable columns={5} />
      ) : (
        <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-slate-50 dark:bg-zinc-900/50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400">
                <tr>
                  <th className="px-4 py-3">Credenciais</th>
                  <th className="px-4 py-3">Funções</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtered.length === 0 ? (
                  <tr><td colSpan={4} className="px-4 py-10 text-center text-sm text-slate-400 dark:text-zinc-500">
                    <i className="pi pi-users text-3xl block mb-2 opacity-40" />
                    Nenhum usuário encontrado.
                  </td></tr>
                ) : filtered.map(user => (
                  <tr key={user.clientId} className="border-t border-slate-100 dark:border-zinc-800">
                    <td>
                      <div className='flex flex-col gap-1 py-2'>
                        <p className="px-4 text-sm text-slate-700 dark:text-zinc-200">{user.name} {user.lastname}</p>
                        <p className="px-4 text-xs font-bold text-slate-600 dark:text-zinc-300">{user.username || 'Usuário não definido'} / {user.cpfCnpj || 'CPF/CNPJ pendente'}</p>
                        <p className="px-4 text-xs text-slate-500 dark:text-zinc-400">{user.email || 'E-mail não informado'}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1">
                        {user.role.slice(0, 2).map(r => (
                          <span key={r.roleId || r.roleName} className="inline-flex rounded-full bg-indigo-50 dark:bg-indigo-900/20 px-2 py-0.5 text-xs text-indigo-700 dark:text-indigo-300">
                            {r.label || r.roleName}
                          </span>
                        ))}
                        {user.role.length > 2 && (
                          <span className="inline-flex rounded-full bg-slate-100 dark:bg-zinc-800 px-2 py-0.5 text-xs text-slate-500 dark:text-zinc-400">+{user.role.length - 2}</span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_BADGE[user.status]}`}>
                        {STATUS_LABEL[user.status]}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1.5">
                        <button type="button"
                          onClick={() => handleToggleEdit(user)}
                          className="flex items-center gap-1 rounded-full border border-slate-200 dark:border-zinc-700 px-2.5 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
                          <i className="pi pi-pencil text-xs" />
                          Editar
                        </button>
                        {user.status === 'PENDING_ACTIVATION' && (
                          <button type="button"
                            disabled={actionLoading === user.clientId}
                            onClick={() => void handleGenerateCode(user)}
                            className="flex items-center gap-1 rounded-full border border-slate-200 dark:border-zinc-700 px-2.5 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 disabled:opacity-40 transition-colors">
                            {actionLoading === user.clientId ? <i className="pi pi-spin pi-spinner text-xs" /> : <i className="pi pi-key text-xs" />}
                            Gerar código
                          </button>
                        )}
                        {user.status === 'ACTIVE' && (
                          <button type="button"
                            disabled={actionLoading === `${user.clientId}-reset`}
                            onClick={() => void handleResetActivation(user)}
                            className="flex items-center gap-1 rounded-full border border-amber-200 dark:border-amber-800 px-2.5 py-1.5 text-xs font-medium text-amber-700 dark:text-amber-300 hover:bg-amber-50 dark:hover:bg-amber-900/20 disabled:opacity-40 transition-colors">
                            {actionLoading === `${user.clientId}-reset` ? <i className="pi pi-spin pi-spinner text-xs" /> : <i className="pi pi-refresh text-xs" />}
                            Redefinir Senha
                          </button>
                        )}

                        <button type="button"
                          disabled={actionLoading === `${user.clientId}-reset`}
                          onClick={async () => {
                            if (user.status === 'BLOCKED') {
                              void handleResetActivation(user)
                              return;
                            }

                            const blockedStatus: UserActivationStatus = 'BLOCKED';
                            const editedUser = {
                              ...user,
                              sel: true,
                              status: blockedStatus,
                            };

                            patchUser(user.clientId, { sel: true, status: blockedStatus });

                            await updateUsersMutation.mutateAsync([toUpdatePayload(editedUser)]);

                          }}
                          className={
                            `flex items-center gap-1 rounded-full border  px-2.5 py-1.5 text-xs font-medium disabled:opacity-40 transition-colors
                            ${user.status === 'BLOCKED' ? 'border-blue-200 dark:border-blue-800 text-blue-700 dark:text-blue-300 hover:bg-blue-50 dark:hover:bg-blue-900/20'
                              : 'border-red-500 dark:border-red-800 text-red-500 dark:text-red-300 hover:bg-red-50 dark:hover:bg-red-900/20'}`
                          }>
                          {user.status === 'BLOCKED' ? <LockKeyholeOpen size='14' /> : <LockKeyhole size='14' />}
                          {user.status === 'BLOCKED' ? 'Reativar Acesso' : 'Bloquear Acesso'}
                        </button>

                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Drawer direction="bottom" handleOnly open={editingUser !== null} onOpenChange={(open) => {
        if (!open) {
          handleCloseUserDrawer();
        }
      }}>
        <DrawerContent
          className="max-h-[88vh] border-slate-200 bg-white text-slate-900 shadow-2xl dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-100"
          style={{ touchAction: 'pan-y' }}
        >
          {editingUser && (
            <>
              <DrawerHeader className="mx-auto w-full max-w-6xl border-b border-slate-200 px-4 pb-4 pt-5 text-left dark:border-zinc-800 sm:px-6">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <DrawerTitle className="text-xl font-semibold text-slate-900 dark:text-zinc-50">
                      {editingUser.userId ? 'Editar usuário' : 'Novo usuário'}
                    </DrawerTitle>
                    <DrawerDescription className="mt-1 text-sm text-slate-500 dark:text-zinc-400">
                      {editingUser.name || editingUser.lastname
                        ? `${editingUser.name} ${editingUser.lastname}`.trim()
                        : editingUser.username || 'Preencha os dados do acesso'}
                    </DrawerDescription>
                  </div>
                  <span className={`inline-flex w-fit rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_BADGE[editingUser.status]}`}>
                    {STATUS_LABEL[editingUser.status]}
                  </span>
                </div>
              </DrawerHeader>

              <div className="mx-auto w-full max-w-6xl flex-1 overflow-y-auto px-4 py-5 sm:px-6">
                <div className="grid gap-5">
                  <div className="space-y-5">
                    <section className="space-y-3">
                      <div className="grid gap-3 md:grid-cols-2">
                        <label className="space-y-1.5">
                          <span className="text-xs font-semibold text-slate-600 dark:text-zinc-300">Usuário</span>
                          <input value={editingUser.username} onChange={(event) => patchUser(editingUser.clientId, { username: event.target.value })} className={inputClass} placeholder="Usuário" />
                        </label>
                        <label className="space-y-1.5">
                          <span className="text-xs font-semibold text-slate-600 dark:text-zinc-300">E-mail</span>
                          <input value={editingUser.email} onChange={(event) => patchUser(editingUser.clientId, { email: event.target.value })} className={inputClass} placeholder="E-mail" />
                        </label>
                        <label className="space-y-1.5">
                          <span className="text-xs font-semibold text-slate-600 dark:text-zinc-300">Nome</span>
                          <input value={editingUser.name} onChange={(event) => patchUser(editingUser.clientId, { name: event.target.value })} className={inputClass} placeholder="Nome" />
                        </label>
                        <label className="space-y-1.5">
                          <span className="text-xs font-semibold text-slate-600 dark:text-zinc-300">Sobrenome</span>
                          <input value={editingUser.lastname} onChange={(event) => patchUser(editingUser.clientId, { lastname: event.target.value })} className={inputClass} placeholder="Sobrenome" />
                        </label>
                        <label className="space-y-1.5">
                          <span className="text-xs font-semibold text-slate-600 dark:text-zinc-300">CPF/CNPJ</span>
                          <input value={editingUser.cpfCnpj} onChange={(event) => patchUser(editingUser.clientId, { cpfCnpj: event.target.value })} className={inputClass} placeholder="CPF/CNPJ" />
                        </label>

                        <label className="space-y-1.5">
                          <span className="text-xs font-semibold text-slate-600 dark:text-zinc-300">Nascimento</span>
                          <AppDatePicker
                            value={getBirthDate(editingUser)}
                            onChange={(date) => {
                              patchUser(editingUser.clientId, {
                                day: date ? String(date.getDate()) : '',
                                month: date ? String(date.getMonth() + 1) : '',
                                year: date ? String(date.getFullYear()) : '',
                              });
                            }}
                            maxDate={new Date()}
                            placeholder="Selecione a data de nascimento"
                          />
                        </label>

                      </div>
                    </section>
                  </div>

                  <aside className="space-y-3">
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3 dark:border-zinc-800 dark:bg-zinc-900/70">
                      <p className="text-sm font-semibold text-slate-700 dark:text-zinc-200">Funções</p>
                      <GlassMultiSelect
                        value={editingUser.role.map((role) => role.roleName)}
                        onChange={(selectedRoles) => handleRoleChange(editingUser, selectedRoles)}
                        options={roles.map((role) => ({
                          label: role.label,
                          value: role.roleName,
                        }))}
                        placeholder="Selecione as funções"
                        search
                        searchPlaceholder="Pesquisar função..."
                        inlineOptions
                        className="mt-3"
                        buttonClassName="border-slate-200 bg-white dark:border-zinc-700 dark:bg-zinc-950"
                        optionsClassName="bg-white dark:bg-zinc-950"
                        emptyText="Nenhuma função disponível"
                      />
                    </div>
                  </aside>
                </div>
              </div>

              <DrawerFooter className="mx-auto w-full max-w-6xl flex-col border-t border-slate-200 px-4 py-4 dark:border-zinc-800 sm:flex-row sm:justify-end sm:px-6">
                <button type="button"
                  onClick={handleCloseUserDrawer}
                  className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-50 dark:border-zinc-700 dark:text-zinc-300 dark:hover:bg-zinc-800">
                  Cancelar
                </button>
                <button type="button"
                  disabled={updateUsersMutation.isPending}
                  onClick={() => void handleSaveUser()}
                  className="rounded-full bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-indigo-500 disabled:opacity-50">
                  {updateUsersMutation.isPending && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
                  Salvar usuário
                </button>
              </DrawerFooter>
            </>
          )}
        </DrawerContent>
      </Drawer>

      <Modal open={activationModal !== null} onClose={() => setActivationModal(null)} confirmation>
        <ModalHeader title={activationModal?.title ?? 'Código de Ativação'} onClose={() => setActivationModal(null)} />
        <ModalBody className="space-y-3">
          {activationModal && (
            <>
              <p className="text-sm text-slate-600 dark:text-zinc-300">
                Usuário: <strong>{activationModal.user.username}</strong>
              </p>
              <div className="rounded-xl bg-slate-50 dark:bg-zinc-800 border border-slate-200 dark:border-zinc-700 p-4 text-center">
                <p className="text-xs text-slate-500 dark:text-zinc-400 mb-1">Código de ativação</p>
                <p className="text-2xl font-bold font-mono tracking-widest text-indigo-600 dark:text-indigo-400">
                  {activationModal.code.activationCode}
                </p>
                <p className="text-xs text-slate-400 dark:text-zinc-500 mt-2">
                  Expira em: {formatActivationExpiration(activationModal.code.expiresAt)}
                </p>
              </div>
              <p className="text-xs text-slate-500 dark:text-zinc-400">{activationModal.code.message}</p>
              <p className="text-xs text-slate-500 dark:text-zinc-400">
                {activationModal.isOperational
                  ? 'Oriente o usuário operacional a usar o app Lumos OP na opção Primeiro acesso.'
                  : 'Oriente o usuário administrativo a acessar o primeiro acesso web.'}
              </p>
            </>
          )}
        </ModalBody>
        <ModalFooter>
          <button type="button" onClick={() => void handleShareActivationCode()}
            className="rounded-full border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-300 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
            <i className="pi pi-share-alt mr-1.5 text-xs" />Compartilhar
          </button>
          <button type="button" onClick={() => void handleCopyActivationCode()}
            className="rounded-full border border-indigo-200 dark:border-indigo-800 px-4 py-2 text-sm font-semibold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-900/20 transition-colors">
            <i className="pi pi-copy mr-1.5 text-xs" />Copiar
          </button>
          <button type="button" onClick={() => setActivationModal(null)}
            className="rounded-full bg-slate-800 dark:bg-zinc-100 text-white dark:text-zinc-900 px-4 py-2 text-sm font-semibold hover:opacity-90 transition-all">
            Fechar
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}


