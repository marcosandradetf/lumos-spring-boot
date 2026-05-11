import { Injectable } from '@angular/core';
import { Router } from '@angular/router';


@Injectable({ providedIn: 'root' })
export class SearchService {
    constructor(private router: Router) {}

    searchRoutes(term: string): any[] {
        if (!term || term.length < 2) return [];
        const normalizedTerm = term.toLowerCase();
        const results: any[] = [];

        const extract = (routes: any[], parentPath: string = '') => {
            for (const route of routes) {
                // 1. Monta o caminho técnico (ex: pedidos/:status)
                const currentPath = route.path ? `${parentPath}/${route.path}`.replace(/\/+/g, '/') : parentPath;

                // 2. Prepara o caminho final (substituindo :params se existirem)
                let finalPath = currentPath;

                if (route.data?.['routeParams']) {
                    const params = route.data['routeParams'];
                    Object.keys(params).forEach(key => {
                        finalPath = finalPath.replace(`:${key}`, params[key]);
                    });
                }

                // 3. ADICIONA AO RESULTADO (Apenas uma vez!)
                if (route.data?.['title']) {
                    results.push({
                        label: route.data['title'],
                        path: finalPath,
                        icon: route.data['icon'] || 'pi pi-link',
                        queryParams: route.data['query'] || null
                    });
                }

                // 4. Varre os filhos usando o currentPath (para manter os tokens :params acessíveis)
                if (route.children) {
                    extract(route.children, currentPath);
                }
            }
        };

        extract(this.router.config);

        // 5. Filtra e remove duplicatas globais (caso a mesma rota apareça em árvores diferentes)
        return results
            .filter(r =>
                r.label.toLowerCase().includes(normalizedTerm) ||
                r.path.toLowerCase().includes(normalizedTerm)
            )
            .filter((value, index, self) =>
                index === self.findIndex((t) => t.path === value.path && t.label === value.label)
            )
            .slice(0, 6);
    }
}
