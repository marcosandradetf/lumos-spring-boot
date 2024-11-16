import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ufRequest} from '../uf-request.dto';
import {citiesRequest} from '../cities-request.dto';

@Injectable({
  providedIn: 'root'
})
export class IbgeService {
  private endpoint: string = 'https://servicodados.ibge.gov.br/api/v1/localidades'

  constructor(private http: HttpClient) { }

  getUfs(){
    return this.http.get<ufRequest[]>(this.endpoint + '/estados/?orderBy=nome');
  }

  getCities(uf: string){
    return this.http.get<citiesRequest[]>(this.endpoint + '/estados/' + uf + '/municipios/?orderBy=nome');
  }

}
