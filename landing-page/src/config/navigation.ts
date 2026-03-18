/**
 * Navigation Configuration
 *
 * @description
 * Centralized navigation configuration for header and footer.
 * All navigation items are defined here for consistency and easy maintenance.
 *
 * Items with a `feature` property will only be shown if that feature is enabled
 * in the site config's feature flags.
 */

import type { Navigation } from '../lib/types';

export const navigation: Navigation = {
  /**
   * Header Navigation
   * - main: Primary navigation links
   * - cta: Call-to-action buttons on the right
   */
  header: {
    main: [
      { label: 'Recursos', href: '/recursos' },
      { label: 'Preços', href: '/precos' },
      { label: 'Parceiros', href: '/parceiros' },
      { label: 'Como usar', href: '/como-usar', feature: 'docs' },
      { label: 'Blog', href: '/blog', feature: 'blog' },
      { label: 'Sobre', href: '/sobre' },
    ],
    cta: [
      { label: 'Entrar', href: 'https://lumos.thryon.com.br', variant: 'ghost' },
      { label: 'Teste Grátis', href: '/contato', variant: 'primary' },
    ],
  },

  /**
   * Footer Navigation
   * Organized into 5 columns: Product, Solutions, Resources, Company, Legal
   */
  footer: {
    product: [
      { label: 'Recursos', href: '/features' },
      { label: 'Segurança', href: '/seguranca' },
      { label: 'Preços', href: '/precos' },
      { label: 'FAQ', href: '/faq' },
    ],
    solutions: [
      { label: 'Enterprise', href: '/enterprise' },
      { label: 'Parceiros', href: '/parceiros' },
      { label: 'Solicite uma demonstração', href: '/demonstração' },
      { label: 'Status', href: '/status' },
    ],
    resources: [
      { label: 'Documentation', href: '/docs', feature: 'docs' },
      { label: 'Blog', href: '/blog', feature: 'blog' },
      { label: 'Changelog', href: '/changelog', feature: 'changelog' },
      { label: 'Roadmap', href: '/roadmap', feature: 'roadmap' },
    ],
    company: [
      { label: 'About', href: '/about' },
      { label: 'Careers', href: '/careers' },
      { label: 'Contact', href: '/contact' },
      { label: 'Testimonials', href: '/testimonials', feature: 'testimonials' },
    ],
    legal: [
      { label: 'Privacy', href: '/privacy' },
      { label: 'Terms', href: '/terms' },
    ],
  },
};
