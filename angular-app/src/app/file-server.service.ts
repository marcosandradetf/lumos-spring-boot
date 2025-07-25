import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FileServerService {
  private apiUrl = environment.springboot +  '/download/';  // URL do seu servidor (ajuste conforme necessário)

  constructor(private http: HttpClient) { }

  // Função para baixar o arquivo
  downloadFile(filename: string): Observable<Blob> {
    const url = `${this.apiUrl}${filename}`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
