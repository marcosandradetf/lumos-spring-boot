import {Component} from '@angular/core';
import {AlertMessageComponent} from "../../shared/components/alert-message/alert-message.component";
import {ButtonComponent} from "../../shared/components/button/button.component";
import {FormsModule, NgForm} from "@angular/forms";
import {ModalComponent} from "../../shared/components/modal/modal.component";
import {NgForOf, NgIf} from "@angular/common";
import {TableComponent} from "../../shared/components/table/table.component";
import {Router} from '@angular/router';
import {UserService} from '../user/user-service.service';
import {UtilsService} from '../../core/service/utils.service';
import {AuthService} from '../../core/auth/auth.service';
import {Title} from '@angular/platform-browser';
import {catchError, tap, throwError} from 'rxjs';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [
    AlertMessageComponent,
    ButtonComponent,
    FormsModule,
    ModalComponent,
    NgForOf,
    NgIf
  ],
  templateUrl: './account.component.html',
  styleUrl: './account.component.scss'
})
export class AccountComponent {

  user: {
    userId: string,
    username: string,
    name: string,
    lastname: string,
    email: string,
    year: string;
    month: string;
    day: string;
    role: string[],
    status: boolean
    sel: boolean
  } = {
    userId: '',
    username: '',
    name: '',
    lastname: '',
    email: '',
    year: '',
    month: '',
    day: '',
    role: [],
    status: false,
    sel: false
  };

  password: {
    oldPassword: string,
    password: string,
    passwordConfirm: string,
  } = {
    oldPassword: '',
    password: '',
    passwordConfirm: '',
  }

  errorPassword: string = ''
  showPassword: boolean[] = [false, false, false];

  constructor(protected router: Router, private userService: UserService, protected utils: UtilsService,
              protected authService: AuthService, private titleService: Title) {

    this.titleService.setTitle("Configurações de Acesso");
    const uuid = authService.getUser().uuid;

    this.userService.getUser(uuid).subscribe(
      user => {
        this.user = user;
      });

  }


  months: {
    number: string,
    name: string,
  }[] = [
    {number: '1', name: "Janeiro"},
    {number: '2', name: "Fevereiro"},
    {number: '3', name: "Março"},
    {number: '4', name: "Abril"},
    {number: '5', name: "Maio"},
    {number: '6', name: "Junho"},
    {number: '7', name: "Julho"},
    {number: '8', name: "Agosto"},
    {number: '9', name: "Setembro"},
    {number: '10', name: "Outubro"},
    {number: '11', name: "Novembro"},
    {number: '12', name: "Dezembro"},
  ];

  formSubmitted: boolean = false;
  serverMessage: string | null = null;
  alertType: string | null = null;
  loading: boolean = false;
  openConfirmationModal: boolean = false;
  usernamePattern: string = '^[a-zA-Z0-9._-]{3,20}$';
  emailPattern: string = '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$';
  private email: string = "";

  getMonth(monthNumber: string) {
    return this.months.find(m => m.number === monthNumber);
  }

  getMaxDay(month: string, year: string): number {
    const daysInMonth: Record<number, number> = {
      1: 31, // Janeiro
      2: this.isLeapYear(year) ? 29 : 28, // Fevereiro
      3: 31, // Março
      4: 30, // Abril
      5: 31, // Maio
      6: 30, // Junho
      7: 31, // Julho
      8: 31, // Agosto
      9: 30, // Setembro
      10: 31, // Outubro
      11: 30, // Novembro
      12: 31, // Dezembro
    };
    return daysInMonth[parseInt(month, 10)] || 31; // Valor padrão para meses inválidos
  }

  isLeapYear(strYear: string): boolean {
    const year = parseInt(strYear, 10);
    return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
  }


  submitUser(passForm: NgForm) {
    if (passForm.invalid) {
      return;
    }

    if (this.password.password !== this.password.passwordConfirm) {
      this.errorPassword = "As senhas não conferem, tente novamente.";
      return;
    }

    this.userService.setPassword(this.user.userId, this.password).pipe(
      tap(
        response => {
          this.alertType = 'alert-success';
          this.serverMessage = response.message;
          this.openConfirmationModal = false;
        }
      ),
      catchError(err => {
        this.alertType = "alert-error";
        // @ts-ignore
        this.serverMessage(err.error.message);
        throw err;
      })
    ).subscribe();

  }

  togglePasswordVisibility(number: number) {
    this.showPassword[number] = !this.showPassword[number];
  }

}
