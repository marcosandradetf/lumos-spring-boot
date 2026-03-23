import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient, HttpParams, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';

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


    public getInstallationPdf(execution: any, type: string) {
        return this.http.post(
            `${this.endpoint}/api/execution/generate-report/${type}/${execution.installation_id}/${execution.installation_type}`,
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


    // 🔒 chamadas tipadas corretamente
    private getReportJson(filters: any) {
        return this.http.post<any[]>(
            `${this.endpoint}/api/report/execution/generate-report`,
            filters
        );
    }

    private getReportPdf(filters: any) {
        return this.http.post(
            `${this.endpoint}/api/report/execution/generate-report`,
            filters,
            {
                responseType: 'blob',
                observe: 'response'
            }
        );
    }

    // 🌟 função pública
    getReport(filters: any): Observable<any[] | HttpResponse<Blob>> {
        if (filters.viewMode === 'GROUP') {
            return this.getReportPdf(filters) as Observable<HttpResponse<Blob>>;
        }

        return this.getReportJson(filters) as Observable<any[]>;
    }

    getContracts() {
        return this.http.get<any[]>(
            `${this.endpoint}/api/report/execution/get-contracts`
        );
    }


    getMaterialReservationReport(filters: any) {
        return this.http.post(
            `${this.endpoint}/api/report/stock/generate-material-reservation-report`,
            filters, // body vazio
            {
                responseType: 'blob',
                observe: 'response'
            }
        ) as Observable<HttpResponse<Blob>>;
    }

    generateOperationalReport(filters: any) {
        return this.http.post(
            `${this.endpoint}/api/report/execution/generate-operational-report`,
            filters,
            {
                responseType: 'blob',
                observe: 'response'
            }
        );
    }


}
