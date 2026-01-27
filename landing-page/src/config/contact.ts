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
  email: 'marcostfandrade@gmail.com',
  supportEmail: 'marcostfandrade@gmail.com',
  salesEmail: 'marcostfandrade@gmail.com',
  address: {
    street: '',
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
    icon: 'simple-icons:instagram',
    label: 'Instagram',
    value: 'Instagram',
    href: 'https://discord.gg/virex',
  },
  {
    icon: 'lucide:twitter',
    label: 'Twitter',
    value: '@virex',
    href: 'https://twitter.com/virex',
  },
];

/** FAQ items displayed on the contact page */
export const contactFAQs: ContactFAQ[] = [
  {
    question: "Qual é o tempo de resposta típico?",
    answer: 'Respondemos à maioria das solicitações em até 24 horas em dias úteis.',
  },
  {
    question: 'Vocês oferecem suporte por telefone?',
    answer:
      'O suporte por telefone está disponível para clientes corporativos. Os demais podem entrar em contato conosco por e-mail ou WhatsApp.',
  },
  {
    question: 'Como faço para reportar um bug?',
    answer: 'Use o formulário com "Suporte técnico" como assunto ou abra uma issue no nosso GitHub.',
  },
];
