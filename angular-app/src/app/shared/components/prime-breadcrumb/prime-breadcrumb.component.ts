import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {MenuItem} from 'primeng/api';
import {Breadcrumb} from 'primeng/breadcrumb';

@Component({
    selector: 'app-prime-breadcrumb',
    standalone: true,
    imports: [
        Breadcrumb
    ],
    templateUrl: './prime-breadcrumb.component.html',
    styleUrl: './prime-breadcrumb.component.scss'
})
export class PrimeBreadcrumbComponent implements OnChanges {
    @Input() title: string | null = null;
    @Input() path: string[] | null = null;

    home: MenuItem = {icon: 'pi pi-home', routerLink: '/'};
    items: MenuItem[] = [];
    smallItems: MenuItem[] = [];

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['path'] && this.path) {
            this.items = this.path.map(item => ({
                label: item
            }));

            if (this.path.length > 1) {
                this.smallItems = [{
                    label: this.path[1]
                }]
            } else if (this.path.length == 1) {
                this.smallItems = [{
                    label: this.path[0]
                }]
            }

        } else {
            this.items = [];
        }
    }


}
