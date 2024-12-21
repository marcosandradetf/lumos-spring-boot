import {Component, OnInit} from '@angular/core';
import {NgClass, NgForOf} from "@angular/common";
import {DeleteMaterialModalComponent} from '../../../../../shared/components/modal-delete/delete.component';
import {MaterialResponse} from '../../../material-response.dto';
import {MaterialService} from '../../../services/material.service';
import { tap, catchError, of } from 'rxjs';
import { AlertMessageComponent } from '../../../../../shared/components/alert-message/alert-message.component';
import {ModalComponent} from '../../../../../shared/components/modal/modal.component';
import {ButtonComponent} from '../../../../../shared/components/button/button.component';
import {Router} from '@angular/router';
import {MaterialFormComponent} from '../material-form/material-form.component';
import {CreateMaterialRequest} from '../../../create-material-request.dto';

@Component({
  selector: 'app-tabela',
  standalone: true,
  imports: [
    NgForOf,
    NgClass,
    AlertMessageComponent,
    ModalComponent,
    ButtonComponent,
    MaterialFormComponent
  ],
  templateUrl: './tabela.component.html',
  styleUrl: './tabela.component.scss'
})
export class TabelaComponent implements OnInit {
  materials: MaterialResponse[] = [];
  currentPage: string = "0";
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

    this.materialService.getFetch(this.currentPage, "20");
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
    this.openUpdateModal = true;
    this.getMaterial();
    if (this.material) this.materialService.setMaterial(this.idMaterial);
  }

  getMaterial(): void {

  }

  changePage(page: number): void {
    if (page.toString() !== this.currentPage) {
      this.currentPage = page.toString();
      this.materialService.getFetch(this.currentPage, "20");
    }
  }

  submitDeleteMaterial() {
    this.deleteMaterial();
  }
}
