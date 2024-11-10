
export class CreateMaterialRequest {
  nomeMaterial: string;
  marcaMaterial: string;
  unidadeCompra: string;
  unidadeRequisicao: string;
  tipoMaterial: number;
  qtdeEstoque: number | null;
  inativo: boolean;
  empresa: number;
  almoxarifado: number;

  constructor() {
    this.nomeMaterial = '';
    this.marcaMaterial = '';
    this.unidadeCompra = '';
    this.unidadeRequisicao = '';
    this.tipoMaterial = 0;
    this.qtdeEstoque = null;
    this.inativo = false;
    this.empresa = 0;
    this.almoxarifado = 0;
  }
}
