import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import { Material } from '../models/material.model';
import {Grupo} from '../models/grupo.model';
import {Tipo} from '../models/tipo.model';
import {Empresa} from '../models/empresa.model';
import {AuthService} from '../core/service/auth.service';


@Injectable({
  providedIn: 'root'
})
export class MaterialService {
  private apiUrl = 'http://localhost:8080/api/material';
  private materiaisSubject: BehaviorSubject<Material[]> = new BehaviorSubject<Material[]>([]);
  public materiais$: Observable<Material[]> = this.materiaisSubject.asObservable();

  constructor(private http: HttpClient, private authService: AuthService) { }

  getAll(): void {
    this.http.get<Material[]>(`${this.apiUrl}`)
    .subscribe((data: Material[]) => {
      this.materiaisSubject.next(data);
    })
  }

  create(material: Material): Observable<Material> {
    return this.http.post<Material>(`${this.apiUrl}`, material);
  }

  // Atualiza a lista de materiais local
  addMaterialFetch(material: Material): void {
    const currentMaterials = this.materiaisSubject.value;
    this.materiaisSubject.next([...currentMaterials, material]);
  }

  updateMaterial(id: number, material: Material): Observable<Material> {
    return this.http.put<Material>(`${this.apiUrl}/${id}`, material);
  }

  // Atualizar materiais localmente
  updateMaterialFetch(updatedMaterial: Material): void {
    const currentMaterials = this.materiaisSubject.value;
    const updatedMaterials = currentMaterials.map(material =>
      material.idMaterial === updatedMaterial.idMaterial ? updatedMaterial : material
    );
    this.materiaisSubject.next(updatedMaterials);
  }

  deleteMaterial(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Atualizar materiais localmente
  deleteMaterialFetch(idMaterial: number): void {
    const currentMaterials = this.materiaisSubject.value;
    const updatedMaterials = currentMaterials.filter(material => material.idMaterial !== idMaterial);
    this.materiaisSubject.next(updatedMaterials);
  }

}
