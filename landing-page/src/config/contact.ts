/**
 * Contact Page Configuration
 *
 * @description
 * Contact information, methods, and FAQ data for the contact page.
 * Modify these values to customize your contact page content.
 */

import type { ContactInfo, ContactMethod, ContactFAQ } from '../lib/types';

/** Contact information used across contact page and legal pages */
export const contact: ContactInfo = {
  /** E-mail geral para parcerias e diretoria */
  email: 'contato@lumosip.com.br',

  /** Canal exclusivo para suporte técnico e abertura de tickets */
  supportEmail: 'suporte@lumosip.com.br',

  /** Canal dedicado a novas implantações e contratos */
  salesEmail: 'comercial@lumosip.com.br',

  address: {
    street: '', // Deixamos vazio conforme sua preferência de privacidade
    city: 'Belo Horizonte',
    state: 'MG',
    zip: '',
    country: 'Brasil',
  },
};

/** Contact methods displayed on the contact page */
export const contactMethods: ContactMethod[] = [
  {
    icon: 'lucide:mail',
    label: 'Email',
    value: contact.email,
    href: `mailto:${contact.email}`,
  },
  {
    icon: 'lucide:mail',
    label: 'Email',
    value: contact.salesEmail ?? '',
    href: `mailto:${contact.email}`,
  },
];

/** FAQ items displayed on the contact page */
export const contactFAQs: ContactFAQ[] = [
  {
    question: 'Qual é o tempo de resposta típico?',
    answer: 'Respondemos à maioria das solicitações em até 24 horas em dias úteis.',
  },
  {
    question: 'Vocês oferecem suporte por telefone?',
    answer:
      'O suporte por telefone está disponível para clientes Enterprise. Os demais podem entrar em contato conosco por e-mail ou WhatsApp.',
  },
  {
    question: 'Como faço para reportar um bug?',
    answer: 'Use o formulário com "Suporte técnico" como assunto ou envie um email.',
  },
];
