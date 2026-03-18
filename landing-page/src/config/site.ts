/**
 * Site Configuration
 *
 * @description
 * Core site metadata and branding settings.
 * These values can be customized via environment variables or by editing the defaults below.
 */

import type { SocialLinks, LegalConfig } from '../lib/types';

/** Site name displayed in header, footer, and meta tags */
export const name = 'Lumos – Gestão de Iluminação Pública';

/** Site description for SEO and meta tags */
export const description =
  'O Lumos é um software completo de gestão de iluminação pública para empresas que operam contratos com prefeituras. Automatize operações de campo, controle contratos, reduza glosas no faturamento e tenha total rastreabilidade das execuções em tempo real.';

/** Production URL of your site (used for sitemap, RSS, canonical URLs) */
export const url = 'https://lumosip.com.br';

/** Author name for meta tags and copyright */
export const author = import.meta.env.SITE_AUTHOR || 'Lumos IP';

/** Path to logo file (relative to /public) */
export const logo = '/lumos_logo.svg';

/** Path to Open Graph image (relative to /public) */
export const ogImage = '/images/og-image.png';

/** Social media links */
export const social: SocialLinks = {
  linkedin: 'https://linkedin.com/company/lumos_ip',
  whatsapp: 'https://wa.me/5531986975086',
  twitter: 'https://x.com/lumos_ip',
};

/** Legal configuration for privacy policy and terms pages */
export const legal: LegalConfig = {
  /** E-mail para questões de privacidade e dados (DPO) */
  privacyEmail: 'privacidade@lumosip.com.br',

  /** E-mail para questões jurídicas e contratuais */
  legalEmail: 'juridico@lumosip.com.br',

  /** Data de atualização - Mantendo o sistema com aspecto de manutenção ativa */
  lastUpdated: '17 de Março de 2026',
};