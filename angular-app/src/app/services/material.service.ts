import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Material } from '../models/material.model';


@Injectable({
  providedIn: 'root'
})
export class MaterialService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  getTipos() {
    return this.http.get<any[]>(`${this.apiUrl}/tipo`);
  }

  getGrupos() {
    return this.http.get<any[]>(`${this.apiUrl}/grupo`);
  }

  getEmpresas() {
    return this.http.get<any[]>(`${this.apiUrl}/empresa`);
  }

  getAlmoxarifados() {
    return this.http.get<any[]>(`${this.apiUrl}/almoxarifado`);
  }

  getAll(): Observable<Material[]> {
    return this.http.get<Material[]>(`${this.apiUrl}/material`);
  }

  getById(id: number): Observable<Material> {
    return this.http.get<Material>(`${this.apiUrl}/material/${id}`);
  }

  create(material: Material): Observable<Material> {
    return this.http.post<Material>(`${this.apiUrl}/material`, material);
  }

  updateMaterial(id: number, material: Material): Observable<Material> {
    return this.http.put<Material>(`${this.apiUrl}/material/${id}`, material);
  }

  deleteMaterial(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/material/${id}`);
  }

}
