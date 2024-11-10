import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import { MaterialResponse } from '../material-response.dto';
import {Grupo} from '../../../core/models/grupo.model';
import {Tipo} from '../../../core/models/tipo.model';
import {Empresa} from '../../../core/models/empresa.model';
import {AuthService} from '../../../core/auth/auth.service';
import {CreateMaterialRequest} from '../create-material-request.dto';


@Injectable({
  providedIn: 'root'
})
export class MaterialService {
  private apiUrl = 'http://localhost:8080/api/material';
  private materiaisSubject: BehaviorSubject<MaterialResponse[]> = new BehaviorSubject<MaterialResponse[]>([]);
  public materiais$: Observable<MaterialResponse[]> = this.materiaisSubject.asObservable();

  constructor(private http: HttpClient) { }

  getFetch(): void {
    this.http.get<MaterialResponse[]>(`${this.apiUrl}`)
    .subscribe((data: MaterialResponse[]) => {
      this.materiaisSubject.next(data);
    })
  }

  getAll(){
    return this.http.get<MaterialResponse[]>(`${this.apiUrl}`);
  }

  create(material: CreateMaterialRequest): Observable<CreateMaterialRequest> {
    return this.http.post<CreateMaterialRequest>(`${this.apiUrl}`, material);
  }

  // Atualiza a lista de materiais local
  addMaterialFetch(material: any): void {
    const currentMaterials = this.materiaisSubject.value;
    this.materiaisSubject.next([...currentMaterials, material]);
  }

  updateMaterial(id: number, material: MaterialResponse): Observable<MaterialResponse> {
    return this.http.put<MaterialResponse>(`${this.apiUrl}/${id}`, material);
  }

  // Atualizar materiais localmente
  updateMaterialFetch(updatedMaterial: MaterialResponse): void {
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
