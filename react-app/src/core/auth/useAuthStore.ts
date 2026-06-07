// src/core/auth/useAuthStore.ts
import { create } from 'zustand';
import { jwtDecode } from 'jwt-decode';
import api, { type AuthUser } from './api';
import { unsubscribeFromTopic } from '@/core/notifications/fcmWeb';

interface AuthState {
    user: AuthUser | null;
    isLoggedIn: boolean;
    isLoading: boolean;
    refreshToken: () => Promise<string | null>;
    login: (u: string, p: string) => Promise<void>;
    logout: () => Promise<void>;
    initialize: () => Promise<void>;
    applyAuthResponse: (token: string) => void;
    clearStoredSession: () => void;
}

interface DecodedToken {
    sub: string;
    username: string;
    scope: string;
    fullname: string;
    tenant: string;
    email: string;
    support: boolean;
}

const SESSION_USER_KEY = 'user';

const getStoredUser = (): AuthUser | null => {
    const raw = localStorage.getItem(SESSION_USER_KEY);
    if (!raw) return null;

    try {
        return JSON.parse(raw) as AuthUser;
    } catch {
        return null;
    }
};

const isJwtExpired = (token: string) => {
    try {
        const decoded = jwtDecode<{ exp?: number }>(token);
        if (!decoded.exp) return true;
        return Date.now() >= decoded.exp * 1000;
    } catch {
        return true;
    }
};

const _initialUser = getStoredUser();

export const useAuthStore = create<AuthState>((set, get) => ({
    user: _initialUser,
    isLoggedIn: !!_initialUser && !isJwtExpired(_initialUser.accessToken),
    isLoading: false,

    clearStoredSession: () => {
        localStorage.removeItem(SESSION_USER_KEY);
        localStorage.removeItem('tenant');
        localStorage.removeItem('fcmToken');
        set({ user: null, isLoggedIn: false, isLoading: false });
    },

    initialize: async () => {
        const storedUser = getStoredUser();
        if (!storedUser) {
            set({ user: null, isLoggedIn: false, isLoading: false });
            return;
        }

        if (!isJwtExpired(storedUser.accessToken)) {
            set({ user: storedUser, isLoggedIn: true, isLoading: false });
            return;
        }

        set({ isLoading: true });
        try {
            const refreshed = await get().refreshToken();
            if (!refreshed) get().clearStoredSession();
        } catch {
            get().clearStoredSession();
        } finally {
            set({ isLoading: false });
        }
    },

    applyAuthResponse: (accessToken: string) => {
        const decoded = jwtDecode<DecodedToken>(accessToken);
        const userData: AuthUser = {
            uuid: decoded.sub,
            username: decoded.username,
            accessToken,
            roles: decoded.scope.split(' '),
            fullName: decoded.fullname,
            tenant: decoded.tenant,
            email: decoded.email,
            support: decoded.support
        };

        localStorage.setItem(SESSION_USER_KEY, JSON.stringify(userData));
        localStorage.setItem('tenant', decoded.tenant);
        set({ user: userData, isLoggedIn: true });
    },

    refreshToken: async () => {
        const currentUser = get().user;
        if (!currentUser) return null;

        try {
            const response = await api.post<{ accessToken: string }>('/api/auth/refresh-token', {});
            const token = response.data.accessToken;
            get().applyAuthResponse(token);
            return token;
        } catch {
            return null;
        }
    },

    login: async (username, password) => {
        set({ isLoading: true });
        try {
            const response = await api.post('/api/auth/login', { username, password });
            get().applyAuthResponse(response.data.accessToken);
        } finally {
            set({ isLoading: false });
        }
    },

    logout: async () => {
        set({ isLoading: true });
        const roles = get().user?.roles ?? [];
        try {
            await api.post('/api/auth/logout', {});
        } catch (e) {
            console.error('Erro no logout', e);
        } finally {
            if (roles.length > 0) {
                try {
                    await unsubscribeFromTopic(roles);
                } catch (error) {
                    console.warn('Falha ao remover inscrição de tópicos FCM no logout.', error);
                }
            }
            get().clearStoredSession();
            window.location.href = '/auth/login';
        }
    }
}));
