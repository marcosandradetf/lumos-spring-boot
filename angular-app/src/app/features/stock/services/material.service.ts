import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import { MaterialResponse } from '../material-response.dto';
import {Group} from '../../../core/models/grupo.model';
import {Type} from '../../../core/models/tipo.model';
import {Company} from '../../../core/models/empresa.model';
import {AuthService} from '../../../core/auth/auth.service';
import {CreateMaterialRequest} from '../create-material-request.dto';


@Injectable({
  providedIn: 'root'
})
export class MaterialService {
  private apiUrl = 'http://localhost:8080/api/material';
  private materialsSubject: BehaviorSubject<MaterialResponse[]> = new BehaviorSubject<MaterialResponse[]>([]);
  public materials$: Observable<MaterialResponse[]> = this.materialsSubject.asObservable();
  public totalPages: number = 0;
  public pages: number[] = [];
  private material: MaterialResponse | null = null;

  constructor(private http: HttpClient) { }

  getFetch(page: string, size: string): void {
    let params = new HttpParams().set('page', page).set('size', size);

    this.http.get<{ content: MaterialResponse[], totalPages: number, currentPage: number }>(`${this.apiUrl}`, { params })
      .subscribe(response => {
        this.materialsSubject.next(response.content); // Atualiza o conteúdo
        this.totalPages = response.totalPages;
        this.pages = Array.from({ length: this.totalPages }, (_, i) => i);
      });
  }


  getBySearch(page: string, size: string, search: string){
    let params = new HttpParams().set('name', search).set('page', page).set('size', size);
    this.http.get<{ content: MaterialResponse[], totalPages: number, currentPage: number }>(`${this.apiUrl}/search`, { params })
      .subscribe(response => {
        this.materialsSubject.next(response.content); // Atualiza o conteúdo
        this.totalPages = response.totalPages;
        this.pages = Array.from({ length: this.totalPages }, (_, i) => i);
      });
  }

  create(material: CreateMaterialRequest): Observable<CreateMaterialRequest> {
    return this.http.post<CreateMaterialRequest>(`${this.apiUrl}`, material);
  }

  // Atualiza a lista de materiais local
  addMaterialFetch(material: any): void {
    const currentMaterials = this.materialsSubject.value;
    this.materialsSubject.next([...currentMaterials, material]);
  }

  updateMaterial(id: number, material: MaterialResponse): Observable<MaterialResponse> {
    return this.http.put<MaterialResponse>(`${this.apiUrl}/${id}`, material);
  }

  // Atualizar materiais localmente
  updateMaterialFetch(updatedMaterial: MaterialResponse): void {
    const currentMaterials = this.materialsSubject.value;
    const updatedMaterials = currentMaterials.map(material =>
      material.idMaterial === updatedMaterial.idMaterial ? updatedMaterial : material
    );
    this.materialsSubject.next(updatedMaterials);
  }

  deleteMaterial(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, {
      withCredentials: true,
      responseType: 'text'
    });
  }

  // Atualizar materiais localmente
  deleteMaterialFetch(idMaterial: number): void {
    const currentMaterials = this.materialsSubject.value;
    const updatedMaterials = currentMaterials.filter(material => material.idMaterial !== idMaterial);
    this.materialsSubject.next(updatedMaterials);
  }

  getMaterialsByDeposit(page: string, size: string, depositId: string[]) {
    let params = new HttpParams().set('page', page).set('size', size);
    depositId.forEach(id => {
      params = params.append('deposit', id);
    });

    this.http.get<{ content: MaterialResponse[], totalPages: number, currentPage: number }>(`${this.apiUrl}/filter-by-deposit`, { params })
      .subscribe(response => {
        this.materialsSubject.next(response.content); // Atualiza o conteúdo
        this.totalPages = response.totalPages;
        this.pages = Array.from({ length: this.totalPages }, (_, i) => i);
      });

  }

  getById(id: number) {
    return this.http.get<string>(`${this.apiUrl}/${id}`);
  }

  setMaterial(pIdMaterial: number) {
    t
  }

  getMaterial() {
    return this.material;
  }

}
