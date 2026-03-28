import {Component, Input} from '@angular/core';
import {Router} from '@angular/router';
import {NgForOf, NgIf} from '@angular/common';
import {ButtonDirective} from 'primeng/button';

@Component({
    selector: 'app-guide-state',
    standalone: true,
    imports: [
        NgIf,
        ButtonDirective,
        NgForOf
    ],
    templateUrl: './guide-state.component.html'
})
export class GuideStateComponent {

    @Input() icon: string = 'pi pi-info-circle';
    @Input() title!: string;
    @Input() subtitle?: string;

    // lista de instruções
    @Input() steps:  [boolean, string][] = [];

    // botão principal
    @Input() hasButtonLabel: boolean = false;
    @Input() buttonLabel?: string;
    @Input() buttonIcon: string = 'pi-arrow-right';
    @Input() buttonRoute?: string;

    // botão secundário (opcional)
    @Input() hasSecondaryLabel: boolean = false;
    @Input() secondaryLabel?: string;
    @Input() secondaryRoute?: string;
    @Input() secondaryIcon: string = 'pi-arrow-right';

    @Input() hasTertiaryLabel: boolean = false;
    @Input() tertiaryLabel?: string;
    @Input() tertiaryRoute?: string;
    @Input() tertiaryIcon: string = 'pi-arrow-right';

    constructor(private router: Router) {
    }

    navigate(route?: string) {
        if (!route) return;
        void this.router.navigate([route]);
    }

    get hasPrimary(): boolean {
        return !!this.buttonLabel;
    }

    get hasSecondary(): boolean {
        return !!this.secondaryLabel;
    }

    get hasTertiary(): boolean {
        return !!this.tertiaryLabel;
    }

    get primaryLevel(): 'primary' | 'secondary' | 'tertiary' {
        if (this.buttonLabel) return 'primary';
        if (this.secondaryLabel) return 'secondary';
        return 'tertiary';
    }

    get filteredSteps(): [boolean, string][] {
        return this.steps
            .filter(([condition]) => condition);
    }
}
