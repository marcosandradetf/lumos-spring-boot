import { Component } from '@angular/core';
import { NgIf } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { SharedState } from '../../../core/service/shared-state';
import { Utils } from '../../../core/service/utils';
import { ShareRequest } from '../../../core/service/share.models';
import { UtilsService } from '../../../core/service/utils.service';

@Component({
    selector: 'app-share-modal',
    standalone: true,
    imports: [NgIf, DialogModule, ButtonModule],
    templateUrl: './share-modal.component.html',
    styleUrl: './share-modal.component.scss'
})
export class ShareModalComponent {
    visible = false;
    request: ShareRequest | null = null;

    constructor(private utils: UtilsService) {
        SharedState.shareRequest$.subscribe(req => {
            this.request = req;
            this.visible = !!req;
        });
    }

    close() {
        this.visible = false;
        SharedState.closeShareModal();
    }

    private getMessage(): string {
        return this.request?.message ?? '';
    }

    async copyMessage() {
        const ok = await Utils.copyToClipboard(this.getMessage());
        if (ok) {
            this.utils.showMessage('Mensagem copiada!', 'success', 'Lumos™');
        } else {
            this.utils.showMessage('Não foi possível copiar a mensagem.', 'error', 'Lumos™');
        }
    }

    shareWhatsApp() {
        const message = this.getMessage();
        const phone = this.request?.whatsappPhone?.replace(/\D/g, '') ?? '';
        const base = 'https://api.whatsapp.com/send';
        const url = phone
            ? `${base}?phone=55${phone}&text=${encodeURIComponent(message)}`
            : `${base}?text=${encodeURIComponent(message)}`;
        window.open(url, '_blank', 'noopener,noreferrer');
    }

    shareEmail() {
        const message = this.getMessage();
        const subject = encodeURIComponent(this.request?.subject ?? 'Compartilhar');
        const body = encodeURIComponent(message);
        window.open(`mailto:?subject=${subject}&body=${body}`, '_blank');
    }
}
