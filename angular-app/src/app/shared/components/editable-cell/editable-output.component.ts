import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    selector: 'app-editable-output',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <div
      class="group flex items-center gap-2 px-2 py-1 rounded-md
             cursor-pointer transition-all duration-200
             hover:bg-blue-50 hover:ring-1 hover:ring-blue-400
             dark:hover:bg-blue-500/10 dark:hover:ring-blue-400/40"
      [class.justify-end]="align === 'right'"
      [class.justify-start]="align === 'left'">

      <span class="text-gray-800 dark:text-gray-100">
        <ng-content></ng-content>
      </span>

      <i class="pi pi-pencil text-xs
                text-gray-400 dark:text-gray-500
                opacity-0 group-hover:opacity-100
                transition-opacity duration-200">
      </i>
    </div>
  `
})
export class EditableOutputComponent {
    @Input() align: 'left' | 'right' = 'right';
}
