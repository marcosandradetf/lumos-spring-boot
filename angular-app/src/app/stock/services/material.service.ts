import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import {MaterialFormDTO, MaterialResponse} from '../../models/material-response.dto';
import {CreateMaterialRequest} from '../../models/create-material-request.dto';
import {environment} from '../../../environments/environment';

export enum State {
  create,
  update,
}

@Injectable({
  providedIn: 'root'
})
export class MaterialService {
  private apiUrl = environment.springboot + '/api/material';
  private materialsSubject: BehaviorSubject<MaterialResponse[]> = new BehaviorSubject<MaterialResponse[]>([]);
  public materials$: Observable<MaterialResponse[]> = this.materialsSubject.asObservable();

  public currentPage: number = 0;     // ← corresponde a "page"
  public pageSize: number = 15;       // ← mais claro que "rows"
  public totalElements: number = 0;   // ← corresponde a "totalElements"
  public totalPages: number = 0;      // ← corresponde a "totalPages"
  public isLastPage: boolean = false; // ← corresponde a "last"


  materialSubject: BehaviorSubject<CreateMaterialRequest> = new BehaviorSubject<CreateMaterialRequest>({
    buyUnit: '',
    deposit: '',
    inactive: false,
    allDeposits: false,
    materialBrand: '',

    materialPower: '',
    materialAmps: '',
    materialLength: '',

    materialName: '',
    materialType: '',
    requestUnit: '',
  });
  public material$: Observable<CreateMaterialRequest> = this.materialSubject.asObservable();

  public stateSubject: BehaviorSubject<State> = new BehaviorSubject<State>(State.create);
  private materialId: number = 0;

  constructor(private http: HttpClient) {
  }

  getFetch(page: number, depositId?: number): void {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', this.pageSize.toString());

    if (depositId !== undefined) {
      params = params.set('depositId', depositId.toString());
    }

    this.http.get<{
      content: MaterialResponse[],
      totalPages: number,
      totalElements: number,
      page: number,
      size: number,
      last: boolean
    }>(`${this.apiUrl}`, { params })
      .subscribe(response => {
        this.materialsSubject.next(response.content);
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.currentPage = response.page;
        this.pageSize = response.size;
        this.isLastPage = response.last;
      });
  }

  getBySearch(page: number, search: string, depositId: number | null = null) {
    let params = new HttpParams()
      .set('name', search)
      .set('page', page.toString())
      .set('size', this.pageSize.toString());

    if (depositId !== null) {
      params = params.set('depositId', depositId.toString());
    }

    this.http.get<{
      content: MaterialResponse[],
      totalPages: number,
      totalElements: number,
      page: number,
      size: number,
      last: boolean
    }>(`${this.apiUrl}/search`, { params })
      .subscribe(response => {
        this.materialsSubject.next(response.content);
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.currentPage = response.page;
        this.pageSize = response.size;
        this.isLastPage = response.last;
      });
  }

  create(material: MaterialFormDTO) {
    return this.http.post(`${this.apiUrl}/create`, material);
  }

  findByBarCode(barcode: string): Observable<MaterialFormDTO> {
    return this.http.get<MaterialFormDTO>(`${this.apiUrl}/find-by-barcode?barcode=${barcode}`);
  }

  // Atualiza a lista de materiais local
  addMaterialFetch(materials: MaterialResponse[]): void {
    materials.forEach(material => {
      const currentMaterials = this.materialsSubject.value;
      this.materialsSubject.next([...currentMaterials, material]);
    });
  }

  updateMaterial(material: CreateMaterialRequest): Observable<MaterialResponse[]> {
    return this.http.put<MaterialResponse[]>(`${this.apiUrl}/${this.materialId}`, material);
  }

  // Atualizar materiais localmente
  updateMaterialFetch(materialsResponse: MaterialResponse[]): void {
    const currentMaterials = this.materialsSubject.value;
    materialsResponse.forEach(updatedMaterial => {
      const updatedMaterials = currentMaterials.map(material =>
        material.idMaterial === updatedMaterial.idMaterial ? updatedMaterial : material
      );
      this.materialsSubject.next(updatedMaterials);
    });

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


  getById(id: number) {
    return this.http.get<string>(`${this.apiUrl}/${id}`);
  }

  setMaterial(_material: MaterialResponse) {
    const material: CreateMaterialRequest = {
      buyUnit: _material.buyUnit,
      deposit: '',
      inactive: _material.inactive,
      allDeposits: false,

      materialBrand: _material.materialBrand,
      materialPower: _material.materialPower,
      materialAmps: _material.materialAmps,
      materialLength: _material.materialLength,
      materialName: _material.materialName,
      materialType: '',
      requestUnit: _material.requestUnit
    }
    this.materialId = _material.idMaterial;
    this.materialSubject.next(material);
  }

  getMaterialObservable() {
    return this.materialSubject.asObservable();
  }

  getState() {
    return this.stateSubject.value;
  }

  resetObject() {
    const material: CreateMaterialRequest = {
      buyUnit: '',
      deposit: '',
      inactive: false,
      allDeposits: false,
      materialBrand: '',

      materialPower: '',
      materialAmps: '',
      materialLength: '',

      materialName: '',
      materialType: '',
      requestUnit: '',
    };
    this.materialSubject.next(material);// Reseta a instância do material
  }

  setState(state: State) {
    this.stateSubject.next(state);
  }

  importData(
    materials: {
      materialName: '',
      materialBrand: '',
      materialPower: '',
      materialAmps: '',
      materialLength: '',
      buyUnit: '',
      requestUnit: '',
      materialTypeName: '',
      materialGroupName: '',
      companyName: '',
      depositName: '',
    }[]
  ) {
    return this.http.post(this.apiUrl + "/import", materials);
  }
}
