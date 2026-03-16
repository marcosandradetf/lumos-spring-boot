import {Injectable, inject, NgZone} from '@angular/core';
import {Messaging, onMessage, getToken} from '@angular/fire/messaging';
import {HttpClient, HttpParams} from '@angular/common/http';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {NotificationPopupComponent} from '../../shared/components/notification-popup/notification-popup.component';
import {environment} from '../../../environments/environment';
import {BehaviorSubject, firstValueFrom, Observable} from 'rxjs';
import {openDB} from 'idb';
import {MessageService} from 'primeng/api';
import {UtilsService} from './utils.service';

@Injectable({providedIn: 'root'})
export class FcmService {
    private messaging = inject(Messaging);
    private dialogService = inject(DialogService);
    private messageService = inject(MessageService)
    private http = inject(HttpClient);
    private zone = inject(NgZone);
    private notificationCount = new BehaviorSubject<number>(0);

    private utils = inject(UtilsService);
    notifications$ = this.notificationCount.asObservable();

    private hasNotifications = new BehaviorSubject<boolean>(true);
    hasNotifications$ = this.hasNotifications.asObservable()

    ref: DynamicDialogRef | undefined;

    constructor() {
        void this.zone.run(async () => {
            await this.syncCountFromDB();
        });

        const bc = new BroadcastChannel('lumos_notifications');
        bc.onmessage = (event) => {
            if (event.data.type === 'NEW_NOTIFICATION') {
                this.zone.run(async () => {
                    await this.syncCountFromDB();

                    // Busca a última notificação para mostrar no Toast
                    // já que o app estava em background e o usuário voltou/estava em outra aba
                    const history = await this.getHistory();
                    if (history.length > 0) {
                        this.showNotificationToast(history[0]);
                    }
                });
            }
        };
    }


    private async syncCountFromDB() {
        try {
            const history = await this.getHistory();
            const unread = history.filter(n => n.read === 0).length;
            this.notificationCount.next(unread);
        } catch (e) {
            console.log('Erro ao sincronizar contador:', e);
        }
    }

    // No fcm.service.ts
    async getHistory() {
        // É crucial passar a versão (1) e o bloco de upgrade aqui também!
        const db = await openDB('lumos_db', 1, {
            upgrade(db) {
                if (!db.objectStoreNames.contains('notifications')) {
                    const store = db.createObjectStore('notifications', {
                        keyPath: 'id',
                        autoIncrement: true
                    });
                    store.createIndex('by_time', 'time');
                    store.createIndex('by_read', 'read');
                    store.createIndex('by_read_type_time', ['read', 'type', 'time']);
                }
            },
        });

        try {
            // Agora o 'db' está garantido com a store 'notifications'
            const items = await db.getAllFromIndex('notifications', 'by_time');
            return items.reverse();
        } catch (error) {
            console.error("Erro ao buscar histórico:", error);
            return [];
        }
    }

    private async saveToIndexedDB(notification: any) {
        // Adicionamos o bloco 'upgrade' aqui também!
        const db = await openDB('lumos_db', 1, {
            upgrade(db) {
                if (!db.objectStoreNames.contains('notifications')) {
                    const store = db.createObjectStore('notifications', {
                        keyPath: 'id',
                        autoIncrement: true
                    });
                    store.createIndex('by_time', 'time');
                    store.createIndex('by_read', 'read')
                    store.createIndex('by_read_type_time', ['read', 'type', 'time']);
                }
            },
        });

        try {
            await db.add('notifications', notification);

            const current = this.notificationCount.value;
            this.notificationCount.next(current + 1);
        } catch (e) {
            console.error('Erro ao adicionar no IDB:', e);
        }
    }

    // No FcmService.ts
    initListen() {
        onMessage(this.messaging, (payload) => {
            const data = payload.data;
            const newNotification = {
                title: data?.['title'] || 'Nova Notificação',
                subtitle: data?.['subtitle'] || 'Nova Notificação',
                body: data?.['body'] || 'Teste de Notificação',
                relatedId: data?.['relatedId'],
                type: data?.['type'],
                uri: data?.['uri'],
                time: Date.now(),
                timeIso: new Date().toISOString(),
                read: 0
            };

            void this.saveToIndexedDB(newNotification);

            // 1. Abre o Popup (Ação imediata)
            if (data?.['isWebPopup'] === 'true') {
                this.zone.run(() => {
                    this.openModal(newNotification);
                });
            } else {
                this.showNotificationToast(newNotification);
            }

        });
    }

    private showNotificationToast(notification: any) {
        this.messageService.add({
            key: 'notifications',
            severity: 'info',
            summary: 'Instalação aguardando validação contratual',
            detail: 'Uma execução em campo foi concluída e precisa ser vinculada a um contrato para liberar o status.',
            life: 8000,
            data: notification // Passamos o objeto completo para o template
        });
    }

    private getSeverity(type: string): string {
        switch (type) {
            case 'ALERT': return 'warn';
            case 'ERROR': return 'error';
            case 'SUCCESS': return 'success';
            default: return 'info';
        }
    }

