import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropdownModule } from 'primeng/dropdown';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ChartModule } from 'primeng/chart';
import { ButtonModule } from 'primeng/button';
import { TabViewModule } from 'primeng/tabview';


@Component({
    selector: 'app-executive',
    standalone: true,
    imports: [
        CommonModule,
        DropdownModule,
        CalendarModule,
        CardModule,
        TableModule,
        TagModule,
        ChartModule,
        ButtonModule,
        TabViewModule
    ],
    templateUrl: './executive.component.html',
    styleUrl: './executive.component.scss'
})
export class ExecutiveComponent {
    // ===== EXECUTIVE KPIs =====
    avgProductivity = 87;
    totalHours = 1240;
    totalServices = 342;
    criticalInventory = 6;
    inconsistencyCount = 4;

    // ===== FILTERS =====
    cities = [
        { label: 'Prefeitura A', value: 1 },
        { label: 'Prefeitura B', value: 2 }
    ];

    contracts = [
        { label: 'Contrato 001', value: 1 },
        { label: 'Contrato 002', value: 2 }
    ];

    // ===== CHARTS =====
    productivityTrend = {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May'],
        datasets: [
            {
                label: 'Productivity (%)',
                data: [78, 82, 85, 83, 89],
                fill: false
            }
        ]
    };

    warehouseStockChart = {
        labels: ['Central', 'Zona Norte', 'Zona Sul'],
        datasets: [
            {
                label: 'Itens Críticos',
                data: [2, 3, 1]
            }
        ]
    };

    // ===== TEAM PERFORMANCE =====
    teamPerformance = [
        { id: 1, name: 'Equipe Alpha', hours: 400, services: 120, productivity: 88 },
        { id: 2, name: 'Equipe Beta', hours: 350, services: 95, productivity: 75 },
        { id: 3, name: 'Equipe Gama', hours: 490, services: 127, productivity: 91 }
    ];

    selectedTeamId?: number;

    userPerformance = [
        { name: 'João Silva', hours: 160, services: 45, productivity: 84 },
        { name: 'Maria Souza', hours: 170, services: 52, productivity: 91 },
        { name: 'Carlos Lima', hours: 150, services: 38, productivity: 72 }
    ];

    // ===== INCONSISTENCIES =====
    riskItems = [
        {
            contract: 'Contrato 001',
            type: 'Overtime',
            description: 'Funcionário excedeu limite diário',
            severity: 'danger'
        },
        {
            contract: 'Contrato 002',
            type: 'Stock',
            description: 'Material abaixo do mínimo',
            severity: 'warning'
        }
    ];

    toggleDarkMode() {
        document.documentElement.classList.toggle('dark');
    }

    selectTeam(teamId: number) {
        this.selectedTeamId = teamId;
        // aqui você buscaria funcionários reais via API
    }
}
