package com.lumos.ui.installationholder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NotListedLocation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.InstallationView
import com.lumos.midleware.SecureStorage
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.repository.DirectExecutionRepository
import com.lumos.repository.PreMeasurementInstallationRepository
import com.lumos.repository.ViewRepository
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.NothingData
import com.lumos.utils.Utils
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

@Composable
fun InstallationHolderScreen(
    directExecutionRepository: DirectExecutionRepository,
    preMeasurementInstallationRepository: PreMeasurementInstallationRepository,
    viewRepository: ViewRepository,
    navController: NavHostController,
    roles: Set<String>,
    secureStorage: SecureStorage
) {
    val requiredRoles = setOf("MOTORISTA", "ELETRICISTA")

    val executions by viewRepository.getFlowInstallations(listOf("PENDING", "IN_PROGRESS"))
        .collectAsState(emptyList())

    var isSyncing by remember { mutableStateOf(false) }
    var responseError by remember { mutableStateOf<String?>(null) }
    var stockCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isSyncing = true
        val TWELVE_HOURS = 12 * 60 * 60 * 1000L

        val now = System.currentTimeMillis()

        val lastTeamCheck = secureStorage.getLastTeamCheck()
        val isStaleCheckTeam = now >= lastTeamCheck && (now - lastTeamCheck > TWELVE_HOURS)

        if (!roles.any { it in requiredRoles }) {
            navController.navigate(Routes.NO_ACCESS + "/${BottomBar.EXECUTIONS.value}/Instalações")
        }

        if (isStaleCheckTeam)
            navController.navigate("${Routes.TEAM_SCREEN}/${BottomBar.EXECUTIONS.value}")
        else {
            directExecutionRepository.syncDirectExecutions()
            preMeasurementInstallationRepository.syncExecutions()
        }

        stockCount = directExecutionRepository.countStock()

        isSyncing = false
    }

    ContentCitiesScreen(
        executions = executions,
        navController = navController,
        isSyncing = isSyncing,
        select = { id, type, contractor, contractId, creationDate, instructions ->
            if (type != "PreMeasurementInstallation") {
                navController.navigate(
                    "${Routes.DIRECT_EXECUTION_HOME_SCREEN}/$id/$contractor/$contractId/$creationDate/$instructions"
                )
            } else {
                navController.navigate(
                    "${Routes.PRE_MEASUREMENT_INSTALLATION_STREETS}/$id/$contractor/$contractId/$instructions"
                )
            }
        },
        error = responseError,
        refresh = {
            scope.launch {
                directExecutionRepository.syncDirectExecutions()
                preMeasurementInstallationRepository.syncExecutions()
            }
        },
        stockDataSize = stockCount
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentCitiesScreen(
    executions: List<InstallationView>,
    navController: NavHostController,
    isSyncing: Boolean,
    select: (String, String, String, Long?, String, String?) -> Unit,
    error: String?,
    refresh: () -> Unit,
    stockDataSize: Int
) {
    var showModal by remember { mutableStateOf(false) }

    AppLayout(
        title = "Instalações disponíveis",
        selectedIcon = BottomBar.EXECUTIONS.value,
        navigateToMore = { navController.navigate(Routes.MORE) },
        navigateToHome = { navController.navigate(Routes.HOME) },
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
                body = "Você precisa carregar os dados do estoque para criar uma nova instalação. Deseja fazer isso agora?",
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

                if (executions.isEmpty() && !isSyncing) {
                    item {
                        NothingData(
                            "Nenhuma execução disponível no momento, volte mais tarde!"
                        )
                    }
                }

                items(executions) { execution -> // Iteração na lista

                    val shape = RoundedCornerShape(16.dp)

                    ElevatedCard(
                        shape = shape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 10.dp)
                            .clickable {
                                if (stockDataSize > 0)
                                    select(
                                        execution.id,
                                        execution.type,
                                        execution.contractor,
                                        execution.contractId,
                                        execution.creationDate,
                                        execution.instructions
                                    )
                                else showModal = true
                            },
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp) // layout mais respirado
                        ) {

                            // ----------------------------------------------
                            // Cabeçalho com ícone circular + título
                            // ----------------------------------------------
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // Ícone com fundo suave
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Power,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = Utils.abbreviate(execution.contractor),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )

                                    Text(
                                        text = Utils.timeSinceCreation(execution.creationDate),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ----------------------------------------------
                            // Chips de Tipo da Execução e Status
                            // ----------------------------------------------

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                // Tipo da execução
                                val (tagText, tagColor, tagIcon) = when (execution.type) {
                                    "PreMeasurementInstallation" -> Triple(
                                        "Com Pré-Medição",
                                        MaterialTheme.colorScheme.primaryContainer,
                                        Icons.Default.Map
                                    )

                                    else -> Triple(
                                        "Sem Pré-Medição",
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        Icons.AutoMirrored.Filled.NotListedLocation
                                    )
                                }

                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            tagText,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            tagIcon,
                                            contentDescription = null
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = tagColor
                                    )
                                )

                                // Status
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            Utils.translateStatus(execution.executionStatus),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }

                        }
                    }


                }
            }
        }

    }
}


@Preview
@Composable
fun PrevContentCitiesScreen() {
    val values =
        listOf(
            InstallationView(
                id = UUID.randomUUID().toString(),
                contractor = "Etapa 8 - PREFEITURA ITAMBACURI",
                executionStatus = "PENDING",
                type = "PreMeasurementInstallation",
                itemsQuantity = 12,
                creationDate = Instant.now().toString(),
                streetsQuantity = 3,
            ),
            InstallationView(
                contractor = "Ibrite",
                executionStatus = "PENDING",
                type = "",
                itemsQuantity = 12,
                creationDate = "PreMeasurementInstallation",
                id = UUID.randomUUID().toString(),
                streetsQuantity = 2,
            ),
            InstallationView(
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
        executions = values,
        navController = rememberNavController(),
        isSyncing = false,
        select = { _, _, _, _, _, _ -> },
        error = "Você já pode começar com o que temos por aqui! Assim que a conexão voltar, buscamos o restante automaticamente — ou puxe para atualizar agora mesmo.",
        refresh = {},
        stockDataSize = 0
    )
}

