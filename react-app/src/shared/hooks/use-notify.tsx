import { useCallback } from 'react';
import { toast, type ExternalToast } from 'sonner';

type NotifyType = 'success' | 'info' | 'warn' | 'error';
type SoundType = 'pop' | 'select' | 'open' | 'bip' | 'error';

/**
 * Configuração padrão para todos os toasts para manter consistência de UI.
 */
const toastConfig: ExternalToast = {
  duration: 4000,
  closeButton: true,
  style: {
    padding: '12px 16px',
    borderRadius: '12px',
  },
};

export const useNotify = () => {
  /**
   * PlaySound otimizado: Garante que o áudio recomece se for chamado rapidamente
   * e reduz levemente o volume para não ser agressivo (UX Premium).
   */
  const playSound = useCallback((type: SoundType) => {
    const file = type === 'open' ? 'sci' : type;
    const audio = new Audio(`/${file}.mp3`);
    audio.volume = 0.4; // Volume equilibrado
    audio.currentTime = 0;
    void audio.play().catch(() => {});
  }, []);

  const success = useCallback((message: string, sound: boolean = false) => {
    if (sound) playSound('pop');
    toast.success(message, {
      ...toastConfig,
      icon: <i className="pi pi-check-circle" style={{ color: '#22c55e', fontSize: '1.2rem' }} />,
    });
  }, [playSound]);

  const error = useCallback((message: string, sound: boolean = false) => {
    if (sound) playSound('error');
    toast.error(message, {
      ...toastConfig,
      icon: <i className="pi pi-times-circle" style={{ color: '#ef4444', fontSize: '1.2rem' }} />,
    });
  }, [playSound]);

  const warn = useCallback((message: string, sound: boolean = false) => {false
    if (sound) playSound('bip');
    toast.warning(message, {
      ...toastConfig,
      icon: <i className="pi pi-exclamation-triangle" style={{ color: '#f59e0b', fontSize: '1.2rem' }} />,
    });
  }, [playSound]);

  const info = useCallback((message: string, sound: boolean = false) => {
    if (sound) playSound('pop');
    toast.info(message, {
      ...toastConfig,
      icon: <i className="pi pi-info-circle" style={{ color: '#3b82f6', fontSize: '1.2rem' }} />,
    });
  }, [playSound]);

  /**
   * Função genérica que unifica as chamadas.
   */
  const notify = useCallback((
      message: string,
      type: NotifyType = 'info',
      shouldPlaySound: boolean = false
  ) => {
    const handlers = { success, error, warn, info };
    const handler = handlers[type] || info;
    handler(message, shouldPlaySound);
  }, [error, info, success, warn]);

  return { notify, success, error, warn, info, playSound };
};