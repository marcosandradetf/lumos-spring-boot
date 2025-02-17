import { Injectable } from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import * as http from 'node:http';

@Injectable({
  providedIn: 'root'
})
export class ExecutionService {
  private baseUrl = environment.springboot;

  constructor(private http: HttpClient) { }

  public getMeasurements() {
    return this.http.get<
      {
        measurement: {
          measurementId: number,
          latitude: number,
          longitude: number,
          address: string,
          city: string,
          depositId: number,
          deviceId: string,
          depositName: string,
          measurementType: string,
          measurementStyle: string,
          createdBy: string
        },
        items: {
          materialId: string,
          materialQuantity: number,
          lastPower: string,
          measurementId: number,
          material: string,
        }[]
      }[]
    >(this.baseUrl + '/api/execution/measurements');
  }

}
