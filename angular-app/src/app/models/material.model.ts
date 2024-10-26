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
  qtdeEstoque: number | null;
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
    this.qtdeEstoque = null;
    this.inativo = false;
    this.empresa = new Empresa();
    this.almoxarifado = new Almoxarifado();
  }
}
