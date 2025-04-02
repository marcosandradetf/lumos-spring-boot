import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ufRequest} from '../uf-request.dto';
import {citiesRequest} from '../cities-request.dto';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private endpoint: string = environment.springboot + '/api/report';

  constructor(private http: HttpClient) {
  }

  generateReportPdf(html: string, title: string) {
    return this.http.post(`${this.endpoint}/pdf/generate/${title}`, html, {
      responseType: 'blob' // Garante que a resposta seja tratada como um arquivo
    });
  }



}
