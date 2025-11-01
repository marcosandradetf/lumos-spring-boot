import {Injectable} from '@angular/core';
import {environment} from '../../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {CompanyRequest, CompanyResponse} from '../dto/company.dto';

@Injectable({
  providedIn: 'root'
})
export class CompanyService {
  private endpoint = environment.springboot + '/api';

  constructor(private http: HttpClient) { }

  getCompanies() {
    return this.http.get<CompanyResponse[]>(`${this.endpoint}/company`);
  }

  create(company: CompanyRequest, logo: File) {
    const formData = new FormData();

    formData.append('logo', logo);

    formData.append('company', new Blob(
      [JSON.stringify(company)],
      { type: 'application/json' }
    ));

    return this.http.post<number>(`${this.endpoint}/company/create`, formData);
  }

}
