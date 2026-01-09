import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import {MaterialFormDTO, MaterialStockResponse} from '../../models/material-response.dto';
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

  public currentPage: number = 0;     // ← corresponde a "page"
  public pageSize: number = 15;       // ← mais claro que "rows"
  public totalElements: number = 0;   // ← corresponde a "totalElements"
  public totalPages: number = 0;      // ← corresponde a "totalPages"
  public isLastPage: boolean = false; // ← corresponde a "last"

  constructor(private http: HttpClient) {
  }


  getMaterials(page: number, size: number) {
    const params = new HttpParams()
      .set('depositId', page.toString())
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(this.apiUrl, {params});
  }

  getBySearch(page: number,depositId: number, search: string) {
    let params = new HttpParams()
      .set('depositId', depositId)
      .set('name', search)
      .set('page', page.toString())
      .set('size', this.pageSize.toString());

    return this.http.get<any>(this.apiUrl, {params});
  }

  create(material: MaterialFormDTO) {
    return this.http.post(`${this.apiUrl}/create`, material);
  }

  findByBarCode(barcode: string): Observable<MaterialFormDTO> {
    return this.http.get<MaterialFormDTO>(`${this.apiUrl}/find-by-barcode?barcode=${barcode}`);
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
