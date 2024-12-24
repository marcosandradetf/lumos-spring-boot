

export interface ItemRequest {
  selected: boolean;
  idMaterial: number;
  nomeMaterial: string;
  marcaMaterial: string;
  unidadeRequisicao: string;
  tipoMaterial: string;
  qtdeEstoque: number | null;
  almoxarifado: string;
  valor: string;
}
