import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import { MaterialResponse } from '../material-response.dto';
import {Grupo} from '../../../core/models/grupo.model';
import {Tipo} from '../../../core/models/tipo.model';
import {Empresa} from '../../../core/models/empresa.model';
import {AuthService} from '../../../core/auth/auth.service';


@Injectable({
  providedIn: 'root'
})
export class EstoqueService {
  private apiUrl = 'http://localhost:8080/api';
  private onPathSubject = new BehaviorSubject<string>(''); // Inicializa o caminho
  private onPathSideBarSubject = new BehaviorSubject<string>(''); // Inicializa o caminho
  onPath$ = this.onPathSubject.asObservable(); // Observable para se inscrever
  onPathSideBar$ = this.onPathSideBarSubject.asObservable();

  setPath(path: string): void {
    this.onPathSubject.next(path); // Atualiza o valor do BehaviorSubject
  }

  setPathSideBar(path: string): void {
    this.onPathSideBarSubject.next(path); // Atualiza o valor do BehaviorSubject
  }

  constructor(private http: HttpClient, private authService: AuthService) { }

  getTipos() {
    return this.http.get<Tipo[]>(`${this.apiUrl}/tipo`);
  }

  getGrupos() {
    return this.http.get<Grupo[]>(`${this.apiUrl}/grupo`);
  }

  getEmpresas() {
    return this.http.get<Empresa[]>(`${this.apiUrl}/empresa`);
  }

  getAlmoxarifados() {
    return this.http.get<any[]>(`${this.apiUrl}/almoxarifado`);
  }


}
