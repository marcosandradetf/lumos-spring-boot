import { Component } from '@angular/core';
import {Drawer} from 'primeng/drawer';
import {SharedState} from '../../../core/service/shared-state';
import {AsyncPipe, NgOptimizedImage} from '@angular/common';
import {SidebarComponent} from '../sidebar/sidebar.component';
import {Router} from '@angular/router';

@Component({
  selector: 'app-sidebar-drawer',
  standalone: true,
    imports: [
        Drawer,
        AsyncPipe,
        SidebarComponent,
        NgOptimizedImage
    ],
  templateUrl: './sidebar-drawer.component.html',
  styleUrl: './sidebar-drawer.component.scss'
})
export class SidebarDrawerComponent {

    protected readonly SharedState = SharedState;

    constructor(protected router: Router,) {

    }

    onDrawerChange(open: boolean) {
        SharedState.showMenuDrawer$.next(open);
    }
}
