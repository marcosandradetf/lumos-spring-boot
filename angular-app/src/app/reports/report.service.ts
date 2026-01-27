import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient, HttpParams} from '@angular/common/http';

@Injectable({
    providedIn: 'root'
})
export class ReportService {
    private endpoint = environment.springboot;

    constructor(private http: HttpClient) {
    }

    public getMaintenancePdf(maintenanceId: string, type: string) {
        return this.http.post(
            `${this.endpoint}/api/maintenance/generate-report/${type}/${maintenanceId}`,
            null,
            {
                responseType: 'blob',
                observe: 'response' // 👈 ESSENCIAL
            }
        );
    }


    public getInstallationPdf(executionId: number, type: string) {
        return this.http.post(
            `${this.endpoint}/api/execution/generate-report/${type}/${executionId}`,
            null,
            {
                responseType: 'blob',
                observe: 'response'
            }
        );
    }

    public getFinishedMaintenances() {
        return this.http.get<any[]>(`${this.endpoint}/api/maintenance/get-finished`);
    }

    public getFinishedInstallations() {
        return this.http.get<any[]>(`${this.endpoint}/api/execution/get-finished`);
    }


    archiveOrDelete(maintenanceId: string, action: string) {
        return this.http.post(`${this.endpoint}/api/maintenance/archive-or-delete`, {
            maintenanceId: maintenanceId,
            action: action
        });
    }

}
