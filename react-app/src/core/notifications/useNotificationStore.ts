import { create } from 'zustand';
import {
  getNotificationPermission,
  initializeForegroundListener,
  listenServiceWorkerBroadcast,
  requestPermissionAndSubscribe,
  type NotificationStatus,
} from './fcmWeb';

export type { NotificationStatus } from './fcmWeb';

export interface AppNotification {
  id: number;
  title: string;
  subtitle?: string;
  body: string;
  type: string;
  uri?: string;
  tenant?: string;
  relatedId?: string;
  time: number;
  timeIso: string;
  read: 0 | 1;
}

interface NotificationState {
  status: NotificationStatus;
  count: number;
  history: AppNotification[];
  initialized: boolean;
  initialize: () => void;
  syncStatus: () => void;
  requestPermission: (roles?: string[]) => Promise<NotificationStatus>;
  setCount: (count: number) => void;
  setHistory: (history: AppNotification[]) => void;
  addNotification: (notification: Omit<AppNotification, 'id' | 'time' | 'timeIso' | 'read'> & Partial<Pick<AppNotification, 'id' | 'time' | 'timeIso' | 'read'>>) => void;
  markAsRead: (notificationId: number) => void;
  markAllAsRead: () => void;
  removeNotification: (notificationId: number) => void;
  clearAll: () => void;
  increment: () => void;
  reset: () => void;
}

const STATUS_KEY = 'notificationStatus';
const COUNT_KEY = 'notificationCount';
const HISTORY_KEY = 'notificationHistory';
const MAX_HISTORY_ITEMS = 200;

let stopBroadcastListener: (() => void) | null = null;

function parseHistory(rawHistory: string | null): AppNotification[] {
  if (!rawHistory) return [];

  try {
    const parsed = JSON.parse(rawHistory) as unknown;
    if (!Array.isArray(parsed)) return [];

    return parsed
      .map((item, index) => {
        if (!item || typeof item !== 'object') return null;
        const record = item as Partial<AppNotification>;
        const time = Number(record.time ?? Date.now());
        const id = Number(record.id ?? time + index);

        return {
          id,
          title: String(record.title ?? 'Notificação'),
          subtitle: record.subtitle ? String(record.subtitle) : undefined,
          body: String(record.body ?? ''),
          type: String(record.type ?? 'INFO'),
          uri: record.uri ? String(record.uri) : undefined,
          tenant: record.tenant ? String(record.tenant) : undefined,
          relatedId: record.relatedId ? String(record.relatedId) : undefined,
          time,
          timeIso: String(record.timeIso ?? new Date(time).toISOString()),
          read: Number(record.read) === 1 ? 1 : 0,
        } as AppNotification;
      })
      .filter((item): item is AppNotification => item !== null)
      .sort((a, b) => b.time - a.time)
      .slice(0, MAX_HISTORY_ITEMS);
  } catch {
    return [];
  }
}

function persistState(status: NotificationStatus, count: number, history: AppNotification[]) {
  localStorage.setItem(STATUS_KEY, status);
  localStorage.setItem(COUNT_KEY, String(Math.max(0, count)));
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history.slice(0, MAX_HISTORY_ITEMS)));
}

function unreadCount(history: AppNotification[]) {
  return history.filter((notification) => notification.read === 0).length;
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  status: 'default',
  count: 0,
  history: [],
  initialized: false,

  initialize: () => {
    const browserStatus = getNotificationPermission();
    const savedStatus = localStorage.getItem(STATUS_KEY) as NotificationStatus | null;
    const status = browserStatus ?? savedStatus ?? 'default';
    const history = parseHistory(localStorage.getItem(HISTORY_KEY));
    const savedCount = Number(localStorage.getItem(COUNT_KEY) ?? '0');
    const count = history.length > 0 ? unreadCount(history) : Math.max(0, savedCount || 0);

    set({
      status,
      count,
      history,
      initialized: true,
    });

    persistState(status, count, history);

    if (!stopBroadcastListener) {
      stopBroadcastListener = listenServiceWorkerBroadcast((payload) => {
        get().addNotification({
          title: payload.title,
          subtitle: payload.subtitle,
          body: payload.body,
          type: payload.type,
          uri: payload.uri,
          tenant: payload.tenant,
          relatedId: payload.relatedId,
          read: 0,
        });
      });
    }

    void initializeForegroundListener((payload) => {
      get().addNotification({
        title: payload.title,
        subtitle: payload.subtitle,
        body: payload.body,
        type: payload.type,
        uri: payload.uri,
        tenant: payload.tenant,
        relatedId: payload.relatedId,
        read: 0,
      });
    });
  },

  syncStatus: () => {
    const status = getNotificationPermission();
    set((state) => {
      persistState(status, state.count, state.history);
      return { status };
    });
  },

  requestPermission: async (roles = []) => {
    const status = await requestPermissionAndSubscribe(roles);
    set((state) => {
      persistState(status, state.count, state.history);
      return { status };
    });
    return status;
  },

  setCount: (count) => {
    const next = Math.max(0, Number(count) || 0);
    set((state) => {
      persistState(state.status, next, state.history);
      return { count: next };
    });
  },

  setHistory: (history) => {
    const normalized: AppNotification[] = history
      .map((item) => ({ ...item, read: item.read === 1 ? 1 as const : 0 as const }))
      .sort((a, b) => b.time - a.time)
      .slice(0, MAX_HISTORY_ITEMS);
    const nextCount = unreadCount(normalized);
    set((state) => {
      persistState(state.status, nextCount, normalized);
      return { history: normalized, count: nextCount };
    });
  },

  addNotification: (notification) => {
    const now = Date.now();
    const nextNotification: AppNotification = {
      id: notification.id ?? now,
      title: notification.title,
      subtitle: notification.subtitle,
      body: notification.body,
      type: notification.type ?? 'INFO',
      uri: notification.uri,
      tenant: notification.tenant,
      relatedId: notification.relatedId,
      time: notification.time ?? now,
      timeIso: notification.timeIso ?? new Date(now).toISOString(),
      read: notification.read === 1 ? 1 : 0,
    };

    set((state) => {
      const history = [nextNotification, ...state.history]
        .sort((a, b) => b.time - a.time)
        .slice(0, MAX_HISTORY_ITEMS);
      const nextCount = unreadCount(history);
      persistState(state.status, nextCount, history);
      return {
        history,
        count: nextCount,
      };
    });
  },

  markAsRead: (notificationId) => {
    set((state) => {
      const history = state.history.map((notification) => (
        notification.id === notificationId
          ? { ...notification, read: 1 as const }
          : notification
      ));
      const nextCount = unreadCount(history);
      persistState(state.status, nextCount, history);
      return { history, count: nextCount };
    });
  },

  markAllAsRead: () => {
    set((state) => {
      const history = state.history.map((notification) => ({ ...notification, read: 1 as const }));
      persistState(state.status, 0, history);
      return { history, count: 0 };
    });
  },

  removeNotification: (notificationId) => {
    set((state) => {
      const history = state.history.filter((notification) => notification.id !== notificationId);
      const nextCount = unreadCount(history);
      persistState(state.status, nextCount, history);
      return { history, count: nextCount };
    });
  },

  clearAll: () => {
    set((state) => {
      const history: AppNotification[] = [];
      persistState(state.status, 0, history);
      return { history, count: 0 };
    });
  },

  increment: () => {
    const next = get().count + 1;
    set((state) => {
      persistState(state.status, next, state.history);
      return { count: next };
    });
  },

  reset: () => {
    set((state) => {
      persistState(state.status, 0, state.history);
      return { count: 0 };
    });
  },
}));
