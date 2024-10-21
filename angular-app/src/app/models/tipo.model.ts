import {Grupo} from './grupo.model';

export class Tipo {
  idTipo: number;
  nomeTipo: string;
  grupo: Grupo;

  constructor() {
    this.idTipo = 0;
    this.nomeTipo = "";
    this.grupo = new Grupo();
  }
}
