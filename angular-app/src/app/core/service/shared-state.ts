import { BehaviorSubject } from 'rxjs';
import { ShareRequest } from './share.models';

export class SharedState {
    // estado observável global
    static readonly currentPath$ = new BehaviorSubject<string[]>([]);
    static readonly title$ = new BehaviorSubject<string>('');
    static readonly showMenuDrawer$ = new BehaviorSubject<boolean>(false);

    static readonly showNotificationDrawer$ = new BehaviorSubject<boolean>(false);
    static readonly showAccountDrawer$ = new BehaviorSubject<boolean>(false);

    static readonly shareRequest$ = new BehaviorSubject<ShareRequest | null>(null);

    // setter opcional
    static setCurrentPath(currentPath: string[]) {
        SharedState.currentPath$.next(currentPath);
    }

    static toggleAccountDrawer() {
        const current = SharedState.showAccountDrawer$.value;
        this.showAccountDrawer$.next(!current);
    }

    static toggleNotificationDrawer() {
        const current = SharedState.showNotificationDrawer$.value;
        this.showNotificationDrawer$.next(!current);
    }

    static openShareModal(request: ShareRequest) {
        SharedState.shareRequest$.next(request);
    }

    static closeShareModal() {
        SharedState.shareRequest$.next(null);
    }
}
