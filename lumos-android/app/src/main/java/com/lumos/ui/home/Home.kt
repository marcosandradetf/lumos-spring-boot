package com.lumos.ui.home

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lumos.data.repository.ContractStatus
import com.lumos.domain.model.Contract
import com.lumos.domain.model.ExecutionHolder
import com.lumos.midleware.SecureStorage
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.DirectExecutionViewModel
import com.lumos.ui.viewmodel.IndirectExecutionViewModel
import com.lumos.utils.ConnectivityUtils

@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController,
    notificationsBadge: String,
    indirectExecutionViewModel: IndirectExecutionViewModel,
    directExecutionViewModel: DirectExecutionViewModel,
    contractViewModel: ContractViewModel,
    roles: Set<String>,
    secureStorage: SecureStorage
) {
    val TWELVE_HOURS = 12 * 60 * 60 * 1000L


    val context = LocalContext.current
    val executions = directExecutionViewModel.directExecutions.collectAsState()
    val contracts = contractViewModel.contracts.collectAsState()
    val others = setOf("ADMIN", "RESPONSAVEL_TECNICO", "ANALISTA")
    val operators = setOf("ELETRICISTA", "MOTORISTA")

    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val currentVersionCode =
        packageInfo.longVersionCode

    var updateModal by remember { mutableStateOf(false) }
    var encodedUrl by remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        contractViewModel.loadFlowContracts(ContractStatus.ACTIVE)

        val lastCheck = secureStorage.getLastUpdateCheck()
        val now = System.currentTimeMillis()
        val isStaleCheck = now >= lastCheck && (now - lastCheck > TWELVE_HOURS)

        if (isStaleCheck) { // 12h
            secureStorage.setLastUpdateCheck()

            directExecutionViewModel.checkUpdate(currentVersionCode) { newVersion, apkUrl ->
                if (newVersion != null && apkUrl != null && newVersion > currentVersionCode) {
                    encodedUrl = Uri.encode(apkUrl)
                    if (ConnectivityUtils.wifiConnected(context))
                        navController.navigate(Routes.UPDATE + "/$encodedUrl")
                    else
                        updateModal = true
                }

            }

            directExecutionViewModel.syncExecutions()
            contractViewModel.syncContracts()
        }

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
    ) { modifier, _ ->
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

            Button(
                onClick = {
                    directExecutionViewModel.checkUpdate(currentVersionCode) { newVersion, apkUrl ->
                        Log.e("v", newVersion.toString())
                        newVersion?.let {
                            if (newVersion > currentVersionCode) {
                                apkUrl?.let {
                                    encodedUrl = Uri.encode(apkUrl)

                                    if (ConnectivityUtils.wifiConnected(context))
                                        navController.navigate(Routes.UPDATE + "/$encodedUrl")
                                    else
                                        updateModal = true
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "Ícone de atualização",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Verificar Atualização",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
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

        if (updateModal) {
            Confirm(
                "Nova atualização disponível",
                body = "Deseja atualizar agora?",
                icon = Icons.Filled.SystemUpdate,
                confirm = {
                    navController.navigate(Routes.UPDATE + "/$encodedUrl")
                },
                cancel = {
                    updateModal = false
                },
            )
        }
    }
}

@Composable
fun MaintenanceStatusCard(
    executions: List<ExecutionHolder>,
    navController: NavHostController
) {
    val text = if (executions.size > 1) "Sua equipe possuí ${executions.size} execuções alocadas"
    else "Sua equipe possuí ${executions.size} execução alocada"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Routes.DIRECT_EXECUTION_SCREEN) },
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
    val text =
        if (contracts.size > 1) "${contracts.size} contratos estão disponíveis para pré-medição"
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