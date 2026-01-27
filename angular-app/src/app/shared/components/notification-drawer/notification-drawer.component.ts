import { Component } from '@angular/core';
import {Drawer} from 'primeng/drawer';
import {SharedState} from '../../../core/service/shared-state';
import {AsyncPipe, NgClass, NgIf, NgOptimizedImage} from '@angular/common';
import {SidebarComponent} from '../sidebar/sidebar.component';
import {Router} from '@angular/router';
import {Ripple} from 'primeng/ripple';

@Component({
  selector: 'app-notification-drawer',
  standalone: true,
    imports: [
        Drawer,
        AsyncPipe,
        SidebarComponent,
        NgOptimizedImage,
        NgClass,
        NgIf,
        Ripple
    ],
  templateUrl: './notification-drawer.component.html',
  styleUrl: './notification-drawer.component.scss'
})
export class NotificationDrawerComponent {
    notifications = [
        {
            id: 1,
            title: 'Nova pré-medição',
            message: 'Uma nova pré-medição foi enviada para análise.',
            time: 'há 5 minutos',
            read: false
        },
        {
            id: 2,
            title: 'Análise concluída',
            message: 'A pré-medição foi aprovada com sucesso.',
            time: 'ontem',
            read: false
        }
    ];

    protected readonly SharedState = SharedState;

    constructor(protected router: Router,) {

    }

    onDrawerChange(open: boolean) {
        SharedState.showNotificationDrawer$.next(open);
    }

    openNotification(notification: any): void {
        // 1. Marca como lida
        if (!notification.read) {
            notification.read = true;
            // opcional: backend
            // this.notificationService.markAsRead(notification.id);
        }

        // 2. Fecha o drawer
        this.SharedState.showNotificationDrawer$.next(false);

        // 3. Navega (se existir rota)
        if (notification.route) {
            void this.router.navigate([notification.route], {
                queryParams: notification.params
            });
        }
    }

    markAllAsRead(): void {
        this.notifications.forEach(n => (n.read = true));

        // opcional backend
        // this.notificationService.markAllAsRead();
    }


}
