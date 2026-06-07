import axios from 'axios';

export interface UfResponse {
    sigla: string;
    nome: string;
}

export interface CityResponse {
    id: string;
    nome: string;
    microrregiao: {
        nome: string;
        mesorregiao: {
            nome: string;
        }
    }
}

// Criando uma instância dedicada com a baseURL do IBGE
const ibgeClient = axios.create({
    baseURL: 'https://servicodados.ibge.gov.br/api/v1/localidades',
    timeout: 10000, // 10 segundos de limite
});

export const ibgeApi = { 
    async getUfs(): Promise<UfResponse[]> {
        // Usando Template Literals (``) que é mais moderno que concatenar com '+'
        const { data } = await ibgeClient.get<UfResponse[]>('/estados?orderBy=nome');
        return data;
    },

    async getCities(uf: string): Promise<CityResponse[]> {
        const { data } = await ibgeClient.get<CityResponse[]>(`/estados/${uf}/municipios?orderBy=nome`);
        return data;
    }
};