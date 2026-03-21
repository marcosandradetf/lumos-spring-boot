import {Injectable} from '@angular/core';
import {environment} from '../../../environments/environment';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';

@Injectable({
    providedIn: 'root'
})
export class ServiceRequestService {
    private baseUrl = environment.springboot;

    constructor(private http: HttpClient) {
    }

    hasContractActive(ibgeCode: string) {
        const headers = new HttpHeaders({
            'X-App-Client': 'lumos-web',
            'X-App-Version': '1.0.0'
        });

        const params = new HttpParams().set('ibgeCode', ibgeCode);

        return this.http.get<any>(
            `${this.baseUrl}/public/contracts/verify`,
            {
                headers,
                params
            }
        );
    }

}
