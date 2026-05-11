import { Component, OnInit } from '@angular/core';
import { NgIf, NgOptimizedImage } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../auth.service';
import { Utils } from '../../../service/utils';
import { ContractService } from '../../../../contract/services/contract.service';
import { UserService } from '../../../../manage/user/user-service.service';
import { TeamService } from '../../../../manage/team/team-service.service';
import { StockService } from '../../../../stock/services/stock.service';
import { MaterialService } from '../../../../stock/services/material.service';
import { UtilsService } from '../../../service/utils.service';

@Component({
    selector: 'app-auto-login',
    standalone: true,
    imports: [NgIf, NgOptimizedImage],
    templateUrl: './auto-login.component.html',
    styleUrl: './auto-login.component.scss',
})
export class AutoLoginComponent implements OnInit {
    loading = true;
    errorMessage: string | null = null;
    redirectPath: string = '/';

    constructor(
        private readonly authService: AuthService,
        private readonly router: Router,
        private readonly route: ActivatedRoute,
        private readonly contractService: ContractService,
        private readonly userService: UserService,
        private readonly teamService: TeamService,
        private readonly stockService: StockService,
        private readonly materialService: MaterialService,
        private readonly utilsService: UtilsService
    ) {}

    ngOnInit(): void {
        const redirect = this.route.snapshot.queryParamMap.get('redirect');
        if (redirect) this.redirectPath = redirect;

        this.authService.autoLoginWithRefreshToken().subscribe({
            next: () => {
                const roles = this.authService.getUser().getRoles();
                const hasPermission = roles.includes('ADMIN') || roles.includes('ANALISTA') || roles.includes('RESPONSAVEL_TECNICO');
                localStorage.setItem('isSupport', this.authService.getUser().support ? 'true' : 'false');

                if (localStorage.getItem('onboarding') || !hasPermission) {
                    this.loading = false;
                    localStorage.setItem('menuOpen', 'true');
                    this.utilsService.toggleMenu(true);
                    void this.router.navigate([this.redirectPath]);
                    return;
                }

                this.checkState(this.redirectPath);
            },
            error: () => {
                this.loading = false;
                this.errorMessage = 'Nao foi possivel autenticar automaticamente. Redirecionando para login...';
                localStorage.removeItem('menuOpen');
                setTimeout(() => {
                    void this.router.navigate(['/auth/login'], {
                        queryParams: { redirect: this.redirectPath }
                    });
                }, 1400);
            }
        });
    }

    private checkState(redirectPath: string) {
        forkJoin({
            referenceContractItems: this.contractService.getContractReferenceItems(),
            contracts: this.contractService.getAllContracts(
                {
                    contractor: null,
                    startDate: new Date(new Date().setMonth(new Date().getMonth() - 6)),
                    endDate: new Date(),
                    status: null,
                }
            ),
            users: this.userService.getUsers(),
            teams: this.teamService.getTeams(),
            stockists: this.stockService.getStockists(),
            deposits: this.stockService.getDeposits(),
            materials: this.materialService.getCatalogue(),
        }).subscribe({
            next: ({ referenceContractItems, contracts, users, teams, stockists, deposits, materials }) => {
                const a = referenceContractItems.length;
                const b = contracts.length;
                const c = users.length;
                const d = users.filter(user => {
                    const roles = user.role.map(r => r.roleName);
                    return roles.includes('ELETRICISTA') || roles.includes('MOTORISTA');
                }).length;

                const e = teams.length;
                const f = stockists.length;
                const g = deposits.filter(deposit => !deposit.isTruck).length;
                const h = deposits.filter(deposit => deposit.isTruck).length;
                const i = materials.length;

                if (a === 0
                    || b === 0
                    || c === 0
                    || d === 0
                    || e === 0
                    || f === 0
                    || g === 0
                    || h === 0
                    || i === 0
                ) {
                    void this.router.navigate(['/configuracoes/onboarding']);
                } else {
                    localStorage.setItem('onboarding', 'finished');
                    this.utilsService.setOnboarding(false);
                    void this.router.navigate([redirectPath]);
                }
                if (window.matchMedia('(min-width: 1280px)').matches) {
                    localStorage.setItem('menuOpen', 'true');
                    this.utilsService.toggleMenu(true);
                }
                this.loading = false;
            },
            error: (err) => {
                Utils.handleHttpError(err, this.router);
                this.loading = false;
            }
        });
    }
}
