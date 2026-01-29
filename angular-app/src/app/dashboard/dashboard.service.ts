import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class DashboardService {
    private baseUrl = environment.springboot + "/api/metrics";

    constructor(
        private http: HttpClient
    ) {
    }

    getMetrics() {
        return this.http.get<any[]>(this.baseUrl + '/get-metrics');
    }


}
