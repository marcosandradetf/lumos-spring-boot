import {Component, inject, OnInit} from '@angular/core';
import {Drawer} from 'primeng/drawer';
import {SharedState} from '../../../core/service/shared-state';
import {AsyncPipe, DatePipe, NgClass, NgForOf, NgIf, NgOptimizedImage} from '@angular/common';
import {SidebarComponent} from '../sidebar/sidebar.component';
import {Router} from '@angular/router';
import {Ripple} from 'primeng/ripple';
import {FcmService} from '../../../core/service/fcm.service';

@Component({
    selector: 'app-notification-drawer',
    standalone: true,
    imports: [
        Drawer,
        AsyncPipe,
        NgClass,
        NgIf,
        Ripple,
        NgForOf,
        DatePipe
    ],
    templateUrl: './notification-drawer.component.html',
    styleUrl: './notification-drawer.component.scss'
})
export class NotificationDrawerComponent {
    notifications: any[] = [];
    private fcmService = inject(FcmService);

    // No seu componente do Drawer
    notificationsGrouped: { [key: string]: any[] } = {};
    groupKeys: string[] = [];

    async loadNotifications() {
        const data = await this.fcmService.getHistory();
        this.groupNotifications(data);
    }

    private groupNotifications(data: any[]) {
        const groups: { [key: string]: any[] } = {};

        data.forEach(notif => {
            const date = new Date(notif.timeIso);
            let dateLabel = '';

            const today = new Date();
            const yesterday = new Date();
            yesterday.setDate(today.getDate() - 1);

            if (date.toDateString() === today.toDateString()) {
                dateLabel = 'Hoje';
            } else if (date.toDateString() === yesterday.toDateString()) {
                dateLabel = 'Ontem';
            } else {
                // Ex: 15 de Março
                dateLabel = date.toLocaleDateString('pt-BR', { day: 'numeric', month: 'long' });
            }

            if (!groups[dateLabel]) groups[dateLabel] = [];
            groups[dateLabel].push(notif);
        });

        this.notificationsGrouped = groups;
        this.groupKeys = Object.keys(groups); // Mantém a ordem dos grupos
    }

    protected readonly SharedState = SharedState;

    constructor(protected router: Router,) {

    }

    async onDrawerChange(open: boolean) {
        await this.loadNotifications();
        SharedState.showNotificationDrawer$.next(open);
    }

    async openNotification(notification: any) {
        // Marca como lida
        if (notification.read === 0) {
            notification.read = 1;
            await this.fcmService.markAsRead(notification.id)
        }

        //  Navega (se existir rota)
        if (notification.uri) {
            this.SharedState.showNotificationDrawer$.next(false);
            void this.router.navigate([notification.uri]);
        }
    }

    async clearAll() {
        await this.fcmService.clearAll();
        this.fcmService.resetCount();
        this.notifications = [];
        this.groupKeys = [];
        this.notificationsGrouped = {};
    }

    getIconBg(type: string) {
        switch (type) {
            case 'ERROR': return 'bg-red-100 dark:bg-red-900/30';
            case 'ALERT': return 'bg-orange-100 dark:bg-orange-900/30';
            case 'ALERT_BANNER': return 'bg-red-100 dark:bg-red-900/30';
            case 'SUCCESS': return 'bg-green-100 dark:bg-green-900/30';
            default: return 'bg-blue-100 dark:bg-blue-900/30';
        }
    }

    getIconClass(type: string) {
        switch (type) {
            case 'ERROR': return 'pi pi-times text-red-600';
            case 'ALERT': return 'pi pi-exclamation-triangle text-orange-600';
            case 'ALERT_BANNER': return 'pi pi-exclamation-triangle text-red-600';
            case 'SUCCESS': return 'pi pi-check text-green-600';
            default: return 'pi pi-info-circle text-blue-600';
        }
    }


}
