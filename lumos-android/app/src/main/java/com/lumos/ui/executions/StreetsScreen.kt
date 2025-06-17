package com.lumos.ui.executions

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.data.repository.ReservationStatus
import com.lumos.data.repository.ExecutionStatus
import com.lumos.domain.model.Execution
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.ExecutionViewModel
import androidx.core.net.toUri
import com.lumos.domain.model.Reserve
import com.lumos.ui.components.Confirm
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.lumos.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StreetsScreen(
    contractId: Long,
    contractor: String,
    executionViewModel: ExecutionViewModel,
    context: Context,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    navController: NavHostController,
    notificationsBadge: String,
    pSelected: Int,
    onNavigateToExecution: (Long) -> Unit,
) {
    var executions by remember { mutableStateOf<List<Execution>>(emptyList()) }
    var reserves by remember { mutableStateOf<List<Reserve>>(emptyList()) }

    val isSyncing by executionViewModel.isSyncing.collectAsState()
    val responseError by executionViewModel.syncError.collectAsState()
    val isLoadingReserves by executionViewModel.isLoadingReserves.collectAsState()
    var showAlert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        executionViewModel.syncExecutions()
        val fetched = executionViewModel.getExecutionsByContract(contractId)

        withContext(Dispatchers.Main) {
            executions = fetched
        }

    }

    var selectedStreetId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(selectedStreetId) {
        if (selectedStreetId != null) {
            val fetchedReserves = executionViewModel.getReservesOnce(
                selectedStreetId!!,
                listOf(ReservationStatus.APPROVED)
            )

            withContext(Dispatchers.Main) {
                reserves = fetchedReserves

                if (reserves.isNotEmpty()) {
                    showAlert = true
                } else {
                    onNavigateToExecution(selectedStreetId!!)
                }
                selectedStreetId = null
            }
        }
    }


    Content(
        contractor=  contractor,
        executions = executions,
        reserves = reserves,
        onNavigateToHome = onNavigateToHome,
        onNavigateToMenu = onNavigateToMenu,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToNotifications = onNavigateToNotifications,
        context = context,
        navController = navController,
        notificationsBadge = notificationsBadge,
        isSyncing = isSyncing,
        isLoadingReserves = isLoadingReserves,
        pSelected = pSelected,
        select = { streetId ->
            selectedStreetId = streetId
        },
        alert = showAlert,
        onDismiss = {
            showAlert = false
        },
        onConfirmed = { streetId ->
            executionViewModel.setReserveStatus(streetId, ReservationStatus.COLLECTED)
            executionViewModel.setExecutionStatus(streetId, ExecutionStatus.IN_PROGRESS)
            executionViewModel.queueSyncFetchReservationStatus(
                streetId,
                ReservationStatus.COLLECTED,
                context
            )
            executionViewModel.queueSyncStartExecution(
                streetId,
                context
            )
            onNavigateToExecution(streetId)
        },
        error = responseError,
        refresh = {
            executionViewModel.syncExecutions()
        }
    )
}

