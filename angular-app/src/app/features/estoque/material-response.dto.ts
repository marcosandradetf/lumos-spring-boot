import {Tipo} from '../../core/models/tipo.model';
import {Grupo} from '../../core/models/grupo.model';
import {Empresa} from '../../core/models/empresa.model';
import {Almoxarifado} from '../../core/models/almoxarifado.model';

export interface MaterialResponse {
  idMaterial: number;
  nomeMaterial: string;
  marcaMaterial: string;
  unidadeCompra: string;
  unidadeRequisicao: string;
  tipoMaterial: string;
  grupoMaterial: string;
  qtdeEstoque: number | null;
  inativo: boolean;
  empresa: string;
  almoxarifado: string;

}
