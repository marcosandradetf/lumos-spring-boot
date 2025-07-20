import { Injectable } from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient, HttpParams} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private puppeteer = environment.puppeteer;
  private maintenance = environment.maintenance;

  constructor(private http: HttpClient) {
  }

  public getPDF(maintenanceId: string, streetIds: number[], type: string) {
    let params = new HttpParams();
    streetIds.forEach(id => {
      params = params.append('streets', id.toString());
    });

    return this.http.post(`${this.puppeteer}/generate-maintenance-pdf/${maintenanceId}/${type}`, null, {
      params,
      responseType: 'blob'  // importante para receber arquivo binário
    });
  }

  public getFinishedMaintenances() {
    return this.http.get<any[]>(`${this.maintenance}/api/maintenance/get-finished`);
  }


}
