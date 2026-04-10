importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-messaging-compat.js');

const version = 5;

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
  const request = indexedDB.open('lumos_db', version);

  request.onupgradeneeded = (event) => {
    const db = event.target.result;
    let store;

    // 1. Cria ou obtém a store
    if (!db.objectStoreNames.contains('notifications')) {
      store = db.createObjectStore('notifications', {
        keyPath: 'id',
        autoIncrement: true
      });
    } else {
      // Se a store já existe, usamos a transação do próprio evento para acessá-la
      store = event.target.transaction.objectStore('notifications');
    }

    // 2. Lista de índices que você deseja garantir que existam
    const indices = [
      { name: 'by_time', key: 'time' },
      { name: 'by_read', key: 'read' },
      { name: 'by_tenant', key: 'tenant' },
      { name: 'by_tenant_time', key: ['tenant', 'time'] },
      { name: 'by_tenant_read_type_time', key: ['tenant', 'read', 'type', 'time'] }
    ];

    // 3. Cria apenas os que ainda não estão lá
    indices.forEach(idx => {
      if (!store.indexNames.contains(idx.name)) {
        store.createIndex(idx.name, idx.key);
        console.log(`[SW] Índice criado: ${idx.name}`);
      }
    });
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
    const tenant = data?.['tenant'];

    store.add({
      title: data?.['title'] || 'Nova Notificação',
      subtitle: data?.['subtitle'] || 'Nova Notificação',
      body: data?.['body'] || 'Teste de Notificação',
      relatedId: data?.['relatedId'],
      type: data?.['type'],
      uri: data?.['uri'],
      time: Date.now(),
      timeIso: new Date().toISOString(),
      read: 0,
      tenant: tenant
    });

    tx.oncomplete = () => {
      bc.postMessage({ type: 'NEW_NOTIFICATION' });
      db.close()
    };
  };

  request.onerror = (err) => console.error('[SW] Erro IDB:', err);
}

// No Service Worker
messaging.onBackgroundMessage((payload) => {
  console.log('[SW] Mensagem recebida:', payload);

  // Use event.waitUntil para o SW não ser encerrado antes de salvar e mostrar a notificação
  const promiseChain = Promise.all([
    new Promise((resolve) => {
      // Adapte sua função saveNotificationNative para aceitar um callback de sucesso/resolve
      saveNotificationNative(payload.data, resolve);
    }),
    self.registration.showNotification(payload.data.title, {
      body: payload.data.body,
      icon: '../public/icon-512.png',
      data: { url: payload.data.uri }
    })
  ]);

  return promiseChain;
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
