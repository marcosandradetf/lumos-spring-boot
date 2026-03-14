importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-messaging-compat.js');

const firebaseConfig = {
  apiKey: "AIzaSyAxDhw4uOmEoq-Yew4G-Zbe6K-5GDMzsCE",
  authDomain: "lumos-push.firebaseapp.com",
  projectId: "lumos-push",
  storageBucket: "lumos-push.firebasestorage.app",
  messagingSenderId: "37243759038",
  appId: "1:37243759038:web:47343c71f1c322ef7a31ef"
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();
const bc = new BroadcastChannel('lumos_notifications');

// Funçao Nativa (Sem bibliotecas externas)
function saveNotificationNative(data) {
  // Aumentamos a versão para 2 para garantir o reset
  const request = indexedDB.open('lumos_db', 2);

  request.onupgradeneeded = (event) => {
    const db = event.target.result;
    if (!db.objectStoreNames.contains('notifications')) {
      const store = db.createObjectStore('notifications', { keyPath: 'id', autoIncrement: true });
      store.createIndex('by_time', 'time');
      store.createIndex('by_read', 'read');
      console.log('[SW] Database upgrade concluído.');
    }
  };

  request.onsuccess = (event) => {
    const db = event.target.result;

    // IMPORTANTE: Verifique se a store existe antes de abrir a transação
    if (!db.objectStoreNames.contains('notifications')) {
      console.error('[SW] Store não encontrada!');
      return;
    }

    const tx = db.transaction('notifications', 'readwrite');
    const store = tx.objectStore('notifications');

    store.add({
      title: data.title || 'Nova Notificação',
      body: data.body || '',
      relatedId: data.relatedId,
      type: data.type,
      uri: data.uri,
      time: new Date().toISOString(),
      read: 0
    });

    tx.oncomplete = () => {
      bc.postMessage({ type: 'NEW_NOTIFICATION' });
      db.close()
    };
  };

  request.onerror = (err) => console.error('[SW] Erro IDB:', err);
}

messaging.onBackgroundMessage((payload) => {
  console.log('[SW] Mensagem recebida:', payload);


  saveNotificationNative(payload.data);

  return self.registration.showNotification(payload.data.title, {
    body: payload.data.body,
    icon: '/assets/icons/icon-72x72.png',
    data: { url: payload.data.uri }
  });
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const urlToOpen = event.notification.data?.url || '/';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      for (let client of windowClients) {
        if (client.url.includes(urlToOpen) && 'focus' in client) return client.focus();
      }
      if (clients.openWindow) return clients.openWindow(urlToOpen);
    })
  );
});
