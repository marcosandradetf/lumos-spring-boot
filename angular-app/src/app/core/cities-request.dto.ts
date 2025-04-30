export interface citiesRequest {
  nome: string;
  microrregiao: {
    nome: string;
    mesorregiao: {
      nome: string;
    }
  }
}
