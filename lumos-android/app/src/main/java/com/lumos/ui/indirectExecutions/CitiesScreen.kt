package com.lumos.ui.indirectExecutions

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
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SentimentVerySatisfied
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
import com.lumos.domain.model.ExecutionHolder
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.DirectExecutionViewModel
import com.lumos.ui.viewmodel.IndirectExecutionViewModel

@Composable
fun CitiesScreen(
    indirectExecutionViewModel: IndirectExecutionViewModel,
    directExecutionViewModel: DirectExecutionViewModel,
    context: Context,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    navController: NavHostController,
    notificationsBadge: String,
    pSelected: Int,
    onNavigateToStreetScreen: (Long, String) -> Unit,
    roles: Set<String>,
    directExecution: Boolean
) {
    val requiredRoles = setOf("MOTORISTA", "ELETRICISTA")
    val title = if(directExecution) "Execuções sem pré-medição" else "Execuções com pré-medição"

//    val allExecutions by executionViewModel.executions.collectAsState()
    val allExecutions by if (directExecution) {
        directExecutionViewModel.directExecutions.collectAsState()
    } else {
        indirectExecutionViewModel.executions.collectAsState()
    }

    val isSyncing by indirectExecutionViewModel.isSyncing.collectAsState()
    val responseError by indirectExecutionViewModel.syncError.collectAsState()
    var executions by remember { mutableStateOf<List<ExecutionHolder>>(emptyList()) }


    LaunchedEffect(Unit) {
        if (!roles.any { it in requiredRoles }) {
            navController.navigate(Routes.NO_ACCESS + "/Execuções")
        }

        if (directExecution)
            directExecutionViewModel.syncExecutions()
        else
            indirectExecutionViewModel.syncExecutions()
    }

    LaunchedEffect(allExecutions) {
        executions = allExecutions
            .groupBy { it.contractId }
            .map { (_, list) -> list.first() } // ou .last() se quiser o último
    }

    ContentCitiesScreen(
        title = title,
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
        select = { contractId, contractor ->
            if (directExecution) null
            else onNavigateToStreetScreen(contractId, contractor)
        },
        error = responseError,
        refresh = {
            if (directExecution)
                directExecutionViewModel.syncExecutions()
            else
                indirectExecutionViewModel.syncExecutions()
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentCitiesScreen(
    title: String,
    executions: List<ExecutionHolder>,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    isSyncing: Boolean,
    pSelected: Int,
    select: (Long, String) -> Unit,
    error: String?,
    refresh: () -> Unit,
) {

    AppLayout(
        title = title,
        pSelected = pSelected,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        navigateBack = onNavigateToMenu,
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

                    Card(
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(3.dp)
                            .clickable {
                                select(execution.contractId, execution.contractor)
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
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(5.dp)
                                        ) {
                                            Text(
                                                text = "PENDENTE",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontSize = 12.sp
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


@Preview()
@Composable
fun PrevContentCitiesScreen() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val values =
        listOf(
            ExecutionHolder(
                contractId = 1,
                contractor = "Contagem",
                executionStatus = "PENDING",
                type = "",
                itemsQuantity = 12,
                creationDate = "",
            ),
            ExecutionHolder(
                contractId = 1,
                contractor = "Ibrite",
                executionStatus = "PENDING",
                type = "",
                itemsQuantity = 12,
                creationDate = "",
            ),
            ExecutionHolder(
                contractId = 1,
                contractor = "Belo Horizonte",
                executionStatus = "PENDING",
                type = "",
                itemsQuantity = 12,
                creationDate = "",
            ),
        )

    ContentCitiesScreen(
        title = "Execuções sem pré-medição",
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
        select = { _, _ -> },
        error = "Você já pode começar com o que temos por aqui! Assim que a conexão voltar, buscamos o restante automaticamente — ou puxe para atualizar agora mesmo.",
        refresh = {}
    )
}

