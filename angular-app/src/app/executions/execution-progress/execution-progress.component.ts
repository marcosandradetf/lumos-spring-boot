import { Component } from '@angular/core';
import {Title} from '@angular/platform-browser';
import {SharedState} from '../../core/service/shared-state';

@Component({
  selector: 'app-execution-progress',
  standalone: true,
  imports: [],
  templateUrl: './execution-progress.component.html',
  styleUrl: './execution-progress.component.scss'
})
export class ExecutionProgressComponent {
    constructor(private title: Title) {
        this.title.setTitle('Instações em Curso');
        SharedState.setCurrentPath(["Instalações","Em Curso"]);
    }
}
