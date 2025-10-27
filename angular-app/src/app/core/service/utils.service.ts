import {Injectable} from '@angular/core';
import {MessageService} from 'primeng/api';
import {BehaviorSubject, Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {NgModel} from '@angular/forms';

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

  blockSpecialChars(event: Event) {
    (event.target as HTMLInputElement).value = (event.target as HTMLInputElement).value.replace(/[^a-zA-Z0-9À-ÿ\s,-]/g, '');
  }

  formatAddressOnBlur(event: Event) {
    const input = event.target as HTMLInputElement;
    let value = input.value.trim().toLowerCase();

    // Corrige duplicações de vírgula e hífen apenas se necessário
    value = value
      .replace(/,{2,}/g, ',')  // duas ou mais vírgulas → uma só
      .replace(/-{2,}/g, '-')  // dois ou mais hifens → um só

      // Remove vírgula ou hífen no início ou fim
      .replace(/^[, -]+/, '')
      .replace(/[, -]+$/, '');

    // Capitaliza
    input.value = this.capitalizeWithAcronyms(value);
  }

  private capitalize(str: string): string {
    return str.replace(/\b\w/g, c => c.toUpperCase());
  }


  public capitalizeWithAcronyms(str: string): string {
    const words = str.split(' ');
    const lastIndex = words.length - 1;

    for (let i = 0; i < words.length; i++) {
      const word = words[i];
      if (i === lastIndex && word.length === 2) {
        // Última palavra e tem tamanho 2 -> uppercase total
        words[i] = word.toUpperCase();
      } else {
        // Capitaliza normal
        words[i] = word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
      }
    }

    return words.join(' ');
  }

  formatNumber(event: Event) {
    (event.target as HTMLInputElement).value = (event.target as HTMLInputElement).value.replace(/\D/g, ''); // Exibe o valor formatado no campo de input
  }

  formatFloatNumber(event: Event, model: NgModel | null = null) {
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
    if (model) model.viewToModelUpdate(value); // atualiza o ngModel sem evento recursivo
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

  formatPlate(event: Event) {
    let value = (event.target as HTMLInputElement).value;
    value = value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();

    (event.target as HTMLInputElement).value = value;
  }

  showMessage(messageContent: string,
              typeMessage: 'success' | 'info' | 'warn' | 'error' | 'contrast' | 'secondary',
              summary: string = typeMessage,
              stick: boolean = false,
              key: string | null = null,) {
    switch (typeMessage) {

      case 'success':
        this.messageService.add({
          severity: 'success',
          summary: summary,
          detail: messageContent,
          sticky: stick,
          key: key ?? undefined
        });
        break;
      case 'info':
        this.messageService.add({
          severity: 'info',
          summary: summary,
          detail: messageContent,
          sticky: stick,
          key: key ?? undefined
        });
        break;
      case 'warn':
        this.messageService.add({
          severity: 'warn',
          summary: summary,
          detail: messageContent,
          sticky: stick,
          key: key ?? undefined
        });
        break;
      case 'error':
        this.messageService.add({
          severity: 'error',
          summary: summary,
          detail: messageContent,
          sticky: stick,
          key: key ?? undefined
        });
        break;
      case 'contrast':
        this.messageService.add({
          severity: 'contrast',
          summary: summary,
          detail: messageContent,
          sticky: stick,
          key: key ?? undefined
        });
        break;
      case 'secondary':
        this.messageService.add({
          severity: 'secondary',
          summary: summary,
          detail: messageContent,
          sticky: stick,
          key: key ?? undefined
        });
        break;
    }

  }


  playSound(type: 'pop' | 'select' | 'open') {
    if (type === 'open') {
      const audio = new Audio('/public/sci.mp3');
      audio.play().catch(err => {
      });
    } else if (type === 'select') {
      const audio = new Audio('/public/select.mp3');
      audio.play().catch(err => {
      });
    } else if (type === 'pop') {
      const audio = new Audio('/public/pop.mp3');
      audio.play().catch(err => {
      });
    }
  }

  getStatus(status
            :
            string
  ):
    string {
    if (status === 'ACTIVE') {
      return 'ATIVO';
    } else if (status === 'INACTIVE') {
      return 'INATIVO';
    } else if (status === 'ARCHIVED') {
      return 'ARQUIVADO';
    } else {
      return 'STATUS DESCONHECIDO';
    }
  }

  getObject<T>(body: GetObjectRequest): Observable<T> {
    return this.http.post<T>(environment.springboot + '/api/util/generic/get-object', body);
  }

  setObject(body: SetObjectRequest) {
    return this.http.post(environment.springboot + '/api/util/generic/set-object', body);
  }


  formatCity(city: string) {
    let formatedCity = city
      .toLowerCase()
      .replace('de', '') // só remove "de" isolado
      .replace(/prefeitura/g, '') // só remove "de" isolado
      .replace(/municipal/g, '') // só remove "de" isolado
      .replace(/\s+/g, ' '); // limpa espaços duplos

    return this.capitalizeWithAcronyms(formatedCity.trim());
  }

  clearToast(key: string) {
    this.messageService.clear(key);
  }

}

export interface GetObjectRequest {
  fields: string[];
  table: string;
  where: string;
  equal: string[] | number[];
}

export interface SetObjectRequest {
  command: string;
  tables: string[];
  where: string;
  equal: any;
}
