import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UtilsService {
  serverMessage: string | null = null;
  alertType: string | null = null;


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

  multiplyValue(value: string, multiplier: number): string {
    // Verifica se o valor está vazio ou se o multiplicador é inválido
    if (!value || isNaN(multiplier) || multiplier === 0) {
      return '';
    }

    // Converte o valor para número e multiplica
    const numericValue = parseFloat(value.replace(/\D/g, '')) / 100;
    const result = numericValue * multiplier;

    // Retorna o resultado formatado
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(result);
  }

  sumValue(value: string, sum: string): string {

    // Converte o valor para número e multiplica
    const numericValue = parseFloat(value.replace(/\D/g, '')) / 100;
    const sumValue = parseFloat(sum.replace(/\D/g, '')) / 100;
    const result = numericValue + sumValue;

    // Retorna o resultado formatado
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(result);
  }

  subValue(value: string, sub: string): string {

    // Converte o valor para número e multiplica
    const numericValue = parseFloat(value.replace(/\D/g, '')) / 100;
    const subValue = parseFloat(sub.replace(/\D/g, '')) / 100;
    const result = numericValue - subValue;

    // Retorna o resultado formatado
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(result);
  }

  formatNumber(event: Event) {
    (event.target as HTMLInputElement).value = (event.target as HTMLInputElement).value.replace(/\D/g, ''); // Exibe o valor formatado no campo de input
  }

  showMessage(message: string, error: boolean, timeout = 3000) {
    this.serverMessage = message;
    error ? this.alertType = 'alert-error' : this.alertType = 'alert-success';
    setTimeout(() => {
      this.serverMessage = null;
      this.alertType = null;
    }, timeout);
  }

}
