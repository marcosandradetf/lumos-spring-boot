export interface citiesRequest {
  nome: string;
  microrregiao: {
    nome: string;
    mesorregiao: {
      UF: {
        sigla: string,
        nome: string
      }
    }
  }
}
