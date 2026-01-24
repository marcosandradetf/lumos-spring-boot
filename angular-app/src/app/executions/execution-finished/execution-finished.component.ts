import { Component } from '@angular/core';
import {SharedState} from '../../core/service/shared-state';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-execution-finished',
  standalone: true,
  imports: [],
  templateUrl: './execution-finished.component.html',
  styleUrl: './execution-finished.component.scss'
})
export class ExecutionFinishedComponent {
    constructor(private title: Title) {
        this.title.setTitle("Execuções - Finalizadas")
        SharedState.setCurrentPath(["Execuções","Finalizadas"]);
    }
}
