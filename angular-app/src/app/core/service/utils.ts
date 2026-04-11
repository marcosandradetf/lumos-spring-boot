import {Router} from '@angular/router';
import {SharedState} from './shared-state';
import {ShareRequest} from './share.models';

export class Utils {
    static isMobileDevice(): boolean {
        if (typeof navigator === 'undefined') return false;
        const ua = navigator.userAgent ?? '';
        return /Android|iPhone|iPad|iPod/i.test(ua);
    }

    static isShareAvailable(): boolean {
        return typeof navigator !== 'undefined' && typeof navigator.share === 'function';
    }

    static async shareMessage(message: string, options: Omit<ShareRequest, 'message'> = {}): Promise<void> {
        const payload: ShareRequest = { message, ...options };

        if (Utils.isMobileDevice() && Utils.isShareAvailable()) {
            const shareData: ShareData = {
                title: payload.title ?? 'Compartilhar',
                text: payload.message,
            };

            if (payload.file) {
                const canShare = typeof navigator.canShare === 'function'
                    ? navigator.canShare({ files: [payload.file] })
                    : true;
                if (canShare) {
                    shareData.files = [payload.file];
                }
            }

            await navigator.share(shareData);
            return;
        }

        SharedState.openShareModal(payload);
    }

    static async copyToClipboard(text: string): Promise<boolean> {
        if (typeof navigator === 'undefined' || !navigator.clipboard?.writeText) return false;
        await navigator.clipboard.writeText(text);
        return true;
    }

    static openEmail(to: string, subject: string, body: string): void {
        const encodedSubject = encodeURIComponent(subject);
        const encodedBody = encodeURIComponent(body);
        window.open(`mailto:${to}?subject=${encodedSubject}&body=${encodedBody}`, '_blank');
    }
    static normalizeString(value: string): string {
        return value
            .normalize('NFD')                 // separa acentos
            .replace(/[\u0300-\u036f]/g, '')  // remove acentos
            .replace(/\s+/g, '_')             // espaço → underline
            .replace(/[^a-zA-Z0-9_]/g, '')    // remove caracteres especiais
            .toLowerCase();
    }

    static formatNowToDDMMYYHHmm(formatted: boolean = false): string {
        const now = new Date();

        const pad = (n: number) => n.toString().padStart(2, '0');

        const dd = pad(now.getDate());
        const mm = pad(now.getMonth() + 1);
        const yy = pad(now.getFullYear() % 100);
        const hh = pad(now.getHours());
        const min = pad(now.getMinutes());

        return formatted ? `${dd}/${mm}/${yy} ${hh}:${min}` : `${dd}${mm}${yy}${hh}${min}`;
    }

    static diffInDays(start: Date, end: Date): number {
        const msPerDay = 1000 * 60 * 60 * 24;
        const diff = end.getTime() - start.getTime();
        return Math.floor(diff / msPerDay);
    }

    static diffInHours(start: Date | string, end: Date | string): number {
        const s = new Date(start).getTime();
        const e = new Date(end).getTime();
        return Math.floor((e - s) / 3_600_000);
    }

    static abbreviate(name: string): string {
        const tokens = name.split(' ');
        const result: string[] = [];

        let replace = false;

        for (const token of tokens) {
            const word = token.toLowerCase();

            if (word === 'prefeitura') {
                result.push('PREF.');
                replace = true;

            } else if (word === 'municipal' && replace) {
                result.push('MUN.');

            } else if (word === 'de' && replace) {
                // ignora "de"

            } else if (word.split('-').length > 1) {
                result.push(word.split('-')[0]);

            } else if (word === "manutenção") {

            } else {
                result.push(token);
                replace = false;
            }
        }

        return result.join(' ').toUpperCase();
    }

    static formatValue(event: Event) {
        let value = (event.target as HTMLInputElement).value.replace(/\D/g, '');

        // Verifica se targetValue está vazio e define um valor padrão
        if (!value) {
            (event.target as HTMLInputElement).value = ''; // Atualiza o valor no campo de input
            return;
        }

        // Divide o valor por 100 para inserir as casas decimais
        (event.target as HTMLInputElement).value = new Intl.NumberFormat('pt-BR', {
            //style: 'currency',
            currency: 'BRL',
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        }).format(parseFloat(value) / 100); // Exibe o valor formatado no campo de input

    }

    static handleHttpError(error: any, router: Router) {
        if (error.status === 0) {
            // 🔥 erro de rede / backend offline
            console.error('Servidor inacessível ou sem conexão');

            void router.navigate(['/status/offline']);
            return;
        }

        if (error.status === 403) {
            // 🔥 erro interno
            void router.navigate(['/status/403']);
            return;
        }

        if (error.status >= 500) {
            // 🔥 erro interno
            void router.navigate(['/status/500']);
            return;
        }
    }

    static getLumosTenant(): string {
        return '';
    }


}
