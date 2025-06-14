package com.lumos.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lumos.data.repository.ContractStatus
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Execution
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.ExecutionViewModel

@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController,
    notificationsBadge: String,
    executionViewModel: ExecutionViewModel,
    contractViewModel: ContractViewModel,
    roles: Set<String>,
) {
    val context = LocalContext.current
    val executions = executionViewModel.executions.collectAsState()
    val contracts = contractViewModel.contracts.collectAsState()
    val others = setOf("ADMIN", "RESPONSAVEL_TECNICO", "ANALISTA")
    val operators = setOf("ELETRICISTA", "MOTORISTA")

    LaunchedEffect(Unit) {
        executionViewModel.syncExecutions()
        contractViewModel.syncContracts()

        contractViewModel.loadFlowContracts(ContractStatus.ACTIVE)
    }


    AppLayout(
        title = "Início",
        pSelected = BottomBar.HOME.value,
        notificationsBadge = notificationsBadge,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        context = context
    ) { modifier, snackBar ->
        Column(
            modifier = modifier
                .padding(2.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Cards Minimalistas
            if (roles.any { it in operators }) {
                MaintenanceStatusCard(executions.value, navController)
            }
            if (roles.any { it in others }) {
                Spacer(modifier = Modifier.height(16.dp))
                PreMeasurementCard(contracts.value, navController)
            }
            Spacer(modifier = Modifier.height(16.dp))
            AlertsCard()

            // Botão de Ação Principal
            Spacer(modifier = Modifier.height(24.dp))
            ReportProblemButton()

            // Lista de Atividades Recentes
            Spacer(modifier = Modifier.height(24.dp))
            RecentActivitiesList()
        }
    }
}

@Composable
fun MaintenanceStatusCard(
    executions: List<Execution>,
    navController: NavHostController
) {
    val text = if (executions.size > 1) "Sua equipe possuí ${executions.size} execuções alocadas"
    else "Sua equipe possuí ${executions.size} execução alocada"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Routes.EXECUTION_SCREEN) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone ilustrativo
            Icon(
                imageVector = Icons.Filled.Build, // Ícone de "tarefa"
                contentDescription = "Execuçoes ícone",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Título
            Text(
                text = "Status das Execuções",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Mensagem informativa
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (executions.isNotEmpty())
                Text(
                    text = "Clique para saber mais",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline
                )
        }
    }
}

@Composable
fun PreMeasurementCard(
    contracts: List<Contract>,
    navController: NavHostController
) {
    val text = if (contracts.size > 1) "${contracts.size} contratos estão disponíveis para pré-medição"
    else "${contracts.size} contrato está disponível para pré-medição"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Routes.CONTRACT_SCREEN) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone ilustrativo
            Icon(
                imageVector = Icons.Filled.Map, // Ícone de "tarefa"
                contentDescription = "Pré-medicoes ícone",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.inverseOnSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Título
            Text(
                text = "Status das Pré-medições",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Mensagem informativa
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Clique para saber mais",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@Composable
fun AlertsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Alertas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nenhum alerta no momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun ReportProblemButton() {
    Button(
        onClick = { /* Ação para reportar problema */ },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer) // Azul
    ) {
        Text(text = "Reportar Problema", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun RecentActivitiesList() {


    Column {
        Text(
            text = "Atividades Recentes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

    }
}



//@Preview
//@Composable
//fun PrevHome() {
//    HomeScreen(
//        {},
//        {},
//        {},
//        rememberNavController(),
//        "12",
//
//    )
//}