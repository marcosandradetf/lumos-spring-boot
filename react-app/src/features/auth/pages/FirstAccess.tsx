import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';

import api from '@/core/auth/api';
import { useNotify } from '@/shared/hooks/use-notify';

const inputClass =
  'w-full rounded-2xl border border-slate-400/25 bg-slate-900/60 py-3.5 pl-4 pr-4 text-slate-50 outline-none transition-all placeholder:text-slate-500 hover:border-slate-400/35 hover:bg-slate-900/70 focus:border-blue-400 focus:bg-slate-900/85 focus:ring-4 focus:ring-blue-500/15';

function maskCpf(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  return digits
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
}

export default function FirstAccess() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { notify } = useNotify();

  const [cpf, setCpf] = useState('');
  const [activationCode, setActivationCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const cpfQuery = searchParams.get('cpf');
    if (cpfQuery) {
      setCpf(maskCpf(cpfQuery));
    }
  }, [searchParams]);

  const activate = async (event: React.FormEvent) => {
    event.preventDefault();
    setErrorMessage(null);

    const digitsCpf = cpf.replace(/\D/g, '');

    if (!digitsCpf || !activationCode.trim() || !newPassword || !confirmPassword) {
      setErrorMessage('Preencha CPF, código de ativação e uma nova senha válida.');
      return;
    }

    if (digitsCpf.length !== 11) {
      setErrorMessage('Informe um CPF válido.');
      return;
    }

    if (newPassword.length < 8) {
      setErrorMessage('A nova senha deve ter pelo menos 8 caracteres.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage('As senhas informadas não conferem.');
      return;
    }

    setLoading(true);
    try {
      await api.post('/api/auth/activate', {
        cpf: digitsCpf,
        activationCode: activationCode.trim(),
        newPassword,
      });

      notify('Conta ativada com sucesso.', 'success');
      navigate('/auth/login?activated=1', { replace: true });
    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setErrorMessage(message ?? 'Não foi possível concluir a ativação. Revise os dados e tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="relative min-h-screen overflow-hidden bg-slate-950 text-slate-50">
      <img
        src="/lumos-login-bg.png"
        alt="Iluminação pública urbana"
        className="absolute inset-0 h-full w-full object-cover object-center opacity-15"
      />

      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,_rgba(14,165,233,0.24),_transparent_30%),radial-gradient(circle_at_bottom_right,_rgba(245,158,11,0.12),_transparent_26%)]" />

      <div className="relative z-10 mx-auto flex min-h-screen w-full  items-center justify-center px-4 py-8 sm:px-6">
        <div className="">
          <div className="hidden rounded-[2rem] border border-white/10 bg-black/20 p-10 lg:block">
            <div className="inline-flex items-center gap-2 rounded-full border border-emerald-400/20 bg-emerald-400/10 px-4 py-2 text-sm text-emerald-100">
              <i className="pi pi-shield" /> Ativação segura
            </div>

            <h1 className="mt-6 text-4xl font-semibold leading-tight text-white">Defina sua senha sem expor credenciais.</h1>

            <p className="mt-4 max-w-xl text-base leading-7 text-slate-300">
              No primeiro acesso, use seu CPF e o código gerado pelo administrador para ativar a conta e criar sua senha com segurança.
            </p>

            <div className="mt-8 grid gap-3">
              <div className="rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-4 text-slate-200"><i className="pi pi-check-circle mr-2 text-emerald-300" />O código é temporário e expira automaticamente.</div>
              <div className="rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-4 text-slate-200"><i className="pi pi-lock mr-2 text-cyan-200" />Sua senha é definida apenas por você e nunca é exibida ao administrador.</div>
              <div className="rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-4 text-slate-200"><i className="pi pi-key mr-2 text-amber-200" />Após ativar, volte ao login e entre normalmente.</div>
            </div>
          </div>

          <article className="rounded-[2rem] border border-white/10 bg-white/10 p-6 shadow-2xl shadow-slate-950/30 backdrop-blur-2xl sm:p-8">
            <div className="mb-8 text-center">
              <div className="mx-auto mb-4 flex h-[4.75rem] w-[4.75rem] items-center justify-center rounded-[1.5rem] border border-white/10 bg-white/[0.08]">
                <img src="/icon-192.png" width="64" height="64" alt="Lumos" className="drop-shadow-lg" />
              </div>

              <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-cyan-400/20 bg-cyan-400/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.24em] text-cyan-100">
                <i className="pi pi-key" /> Ativação da conta
              </div>

              <h2 className="text-3xl font-semibold tracking-tight text-white">Primeiro acesso</h2>
              <p className="mt-2 text-sm leading-6 text-slate-300">Informe seus dados para ativar a conta e cadastrar sua senha.</p>
            </div>

            <form onSubmit={activate} className="space-y-5">
              <div className="space-y-5 rounded-[1.75rem] border border-white/10 bg-black/20 p-5 sm:p-6">
                <div className="grid gap-5">
                  <div className="flex flex-col gap-2">
                    <label className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-300">CPF</label>
                    <input
                      type="text"
                      value={cpf}
                      onChange={(event) => setCpf(maskCpf(event.target.value))}
                      inputMode="numeric"
                      autoComplete="username"
                      placeholder="Digite seu CPF"
                      className={inputClass}
                    />
                  </div>

                  <div className="flex flex-col gap-2">
                    <label className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-300">Código de ativação</label>
                    <input
                      type="text"
                      value={activationCode}
                      onChange={(event) => setActivationCode(event.target.value.toUpperCase())}
                      autoComplete="one-time-code"
                      placeholder="Digite o código recebido"
                      className={inputClass}
                    />
                  </div>

                  <div className="grid gap-5 sm:grid-cols-2">
                    <div className="flex flex-col gap-2">
                      <label className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-300">Nova senha</label>
                      <input
                        type="password"
                        value={newPassword}
                        onChange={(event) => setNewPassword(event.target.value)}
                        autoComplete="new-password"
                        placeholder="No mínimo 8 caracteres"
                        className={inputClass}
                      />
                    </div>
                    <div className="flex flex-col gap-2">
                      <label className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-300">Confirmar senha</label>
                      <input
                        type="password"
                        value={confirmPassword}
                        onChange={(event) => setConfirmPassword(event.target.value)}
                        autoComplete="new-password"
                        placeholder="Repita a senha"
                        className={inputClass}
                      />
                    </div>
                  </div>
                </div>
              </div>

              {errorMessage && (
                <p className="rounded-xl border border-red-500/30 bg-red-500/15 px-4 py-3 text-sm text-red-200">{errorMessage}</p>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full rounded-2xl bg-gradient-to-r from-emerald-500 to-cyan-500 py-3.5 font-semibold text-white shadow-lg shadow-emerald-500/20 transition hover:from-emerald-600 hover:to-cyan-500 disabled:opacity-50"
              >
                {loading ? <i className="pi pi-spin pi-spinner mr-2" /> : null}
                Ativar conta
              </button>

              <Link to="/auth/login" className="block text-center text-sm font-medium text-blue-200 transition-colors hover:text-blue-100">
                Voltar para o login
              </Link>
            </form>
          </article>
        </div>
      </div>
    </section>
  );
}
