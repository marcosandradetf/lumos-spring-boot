import { useState, useEffect, useMemo } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { Modal, ModalBody, ModalFooter, ModalHeader } from '@/shared/ui/modal';
import { SkeletonTable } from '@/shared/ui/skeleton-table';
import { usersApi } from '@/features/manage/api/usersApi';
import { useUsers } from '@/features/manage/hooks/useUsers';
import type { ActivationCodeResponse, ManagedUser, UserActivationStatus } from '@/features/manage/types/manageTypes';

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

export default function Users() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const [search, setSearch] = useState('');
  const [activationModal, setActivationModal] = useState<{ user: ManagedUser; code: ActivationCodeResponse } | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  useEffect(() => {
    setPageContext(['Configurações', 'Usuários'], 'Usuários');
  }, [setPageContext]);

  const { data: users = [], isLoading } = useUsers();
  const generateCodeMutation = useMutation({
    mutationFn: usersApi.generateActivationCode,
  });
  const resetActivationMutation = useMutation({
    mutationFn: usersApi.resetActivation,
  });

  const filtered = useMemo(() =>
    (users as ManagedUser[]).filter(u =>
      !search || u.name.toLowerCase().includes(search.toLowerCase()) ||
      u.username.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase())
    ), [users, search]);

  const handleGenerateCode = async (user: ManagedUser) => {
    setActionLoading(user.userId);
    try {
      const code = await generateCodeMutation.mutateAsync(user.userId);
      setActivationModal({ user, code });
    } catch {
      notify('Erro ao gerar código.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleResetActivation = async (user: ManagedUser) => {
    setActionLoading(user.userId + '-reset');
    try {
      const code = await resetActivationMutation.mutateAsync(user.userId);
      setActivationModal({ user, code });
      notify('Ativação redefinida. Novo código gerado.', 'success');
    } catch {
      notify('Erro ao redefinir ativação.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <section className="p-4 md:p-6 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Usuários</h1>
        <div className="relative max-w-xs w-full">
          <i className="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm" />
          <input type="text" placeholder="Pesquisar usuário..." value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 pl-9 pr-3 py-2 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors" />
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
                  <th className="px-4 py-3">Nome</th>
                  <th className="px-4 py-3">Usuário</th>
                  <th className="px-4 py-3">E-mail</th>
                  <th className="px-4 py-3">Funções</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtered.length === 0 ? (
                  <tr><td colSpan={6} className="px-4 py-10 text-center text-sm text-slate-400 dark:text-zinc-500">
                    <i className="pi pi-users text-3xl block mb-2 opacity-40" />
                    Nenhum usuário encontrado.
                  </td></tr>
                ) : filtered.map(user => (
                  <tr key={user.userId} className="border-t border-slate-100 dark:border-zinc-800">
                    <td className="px-4 py-3 text-sm font-medium text-slate-700 dark:text-zinc-200">{user.name} {user.lastname}</td>
                    <td className="px-4 py-3 text-sm text-slate-600 dark:text-zinc-300">{user.username}</td>
                    <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400">{user.email}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1">
                        {user.role.slice(0, 2).map(r => (
                          <span key={r.roleId ?? r} className="inline-flex rounded-full bg-indigo-50 dark:bg-indigo-900/20 px-2 py-0.5 text-xs text-indigo-700 dark:text-indigo-300">
                            {r.label ?? r.roleId ?? String(r)}
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
                      <div className="flex gap-1.5">
                        {user.status === 'PENDING_ACTIVATION' && (
                          <button type="button"
                            disabled={actionLoading === user.userId}
                            onClick={() => void handleGenerateCode(user)}
                            className="flex items-center gap-1 rounded-xl border border-slate-200 dark:border-zinc-700 px-2.5 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 disabled:opacity-40 transition-colors">
                            {actionLoading === user.userId ? <i className="pi pi-spin pi-spinner text-xs" /> : <i className="pi pi-key text-xs" />}
                            Gerar código
                          </button>
                        )}
                        {user.status === 'ACTIVE' && (
                          <button type="button"
                            disabled={actionLoading === user.userId + '-reset'}
                            onClick={() => void handleResetActivation(user)}
                            className="flex items-center gap-1 rounded-xl border border-amber-200 dark:border-amber-800 px-2.5 py-1.5 text-xs font-medium text-amber-700 dark:text-amber-300 hover:bg-amber-50 dark:hover:bg-amber-900/20 disabled:opacity-40 transition-colors">
                            {actionLoading === user.userId + '-reset' ? <i className="pi pi-spin pi-spinner text-xs" /> : <i className="pi pi-refresh text-xs" />}
                            Redefinir
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Activation code modal */}
      <Modal open={activationModal !== null} onClose={() => setActivationModal(null)} confirmation>
        <ModalHeader title="Código de Ativação" onClose={() => setActivationModal(null)} />
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
                  Expira em: {new Date(activationModal.code.expiresAt).toLocaleString('pt-BR')}
                </p>
              </div>
              <p className="text-xs text-slate-500 dark:text-zinc-400">{activationModal.code.message}</p>
            </>
          )}
        </ModalBody>
        <ModalFooter>
          <button type="button" onClick={() => {
            if (activationModal) navigator.clipboard.writeText(activationModal.code.activationCode).catch(() => {});
            notify('Código copiado!', 'success');
          }}
            className="rounded-xl border border-indigo-200 dark:border-indigo-800 px-4 py-2 text-sm font-semibold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-900/20 transition-colors">
            <i className="pi pi-copy mr-1.5 text-xs" />Copiar
          </button>
          <button type="button" onClick={() => setActivationModal(null)}
            className="rounded-xl bg-slate-800 dark:bg-zinc-100 text-white dark:text-zinc-900 px-4 py-2 text-sm font-semibold hover:opacity-90 transition-all">
            Fechar
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
