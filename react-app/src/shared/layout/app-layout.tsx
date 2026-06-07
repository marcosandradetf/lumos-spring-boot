import { useEffect, useMemo, useRef, useState } from 'react';
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { useAuthStore } from '../../core/auth/useAuthStore';
import { useAppStore } from '../../store/use-app-store';
import { useNotificationStore } from '../../core/notifications/useNotificationStore';
import { checkOnboardingState } from '../../core/onboarding/checkOnboardingState';
import { useNotify } from '../hooks/use-notify';
import { Sidebar } from './sidebar';
import { Header } from './header';
import { NotificationDrawer } from './notification-drawer';
import { AccountDrawer } from './account-drawer';
import { NOTIFICATION_GUIDE_URL } from '../utils/utils';



export function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { notify } = useNotify();
  const { isLoggedIn, isLoading, user } = useAuthStore();
  const { showMobileDrawer, setShowMobileDrawer, isMenuOpen, setOnboarding, guideUrl, setGuideUrl } = useAppStore();
  const { initialize, syncStatus, requestPermission } = useNotificationStore();
  const [sidebarWidth, setSidebarWidth] = useState(() => {
    const saved = Number(localStorage.getItem('sidebarWidth') ?? '350');
    if (Number.isNaN(saved)) return 350;
    return Math.max(250, Math.min(350, saved));
  });
  const startupUserRef = useRef<string | null>(null);

  const hasOnboardingPermission = useMemo(() => {
    const roles = user?.roles ?? [];
    return roles.includes('ADMIN') || roles.includes('ANALISTA') || roles.includes('RESPONSAVEL_TECNICO');
  }, [user?.roles]);

  useEffect(() => {
    initialize();
    syncStatus();
  }, [initialize, syncStatus]);

  useEffect(() => {
    if (!isLoggedIn || !user?.uuid) return;
    if (startupUserRef.current === user.uuid) return;
    startupUserRef.current = user.uuid;

    const boot = async () => {
      if (!localStorage.getItem('onboarding') && hasOnboardingPermission) {
        try {
          const isPending = await checkOnboardingState();
          if (isPending) {
            localStorage.removeItem('onboarding');
            setOnboarding(true);
            navigate('/configuracoes/onboarding');
          } else {
            localStorage.setItem('onboarding', 'finished');
            setOnboarding(false);
          }
        } catch {
          notify('Não foi possível validar o onboarding automaticamente.', 'warn');
        }
      }

      const userAgent = window.navigator.userAgent.toLowerCase();
      const isIosWebView = !('Notification' in window);
      const isAndroidWebView = userAgent.includes('wv');
      const canPrompt = !isIosWebView && !isAndroidWebView;

      if (!canPrompt) return;

      useNotificationStore.getState().syncStatus();
      const currentStatus = useNotificationStore.getState().status;

      if (currentStatus === 'granted' && !localStorage.getItem('fcmToken')) {
        const permission = await requestPermission(user.roles ?? []);
        if (permission === 'granted') {
          notify('Notificações ativadas com sucesso.', 'success');
        }
        return;
      }

      const dismissedAt = Number(localStorage.getItem('notificationsPromptDismissedAt') ?? '0');
      const hasDismissedRecently = Date.now() - dismissedAt < 1000 * 60 * 60 * 48; // 48h

      if (hasDismissedRecently) return;
      if (currentStatus !== 'default' && currentStatus !== 'denied') return;

      toast.custom(
        (id) => (
          <div className="group relative w-96 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl dark:border-zinc-800 dark:bg-zinc-950">
            <div className={`absolute left-0 top-0 bottom-0 w-1 ${currentStatus === 'default' ? 'bg-blue-500' : 'bg-amber-500'}`} />
            <div className="pl-4 pr-5 py-4">
              <div className="flex items-start gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-600 dark:bg-zinc-800 dark:text-zinc-300">
                  <i className={currentStatus === 'default' ? 'pi pi-bell text-sm text-blue-500' : 'pi pi-bell-slash text-sm text-amber-500'} />
                </div>
                <div className="min-w-0 flex-1">
                  <h4 className="text-sm font-semibold leading-snug text-slate-800 dark:text-zinc-100">
                    {currentStatus === 'default' ? 'Notificações desativadas' : 'Notificações bloqueadas'}
                  </h4>
                  <p className="mt-1 text-xs leading-relaxed text-slate-500 dark:text-zinc-400">
                    {currentStatus === 'default'
                      ? 'Para não perder atualizações importantes, permita as notificações do Lumos no seu dispositivo.'
                      : 'Você bloqueou notificações do navegador. Permita as notificações do Lumos para manter os alertas operacionais.'}
                  </p>
                  <div className="flex justify-end gap-2 pt-2">
                    <button
                      type="button"
                      className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs text-slate-600 dark:border-zinc-700 dark:text-zinc-300"
                      onClick={() => {
                        toast.dismiss(id);
                        localStorage.setItem('notificationsPromptDismissedAt', '' + Date.now());
                      }}
                    >
                      Ignorar
                    </button>
                    <button
                      type="button"
                      className="rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-3 py-1.5 text-xs font-semibold text-white"
                      onClick={async () => {
                        toast.dismiss(id);
                        if (currentStatus === 'default') {
                          const permission = await requestPermission(user.roles ?? []);
                          if (permission === 'granted') {
                            notify('Notificações ativadas com sucesso.', 'success');
                          } else {
                            notify('Permissão de notificações não concedida.', 'warn');
                          }
                          return;
                        }
                        setGuideUrl(NOTIFICATION_GUIDE_URL);

                      }}
                    >
                      {currentStatus === 'default' ? 'Ativar agora' : 'Como ativar'}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ),
        {
          id: `notifications-startup-${user.uuid}`,
          duration: currentStatus === 'default' ? 50000 : 25000,
          position: 'top-right',
        },
      );
    };

    void boot();
  }, [isLoggedIn, user?.uuid, hasOnboardingPermission, setOnboarding, navigate, requestPermission, notify]);

  useEffect(() => {
    const onVisibility = () => {
      if (document.visibilityState === 'visible') {
        syncStatus();
      }
    };

    window.addEventListener('visibilitychange', onVisibility);
    return () => window.removeEventListener('visibilitychange', onVisibility);
  }, [syncStatus]);

  const startResizing = (event: React.MouseEvent<HTMLDivElement>) => {
    const startX = event.clientX;
    const startWidth = sidebarWidth;
    let currentWidth = startWidth;

    const mouseMove = (moveEvent: MouseEvent) => {
      const nextWidth = startWidth + (moveEvent.clientX - startX);
      const clamped = Math.max(250, Math.min(350, nextWidth));
      currentWidth = clamped;
      setSidebarWidth(clamped);
    };

    const mouseUp = () => {
      window.removeEventListener('mousemove', mouseMove);
      window.removeEventListener('mouseup', mouseUp);
      localStorage.setItem('sidebarWidth', String(currentWidth));
      document.body.style.cursor = 'default';
    };

    document.body.style.cursor = 'col-resize';
    window.addEventListener('mousemove', mouseMove);
    window.addEventListener('mouseup', mouseUp);
  };

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-50 dark:bg-zinc-950">
        <i className="pi pi-spin pi-spinner text-2xl text-indigo-500" />
      </div>
    );
  }

  if (!isLoggedIn) {
    return <Navigate to="/auth/login" state={{ from: location }} replace />;
  }

  return (
    <div className="flex h-screen bg-slate-50 dark:bg-zinc-950 overflow-hidden">
      {/* Sidebar desktop */}
      <Sidebar desktopWidth={sidebarWidth} />
      {isMenuOpen && (
        <div
          className="hidden xl:block w-1 cursor-col-resize hover:bg-blue-400/60"
          onMouseDown={startResizing}
        />
      )}

      {/* Mobile drawer overlay */}
      {showMobileDrawer && (
        <div
          className="fixed inset-0 z-40 bg-black/40 xl:hidden"
          onClick={() => setShowMobileDrawer(false)}
        />
      )}

      {/* Sidebar mobile */}
      <div
        className={`fixed inset-y-0 left-0 z-50 xl:hidden transition-transform duration-300 ${showMobileDrawer ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <Sidebar mobile />
      </div>

      {/* Main content */}
      <div className="flex flex-1 flex-col min-w-0 overflow-hidden">
        <Header />
        <main className="flex-1 overflow-auto">
          <Outlet />
        </main>
      </div>

      <NotificationDrawer />
      <AccountDrawer />

      {guideUrl && (
        <>
          <div
            className="fixed inset-0 z-[90] bg-slate-950/35 backdrop-blur-[2px]"
            onClick={() => setGuideUrl(undefined)}
          />
          <section className="fixed inset-y-0 right-0 z-[95] flex h-full w-full max-w-[96vw] flex-col bg-white shadow-2xl dark:bg-slate-900 lg:w-[min(1200px,92vw)]">
            <div className="flex flex-col gap-3 border-b border-slate-200 px-4 py-4 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between sm:px-5">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">
                  Documentação incorporada
                </p>
                <h3 className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                  Ativando as Notificações
                </h3>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  Guia para permitir notificações no navegador e receber alertas operacionais do Lumos.
                </p>
              </div>

              <button
                type="button"
                onClick={() => setGuideUrl(undefined)}
                className="w-full rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-100 dark:hover:bg-slate-800 sm:w-auto"
              >
                Fechar
              </button>
            </div>
            <div className="flex-1 p-4 sm:p-5">
              <iframe
                src={NOTIFICATION_GUIDE_URL}
                className="min-h-[85vh] w-full rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950"
                referrerPolicy="strict-origin-when-cross-origin"
                title="Documentação incorporada do Lumos"
              />
            </div>
          </section>
        </>
      )}
    </div>
  );
}
