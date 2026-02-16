import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import {SkeletonModule} from 'primeng/skeleton';
import {TableModule} from 'primeng/table';
import {StyleClass} from 'primeng/styleclass';
import {NgForOf} from '@angular/common';

@Component({
    selector: 'app-skeleton-table',
    standalone: true,
    template: `
            <p-table [value]="items" pStyleClass="mt-4">
                <ng-template #header>
                    <tr>
                        <th *ngFor="let col of columns">
                            {{ col }}
                        </th>
                    </tr>
                </ng-template>
                <ng-template #body let-product>
                    <tr>
                        <td *ngFor="let col of columns">
                            <p-skeleton />
                        </td>
                    </tr>
                </ng-template>
            </p-table>
    `,
    imports: [SkeletonModule, TableModule, StyleClass, NgForOf]
})
export class SkeletonTableComponent {
    @Input() columns: string[] = [];
    items: any[] = Array.from({ length: 10 }).map((_, i) => `Item #${i}`);
}
