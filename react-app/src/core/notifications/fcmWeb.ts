import api from '@/core/auth/api';
import { initializeApp, getApps } from 'firebase/app';
import { getMessaging, getToken, isSupported, onMessage, type Messaging } from 'firebase/messaging';

export type NotificationStatus = 'granted' | 'denied' | 'default';

interface ForegroundNotification {
  title: string;
  subtitle?: string;
  body: string;
  type: string;
  uri?: string;
  tenant?: string;
  relatedId?: string;
}

const DEFAULT_FIREBASE_CONFIG = {
  apiKey: 'AIzaSyAxDhw4uOmEoq-Yew4G-Zbe6K-5GDMzsCE',
  authDomain: 'lumos-push.firebaseapp.com',
  projectId: 'lumos-push',
  storageBucket: 'lumos-push.firebasestorage.app',
  messagingSenderId: '37243759038',
  appId: '1:37243759038:web:47343c71f1c322ef7a31ef',
};

const DEFAULT_VAPID_KEY = 'BFrjDQvKE8sixitc6d_Z3zWDpPWljJNKZY3Qn3E-dAkwWLSJM88wvi0HisEGCUypTG2GSZkHvb1MLa37FQ3f5Vk';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY ?? DEFAULT_FIREBASE_CONFIG.apiKey,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ?? DEFAULT_FIREBASE_CONFIG.authDomain,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID ?? DEFAULT_FIREBASE_CONFIG.projectId,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET ?? DEFAULT_FIREBASE_CONFIG.storageBucket,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID ?? DEFAULT_FIREBASE_CONFIG.messagingSenderId,
  appId: import.meta.env.VITE_FIREBASE_APP_ID ?? DEFAULT_FIREBASE_CONFIG.appId,
};

const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY ?? DEFAULT_VAPID_KEY;

const SERVICE_WORKER_REGISTRATION_TIMEOUT_MS = 5000;
const SERVICE_WORKER_READY_TIMEOUT_MS = 5000;
const FCM_TOKEN_TIMEOUT_MS = 5000;

let serviceWorkerRegistrationPromise: Promise<ServiceWorkerRegistration | null> | null = null;
let listenerInitialized = false;

function withTimeout<T>(promise: Promise<T>, timeoutMs: number, label: string): Promise<T | null> {
  let timeoutId: number | undefined;

  const timeout = new Promise<null>((resolve) => {
    timeoutId = window.setTimeout(() => {
      console.warn(`${label} excedeu ${timeoutMs}ms.`);
      resolve(null);
    }, timeoutMs);
  });

  return Promise.race([promise, timeout]).finally(() => {
    if (timeoutId !== undefined) {
      window.clearTimeout(timeoutId);
    }
  });
}

function isWebViewUnsupported(): boolean {
  const userAgent = window.navigator.userAgent.toLowerCase();
  const isIosWebView = !('Notification' in window);
  const isAndroidWebView = userAgent.includes('wv');
  return isIosWebView || isAndroidWebView;
}

async function getMessagingInstance(): Promise<Messaging | null> {
  if (typeof window === 'undefined' || isWebViewUnsupported()) {
    return null;
  }

  const supported = await isSupported();
  if (!supported) {
    return null;
  }

  const app = getApps().length > 0 ? getApps()[0] : initializeApp(firebaseConfig);
  return getMessaging(app);
}

export function getNotificationPermission(): NotificationStatus {
  if (typeof Notification === 'undefined') {
    return 'default';
  }

  return Notification.permission as NotificationStatus;
}

export async function ensureMessagingServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (typeof window === 'undefined' || !('serviceWorker' in navigator)) {
    return null;
  }

  if (!serviceWorkerRegistrationPromise) {
    serviceWorkerRegistrationPromise = (async () => {
      try {
        const registration = await withTimeout(
          navigator.serviceWorker.register('/firebase-messaging-sw.js'),
          SERVICE_WORKER_REGISTRATION_TIMEOUT_MS,
          'Registro do firebase-messaging-sw.js',
        );

        if (!registration) {
          return null;
        }

        const readyRegistration = await withTimeout(
          navigator.serviceWorker.ready,
          SERVICE_WORKER_READY_TIMEOUT_MS,
          'Ativacao do service worker de mensagens',
        );

        return readyRegistration ?? registration;
      } catch (error) {
        console.warn('Falha ao registrar firebase-messaging-sw.js', error);
        return null;
      }
    })();
  }

  const registration = await serviceWorkerRegistrationPromise;
  if (!registration) {
    serviceWorkerRegistrationPromise = null;
  }

  return registration;
}

