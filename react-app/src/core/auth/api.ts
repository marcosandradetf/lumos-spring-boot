import axios from 'axios';
import type { AxiosError, InternalAxiosRequestConfig } from 'axios';

export interface AuthUser {
    uuid: string;
    username: string;
    accessToken: string;
    roles: string[];
    fullName: string;
    tenant: string;
    email: string;
    support: boolean;
}

type RetryableRequest = InternalAxiosRequestConfig & { _retry?: boolean };

const SESSION_USER_KEY = 'user';

const skipAuthUrls = [
    '/login',
    '/logout',
    '/refresh-token',
    '/activate',
    '/forgot-password',
    '/public',
];

const parseStoredUser = (): AuthUser | null => {
    const raw = localStorage.getItem(SESSION_USER_KEY);
    if (!raw) return null;

    try {
        return JSON.parse(raw) as AuthUser;
    } catch {
        return null;
    }
};

const redirectTo = (path: string) => {
    if (window.location.pathname !== path) {
        window.location.assign(path);
    }
};

const isStatusOrAuthFlowPath = (path: string) =>
    path.startsWith('/status') ||
    path.startsWith('/auth') ||
    path.startsWith('/primeiro-acesso') ||
    path.startsWith('/cobranca') ||
    path.startsWith('/acesso-indisponivel');

const clearSession = () => {
    localStorage.removeItem(SESSION_USER_KEY);
    localStorage.removeItem('tenant');
    localStorage.removeItem('fcmToken');
};

const updateStoredToken = (token: string) => {
    const user = parseStoredUser();
    if (!user) return;

    const next = {
        ...user,
        accessToken: token,
    };

    localStorage.setItem(SESSION_USER_KEY, JSON.stringify(next));
};

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    withCredentials: true,
});

const refreshClient = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    withCredentials: true,
});

let isRefreshing = false;
let failedQueue: Array<{
    resolve: (token: string) => void;
    reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token?: string) => {
    failedQueue.forEach((pending) => {
        if (error) pending.reject(error);
        else if (token) pending.resolve(token);
    });
    failedQueue = [];
};

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const user = parseStoredUser();
    const token = user?.accessToken;

    localStorage.removeItem('isLocked');

    const shouldSkip = skipAuthUrls.some((urlPart) => config.url?.includes(urlPart));

    if (token && !shouldSkip) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const responseError = error.response?.data as { message?: string; error?: string; code?: string } | string | undefined;
        const normalizedMessage =
            typeof responseError === 'string'
                ? responseError
                : responseError?.message ?? responseError?.error ?? responseError?.code;

        if (error.response) {
            if (typeof error.response.data === 'string') {
                error.response.data = { message: normalizedMessage ?? error.response.data };
            } else if (error.response.data && typeof error.response.data === 'object') {
                const data = error.response.data as { message?: string };
                if (!data.message && normalizedMessage) {
                    data.message = normalizedMessage;
                }
            } else if (normalizedMessage) {
                error.response.data = { message: normalizedMessage };
            }
        }

        const originalRequest = (error.config ?? {}) as RetryableRequest;
        const errorCode = typeof responseError === 'string' ? undefined : responseError?.error ?? responseError?.code;
        const isAdmin = parseStoredUser()?.roles.includes('ADMIN') ?? false;
        const currentPath = window.location.pathname;
        const status = error.response?.status;


        if (errorCode === 'USER_NOT_ACTIVATED') {
            clearSession();
            redirectTo('/primeiro-acesso');
            return Promise.reject(error);
        }

        if (
            error.response?.status === 401 &&
            !originalRequest._retry &&
            !originalRequest.url?.includes('/api/auth/refresh-token')
        ) {
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({
                        resolve: (token: string) => resolve(token),
                        reject,
                    });
                })
                    .then((token) => {
                        originalRequest.headers.Authorization = `Bearer ${token as string}`;
                        return api(originalRequest);
                    })
                    .catch((refreshError) => Promise.reject(refreshError));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                const refreshResponse = await refreshClient.post<{ accessToken: string }>(
                    '/api/auth/refresh-token',
                    {},
                );
                const newToken = refreshResponse.data.accessToken;
                updateStoredToken(newToken);
                processQueue(null, newToken);
                originalRequest.headers.Authorization = `Bearer ${newToken}`;
                return api(originalRequest);
            } catch (refreshError) {
                processQueue(refreshError);
                clearSession();
                redirectTo('/auth/login');
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        if (!status) {
            if (!isStatusOrAuthFlowPath(currentPath)) {
                redirectTo('/status/offline');
            }
            return Promise.reject(error);
        }

        if (status === 403) {
            if (isStatusOrAuthFlowPath(currentPath)) {
                return Promise.reject(error);
            }

            const billingRoutes: Record<string, string> = {
                TRIAL_EXPIRED: 'teste_finalizado',
                SUBSCRIPTION_EXPIRED: 'expirado',
                SUBSCRIPTION_CANCELED: 'cancelado',
                SUBSCRIPTION_INACTIVE: 'expirado',
            };

            const reason = errorCode ? billingRoutes[errorCode] : undefined;

            if (reason) {
                localStorage.setItem('isLocked', 'true');
                if (isAdmin) {
                    redirectTo('/acesso-indisponivel');
                } else {
                    redirectTo(`/cobranca?motivo=${reason}&action=upgrade`);
                }
            } else {
                redirectTo('/status/403');
            }
        }

        return Promise.reject(error);
    },
);

export default api;