@Composable
fun PendingMaterialsAlert(
    reserves: List<Reserve>,
    onDismiss: () -> Unit = {},
    onConfirmed: (Long) -> Unit,
    context: Context
) {
    val groupedReserves = reserves.groupBy { it.depositId }
    var confirmModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        // Alerta superior tipo "balao do WhatsApp"
        Card(
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Pendência Encontrada",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Existem materiais pendentes de coleta para essa execução.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LazyColumn {
            groupedReserves.forEach { (_, reservesForDeposit) ->

                val firstReserve = reservesForDeposit.first()

                item {
                    // --- Bloco de contato + infos do depósito ---
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        // Contato
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                firstReserve.stockistName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = "tel:${firstReserve.phoneNumber}".toUri()
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Ligar",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Detalhes
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warehouse,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                firstReserve.depositName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                firstReserve.depositAddress,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.ProductionQuantityLimits,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${reservesForDeposit.size} Materiais",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Em caso de dúvida, entre em contato",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                }
            }
        }


        // final
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = { onDismiss() },
            ) {
                Text("Voltar", color = MaterialTheme.colorScheme.error)
            }

            TextButton(
                onClick = { confirmModal = true },
            ) {
                Text("Marcar todos como coletados e prosseguir")
            }

        }

        if (confirmModal)
            Confirm(
                body = "Confirma que já coletou todos os materiais?",
                confirm = {
                    onConfirmed(reserves.first().streetId)
                },
                cancel = {
                    confirmModal = false
                }
            )

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Content(
    contractor: String,
    executions: List<Execution>,
    reserves: List<Reserve>,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    isSyncing: Boolean,
    isLoadingReserves: Boolean,
    pSelected: Int,
    select: (Long) -> Unit,
    alert: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: (Long) -> Unit,
    error: String?,
    refresh: () -> Unit,
) {
    AppLayout(
        title = contractor,
        pSelected = pSelected,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        navigateBack = {
            navController.navigate(Routes.EXECUTION_SCREEN)
        },
        context = context,
        notificationsBadge = notificationsBadge
    ) { _, showSnackBar ->

        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { refresh() },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (error != null && executions.isNotEmpty()) {
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

            AnimatedVisibility(visible = alert && !isLoadingReserves) {
                PendingMaterialsAlert(
                    reserves = reserves,
                    onDismiss = { onDismiss() },
                    onConfirmed = { onConfirmed(it) },
                    context = context
                )
            }
            AnimatedVisibility(visible = !alert && !isLoadingReserves) {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (error != null) 60.dp else 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp) // Espaço entre os cards
                ) {

                    if (executions.isEmpty()) {
                        item {
                            NothingData(
                                "Nenhuma execução disponível no momento, volte mais tarde!"
                            )
                        }
                    }

                    items(executions) { execution -> // Iteração na lista
                        val objective =
                            if (execution.type == "INSTALLATION") "Instalação" else "Manutenção"
                        val status = when (execution.executionStatus) {
                            ExecutionStatus.PENDING -> "PENDENTE"
                            ExecutionStatus.IN_PROGRESS -> "EM PROGRESSO"
                            ExecutionStatus.FINISHED -> "FINALIZADO"
                            else -> "STATUS DESCONHECIDO"
                        }

                        Card(
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(3.dp)
                                .clickable {
                                    select(execution.streetId)
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
                                            .fillMaxHeight(0.7f)
                                            .padding(start = 20.dp)
                                            .width(4.dp)
                                            .background(
                                                color = if (execution.type == "INSTALLATION") MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.tertiary
                                            )
                                    )

                                    // Bolinha com ícone (no meio da linha)
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 10.dp) // posiciona sobre a linha
                                            .size(24.dp) // tamanho do círculo
                                            .clip(CircleShape)
                                            .background(
                                                color = if (execution.type == "INSTALLATION") MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.tertiary
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector =
                                                if (execution.type == "INSTALLATION") Icons.Default.Power
                                                else Icons.Default.Build,
                                            contentDescription = "Local",
                                            tint = Color.White,
                                            modifier = Modifier.size(
                                                if (execution.type == "INSTALLATION") 18.dp
                                                else 14.dp
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
                                                    text = execution.streetName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }

                                        // Informação extra
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "$objective de ${execution.itemsQuantity} Itens",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                                    .padding(5.dp)
                                            ) {
                                                Text(
                                                    text = status,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontSize = 12.sp
                                                )
                                            }

                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Responsável: ${execution.teamName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Informação extra
                                        if (execution.priority)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Column(
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector =
                                                            Icons.Default.Warning,
                                                        contentDescription = "Prioridade",
                                                        tint = Color(0xFFFC4705),
                                                        modifier = Modifier.size(22.dp)
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
    }
}


@Preview()
@Composable
fun PrevStreetsScreen() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val values =
        listOf(
            Execution(
                streetId = 1,
                streetName = "Rua Dona Tina, 251",
                teamName = "Equipe Norte",
                executionStatus = "PENDING",
                priority = true,
                type = "INSTALLATION",
                itemsQuantity = 7,
                creationDate = "",
                latitude = 0.0,
                longitude = 0.0,
                photoUri = "",
                contractId = 1,
                contractor = ""
            ),
            Execution(
                streetId = 2,
                streetName = "Rua Marcos Coelho Neto, 960",
                teamName = "Equipe Sul",
                executionStatus = ExecutionStatus.IN_PROGRESS,
                priority = false,
                type = "MAINTENANCE",
                itemsQuantity = 5,
                creationDate = "",
                latitude = 0.0,
                longitude = 0.0,
                photoUri = "",
                contractId = 1,
                contractor = ""
            ),
            Execution(
                streetId = 3,
                streetName = "Rua Chopin, 35",
                teamName = "Equipe BH",
                executionStatus = ExecutionStatus.FINISHED,
                priority = false,
                type = "INSTALLATION",
                itemsQuantity = 12,
                creationDate = "",
                latitude = 0.0,
                longitude = 0.0,
                photoUri = "",
                contractId = 1,
                contractor = ""

            ),
        )

    val reserves = listOf(
        Reserve(
            reserveId = 1,
            materialName = "LED 120W",
            materialQuantity = 12.0,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 1,
            depositName = "GALPÃO BH",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "Elton Melo",
            phoneNumber = "31999998090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 1,
            materialName = "BRAÇO DE 3,5",
            materialQuantity = 16.0,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 1,
            depositName = "GALPÃO BH",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "Elton Melo",
            phoneNumber = "31999998090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 1,
            materialName = "BRAÇO DE 3,5",
            materialQuantity = 16.0,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 1,
            depositName = "GALPÃO BH",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "Elton Melo",
            phoneNumber = "31999998090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 1,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 1,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090",
            requestUnit = "UN",
            contractId = -1
        )
    )


    Content(
        executions = values,
        reserves = reserves,
        onNavigateToHome = { },
        onNavigateToMenu = { },
        onNavigateToProfile = { },
        onNavigateToNotifications = { },
        context = fakeContext,
        navController = rememberNavController(),
        notificationsBadge = "12",
        isSyncing = false,
        isLoadingReserves = false,
        pSelected = BottomBar.HOME.value,
        select = {},
        alert = false,
        onDismiss = {},
        onConfirmed = {},
        error = "Você já pode começar com o que temos por aqui! Assim que a conexão voltar, buscamos o restante automaticamente — ou puxe para atualizar agora mesmo.",
        refresh = {},
        contractor = "contractor"
    )
}

