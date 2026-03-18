import { defineConfig } from 'astro/config';
import mdx from '@astrojs/mdx';
import sitemap from '@astrojs/sitemap';
import icon from 'astro-icon';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  site: 'https://lumosip.com.br',

  integrations: [
    mdx(),
    icon(),
    sitemap({
      filter: (page) => {
        const url = new URL(page);
        const pathname = url.pathname;

        const allowedRoutes = [
          '/',
          '/sobre',
          '/software-iluminacao-publica',
          '/gestao-contratos-iluminacao-publica',
          '/recursos',
          '/precos',
          '/contato',
          '/parceiros',
          '/como-usar',
          '/faq',
          '/privacidade',
          '/termos',
          '/demonstracao',
        ];

        return allowedRoutes.some((route) => {
          if (route === '/como-usar') {
            return pathname.startsWith('/como-usar');
          }

          return pathname === route || pathname.startsWith(route + '/');
        });
      },
    }),
  ],

  vite: {
    plugins: [tailwindcss()],
  },
});
