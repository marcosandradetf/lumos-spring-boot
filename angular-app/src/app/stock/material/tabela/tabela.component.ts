import {Component, OnInit} from '@angular/core';
import {NgClass, NgForOf} from "@angular/common";
import {catchError, of, tap} from 'rxjs';
import {Router} from '@angular/router';
import {MaterialFormComponent} from '../material-form/material-form.component';
import {AlertMessageComponent} from '../../../shared/components/alert-message/alert-message.component';
import {ModalComponent} from '../../../shared/components/modal/modal.component';
import {ButtonComponent} from '../../../shared/components/button/button.component';
import {MaterialResponse} from '../../../models/material-response.dto';
import {MaterialService, State} from '../../services/material.service';
import {ReactiveFormsModule} from "@angular/forms";
import {Paginator} from 'primeng/paginator';

@Component({
  selector: 'app-tabela',
  standalone: true,
  imports: [
    NgForOf,
    NgClass,
    AlertMessageComponent,
    ModalComponent,
    ButtonComponent,
    MaterialFormComponent,
    ReactiveFormsModule,
    Paginator
  ],
  templateUrl: './tabela.component.html',
  styleUrl: './tabela.component.scss'
})
export class TabelaComponent implements OnInit {
  materials: MaterialResponse[] = [];
  currentPage: number = 0;
  serverMessage: string | null = null;
  alertType: string | null = null;
  idMaterial: number = 0;
  openUpdateModal: boolean = false;
  protected readonly parseInt = parseInt;
  openConfirmationModal: boolean = false;
  material: any = null;

  constructor(protected materialService: MaterialService, private router: Router,
              ) {
  }

  ngOnInit() {
    this.loadMateriais()
  }

  loadMateriais(): void {
    this.materialService.materials$.subscribe((materiais: MaterialResponse[]) => {
      this.materials = materiais;
    });

    this.materialService.getFetch(this.currentPage, 20);
  }

  deleteMaterial(): void {
    this.materialService.deleteMaterial(this.idMaterial).pipe(
      tap(() => {
        this.serverMessage = "Material removido com sucesso!";
        this.alertType = "alert-success";
        this.materialService.deleteMaterialFetch(this.idMaterial);
      }),
      catchError((error) => {
        this.openConfirmationModal = false;
        this.serverMessage = error.error || "Erro ao remover material.";
        this.alertType = "alert-error";
        return of(null);
      })
    ).subscribe();
  }

  updateMaterial(pIdmaterial: number): void {
    this.idMaterial = pIdmaterial;
    const material: MaterialResponse | undefined = this.materials.find(m => m.idMaterial === pIdmaterial);
    if (material !== undefined) {
      this.materialService.setState(State.update)
      this.materialService.setMaterial(material);
      this.openUpdateModal = true;
    }
  }


  submitDeleteMaterial() {
    this.deleteMaterial();
  }

  protected readonly State = State;

  closeUpdateModal() {
    this.openUpdateModal = false;
    this.materialService.setState(State.create);
    this.materialService.resetObject();
  }

  onPageChange(page: number | undefined) {
    if (page) {
      this.materialService.getFetch(this.currentPage, 20);
    }
  }


}
