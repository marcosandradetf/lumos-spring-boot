import { Component } from '@angular/core';
import {Title} from '@angular/platform-browser';
import { PrimeBreadcrumbComponent } from "../shared/components/prime-breadcrumb/prime-breadcrumb.component";

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [PrimeBreadcrumbComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {

  constructor(private titleService: Title) {
    this.titleService.setTitle("Lumos");
  }

}
