import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { AppProviders } from '@/app/providers/AppProviders';
import { router } from '@/routes';
import { useAuthStore } from '@/core/auth/useAuthStore';
import '@/index.css';
import 'primeicons/primeicons.css';
void useAuthStore.getState().initialize();

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <AppProviders>
            <RouterProvider router={router} />
        </AppProviders>
    </React.StrictMode>
);
