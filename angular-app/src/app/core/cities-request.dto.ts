export interface citiesRequest {
    id: string;
    nome: string;
    microrregiao: {
        nome: string;
        mesorregiao: {
            nome: string;
        }
    }
}
