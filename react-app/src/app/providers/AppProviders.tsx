import type { PropsWithChildren } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import { queryClient } from '@/app/query/queryClient';
import { TooltipProvider } from '@/shared/components/ui/tooltip';

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider delayDuration={200}>

        {/* O Toaster fica aqui dentro, mas ele não renderiza HTML na árvore, 
            ele apenas abre um portal global para os alertas */}
        <Toaster
          position="top-right"
          richColors
          theme="system" // UX Premium: O Sonner gerencia dark/light automaticamente baseado no sistema ou na classe 'dark' do <html>
          closeButton
        />
        
        
        {/* Toda a aplicação entra aqui, com acesso ao React Query e Tooltip */}
        {children}
      </TooltipProvider>
    </QueryClientProvider>
  );
}