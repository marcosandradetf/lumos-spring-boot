import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {TableComponent} from '../../shared/components/table/table.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {UserService} from './user-service.service';
import {catchError, tap, throwError} from 'rxjs';
import {UtilsService} from '../../core/service/utils.service';
import {AlertMessageComponent} from '../../shared/components/alert-message/alert-message.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {AuthService} from '../../core/auth/auth.service';
import {Title} from '@angular/platform-browser';
import {NgxMaskDirective, NgxMaskPipe, provideNgxMask} from 'ngx-mask';
import {Toast} from 'primeng/toast';
import {MultiSelect} from 'primeng/multiselect';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {SharedState} from '../../core/service/shared-state';

@Component({
    selector: 'app-user',
    standalone: true,
    imports: [
        TableComponent,
        ButtonComponent,
        FormsModule,
        ReactiveFormsModule,
        NgForOf,
        NgIf,
        AlertMessageComponent,
        ModalComponent,
        NgxMaskDirective,
        NgxMaskPipe,
        Toast,
        NgClass,
        MultiSelect,
        LoadingComponent
    ],
    providers: [provideNgxMask()],
    templateUrl: './user.component.html',
    styleUrl: './user.component.scss'
})

export class UserComponent {


    change: boolean = false;
    users: {
        userId: string;
        username: string;
        name: string;
        lastname: string;
        email: string;
        cpf: string;
        year: string;
        month: string;
        day: string;
        role: string[];
        status: boolean;
        sel: boolean;
        show: boolean;
    }[] = [];

    usersBackup: {
        userId: string;
        username: string;
        name: string;
        lastname: string;
        email: string;
        cpf: string;
        year: string;
        month: string;
        day: string;
        role: string[];
        status: boolean;
        sel: boolean;
        show: boolean;
    }[] = [];


    rolesUser: {
        index: number,
        role: string,
    }[] = [];

    roles: {
        selected: boolean,
        roleId: string,
        roleName: string,
    }[] = [];

    add: boolean = false;

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
    userId: string = '';
    openConfirmationModal: boolean = false;
    usernamePattern: string = '^[a-zA-Z0-9._-]{3,20}$';
    emailPattern: string = '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$';
    cpfPattern: string = '^[0-9]{11}$'; // sem pontuação, pois o valor real é só os números
    private email: string = "";

    getMonth(monthNumber: string) {
        return this.months.find(m => m.number === monthNumber);
    }


    constructor(protected router: Router, private userService: UserService, protected utils: UtilsService,
                protected authService: AuthService, private titleService: Title) {

        this.titleService.setTitle("Configurações - Usuários");
        SharedState.setCurrentPath(["Configurações", "Usuários"]);

        this.userService.getUsers().subscribe(
            users => {
                this.users = users;
                this.usersBackup = JSON.parse(JSON.stringify(this.users));
                this.rolesUser = users.flatMap(user =>
                    user.role.map(role => ({
                        index: this.users.findIndex(u => u.userId === user.userId),
                        role: role
                    }))
                );
            }
        );

        this.userService.getRoles().subscribe(
            roles => {
                this.roles = roles;
            }
        );
    }


    changeUser() {
        return () => {
            this.change = true;
        };
    }

    addUser() {
        return () => {
            this.add = true;
        }
    }

    resetView() {
        return () => {
            this.change = false;
            this.add = false;
            this.users = JSON.parse(JSON.stringify(this.usersBackup));
        }
    }

    submitUsers(form: NgForm) {
        this.formSubmitted = true;

        if (form.invalid) {
            console.log('Formulário inválido');
            return;
        }

        this.loading = true;

        const someSel = this.users.some(u => u.sel);

        if (someSel) {
            this.updateUsers();
        }

    }

    private updateUsers() {
        // Verifica se nenhum usuário foi selecionado
        const noneSelected = this.users.every(u => !u.sel);

        if (noneSelected) {
            // this.serverMessage = "Nenhum usuário foi selecionado."
            this.utils.showMessage("Nenhum usuário foi selecionado.", 'info', 'Atenção');
            this.alertType = "alert-error";
            this.loading = false;
            return;
        }

        this.userService.updateUser(this.users)
            .pipe(tap(r => {
                    this.utils.showMessage("Caso algum usuário tenha sido cadastrado, a senha foi enviada para o email informado.", 'success', 'Usuários atualizados com sucesso.', true)
                    this.users = r;
                    this.usersBackup = JSON.parse(JSON.stringify(this.users));
                    this.change = false;
                    this.loading = false;
                }),
                catchError(err => {
                    this.utils.showMessage(err.error.message, 'error', 'Atenção');
                    this.loading = false;
                    throw err;
                })
            ).subscribe();

    }

