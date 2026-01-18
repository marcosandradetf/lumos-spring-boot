import {Component, OnInit} from '@angular/core';
import {Accordion, AccordionTab} from "primeng/accordion";
import {Card} from "primeng/card";
import {FormsModule} from "@angular/forms";
import {InputText} from "primeng/inputtext";
import {MarkdownComponent} from "ngx-markdown";
import {NgForOf, NgOptimizedImage} from "@angular/common";
import {UtilsService} from '../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {DocItem} from '../core/auth/pages/login/login.component';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-doc',
  standalone: true,
    imports: [
        Accordion,
        AccordionTab,
        Card,
        FormsModule,
        InputText,
        MarkdownComponent,
        NgForOf,
        NgOptimizedImage
    ],
  templateUrl: './doc.component.html',
  styleUrl: './doc.component.scss'
})
export class DocComponent implements OnInit {
    loading = false; // usado no *ngIf geral (QR login)


    // ================= DOCS =================
    docs: DocItem[] = [
        { id: 'overview',     key: 'overview',       title: 'Visão Geral',                 group: 'Comece aqui', file: 'overview.md' },
        { id: 'first-access', key: 'first-access',   title: 'Primeiro Acesso',             group: 'Comece aqui', file: 'first-access.md' },

        { id: 'access',       key: 'access',         title: 'Gestão de Acesso',            group: 'Módulos',     file: 'access.md' },
        { id: 'stock',        key: 'stock',          title: 'Estoque',                     group: 'Módulos',     file: 'stock.md' },
        { id: 'contracts',    key: 'contracts',      title: 'Contratos',                   group: 'Módulos',     file: 'contracts.md' },
        { id: 'execution',    key: 'execution',      title: 'Execução de Serviços',        group: 'Módulos',     file: 'execution.md' },
        { id: 'reports',      key: 'reports',        title: 'Relatórios',                  group: 'Módulos',     file: 'reports.md' },
    ];

    activeDoc = 'overview';
    markdownContent = '';
    docLoading = false;

    docQuery = '';
    filteredDocs: DocItem[] = [...this.docs];

    // ================= HELPERS (títulos no header do doc) =================
    get activeDocTitle(): string {
        return this.docs.find(d => d.id === this.activeDoc)?.title ?? '';
    }

    get activeDocSubtitle(): string {
        return this.docs.find(d => d.id === this.activeDoc)?.group ?? '';
    }

    constructor(
        private titleService: Title,
        protected utils: UtilsService,
        protected router: Router,
        private route: ActivatedRoute,
    ) {
        this.titleService.setTitle("Lumos - Documentação");
    }

    async ngOnInit() {
        // docs: abre doc via query param ?doc=...
        this.route.queryParamMap.subscribe(async params => {
            const key = params.get('doc');
            const doc = this.docs.find(d => d.key === key) ?? this.docs[0];
            await this.selectDoc(doc, false);
        });
    }


    // ================= DOCS METHODS =================
    filterDocs() {
        const q = this.docQuery.trim().toLowerCase();

        this.filteredDocs = !q
            ? [...this.docs]
            : this.docs.filter(d =>
                (d.title + ' ' + d.group + ' ' + d.key + ' ' + d.file)
                    .toLowerCase()
                    .includes(q)
            );
    }

    selectDocByKey(key: string) {
        const doc = this.docs.find(d => d.key === key);
        if (doc) void this.selectDoc(doc);
    }

    async selectDoc(doc: DocItem, updateUrl: boolean = true): Promise<void> {
        this.activeDoc = doc.id;
        this.docLoading = true;

        try {
            this.markdownContent = await this.utils.loadFromAssets(doc.file);

            if (updateUrl) {
                void this.router.navigate([], {
                    queryParams: { doc: doc.key },
                    queryParamsHandling: 'merge',
                    replaceUrl: true,
                });
            }
        } catch (e) {
            this.markdownContent = `# Documento não encontrado\n\nNão foi possível carregar **${doc.file}** em \`assets/docs/\`.\n`;
        } finally {
            this.docLoading = false;
        }
    }

    copyDocLink() {
        const key = this.docs.find(d => d.id === this.activeDoc)?.key ?? '';
        const url = `${location.origin}${location.pathname}?doc=${encodeURIComponent(key)}`;
        navigator.clipboard.writeText(url);
        this.utils.showMessage('Link copiado!', 'success', 'Lumos™');
    }
}
