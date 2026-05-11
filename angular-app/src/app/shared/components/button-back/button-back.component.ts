import {Component, Input} from '@angular/core';
import {RouterLink} from '@angular/router';
import {NgIf} from '@angular/common';

@Component({
    selector: 'app-button-back',
    standalone: true,
    imports: [
        RouterLink,
        NgIf
    ],
    templateUrl: './button-back.component.html',
    styleUrl: './button-back.component.scss'
})
export class ButtonBackComponent {
    @Input() origin: {
        label: string,
        data: any,
        route: string,
        query: any,
        attribute: string,
    } | null = null;

    get navigationState() {
        if (!this.origin) return {};

        return {
            [this.origin.attribute ?? 'data']: this.origin.data
        };
    }

}
