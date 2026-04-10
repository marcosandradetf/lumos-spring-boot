import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';

export type UserActivationStatus = 'PENDING_ACTIVATION' | 'ACTIVE' | 'BLOCKED';

export interface ActivationCodeResponse {
    activationCode: string;
    expiresAt: string;
    message: string;
}

export interface UserManagementResponse {
    userId: string;
    username: string;
    name: string;
    lastname: string;
    email: string;
    cpf: string;
    dateOfBirth?: string;
    year: string;
    month: string;
    day: string;
    role: any[];
    status: any;
    mustChangePassword?: boolean;
    activationExpiresAt?: string | null;
    sel: boolean;
    show: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class UserService {
    private endpoint = environment.springboot + '/api/user';

    constructor(private http: HttpClient) {
    }

    public getUsers() {
        return this.http.get<UserManagementResponse[]>(`${this.endpoint}/get-users`);
    }

    public getUser(uuid: string) {
        return this.http.get<UserManagementResponse & {
            phone: string,
            department: string,
        }>(`${this.endpoint}/get-user/${uuid}`);
    }


    public generateActivationCode(userId: string) {
        return this.http.post<ActivationCodeResponse>(`${this.endpoint}/${userId}/generate-activation-code`, {});
    }

    public resetActivation(userId: string) {
        return this.http.post<ActivationCodeResponse>(`${this.endpoint}/${userId}/reset-activation`, {});
    }

    public getRoles() {
        return this.http.get<{
            selected: boolean,
            roleId: string,
            roleName: string,
            label: string,
            description: string
        }[]>(`${this.endpoint}/get-roles`);

    }

    public updateUser(user: {
        userId: string,
        username: string,
        name: string,
        lastname: string,
        email: string,
        cpf: string,
        year: string | number;
        month: string | number;
        day: string | number;
        role: string[],
        status: UserActivationStatus
        sel: boolean
    }[]) {
        return this.http.post<UserManagementResponse[]>(`${this.endpoint}/update-users`, user);
    }

    setPassword(uuid: string, password: { oldPassword: string; password: string; passwordConfirm: string }) {
        return this.http.post<{ message: string }>(`${this.endpoint}/${uuid}/set-password`, password);
    }
}
