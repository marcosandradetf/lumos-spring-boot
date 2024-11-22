import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UtilsService {

  constructor() { }

  formatValue(value: string): string {
    // Verifica se targetValue está vazio e define um valor padrão
    if (!value) {
      return '';
    }

    // Divide o valor por 100 para inserir as casas decimais
    return new Intl.NumberFormat('pt-BR', {
      //style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(parseFloat(value) / 100);

  }

}
