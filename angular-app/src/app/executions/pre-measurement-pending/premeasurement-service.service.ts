import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {User} from '../../models/user.model';
import {Observable} from 'rxjs';
import * as http from 'node:http';
import {Deposit} from '../../models/almoxarifado.model';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PreMeasurementService {
  private endpoint = environment.springboot + '/api/execution';

  constructor(private http: HttpClient) {
  }

  getPreMeasurement(preMeasurementId: string) {
    return this.http.get<
      {
        preMeasurementId: number;
        city: string;
        createdBy: string;
        createdAt: string;
        preMeasurementType: string;
        preMeasurementStyle: string;
        teamName: string;
        totalPrice: string;

        streets: {
          preMeasurementStreetId: number;
          lastPower: string;
          latitude: number;
          longitude: number;
          address: string;

          items: {
            preMeasurementStreetItemId: number;
            materialId: number;
            materialName: string;
            materialType: string;
            materialPower: string;
            materialLength: string;
            materialQuantity: number;
          }[]

        }[];
      }>(`${this.endpoint + `/get-pre-measurements/${preMeasurementId}`}`);
  }

  getPreMeasurements(status: string): Observable<
    {
      preMeasurementId: number;
      city: string;
      createdBy: string;
      createdAt: string;
      preMeasurementType: string;
      preMeasurementStyle: string;
      teamName: string;

      streets: {
        preMeasurementStreetId: number;
        lastPower: string;
        latitude: number;
        longitude: number;
        address: string;

        items: {
          preMeasurementStreetItemId: number;
          materialId: number;
          materialName: string;
          materialType: string;
          materialPower: string;
          materialLength: string;
          materialQuantity: number;
        }[]

      }[];

    }[]> {
    return this.http.get<
      {
        preMeasurementId: number;
        city: string;
        createdBy: string;
        createdAt: string;
        preMeasurementType: string;
        preMeasurementStyle: string;
        teamName: string;

        streets: {
          preMeasurementStreetId: number;
          lastPower: string;
          latitude: number;
          longitude: number;
          address: string;

          items: {
            preMeasurementStreetItemId: number;
            materialId: number;
            materialName: string;
            materialType: string;
            materialPower: string;
            materialLength: string;
            materialQuantity: number;
          }[]

        }[];

      }[]>(`${this.endpoint + `/get-pre-measurements/${status}`}`);
  }

  getFields(preMeasurementId: number) {
    return this.http.get<
      {
        leds:
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }[];
        ledService: [
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }
        ];
        piService: [
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }
        ];
        arms:
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }[];
        armService: [
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }
        ];
        screws: [
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }
        ];
        straps: [
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }
        ];
        relays: [
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }
        ];
        connectors: [
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }
        ];
        cables: [
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }
        ];
        posts: [
          {
            description: string;
            quantity: number;
            price: string;
            priceTotal: string;
          }
        ];
      }

    >(`${this.endpoint + `/get-fields/${preMeasurementId}`}`);
  }


  savePremeasurementValues(
    values: {
      leds:
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }[];
      ledService: [
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }
      ];
      piService: [
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }
      ];
      arms:
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }[];
      armService: [
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }
      ];
      screws: [
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }
      ];
      straps: [
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }
      ];
      relays: [
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }
      ];
      connectors: [
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }
      ];
      cables: [
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }
      ];
      posts: [
        {
          description: string;
          quantity: number;
          price: string;
          priceTotal: string;
        }
      ];
    },
    preMeasurementId: number) {
    return this.http.post(`${this.endpoint + `/save-pre-measurement-values/${preMeasurementId}`}`, values);
  }


  saveHTMLReport(html: string, preMeasurementId: number) {
    return this.http.post(`${this.endpoint + `/save-pre-measurement-html-report/${preMeasurementId}`}`, html);
  }
}
