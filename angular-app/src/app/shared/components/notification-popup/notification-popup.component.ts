import {Component, inject, OnInit} from '@angular/core';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {Router} from '@angular/router';
import {FcmService} from '../../../core/service/fcm.service';

@Component({
  selector: 'app-notification-popup',
  standalone: true,
    imports: [

    ],
  templateUrl: './notification-popup.component.html',
  styleUrl: './notification-popup.component.scss'
})
export class NotificationPopupComponent implements OnInit {
    private config = inject(DynamicDialogConfig);
    private ref = inject(DynamicDialogRef);
    private router = inject(Router);
    private fcm = inject(FcmService);

    data: any;

    ngOnInit() {
        this.data = this.config.data;
    }

    // Estilização dinâmica baseada no "type" enviado pelo Spring
    getIconClass() {
        switch (this.data.type) {
            case 'INSTALLATION':
                return { container: 'bg-blue-100', icon: 'pi pi-map-marker text-blue-600' };
            case 'ALERT':
                return { container: 'bg-red-100', icon: 'pi pi-exclamation-triangle text-red-600' };
            default:
                return { container: 'bg-yellow-100', icon: 'pi pi-bell text-yellow-600' };
        }
    }

    async goToDetails() {
        await this.fcm.markAsRead(this.data.id);
        if (this.data.uri) {
            // Se tiver uma URI (link completo), abre ou redireciona
            void this.router.navigate([this.data.uri]);
        }
        this.ref.close();
    }

    close() {
        this.ref.close();
    }
}
