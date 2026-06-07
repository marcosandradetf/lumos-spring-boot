import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { useNotificationStore, type AppNotification } from '../../core/notifications/useNotificationStore';
import { useAppStore } from '../../store/use-app-store';
import { useAuthStore } from '../../core/auth/useAuthStore';
import { useNotify } from '../hooks/use-notify';
import { getStatusMeta } from '../utils/utils';

const NOTIFICATION_GUIDE_URL = 'https://lumosip.com.br/como-usar/15-web-config/01-enable-notifications/';

function getDateLabel(dateLike: string | number): string {
  const date = new Date(dateLike);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);

  if (date.toDateString() === today.toDateString()) return 'Hoje';
  if (date.toDateString() === yesterday.toDateString()) return 'Ontem';

  return date.toLocaleDateString('pt-BR', { day: 'numeric', month: 'long' });
}

function getIconBg(type: string) {
  switch (type) {
    case 'ERROR':
      return 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400';
    case 'ALERT':
    case 'ALERT_BANNER':
      return 'bg-orange-100 text-orange-600 dark:bg-orange-900/30 dark:text-orange-400';
    case 'SUCCESS':
      return 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400';
    default:
      return 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400';
  }
}

function getIcon(type: string) {
  switch (type) {
    case 'ERROR':
      return 'pi pi-times';
    case 'ALERT':
    case 'ALERT_BANNER':
      return 'pi pi-exclamation-triangle';
    case 'SUCCESS':
      return 'pi pi-check';
    default:
      return 'pi pi-info-circle';
  }
}

