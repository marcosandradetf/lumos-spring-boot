import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileServerService {
  private apiUrl = 'http://localhost:8081/download/';  // URL do seu servidor (ajuste conforme necessário)

  constructor(private http: HttpClient) { }

  // Função para baixar o arquivo
  downloadFile(filename: string): Observable<Blob> {
    const url = `${this.apiUrl}${filename}`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
