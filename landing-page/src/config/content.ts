/**
 * Content Strings Configuration
 *
 * @description
 * Configurable text content for various site sections.
 * Modify these to customize messaging without touching component code.
 */

import type { AnnouncementConfig, ContentStrings } from '../lib/types';
/** Announcement bar configuration */
export const announcement: AnnouncementConfig = {
  /** Show/hide the announcement bar */
  enabled: true,

  /** ID único para o lançamento do Lumos */
  id: 'lumos-launch-2026',

  /** Texto do anúncio focado na plataforma */
  text: '💡 Conheça o Lumos IP: A nova era na gestão de iluminação pública.',

  /** Link para uma demo ou página de funcionalidades */
  href: '/recursos',

  /** Texto do link */
  linkText: 'Saiba mais',

  /** Visual style: 'gradient' para destaque tecnológico */
  variant: 'gradient',

  /** Permitir que o usuário feche */
  dismissible: true,
};

/** Configurable content strings for various sections */
export const content: ContentStrings = {
  newsletter: {
    title: 'Mantenha sua gestão atualizada',
    description:
      'Receba atualizações sobre novas funcionalidades, relatórios e conformidade contratual.',
    placeholder: 'Seu e-mail corporativo',
    buttonText: 'Cadastrar',
    successMessage: 'Obrigado! Em breve você receberá novidades sobre o Lumos.',
    errorMessage: 'Ocorreu um erro. Por favor, tente novamente.',
    privacyNote: 'Respeitamos seus dados. Cancele a inscrição quando desejar.',
  },
};