    private showMessage(message: string, timeout = 3000) {
        this.serverMessage = message;
        setTimeout(() => {
            this.serverMessage = null;
        }, timeout);
    }

    currentPassword: string | null = null

    resetPassword() {
        this.loading = true;
        if (this.userId !== '') {
            this.userService.resetPassword(this.userId).pipe(
                tap(r => {
                    this.loading = false;
                    this.alertType = "alert-success";
                    this.currentPassword = (r as any).message
                }),
                catchError(err => {
                    this.loading = false;
                    return throwError(() => err);
                })
            ).subscribe();
        }
    }

    newUser() {
        const user = {
            userId: "",
            username: "",
            name: "",
            lastname: "",
            email: "",
            cpf: "",
            year: "",
            month: "",
            day: "",
            role: [],
            status: true,
            sel: true,
            show: false,
        };
        this.users.splice(0, 0, user);
    }


    removeUser() {
        const firstElement = this.users[0];
        if (firstElement.userId === '') {
            this.users = this.users.filter(u => u.userId !== firstElement.userId);
        }
    }

    changeRole(userIndex: number, nomeRole: string) {
        if (userIndex === -1) {
            console.log('Usuário não encontrado');
            return;
        }


        // Obter todas as roles associadas ao usuário
        let roles = this.rolesUser.filter(u => u.index === userIndex);

        // Verificar se a role já existe
        const roleExists = roles.some(role => role.role === nomeRole);

        if (roleExists) {
            // Se a role já existe, removê-la
            roles = roles.filter(role => role.role !== nomeRole);
        } else {
            // Caso contrário, adicionar a nova role
            roles.push({index: userIndex, role: nomeRole});
        }

        // Atualizar as roles do usuário
        this.rolesUser = this.rolesUser.filter(u => u.index !== userIndex);
        // user.role = [];
        this.users[userIndex].role = [];
        roles.forEach(role => {
            this.rolesUser.push(role);
            // user.role.push(role.role);
            this.users[userIndex].role.push(role.role);
        });


    }


    filterRolesByUserId(index: number) {
        return this.rolesUser.filter(role => role.index === index);
    }

    changeRoleFromMulti(userIndex: number, selectedRoles: string[]) {
        // Zera as roles do usuário
        this.rolesUser = this.rolesUser.filter(u => u.index !== userIndex);
        this.users[userIndex].role = [];

        // Reinsere as roles selecionadas
        selectedRoles.forEach(roleName => {
            this.rolesUser.push({index: userIndex, role: roleName});
            this.users[userIndex].role.push(roleName);
        });
    }


    getRolesString(index: number): string {
        return this.rolesUser
            .filter(r => r.index === index)
            .map(r => r.role)
            .join(', ');
    }


    verifyRole(index: number, nomeRole: string): boolean {
        const userRoles = this.filterRolesByUserId(index); // Obtém as roles do usuário filtrado

        // Verifica se alguma role no array corresponde ao nomeRole fornecido
        return userRoles.some(role => role.role === nomeRole);
    }


    handleClick(dropdown: HTMLDetailsElement, month: string, userId: string) {
        // Fecha o dropdown
        dropdown.open = false;

        const userIndex = this.users.findIndex(u => u.userId === userId);
        if (userIndex === -1) {
            return;
        }

        this.users[userIndex].month = month;
    }

    handleClickInsert(dropdown: HTMLDetailsElement, month: string, index: number) {
        // Fecha o dropdown
        dropdown.open = false;

        if (index === -1) {
            return;
        }

        this.users[index].month = month;
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

    confirmResetPassword(userId: string, email: string) {
        if (userId !== '') {
            this.openConfirmationModal = true;
            this.userId = userId;
            this.email = email;
        } else {
            this.utils.showMessage("Ação disponível apenas para usuários cadastrados.", "info", "Lumos™");
        }

    }

    copyPassword() {
        if (this.currentPassword) {
            navigator.clipboard.writeText(this.currentPassword ?? '').then(() => {
                this.openConfirmationModal = false;
                this.currentPassword = null;
                this.utils.showMessage("Senha copiada com sucesso", "success", "Lumos™");
            });
        }
    }


}
