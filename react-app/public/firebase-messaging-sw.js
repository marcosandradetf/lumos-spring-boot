/* eslint-disable no-undef */
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-messaging-compat.js');

const firebaseConfig = {
  apiKey: 'AIzaSyAxDhw4uOmEoq-Yew4G-Zbe6K-5GDMzsCE',
  authDomain: 'lumos-push.firebaseapp.com',
  projectId: 'lumos-push',
  storageBucket: 'lumos-push.firebasestorage.app',
  messagingSenderId: '37243759038',
  appId: '1:37243759038:web:47343c71f1c322ef7a31ef',
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();
const bc = new BroadcastChannel('lumos_notifications');

messaging.onBackgroundMessage((payload) => {
  const data = payload?.data ?? {};

  const normalized = {
    title: data.title || 'Nova Notificação',
    subtitle: data.subtitle,
    body: data.body || 'Você recebeu uma nova notificação.',
    type: data.type || 'INFO',
    uri: data.uri,
    tenant: data.tenant,
    relatedId: data.relatedId,
  };

  bc.postMessage({
    type: 'NEW_NOTIFICATION',
    notification: normalized,
  });

  return self.registration.showNotification(normalized.title, {
    body: normalized.body,
    icon: '/icon-192.png',
    data: { url: normalized.uri || '/' },
  });
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const urlToOpen = event.notification.data?.url || '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      for (const client of windowClients) {
        if (client.url.includes(urlToOpen) && 'focus' in client) {
          return client.focus();
        }
      }

      if (clients.openWindow) {
        return clients.openWindow(urlToOpen);
      }

      return undefined;
    }),
  );
});
