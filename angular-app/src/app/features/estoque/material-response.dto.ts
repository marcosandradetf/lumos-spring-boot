import {Type} from '../../core/models/tipo.model';
import {Group} from '../../core/models/grupo.model';
import {Company} from '../../core/models/empresa.model';
import {Deposit} from '../../core/models/almoxarifado.model';

export interface MaterialResponse {
  selected: boolean;
  idMaterial: number;
  materialName: string;
  materialBrand: string;
  buyUnit: string;
  requestUnit: string;
  materialType: string;
  materialGroup: string;
  stockQt: number | null;
  inactive: boolean;
  company: string;
  deposit: string;
}
