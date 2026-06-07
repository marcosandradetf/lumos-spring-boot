export const NOTIFICATION_GUIDE_URL = 'https://lumosip.com.br/como-usar/15-web-config/01-enable-notifications/';

export function focusNextInputInTableRow(currentInput: HTMLInputElement) {
  const rowElement = currentInput.closest('tr');

  const inputs = Array.from(
    rowElement?.querySelectorAll<HTMLInputElement>('input:not(:disabled)') ?? []
  );

  const currentIndex = inputs.indexOf(currentInput);
  inputs[currentIndex + 1]?.focus();
}

export function getStatusMeta(status: 'default' | 'denied') {
  if (status === 'denied') {
    return {
      title: 'Notificações bloqueadas',
      detail: 'Você bloqueou notificações do navegador. Para não perder atualizações importantes, permita as notificações do Lumos no dispositivo.',
      icon: 'pi pi-bell-slash text-amber-500',
      action: 'Como ativar',
    };
  }

  return {
    title: 'Notificações desativadas',
    detail: 'Para não perder nenhuma atualização importante, ative as notificações do Lumos neste dispositivo.',
    icon: 'pi pi-bell text-blue-500',
    action: 'Ativar agora',
  };
}