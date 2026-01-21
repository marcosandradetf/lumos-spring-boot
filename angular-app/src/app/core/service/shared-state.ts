import { BehaviorSubject } from 'rxjs';

export class SharedState {
    // estado observável global
    static readonly currentPath$ = new BehaviorSubject<string[]>([]);
    static readonly title$ = new BehaviorSubject<string>('');
    static readonly showMenuDrawer$ = new BehaviorSubject<boolean>(false);

    // setter opcional
    static setCurrentPath(currentPath: string[]) {
        SharedState.currentPath$.next(currentPath);
    }

}
