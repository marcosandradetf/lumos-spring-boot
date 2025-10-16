package com.lumos.ui.installationholder

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.InstallationView
import com.lumos.midleware.SecureStorage
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.repository.ViewRepository
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.NothingData
import com.lumos.viewmodel.DirectExecutionViewModel
import com.lumos.viewmodel.PreMeasurementInstallationViewModel
import java.util.UUID

@Composable
fun InstallationHolderScreen(
    directExecutionViewModel: DirectExecutionViewModel,
    preMeasurementInstallationViewModel: PreMeasurementInstallationViewModel,
    viewRepository: ViewRepository,
    navController: NavHostController,
    roles: Set<String>,
    secureStorage: SecureStorage
) {
    val requiredRoles = setOf("MOTORISTA", "ELETRICISTA")

    val executions by viewRepository.getFlowInstallations().collectAsState(emptyList())

    val isSyncing = directExecutionViewModel.isLoading

    val error1 by directExecutionViewModel.syncError.collectAsState()
    val error2 by preMeasurementInstallationViewModel.syncError.collectAsState()

    val responseError = if (error1.isNullOrBlank()) error2 else error1

    LaunchedEffect(Unit) {
        val TWELVE_HOURS = 12 * 60 * 60 * 1000L

        val now = System.currentTimeMillis()

        val lastTeamCheck = secureStorage.getLastTeamCheck()
        val isStaleCheckTeam = now >= lastTeamCheck && (now - lastTeamCheck > TWELVE_HOURS)

        if (!roles.any { it in requiredRoles }) {
            navController.navigate(Routes.NO_ACCESS + "/Instalações")
        }

        if (isStaleCheckTeam)
            navController.navigate("${Routes.TEAM_SCREEN}/${BottomBar.EXECUTIONS.value}")
        else {
            directExecutionViewModel.syncExecutions()
            preMeasurementInstallationViewModel.syncExecutions()
        }

        directExecutionViewModel.countStock()
    }

    ContentCitiesScreen(
        title = "Instalações Pendentes",
        executions = executions,
        navController = navController,
        isSyncing = isSyncing,
        select = { id, type, contractor ->
            if(type == "") navController.navigate("${Routes.DIRECT_EXECUTION_SCREEN}/${id}/${contractor}")
            else navController.navigate("")
        },
        error = responseError,
        refresh = {
            directExecutionViewModel.syncExecutions()
            preMeasurementInstallationViewModel.syncExecutions()
        },
        markAsFinished = { contractId ->
            directExecutionViewModel.markAsFinished(contractId)
        },
        stockDataSize = directExecutionViewModel.stockCount
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentCitiesScreen(
    title: String,
    executions: List<InstallationView>,
    navController: NavHostController,
    isSyncing: Boolean,
    select: (String, String, String) -> Unit,
    error: String?,
    refresh: () -> Unit,
    markAsFinished: (Long) -> Unit = {},
    stockDataSize: Int
) {
    var openModal by remember { mutableStateOf(false) }
    var showModal by remember { mutableStateOf(false) }
    var contractId by remember { mutableLongStateOf(0) }

    AppLayout(
        title = title,
        selectedIcon = BottomBar.EXECUTIONS.value,
        navigateToMore = { navController.navigate(Routes.MORE) },
        navigateToHome = { navController.navigate(Routes.HOME) },
        navigateBack = { navController.popBackStack() },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE)
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK)
        }
    ) { _, _ ->

        if (showModal) {
            Confirm(
                title = "Carregar estoque",
                body = "Você precisa carregar os dados do estoque para criar uma nova manutenção. Deseja fazer isso agora?",
                confirm = {
                    showModal = false
                    navController.navigate(Routes.STOCK)
                },
                cancel = { showModal = false }
            )
        }

        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { refresh() },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!error.isNullOrBlank() && executions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SentimentVerySatisfied,
                        contentDescription = "Alert",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (!error.isNullOrBlank()) 60.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp) // Espaço entre os cards
            ) {

                if (executions.isEmpty()) {
                    item {
                        NothingData(
                            "Nenhuma execução disponível no momento, volte mais tarde!"
                        )
                    }
                }

                items(executions) { execution -> // Iteração na lista

                    Card(
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(3.dp)
                            .clickable {
                                if (stockDataSize > 0) select(
                                    execution.id,
                                    execution.type,
                                    execution.contractor
                                )
                                else showModal = true
                            },
                        elevation = CardDefaults.cardElevation(1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min) // Isso é o truque!
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                // Linha vertical com bolinha no meio
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(0.5f)
                                        .padding(start = 20.dp)
                                        .width(4.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                )

                                // Bolinha com ícone (no meio da linha)
                                Box(
                                    modifier = Modifier
                                        .offset(x = 10.dp) // posiciona sobre a linha
                                        .size(24.dp) // tamanho do círculo
                                        .clip(CircleShape)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Power,
                                        contentDescription = "Local",
                                        tint = Color.White,
                                        modifier = Modifier.size(
                                            18.dp
                                        )
                                    )
                                }
                            }


                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,

                                        ) {
                                        Row {
                                            Text(
                                                text = execution.contractor,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(5.dp)
                                        ) {
                                            Text(
                                                text = "ATIVO",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (execution.type != "PreMeasurementInstallation") {
                                            var expanded by remember(execution.contractId) {
                                                mutableStateOf(
                                                    false
                                                )
                                            }
                                            IconButton(onClick = { expanded = true }) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "Mais opções"
                                                )
                                                DropdownMenu(
                                                    expanded = expanded,
                                                    onDismissRequest = { expanded = false },
                                                ) {
                                                    DropdownMenuItem(
                                                        onClick = {
                                                            contractId = execution.contractId
                                                            expanded = false
                                                            openModal = true
                                                        },
                                                        text = { Text("Marcar como finalizado") },
                                                        leadingIcon = {
                                                            Icon(
                                                                contentDescription = null,
                                                                imageVector = Icons.Default.CloudUpload
                                                            )
                                                        }
                                                    )

                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }

            if (openModal) {
                Confirm(
                    body = "Essa ação finaliza a execução e envia ao sistema, deseja continuar?",
                    confirm = {
                        markAsFinished(contractId)
                        openModal = false
                    },
                    cancel = {
                        openModal = false
                    }
                )
            }
        }

    }
}


@Preview
@Composable
fun PrevContentCitiesScreen() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val values =
        listOf(
            InstallationView(
                id = UUID.randomUUID().toString(),
                contractId = 1,
                contractor = "Contagem",
                executionStatus = "PENDING",
                type = "PreMeasurementInstallation",
                itemsQuantity = 12,
                creationDate = "",
                streetsQuantity = 3,
            ),
            InstallationView(
                contractId = 1,
                contractor = "Ibrite",
                executionStatus = "PENDING",
                type = "",
                itemsQuantity = 12,
                creationDate = "PreMeasurementInstallation",
                id = UUID.randomUUID().toString(),
                streetsQuantity = 2,
            ),
            InstallationView(
                contractId = 1,
                contractor = "Belo Horizonte",
                executionStatus = "PENDING",
                type = "",
                itemsQuantity = 12,
                creationDate = "",
                id = UUID.randomUUID().toString(),
                streetsQuantity = 3,
            ),
        )

    ContentCitiesScreen(
        title = "Execuções sem pré-medição",
        executions = values,
        navController = rememberNavController(),
        isSyncing = false,
        select = { _, _, _ -> },
        error = "Você já pode começar com o que temos por aqui! Assim que a conexão voltar, buscamos o restante automaticamente — ou puxe para atualizar agora mesmo.",
        refresh = {},
        stockDataSize = 0
    )
}

