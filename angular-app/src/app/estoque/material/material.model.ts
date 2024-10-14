export interface Material {
  idMaterial: number;
  nomeMaterial: string;
  unidadeCompra: string;
  unidadeRequisicao: string;
  tipoMaterial: {
    nomeTipo: string; // Adicione o nome do Tipo
  };
  idGrupo: number;
  qtdeEstoque: number;
  inativo: boolean;
  idEmpresa: number;
  idAlmoxarifado: number;
}

