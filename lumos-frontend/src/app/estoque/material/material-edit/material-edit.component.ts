import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {Material} from '../material.model';
import {MaterialService} from '../material.service';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';


@Component({
  selector: 'app-material-edit',
  standalone: true,
  imports: [CommonModule, FormsModule], // Adicione isso
  templateUrl: './material-edit.component.html',
  styleUrl: './material-edit.component.scss'
})
export class MaterialEditComponent implements OnInit {
  material: Material | null = null;

  constructor(
    private materialService: MaterialService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.materialService.getById(id).subscribe(data => {
      this.material = data;
    });
  }

  updateMaterial(): void {
    if (this.material) {
      this.materialService.updateMaterial(this.material.idMaterial, this.material).subscribe(() => {
        this.router.navigate(['/materiais']);
      });
    }
  }
}
