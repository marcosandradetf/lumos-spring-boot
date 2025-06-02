import {Injectable} from '@angular/core';
import {MessageService} from 'primeng/api';
import {BehaviorSubject, Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class UtilsService {
  private menuState = new BehaviorSubject<boolean>(false);

  constructor(private messageService: MessageService, private http: HttpClient) {
  }

  menuState$ = this.menuState.asObservable();

  toggleMenu(isOpen: boolean) {
    this.menuState.next(isOpen);
  }

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

  formatFloatNumber(event: Event) {
    const input = event.target as HTMLInputElement;
    let value = input.value;

    // Remove tudo que não é número ou ponto
    value = value.replace(/[^0-9.]/g, '');

    // Permite apenas um único ponto
    const parts = value.split('.');
    if (parts.length > 2) {
      value = parts[0] + '.' + parts.slice(1).join('');
    }

    // Evita começar com ponto (ex: ".2" vira "0.2")
    if (value.startsWith('.')) {
      value = '0' + value;
    }

    input.value = value;
  }


  formatContractNumber(event: Event) {
    (event.target as HTMLInputElement).value = (event.target as HTMLInputElement).value.replace(/[^0-9,./-]/g, '');
  }


  formatCPF(event: Event) {
    let value = (event.target as HTMLInputElement).value;
    value = value.replace(/\D/g, ''); // Remove tudo que não for número
    value = value.replace(/(\d{3})(\d)/, '$1.$2'); // Adiciona o primeiro ponto
    value = value.replace(/(\d{3})(\d)/, '$1.$2'); // Adiciona o segundo ponto
    value = value.replace(/(\d{3})(\d{1,2})$/, '$1-$2'); // Adiciona o traço antes dos últimos dois dígitos
    (event.target as HTMLInputElement).value = value;
  }

  formatCNPJ(event: Event) {
    let value = (event.target as HTMLInputElement).value;
    value = value.replace(/\D/g, ''); // Remove tudo que não for número
    value = value.replace(/(\d{2})(\d)/, '$1.$2'); // Adiciona o primeiro ponto
    value = value.replace(/(\d{3})(\d)/, '$1.$2'); // Adiciona o segundo ponto
    value = value.replace(/(\d{3})(\d)/, '$1/$2'); // Adiciona a barra
    value = value.replace(/(\d{4})(\d{1,2})$/, '$1-$2'); // Adiciona o traço antes dos últimos dois dígitos
    (event.target as HTMLInputElement).value = value;
  }

  formatPhone(event: Event) {
    let value = (event.target as HTMLInputElement).value;
    value = value.replace(/\D/g, ''); // Remove tudo que não for número

    if (value.length > 10) {
      // Formato para celular (XX) XXXXX-XXXX
      value = value.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3');
    } else {
      // Formato para telefone fixo (XX) XXXX-XXXX
      value = value.replace(/^(\d{2})(\d{4})(\d{4})$/, '($1) $2-$3');
    }

    (event.target as HTMLInputElement).value = value;
  }

  showMessage(messageContent: string,
              typeMessage: 'success' | 'info' | 'warn' | 'error' | 'contrast' | 'secondary',
              summary: string = typeMessage,
              stick: boolean = false,) {
    switch (typeMessage) {
      case 'success':
        this.messageService.add({severity: 'success', summary: summary, detail: messageContent, sticky: stick});
        break;
      case 'info':
        this.messageService.add({severity: 'info', summary: summary, detail: messageContent, sticky: stick});
        break;
        break;
      case 'warn':
        this.messageService.add({severity: 'warn', summary: summary, detail: messageContent, sticky: stick});
        break;
      case 'error':
        this.messageService.add({severity: 'error', summary: summary, detail: messageContent, sticky: stick});
        break;
      case 'contrast':
        this.messageService.add({severity: 'contrast', summary: summary, detail: messageContent, sticky: stick});
        break;
      case 'secondary':
        this.messageService.add({severity: 'secondary', summary: summary, detail: messageContent, sticky: stick});
        break;
    }

  }


  playSound(type: 'pop' | 'select' | 'open') {
    if (type === 'open') {
      const audio = new Audio('sci.mp3');
      audio.play().catch(err => {
      });
    } else if (type === 'select') {
      const audio = new Audio('select.mp3');
      audio.play().catch(err => {
      });
    } else if (type === 'pop') {
      const audio = new Audio('pop.mp3');
      audio.play().catch(err => {
      });
    }
  }

  getStatus(status
            :
            string
  ):
    string {
    if (status === 'PENDING') {
      return 'PENDENTE';
    } else if (status === 'VALIDATING') {
      return 'VALIDANDO';
    } else if (status === 'WAITING_CONTRACTOR') {
      return 'AGUARDANDO CONTRATANTE';
    } else if (status === 'AVAILABLE') {
      return 'DISPONÍVEL';
    } else if (status === 'WAITING_TEAM') {
      return 'AGUARDANDO EQUIPE';
    } else if (status === 'IN_PROGRESS') {
      return 'EM PROGRESSO';
    } else if (status === 'FINISHED') {
      return 'FINALIZADO';
    } else {
      return 'STATUS DESCONHECIDO';
    }
  }

  getObject<T>(params: GetObjectRequest): Observable<T> {
    return this.http.post<T>(environment.springboot + '/api/util/generic/get-object', params);
  }

}

export interface GetObjectRequest {
  fields: string[];
  table: string;
  where: string;
  equal: string[] | number[];
}
