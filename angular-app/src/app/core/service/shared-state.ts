import { BehaviorSubject } from 'rxjs';

export class SharedState {
    // estado observável global
    static readonly currentPath$ = new BehaviorSubject<string[]>([]);
    static readonly title$ = new BehaviorSubject<string>('');
    static readonly showMenuDrawer$ = new BehaviorSubject<boolean>(false);

    static readonly showNotificationDrawer$ = new BehaviorSubject<boolean>(false);
    static readonly showAccountDrawer$ = new BehaviorSubject<boolean>(false);

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

}
