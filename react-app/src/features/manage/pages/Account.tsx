import { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useAuthStore } from '@/core/auth/useAuthStore';
import { useNotify } from '@/shared/hooks/use-notify';
import { Modal, ModalBody, ModalFooter, ModalHeader } from '@/shared/ui/modal';
import { manageApi } from '@/features/manage/api/manageApi';
import { passwordChangeSchema } from '@/features/manage/validations/userSchema';

export default function Account() {
  const { setPageContext } = useAppStore();
  const { user } = useAuthStore();
  const { notify } = useNotify();
  const changePasswordMutation = useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
      manageApi.changePassword(currentPassword, newPassword),
  });

  const [pwModal, setPwModal] = useState(false);
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setPageContext(['Configurações', 'Minha Conta'], 'Minha Conta');
  }, [setPageContext]);

  const handleChangePassword = async () => {
    const validation = passwordChangeSchema.safeParse({
      currentPassword: current,
      nextPassword: next,
      confirmPassword: confirm,
    });
    if (!validation.success) {
      notify(validation.error.issues[0]?.message ?? 'Dados inválidos.', 'warn');
      return;
    }
    setSaving(true);
    try {
      await changePasswordMutation.mutateAsync({ currentPassword: current, newPassword: next });
      notify('Senha alterada com sucesso.', 'success');
      setPwModal(false);
      setCurrent(''); setNext(''); setConfirm('');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Não foi possível alterar a senha.';
      notify(msg, 'error');
    } finally {
      setSaving(false);
    }
  };

  const inputClass = 'w-full rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2.5 text-sm text-slate-800 dark:text-zinc-100 placeholder:text-slate-400 outline-none focus:border-indigo-400 transition-colors';

  const infoCard = (label: string, value: string | undefined) => (
    <div key={label} className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-4">
      <p className="text-xs font-medium text-slate-500 dark:text-zinc-400 mb-1">{label}</p>
      <p className="text-sm font-semibold text-slate-800 dark:text-zinc-100 truncate">{value || '—'}</p>
    </div>
  );

  return (
    <section className="p-4 md:p-6 max-w-3xl mx-auto space-y-6">
      {/* Profile header */}
      <div className="flex items-center gap-4 pb-6 border-b border-slate-200 dark:border-zinc-800">
        <div className="w-14 h-14 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-xl font-bold text-white shadow-sm">
          {user?.fullName?.charAt(0)?.toUpperCase() ?? 'U'}
        </div>
        <div className="min-w-0">
          <h2 className="text-lg font-semibold text-slate-800 dark:text-zinc-100 truncate">{user?.username}</h2>
          <p className="text-sm text-slate-500 dark:text-zinc-400 truncate">{user?.email}</p>
        </div>
      </div>

      {/* Account info */}
      <div>
        <h3 className="text-base font-semibold text-slate-800 dark:text-zinc-100 mb-1">Conta</h3>
        <p className="text-sm text-slate-500 dark:text-zinc-400 mb-4">Informações básicas do seu perfil.</p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {infoCard('Nome completo', user?.fullName)}
          {infoCard('Usuário', user?.username)}
          {infoCard('E-mail', user?.email)}
          {infoCard('Tenant', user?.tenant)}
        </div>

        {/* Roles */}
        {user?.roles && user.roles.length > 0 && (
          <div className="mt-3 rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-4">
            <p className="text-xs font-medium text-slate-500 dark:text-zinc-400 mb-2">Funções no sistema</p>
            <div className="flex flex-wrap gap-2">
              {user.roles.map(role => (
                <span key={role}
                  className="inline-flex rounded-full bg-indigo-50 dark:bg-indigo-900/20 px-3 py-1 text-xs font-semibold text-indigo-700 dark:text-indigo-300">
                  {role}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Security */}
      <div>
        <h3 className="text-base font-semibold text-slate-800 dark:text-zinc-100 mb-1">Segurança</h3>
        <p className="text-sm text-slate-500 dark:text-zinc-400 mb-4">Gerencie credenciais e informações sensíveis.</p>
        <div className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-4 flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-700 dark:text-zinc-200">Senha</p>
            <p className="text-xs text-slate-500 dark:text-zinc-400 mt-0.5">Altere sua senha de acesso</p>
          </div>
          <button type="button" onClick={() => setPwModal(true)}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-700 dark:text-zinc-200 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
            Alterar senha
          </button>
        </div>
      </div>

      {/* Change password modal */}
      <Modal open={pwModal} onClose={() => { setPwModal(false); setCurrent(''); setNext(''); setConfirm(''); }} confirmation>
        <ModalHeader title="Alterar Senha" onClose={() => setPwModal(false)} />
        <ModalBody className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Senha atual</label>
            <input type="password" value={current} onChange={e => setCurrent(e.target.value)} autoFocus
              placeholder="Digite sua senha atual" className={inputClass} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Nova senha</label>
            <input type="password" value={next} onChange={e => setNext(e.target.value)}
              placeholder="Mínimo 8 caracteres" className={inputClass} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Confirmar nova senha</label>
            <input type="password" value={confirm} onChange={e => setConfirm(e.target.value)}
              placeholder="Repita a nova senha" className={inputClass} />
          </div>
          {next && confirm && next !== confirm && (
            <p className="text-xs text-red-500">As senhas não conferem.</p>
          )}
        </ModalBody>
        <ModalFooter>
          <button type="button" onClick={() => { setPwModal(false); setCurrent(''); setNext(''); setConfirm(''); }}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
            Cancelar
          </button>
          <button type="button" disabled={!current || !next || !confirm || saving}
            onClick={() => void handleChangePassword()}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors">
            {saving && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Salvar
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
