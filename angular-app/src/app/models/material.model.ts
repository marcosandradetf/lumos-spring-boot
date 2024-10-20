import {Tipo} from './tipo.model';
import {Grupo} from './grupo.model';
import {Empresa} from './empresa.model';
import {Almoxarifado} from './almoxarifado.model';

export class Material {
  idMaterial: number;
  nomeMaterial: string;
  marcaMaterial: string;
  unidadeCompra: string;
  unidadeRequisicao: string;
  tipoMaterial: Tipo;
  grupoMaterial: Grupo;
  qtdeEstoque: number;
  inativo: boolean;
  empresa: Empresa;
  almoxarifado: Almoxarifado;

  constructor() {
    this.idMaterial = 0;
    this.nomeMaterial = '';
    this.marcaMaterial = '';
    this.unidadeCompra = '';
    this.unidadeRequisicao = '';
    this.tipoMaterial = new Tipo();
    this.grupoMaterial = new Grupo();
    this.qtdeEstoque = 0;
    this.inativo = false;
    this.empresa = new Empresa();
    this.almoxarifado = new Almoxarifado();
  }
}