export function NotificationDrawer() {
  const navigate = useNavigate();
  const { notify } = useNotify();
  const { user } = useAuthStore();
  const { showNotificationDrawer, setShowNotificationDrawer } = useAppStore();
  const {
    history,
    status,
    markAsRead,
    clearAll,
    requestPermission,
    syncStatus,
  } = useNotificationStore();
  const [guideOpen, setGuideOpen] = useState(false);

  useEffect(() => {
    if (!showNotificationDrawer) return;
    syncStatus();
  }, [showNotificationDrawer, syncStatus]);

  const groups = useMemo(() => {
    const grouped = new Map<string, AppNotification[]>();

    history.forEach((notification) => {
      const label = getDateLabel(notification.timeIso || notification.time);
      if (!grouped.has(label)) {
        grouped.set(label, []);
      }
      grouped.get(label)?.push(notification);
    });

    return Array.from(grouped.entries());
  }, [history]);

  const openNotification = (notification: AppNotification) => {
    if (notification.read === 0) {
      markAsRead(notification.id);
    }

    if (notification.uri) {
      setShowNotificationDrawer(false);
      navigate(notification.uri);
    }
  };

  const closeGuide = () => setGuideOpen(false);

  const onStatusAction = async () => {
    if (status === 'denied') {
      setShowNotificationDrawer(false);
      setGuideOpen(true);
      return;
    }

    const permission = await requestPermission(user?.roles ?? []);
    if (permission === 'granted') {
      notify('Notificações ativadas com sucesso.', 'success');
    } else {
      notify('Permissão de notificações não concedida.', 'warn');
    }
  };

  useEffect(() => {
    syncStatus();

    const handleFocus = () => syncStatus();

    window.addEventListener('focus', handleFocus);
    document.addEventListener('visibilitychange', handleFocus);

    return () => {
      window.removeEventListener('focus', handleFocus);
      document.removeEventListener('visibilitychange', handleFocus);
    };
  }, [syncStatus]);

  const statusMeta = useMemo(() => {
    if (status !== 'default' && status !== 'denied') return null;
    return getStatusMeta(status);
  }, [status]);

  return createPortal(
    <>
      <div
        className={[
          'fixed inset-0 z-[300] bg-black/40 transition-opacity duration-300',
          showNotificationDrawer ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none',
        ].join(' ')}
        onClick={() => setShowNotificationDrawer(false)}
      />

      <aside
        className={[
          'fixed right-0 top-0 z-[310] h-screen w-full max-w-[24rem] transform border-l border-white/10 bg-white dark:bg-zinc-950 shadow-2xl transition-transform duration-300',
          showNotificationDrawer ? 'translate-x-0' : 'translate-x-full',
        ].join(' ')}
        aria-hidden={!showNotificationDrawer}
      >
        <div className="flex h-full flex-col">
          <header className="flex items-center justify-between border-b border-slate-200/70 px-4 py-3 dark:border-zinc-800">
            <div className="flex items-center gap-2.5">
              <i className="pi pi-bell text-base text-slate-500 dark:text-zinc-400" />
              <h2 className="text-sm font-semibold text-slate-800 dark:text-zinc-100">Notificações</h2>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => void clearAll()}
                className="rounded-lg px-2.5 py-1 text-xs font-medium text-slate-500 hover:bg-slate-100 hover:text-slate-700 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
              >
                Limpar
              </button>
              <button
                type="button"
                onClick={() => setShowNotificationDrawer(false)}
                className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
              >
                <i className="pi pi-times text-xs" />
              </button>
            </div>
          </header>

          <div className="flex-1 overflow-y-auto">
            {status === 'granted' ? (
              <>
                {groups.map(([groupLabel, items]) => (
                  <section key={groupLabel}>
                    <div className="sticky top-0 z-10 border-y border-slate-200 bg-slate-50/90 px-4 py-2 backdrop-blur-sm dark:border-zinc-800 dark:bg-zinc-900/90">
                      <span className="text-[10px] font-bold uppercase tracking-[0.14em] text-slate-500 dark:text-zinc-400">
                        {groupLabel}
                      </span>
                    </div>

                    {items.map((notification) => (
                      <button
                        key={notification.id}
                        type="button"
                        className={[
                          'relative flex w-full items-start gap-3 border-b border-slate-100 px-4 py-3 text-left transition-colors dark:border-zinc-900',
                          notification.read === 0
                            ? 'bg-blue-50/50 hover:bg-blue-50 dark:bg-blue-950/20 dark:hover:bg-blue-950/30'
                            : 'hover:bg-slate-50 dark:hover:bg-zinc-900',
                        ].join(' ')}
                        onClick={() => openNotification(notification)}
                      >
                        {notification.read === 0 && (
                          <span className="absolute left-0 top-0 h-full w-1 rounded-r bg-blue-500 shadow-[0_0_10px_rgba(59,130,246,0.6)]" />
                        )}

                        <span className={`mt-0.5 flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full ${getIconBg(notification.type)}`}>
                          <i className={`${getIcon(notification.type)} text-sm`} />
                        </span>

                        <span className="min-w-0 flex-1">
                          <span className="flex items-start justify-between gap-2">
                            <span className="truncate text-sm font-semibold text-slate-800 dark:text-zinc-100">
                              {notification.title}
                            </span>
                            <span className="mt-0.5 whitespace-nowrap text-[10px] font-medium text-slate-400 dark:text-zinc-500">
                              {new Date(notification.timeIso || notification.time).toLocaleTimeString('pt-BR', {
                                hour: '2-digit',
                                minute: '2-digit',
                              })}
                            </span>
                          </span>
                          <span className="mt-1 line-clamp-2 text-xs leading-relaxed text-slate-500 dark:text-zinc-400">
                            {notification.body}
                          </span>
                        </span>
                      </button>
                    ))}
                  </section>
                ))}

                {groups.length === 0 && (
                  <div className="flex flex-col items-center justify-center px-6 py-24 text-center">
                    <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 dark:bg-zinc-800">
                      <i className="pi pi-bell-slash text-xl text-slate-400 dark:text-zinc-500" />
                    </div>
                    <p className="text-sm font-medium text-slate-700 dark:text-zinc-200">Tudo limpo por aqui!</p>
                    <p className="mt-1 max-w-[230px] text-xs text-slate-500 dark:text-zinc-400">
                      Você não tem notificações pendentes no momento.
                    </p>
                  </div>
                )}
              </>
            ) : (
              statusMeta && (
                <div className="m-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
                  <div className="flex items-center justify-center">
                    <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 dark:bg-zinc-800">
                      <i className={`${statusMeta.icon} text-base`} />
                    </div>
                  </div>
                  <h3 className="mt-3 text-center text-sm font-semibold text-slate-800 dark:text-zinc-100">
                    {statusMeta.title}
                  </h3>
                  <p className="mt-2 text-center text-xs leading-relaxed text-slate-500 dark:text-zinc-400">
                    {statusMeta.detail}
                  </p>
                  <div className="mt-4 flex items-center justify-center gap-2">
                    <button
                      type="button"
                      onClick={() => setShowNotificationDrawer(false)}
                      className="rounded-xl border border-slate-200 px-3 py-2 text-xs font-medium text-slate-600 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-300 dark:hover:bg-zinc-800"
                    >
                      Ignorar
                    </button>
                    <button
                      type="button"
                      onClick={() => void onStatusAction()}
                      className="rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-3 py-2 text-xs font-semibold text-white shadow-sm hover:brightness-110"
                    >
                      {statusMeta.action}
                    </button>
                  </div>
                </div>
              )
            )}
          </div>
        </div>
      </aside>

      {guideOpen && (
        <>
          <div className="fixed inset-0 z-[320] bg-slate-950/35 backdrop-blur-[2px]" onClick={closeGuide} />
          <section className="fixed inset-y-0 right-0 z-[325] flex h-full w-full max-w-[96vw] flex-col bg-white shadow-2xl dark:bg-slate-900 lg:w-[min(1200px,92vw)]">
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
                onClick={closeGuide}
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
    </>,
    document.body,
  );
}