    private openModal(notificationData: any) {
        this.utils.playSound('open');
        this.dialogService.open(NotificationPopupComponent, {
            data: notificationData,
            width: '720px',
            breakpoints: {
                '768px': '95vw',
                '480px': '100vw'
            },
            showHeader: false,
            modal: true,
            dismissableMask: true,
            baseZIndex: 10000,
            styleClass: 'custom-corporate-dialog',
            contentStyle: {
                padding: '0',
                background: 'transparent',
                overflow: 'visible'
            }
        });
    }

    async getPermission(roles: string[]) {
        switch (Notification.permission) {
            case 'granted':
                await this.subscribeFCMToken(roles);
                this.hasNotifications.next(true);
                break;

            case 'denied':
                this.hasNotifications.next(false);
                break;

            case 'default':
            default:
                console.info('Permissão de notificações ainda não respondida.');
                const permission = await Notification.requestPermission();
                if (permission === 'granted') {
                    await this.subscribeFCMToken(roles);
                    this.hasNotifications.next(true);
                } else {
                    console.warn('Permissão de notificações não concedida.');
                    this.hasNotifications.next(false);
                }
                break;
        }
    }

    private async subscribeFCMToken(roles: string[]) {
        try {
            const token = await getToken(this.messaging, {
                vapidKey: 'BFrjDQvKE8sixitc6d_Z3zWDpPWljJNKZY3Qn3E-dAkwWLSJM88wvi0HisEGCUypTG2GSZkHvb1MLa37FQ3f5Vk',
            });
            if (token) this.subscribeOnTopic(token, roles);
        } catch (err) {
            console.error('Erro ao pegar token FCM:', err);
            this.hasNotifications.next(false);
        }
    }

    async revokePermission(roles: string[]): Promise<void> {
        if (Notification.permission === 'granted') {
            try {
                const token = await getToken(this.messaging, {
                    vapidKey: 'BFrjDQvKE8sixitc6d_Z3zWDpPWljJNKZY3Qn3E-dAkwWLSJM88wvi0HisEGCUypTG2GSZkHvb1MLa37FQ3f5Vk'
                });
                if (token) {
                    await firstValueFrom(
                        this.unSubscribeOnTopic(token, roles)
                    );
                }
            } catch(err) {
                console.error('Erro ao pegar token FCM:', err);
            }
        } else {
            console.log('Usuário não permitiu notificações, fallback ativo');
        }
    }

    private subscribeOnTopic(token: string, roles: string[]) {
        // Usar HttpParams garante que a lista seja formatada corretamente na URL
        const params = new HttpParams().set('roles', roles.join(','));
        console.log("subscribe")

        this.http.post(`${environment.springboot}/api/fcm/subscribe`, {token}, {params})
            .subscribe();

        localStorage.setItem('fcmToken', token);
    }

    resetCount() {
        this.notificationCount.next(0);
    }

    private unSubscribeOnTopic(token: string, roles: string[]): Observable<any> {
        const params = new HttpParams().set('roles', roles.join(','));

        return this.http.post(
            `${environment.springboot}/api/fcm/unsubscribe`,
            { token },
            { params }
        );
    }

    async clearAll() {
        const db = await openDB('lumos_db', 1);
        // Abre uma transação de escrita e limpa a store
        await db.clear('notifications');

        // Zera o contador reativo para o sino atualizar na hora
        this.notificationCount.next(0);
    }

    async markAsRead(notificationId: number) {
        const db = await openDB('lumos_db', 1);

        // 1. Busca a notificação original
        const notification = await db.get('notifications', notificationId);

        if (notification) {
            // 2. Altera o campo necessário
            notification.read = 1;

            // 3. Salva de volta (o 'put' atualiza se o ID já existir)
            await db.put('notifications', notification);

            // 4. Atualiza o contador do sino no sistema
            await this.syncCountFromDB();

            console.log(`Notificação ${notificationId} marcada como lida.`);
        }
    }


    async getBanner(type: string) {

        const db = await openDB('lumos_db', 1, {
            upgrade(db) {
                if (!db.objectStoreNames.contains('notifications')) {
                    const store = db.createObjectStore('notifications', {
                        keyPath: 'id',
                        autoIncrement: true
                    });
                    store.createIndex('by_time', 'time');
                    store.createIndex('by_read', 'read');
                    store.createIndex('by_read_type_time', ['read', 'type', 'time']);
                }
            },
        });

        try {
            // Agora o 'db' está garantido com a store 'notifications'
            const tx = db.transaction('notifications');
            const index = tx.store.index('by_read_type_time');

            const cursor = await index.openCursor(
                IDBKeyRange.bound(
                    [0, type, -Infinity],
                    [0, type, Infinity]
                ),
                'prev'
            );

            return cursor?.value ?? null;
        } catch (error) {
            console.error("Erro ao buscar histórico:", error);
            return null;
        }
    }

    setPermission(b: boolean) {
        this.hasNotifications.next(b);
    }
}
