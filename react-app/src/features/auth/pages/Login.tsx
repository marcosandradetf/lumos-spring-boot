import React, { useMemo, useState, useEffect } from 'react';
import { useLocation, useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useAuthStore } from '../../../core/auth/useAuthStore';
import { useNotify } from '../../../shared/hooks/use-notify';
import { useAppStore } from '../../../store/use-app-store';
import api from '../../../core/auth/api';
import { checkOnboardingState } from '../../../core/onboarding/checkOnboardingState';

export default function Login() {
    const navigate = useNavigate();
    const location = useLocation();
    const [searchParams] = useSearchParams();
    const { login, isLoading, isLoggedIn, applyAuthResponse } = useAuthStore();
    const { toggleMenu, setOnboarding } = useAppStore();
    const { notify } = useNotify();

    // --- UI State ---
    const [activeTab, setActiveTab] = useState<'login' | 'signup' | 'demo'>('login');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [authError, setAuthError] = useState<string | null>(null);
    const [authLoading, setAuthLoading] = useState(false);
    const [finished, setFinished] = useState(false);
    const [message, setMessage] = useState('');

    const redirectPath = useMemo(() => {
        const redirectFromQuery = searchParams.get('redirect');
        if (redirectFromQuery) {
            return redirectFromQuery;
        }

        const from = (location.state as { from?: { pathname?: string; search?: string } } | null)?.from;
        if (from?.pathname) {
            return `${from.pathname}${from.search ?? ''}`;
        }

        return '/';
    }, [location.state, searchParams]);

    // --- Recuperação de Parâmetros da URL ---
    useEffect(() => {
        if (isLoggedIn) {
            navigate(redirectPath);
            return;
        }

        const token = searchParams.get('token');
        const activated = searchParams.get('activated');
        const redirect = searchParams.get('redirect');

        if (activated === '1') {
            setFinished(true);
            setMessage('Conta ativada com sucesso. Faça login com sua nova senha.');
        }

        if (token) {
            handleQrCodeLogin(token, redirect);
        }
    }, [isLoggedIn, navigate, redirectPath, searchParams]);

    const checkStateAndRedirect = async (targetPath: string) => {
        setAuthLoading(true);
        try {
            const isOnboardingPending = await checkOnboardingState();

            if (isOnboardingPending) {
                localStorage.removeItem('onboarding');
                setOnboarding(true);
                navigate('/configuracoes/onboarding');
                return;
            }

            localStorage.setItem('onboarding', 'finished');
            setOnboarding(false);
            navigate(targetPath);
        } catch {
            notify('Erro ao validar configuração inicial.', 'error');
            navigate(targetPath);
        } finally {
            setAuthLoading(false);
        }
    };

    // --- Handlers ---
    const handleQrCodeLogin = async (token: string, redirect: string | null) => {
        try {
            const response = await api.post('/api/auth/login-with-qrcode-token', null, {
                params: { token }
            });
            applyAuthResponse(response.data.accessToken);
            navigate(redirect || redirectPath);
        } catch (error) {
            notify('Erro ao autenticar via QR Code', 'error');
        }
    };

    const normalizeUsername = () => {
        const digits = username.replace(/\D/g, '');
        if (digits.length === 11) setUsername(digits);
    };

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setAuthError(null);
        setFinished(false);
        normalizeUsername();
        setAuthLoading(true);

        try {
            await login(username, password);

            const authUser = useAuthStore.getState().user;
            const roles = authUser?.roles ?? [];
            const hasPermission = roles.includes('ADMIN') || roles.includes('ANALISTA') || roles.includes('RESPONSAVEL_TECNICO');

            localStorage.setItem('isSupport', authUser?.support ? 'true' : 'false');

            if (window.matchMedia('(min-width: 1280px)').matches) {
                localStorage.setItem('menuOpen', 'true');
                toggleMenu(true);
            }

            if (localStorage.getItem('onboarding') || !hasPermission) {
                navigate(redirectPath);
                return;
            }

            await checkStateAndRedirect(redirectPath);
        } catch (error: any) {
            const errorCode = error?.response?.data?.code || error?.response?.data?.error;

            if (errorCode === 'USER_NOT_ACTIVATED') {
                navigate(`/primeiro-acesso?cpf=${/^\d{11}$/.test(username) ? username : ''}`);
                return;
            }

            const msg = error?.response?.data?.message || 'Usuário ou senha inválidos.';
            setAuthError(msg);
        } finally {
            setAuthLoading(false);
        }
    };

    const forgetPassword = () => {
        setAuthError(null);
        setFinished(true);
        setMessage('Uma nova senha foi enviada para seu e-mail (verifique também o spam).');
    };

    return (
        <section className="relative min-h-screen w-full overflow-hidden bg-slate-950 text-slate-50">
            <img
                src="/lumos-login-bg.png"
                alt="Iluminação pública urbana"
                className="absolute inset-0 h-full w-full object-cover object-center opacity-20"
            />

            <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,_rgba(59,130,246,0.25),_transparent_34%),radial-gradient(circle_at_bottom_right,_rgba(20,184,166,0.18),_transparent_30%)]" />
            <div className="pointer-events-none absolute -left-24 top-24 h-72 w-72 rounded-full bg-blue-600/20 blur-3xl" />
            <div className="pointer-events-none absolute bottom-0 right-0 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl" />

            <div className="relative z-10 flex min-h-screen flex-col md:flex-row">
                <aside className="hidden md:flex md:w-[54%] md:items-center md:justify-center md:px-12 md:py-10 md:px-20">
                    <div className="w-full max-w-xl rounded-[2rem] border border-white/10 bg-black/[0.16] p-10 backdrop-blur-2xl">
                        <div className="mb-8 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.06] px-4 py-2 text-sm text-slate-200">
                            <i className="pi pi-lock text-blue-300" />
                            Acesso seguro
                        </div>

                        <div className="space-y-4">
                            <h2 className="text-4xl font-semibold leading-tight text-white xl:text-5xl">
                                Entre na plataforma Lumos.
                            </h2>
                            <p className="max-w-lg text-base leading-7 text-slate-300">
                                Gerencie suas execuções, vincule itens contratuais e consolide suas medições em um ambiente unificado.
                            </p>
                        </div>

                        <div className="mt-10 grid gap-3">
                            <div className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-4 text-slate-200">
                                <i className="pi pi-check-circle text-blue-300" />
                                Gestão de execuções e instalações
                            </div>
                            <div className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-4 text-slate-200">
                                <i className="pi pi-link text-blue-300" />
                                Vínculo de itens e regras contratuais
                            </div>
                            <div className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-4 text-slate-200">
                                <i className="pi pi-file-export text-blue-300" />
                                Fechamento de medições para faturamento
                            </div>
                        </div>
                    </div>
                </aside>

                <main className="flex min-h-screen items-center justify-center px-4 py-2 sm:px-6 md:min-h-0 md:w-[46%] md:px-10">
                    <div className="w-full max-w-lg">
                        <div className="mb-8 text-center">
                            <div className="mx-auto mb-4 flex h-[4.5rem] w-[4.5rem] items-center justify-center">
                                <img src="/icon-192.png" width="64" height="64" alt="Lumos" className="drop-shadow-lg" />
                            </div>
                            <div className="space-y-2">
                                <h1 className="text-3xl font-semibold tracking-tight text-white sm:text-4xl">
                                    Lumos<span className="text-blue-300">™</span>
                                </h1>
                                <p className="mx-auto max-w-md text-sm leading-6 text-slate-300">
                                    Faça login para acessar a plataforma.
                                </p>
                            </div>
                        </div>

                        <div className="mb-6 flex w-full rounded-2xl border border-white/10 bg-black/20 p-1 backdrop-blur-xl">
                            {(['login', 'signup', 'demo'] as const).map((tab) => (
                                <button
                                    key={tab}
                                    type="button"
                                    onClick={() => setActiveTab(tab)}
                                    className={[
                                        'flex flex-1 flex-col items-center justify-center gap-1 rounded-2xl px-3 py-3 text-xs font-semibold transition-all sm:text-sm',
                                        activeTab === tab
                                            ? 'bg-gradient-to-br from-blue-500 to-cyan-500 text-white shadow-[0_14px_28px_rgba(14,165,233,0.18)]'
                                            : 'text-slate-400 hover:-translate-y-[1px] hover:text-slate-100',
                                    ].join(' ')}
                                >
                                    <i className={`pi ${tab === 'login' ? 'pi-sign-in' : tab === 'signup' ? 'pi-user-plus' : 'pi-star'} text-lg`} />
                                    <span>{tab === 'login' ? 'Entrar' : tab === 'signup' ? 'Teste' : 'Demo'}</span>
                                </button>
                            ))}
                        </div>

                        <div className="rounded-[2rem] border border-white/10 bg-white/10 p-1 shadow-2xl shadow-slate-950/30 backdrop-blur-2xl">
                            <div className="p-6 sm:p-8">
                                {activeTab === 'login' && (
                                    <div className="space-y-6">
                                        <div>
                                            <h3 className="text-2xl font-semibold text-white">Acessar conta</h3>
                                            <p className="mt-2 text-sm leading-6 text-slate-300">
                                                Use seu usuário ou CPF e sua senha para entrar.
                                            </p>
                                        </div>

                                        <form onSubmit={handleLogin} className="flex flex-col gap-5">
                                            <div className="flex flex-col gap-2">
                                                <label className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-300">
                                                    Usuário ou CPF
                                                </label>
                                                <div className="relative">
                                                    <i className="pi pi-user pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                                                    <input
                                                        type="text"
                                                        value={username}
                                                        onChange={(e) => setUsername(e.target.value)}
                                                        onBlur={normalizeUsername}
                                                        placeholder="Digite seu usuário ou CPF"
                                                        autoComplete="username"
                                                        required
                                                        className="w-full rounded-2xl border border-slate-400/25 bg-slate-900/60 py-3.5 pl-11 pr-4 text-slate-50 outline-none transition-all placeholder:text-slate-500 hover:border-slate-400/35 hover:bg-slate-900/70 focus:border-blue-400 focus:bg-slate-900/85 focus:ring-4 focus:ring-blue-500/15"
                                                    />
                                                </div>
                                            </div>

                                            <div className="flex flex-col gap-2">
                                                <div className="flex items-center justify-between gap-3">
                                                    <label className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-300">
                                                        Senha
                                                    </label>
                                                    <button
                                                        type="button"
                                                        onClick={forgetPassword}
                                                        className="text-xs font-medium text-blue-200 transition-colors hover:text-blue-100"
                                                    >
                                                        Esqueci minha senha
                                                    </button>
                                                </div>
                                                <div className="relative">
                                                    <i className="pi pi-key pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                                                    <input
                                                        type={showPassword ? 'text' : 'password'}
                                                        value={password}
                                                        onChange={(e) => setPassword(e.target.value)}
                                                        placeholder="Digite sua senha"
                                                        autoComplete="current-password"
                                                        required
                                                        className="w-full rounded-2xl border border-slate-400/25 bg-slate-900/60 py-3.5 pl-11 pr-12 text-slate-50 outline-none transition-all placeholder:text-slate-500 hover:border-slate-400/35 hover:bg-slate-900/70 focus:border-blue-400 focus:bg-slate-900/85 focus:ring-4 focus:ring-blue-500/15"
                                                    />
                                                    <button
                                                        type="button"
                                                        onClick={() => setShowPassword(!showPassword)}
                                                        className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 transition-colors hover:text-slate-100"
                                                    >
                                                        <i className={`pi ${showPassword ? 'pi-eye-slash' : 'pi-eye'}`} />
                                                    </button>
                                                </div>
                                            </div>

                                            {finished && (
                                                <div className="rounded-2xl border-l-4 border-emerald-500 bg-emerald-500/12 p-3 text-sm text-emerald-300">
                                                    {message}
                                                </div>
                                            )}

                                            {authError && (
                                                <div className="rounded-2xl border-l-4 border-red-500 bg-red-500/12 p-3 text-sm text-red-300">
                                                    {authError}
                                                </div>
                                            )}

                                            <Link
                                                to="/primeiro-acesso"
                                                className="text-left text-sm font-medium text-green-400 transition-colors hover:text-blue-100"
                                            >
                                                Primeiro acesso? Ative sua conta aqui
                                            </Link>

                                            <button
                                                type="submit"
                                                disabled={isLoading || authLoading || !username || !password}
                                                className="flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-cyan-500 py-3.5 font-semibold text-white shadow-lg shadow-blue-500/20 transition-all hover:-translate-y-[1px] hover:brightness-105 disabled:cursor-not-allowed disabled:opacity-55"
                                            >
                                                {(isLoading || authLoading) && <i className="pi pi-spin pi-spinner" />}
                                                Entrar na plataforma
                                            </button>
                                        </form>
                                    </div>
                                )}

                                {activeTab === 'signup' && (
                                    <div className="space-y-6">
                                        <div className="space-y-3">
                                            <div className="inline-flex items-center gap-2 rounded-full border border-blue-500/20 bg-blue-500/10 px-3 py-1 text-xs font-medium text-blue-100">
                                                <i className="pi pi-star-fill text-[0.75rem]" />
                                                Experimente com apoio da equipe
                                            </div>
                                            <div>
                                                <h3 className="text-2xl font-semibold text-white">Comece um teste gratuito</h3>
                                                <p className="mt-2 text-sm leading-6 text-slate-300">
                                                    Valide o Lumos com seu cenário real e conheça os módulos mais relevantes para sua operação.
                                                </p>
                                            </div>
                                        </div>

                                        <div className="grid gap-3">
                                            <div className="rounded-2xl border border-blue-500/20 bg-blue-500/10 p-4">
                                                <div className="flex items-start gap-3">
                                                    <i className="pi pi-gift mt-1 text-lg text-blue-200" />
                                                    <div>
                                                        <p className="font-semibold text-white">14 dias para explorar a plataforma</p>
                                                        <p className="mt-1 text-sm leading-6 text-slate-300">
                                                            Teste os fluxos principais sem precisar de cartão de crédito.
                                                        </p>
                                                    </div>
                                                </div>
                                            </div>

                                            <div className="rounded-2xl border border-white/10 bg-black/10 p-4">
                                                <div className="flex items-start gap-3">
                                                    <i className="pi pi-send mt-1 text-lg text-emerald-200" />
                                                    <div>
                                                        <p className="font-semibold text-white">Ativação guiada</p>
                                                        <p className="mt-1 text-sm leading-6 text-slate-300">
                                                            Receba suporte para cadastrar estrutura, equipes e frentes de trabalho.
                                                        </p>
                                                    </div>
                                                </div>
                                            </div>

                                            <div className="rounded-2xl border border-white/10 bg-black/10 p-4">
                                                <div className="flex items-start gap-3">
                                                    <i className="pi pi-shield mt-1 text-lg text-cyan-100" />
                                                    <div>
                                                        <p className="font-semibold text-white">Ambiente seguro desde o primeiro acesso</p>
                                                        <p className="mt-1 text-sm leading-6 text-slate-300">
                                                            Controle de perfis e visibilidade dos dados durante a avaliação.
                                                        </p>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        <button
                                            type="button"
                                            onClick={() => window.location.href = 'https://lumosip.com.br/teste-gratis'}
                                            className="mt-4 flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-cyan-500 py-3.5 font-semibold text-white shadow-lg shadow-blue-500/20 transition-all hover:-translate-y-[1px] hover:brightness-105"
                                        >
                                            <i className="pi pi-arrow-right" />
                                            Começar teste gratuito
                                        </button>

                                        <p className="text-center text-xs text-slate-400">
                                            Já tem conta?
                                            <button
                                                type="button"
                                                onClick={() => setActiveTab('login')}
                                                className="ml-1 font-semibold text-blue-200 transition-colors hover:text-blue-100"
                                            >
                                                Entrar agora
                                            </button>
                                        </p>
                                    </div>
                                )}

                                {activeTab === 'demo' && (
                                    <div className="space-y-6">
                                        <div className="space-y-3 text-center">
                                            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full border border-blue-500/25 bg-blue-500/12 text-blue-100">
                                                <i className="pi pi-desktop text-2xl" />
                                            </div>
                                            <div>
                                                <h3 className="text-2xl font-semibold text-white">Veja o Lumos em ação</h3>
                                                <p className="mt-2 text-sm leading-6 text-slate-300">
                                                    Agende uma apresentação consultiva para entender como o produto se encaixa na sua operação.
                                                </p>
                                            </div>
                                        </div>

                                        <div className="grid gap-3">
                                            <div className="rounded-2xl border border-white/10 bg-black/10 px-4 py-4">
                                                <div className="flex items-center gap-3 text-sm text-slate-200">
                                                    <i className="pi pi-check text-blue-200" />
                                                    Apresentação focada nos fluxos mais críticos da sua equipe
                                                </div>
                                            </div>
                                            <div className="rounded-2xl border border-white/10 bg-black/10 px-4 py-4">
                                                <div className="flex items-center gap-3 text-sm text-slate-200">
                                                    <i className="pi pi-check text-blue-200" />
                                                    Discussão sobre implantação, governança e produtividade
                                                </div>
                                            </div>
                                            <div className="rounded-2xl border border-white/10 bg-black/10 px-4 py-4">
                                                <div className="flex items-center gap-3 text-sm text-slate-200">
                                                    <i className="pi pi-check text-blue-200" />
                                                    Próximos passos claros para piloto ou adoção completa
                                                </div>
                                            </div>
                                        </div>

                                        <button
                                            type="button"
                                            onClick={() => window.location.href = 'https://lumosip.com.br/demonstracao'}
                                            className="mt-4 flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-cyan-500 py-3.5 font-semibold text-white shadow-lg shadow-blue-500/20 transition-all hover:-translate-y-[1px] hover:brightness-105"
                                        >
                                            <i className="pi pi-calendar" />
                                            Solicitar demonstração
                                        </button>

                                        <p className="text-center text-xs text-slate-400">
                                            Prefere explorar por conta própria?
                                            <button
                                                type="button"
                                                onClick={() => setActiveTab('signup')}
                                                className="ml-1 font-semibold text-blue-200 transition-colors hover:text-blue-100"
                                            >
                                                Ativar teste
                                            </button>
                                        </p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </main>
            </div>
        </section>
    );
}
