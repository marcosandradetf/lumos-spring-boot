import {AfterViewInit, Component, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import {Calendar} from 'primeng/calendar';
import {FormsModule} from '@angular/forms';
import {StyleClass} from 'primeng/styleclass';

@Component({
    selector: 'app-team-operational-dashboard',
    standalone: true,
    imports: [
        CommonModule,
        CardModule,
        TableModule,
        ButtonModule,
        ChartModule,
        Calendar,
        FormsModule,
        StyleClass
    ],
    templateUrl: './team-operational-dashboard.component.html'
})
export class TeamOperationalDashboardComponent implements OnInit, AfterViewInit {
    selectedDateRange: Date[] = [];

    ngOnInit() {
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);

        this.selectedDateRange = [firstDay, lastDay];
    }

    teamPerformance = [
        { name: 'Equipe Alpha', meta: 300, real: 330, animatedWidth: 0, achieved: false, superAchieved: false },
        { name: 'Equipe Beta', meta: 280, real: 250, animatedWidth: 0, achieved: false, superAchieved: false },
        { name: 'Equipe Gama', meta: 250, real: 220, animatedWidth: 0, achieved: false, superAchieved: false },
        { name: 'Equipe X', meta: 250, real: 290, animatedWidth: 0, achieved: false, superAchieved: false },
        { name: 'Equipe Y', meta: 290, real: 290, animatedWidth: 0, achieved: false, superAchieved: false }
    ];

    bestTeamName = '';

    ngAfterViewInit() {
        setTimeout(() => {

            const best = [...this.teamPerformance].sort((a, b) => b.real - a.real)[0];
            this.bestTeamName = best.name;

            this.teamPerformance = this.teamPerformance.map(team => {

                const percentage = (team.real / team.meta) * 100;

                return {
                    ...team,
                    animatedWidth: Math.min(percentage, 120),
                    achieved: team.real >= team.meta,
                    superAchieved: team.real >= team.meta * 1.1
                };
            });

        }, 300);
    }

    // ===== KPIs =====
    totalTeams = 3;
    totalCollaborators = 8;
    totalServicesExecuted = 780;
    totalLedInstallations = 420;
    totalOvertimeHours = 34;
    avgProductivity = 86;


    teamChartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        layout: {
            padding: {
                top: 10,
                bottom: 10
            }
        },
        plugins: {
            legend: {
                position: 'bottom'
            }
        },
        scales: {
            x: {
                grid: {
                    display: false
                }
            },
            y: {
                grid: {
                    display: false
                }
            }
        }
    };

    // ===== RANKING =====
    collaboratorRanking = [
        { position: 1, name: 'João Silva', services: 140 },
        { position: 2, name: 'Maria Souza', services: 120 },
        { position: 3, name: 'Carlos Lima', services: 95 }
    ];

    // ===== TABELA INDIVIDUAL =====
    collaborators = [
        {
            name: 'João Silva',
            team: 'Equipe Alpha',
            workedHours: 230,
            overtimeHours: 10,
            servicesExecuted: 140,
            ledInstallations: 85,
            productivity: 92
        },
        {
            name: 'Maria Souza',
            team: 'Equipe Alpha',
            workedHours: 210,
            overtimeHours: 0,
            servicesExecuted: 120,
            ledInstallations: 70,
            productivity: 85
        },
        {
            name: 'Carlos Lima',
            team: 'Equipe Beta',
            workedHours: 240,
            overtimeHours: 20,
            servicesExecuted: 95,
            ledInstallations: 55,
            productivity: 72
        }
    ];

    protected readonly Math = Math;
}
