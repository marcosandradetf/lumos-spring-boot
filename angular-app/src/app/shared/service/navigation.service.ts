import { Injectable } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Location } from '@angular/common';
import { filter } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class NavigationService {
    private history: string[] = [];

    constructor(private router: Router, private location: Location) {
        // Escuta todas as mudanças de rota e armazena no array
        this.router.events
            .pipe(filter(event => event instanceof NavigationEnd))
            .subscribe((event: any) => {
                this.history.push(event.urlAfterRedirects);
            });
    }

    pop() {
        this.history.pop(); // Remove a página atual
        if (this.history.length > 0) {
            this.location.back(); // Comportamento de desempilhar
        } else {
            void this.router.navigateByUrl('/'); // Fallback caso não haja histórico interno
        }
    }
}
