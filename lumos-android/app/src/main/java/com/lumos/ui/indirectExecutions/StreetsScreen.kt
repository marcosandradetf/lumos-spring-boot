package com.lumos.ui.indirectExecutions

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.data.repository.ExecutionStatus
import com.lumos.domain.model.IndirectExecution
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.IndirectExecutionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StreetsScreen(
    contractId: Long,
    contractor: String,
    indirectExecutionViewModel: IndirectExecutionViewModel,
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
    var executions by remember { mutableStateOf<List<IndirectExecution>>(emptyList()) }
    val isSyncing by indirectExecutionViewModel.isSyncing.collectAsState()
    val responseError by indirectExecutionViewModel.syncError.collectAsState()

    LaunchedEffect(Unit) {
        indirectExecutionViewModel.syncExecutions()
        val fetched = indirectExecutionViewModel.getExecutionsByContract(contractId)

        withContext(Dispatchers.Main) {
            executions = fetched
        }

    }

    var selectedStreetId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(selectedStreetId) {
        selectedStreetId?.let {
            onNavigateToExecution(it)
        }
    }

    Content(
        contractor=  contractor,
        executions = executions,
        onNavigateToHome = onNavigateToHome,
        onNavigateToMenu = onNavigateToMenu,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToNotifications = onNavigateToNotifications,
        context = context,
        navController = navController,
        notificationsBadge = notificationsBadge,
        isSyncing = isSyncing,
        pSelected = pSelected,
        select = { streetId ->
            selectedStreetId = streetId
        },
        error = responseError,
        refresh = {
            indirectExecutionViewModel.syncExecutions()
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Content(
    contractor: String,
    executions: List<IndirectExecution>,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    isSyncing: Boolean,
    pSelected: Int,
    select: (Long) -> Unit,
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


            AnimatedVisibility(visible = !isSyncing) {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (error != null) 60.dp else 0.dp),
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
                                        .padding(10.dp)
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


                                        // Informação extra
                                        if (execution.priority)
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(top=10.dp),
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
            IndirectExecution(
                streetId = 1,
                streetName = "Rua Dona Tina, 251",
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
            IndirectExecution(
                streetId = 2,
                streetName = "Rua Marcos Coelho Neto, 960",
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
            IndirectExecution(
                streetId = 3,
                streetName = "Rua Chopin, 35",
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




    Content(
        executions = values,
        onNavigateToHome = { },
        onNavigateToMenu = { },
        onNavigateToProfile = { },
        onNavigateToNotifications = { },
        context = fakeContext,
        navController = rememberNavController(),
        notificationsBadge = "12",
        isSyncing = false,
        pSelected = BottomBar.HOME.value,
        select = {},
        error = "Você já pode começar com o que temos por aqui! Assim que a conexão voltar, buscamos o restante automaticamente — ou puxe para atualizar agora mesmo.",
        refresh = {},
        contractor = "contractor"
    )
}