async function getFcmToken(): Promise<string | null> {
  const messaging = await getMessagingInstance();
  if (!messaging) {
    return null;
  }

  const registration = await ensureMessagingServiceWorker();
  if (!registration) {
    return null;
  }

  const token = await withTimeout(
    getToken(messaging, {
      vapidKey,
      serviceWorkerRegistration: registration,
    }),
    FCM_TOKEN_TIMEOUT_MS,
    'Obtencao do token FCM',
  );

  return token || null;
}

export async function subscribeToTopic(roles: string[]): Promise<string | null> {
  const token = await getFcmToken();
  if (!token) {
    return null;
  }

  await api.post('/api/fcm/subscribe', { token }, {
    params: {
      roles: roles.join(','),
    },
  });

  localStorage.setItem('fcmToken', token);
  return token;
}

export async function unsubscribeFromTopic(roles: string[]): Promise<void> {
  const token = await getFcmToken();
  if (!token) {
    localStorage.removeItem('fcmToken');
    return;
  }

  await api.post('/api/fcm/unsubscribe', { token }, {
    params: {
      roles: roles.join(','),
    },
  });

  localStorage.removeItem('fcmToken');
}

export async function requestPermissionAndSubscribe(roles: string[]): Promise<NotificationStatus> {
  if (isWebViewUnsupported()) {
    return 'default';
  }

  const current = getNotificationPermission();
  if (current === 'granted') {
    try {
      await subscribeToTopic(roles);
    } catch (error) {
      console.warn('Permissao ja concedida, mas falha ao assinar topicos FCM.', error);
    }
    return 'granted';
  }

  if (current === 'denied') {
    return 'denied';
  }

  const permission = await Notification.requestPermission();
  const next = permission as NotificationStatus;
  if (next === 'granted') {
    try {
      await subscribeToTopic(roles);
    } catch (error) {
      console.warn('Permissao concedida, mas falha ao assinar topicos FCM.', error);
    }
  }

  return next;
}

export async function initializeForegroundListener(onForegroundMessage: (payload: ForegroundNotification) => void): Promise<void> {
  if (listenerInitialized || isWebViewUnsupported()) {
    return;
  }

  await ensureMessagingServiceWorker();

  const messaging = await getMessagingInstance();
  if (!messaging) {
    return;
  }

  onMessage(messaging, (payload) => {
    const data = payload.data ?? {};

    const mapped: ForegroundNotification = {
      title: data.title ?? 'Nova Notificação',
      subtitle: data.subtitle,
      body: data.body ?? 'Você recebeu uma nova notificação.',
      type: data.type ?? 'INFO',
      uri: data.uri,
      tenant: data.tenant,
      relatedId: data.relatedId,
    };

    if (mapped.tenant) {
      localStorage.setItem('tenant', mapped.tenant);
    }

    onForegroundMessage(mapped);
  });

  listenerInitialized = true;
}

export function listenServiceWorkerBroadcast(onBroadcastMessage: (payload: ForegroundNotification) => void): () => void {
  const channel = new BroadcastChannel('lumos_notifications');

  channel.onmessage = (event) => {
    if (event.data?.type !== 'NEW_NOTIFICATION' || !event.data?.notification) {
      return;
    }

    const data = event.data.notification as Record<string, string | undefined>;
    onBroadcastMessage({
      title: data.title ?? 'Nova Notificação',
      subtitle: data.subtitle,
      body: data.body ?? 'Você recebeu uma nova notificação.',
      type: data.type ?? 'INFO',
      uri: data.uri,
      tenant: data.tenant,
      relatedId: data.relatedId,
    });
  };

  return () => {
    channel.close();
  };
}
