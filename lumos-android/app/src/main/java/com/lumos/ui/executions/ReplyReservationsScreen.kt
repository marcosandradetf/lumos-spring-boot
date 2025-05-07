package com.lumos.ui.executions

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.data.repository.Status
import com.lumos.domain.model.Execution
import com.lumos.domain.model.Reserve
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.ExecutionViewModel
import com.lumos.utils.ConnectivityUtils

@Composable
fun ReplyReservationsScreen(
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
    streetId: Long
) {
    val reserves by executionViewModel.reserves.collectAsState()
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(streetId) {
        // Inicia carregamento de reservas pendentes
        executionViewModel.loadFlowReserves(
            streetId = streetId,
            status = listOf(Status.PENDING)
        )
    }

    // A lógica de carregamento e navegação acontece de uma vez que as reservas mudam
    LaunchedEffect(reserves) {
        when {
            reserves.isNotEmpty() -> {
                loading = false
            }

            else -> {
                // Carrega as reservas rejeitadas caso não haja pendentes
                executionViewModel.loadFlowReserves(
                    streetId = streetId,
                    status = listOf(Status.REJECTED)
                )
            }
        }
    }

    // Quando as reservas ainda estiverem vazias após carregar "REJECTED", navega para execução
    LaunchedEffect(reserves) {
        if (reserves.isEmpty() && !loading) {
            onNavigateToExecution(streetId)
        }
    }


}


@Composable
fun ContentReply(
    reservations: List<Reserve>,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    loading: Boolean,
    pSelected: Int,
    reply: (Long, Boolean) -> Unit,
) {
    AppLayout(
        title = "Reservas",
        pSelected = pSelected,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        navigateBack = onNavigateToMenu,
        context = context,
        notificationsBadge = notificationsBadge
    ) {
        Loading(loading)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp, top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp) // Espaço entre os cards
        ) {
            items(reservations) { reserve -> // Iteração na lista
//                val objective = if (execution.type == "INSTALLATION") "Instalação" else "Manutenção"
//                val status = when (execution.executionStatus) {
//                    Status.PENDING -> "Pendente"
//                    Status.IN_PROGRESS -> "Em Progresso"
//                    Status.FINISHED -> "Finalizado"
//                    else -> "Status Desconhecido"
//                }
                Card(
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(3.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSecondary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                reply(reserve.reserveId, false)
                            }
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
//                                    .background(
//                                        color = if (execution.type == "INSTALLATION") MaterialTheme.colorScheme.primary
//                                        else MaterialTheme.colorScheme.tertiary
//                                    )
                            )

                            // Bolinha com ícone (no meio da linha)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .offset(x = 10.dp) // posiciona sobre a linha
                                    .size(24.dp) // tamanho do círculo
                                    .clip(CircleShape)
//                                    .background(
//                                        color = if (execution.type == "INSTALLATION") MaterialTheme.colorScheme.primary
//                                        else MaterialTheme.colorScheme.tertiary
//                                    ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HomeRepairService,
                                    contentDescription = "Local",
                                    tint = Color.White,
//                                    modifier = Modifier.size(
//                                        if (execution.type == "INSTALLATION") 18.dp
//                                        else 14.dp
//                                    )
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
                                            text = reserve.materialName,
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
                                        text = "Quantidade: ${reserve.materialQuantity}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
//                                    Box(
//                                        modifier = Modifier.clip(RoundedCornerShape(5.dp))
//                                            .background(Color(0xFFEDEBF6))
//                                            .padding(5.dp)
//                                    ) {
//                                        Text(
//                                            text = status,
//                                            style = MaterialTheme.typography.bodySmall,
//                                            fontWeight = FontWeight.Medium,
//                                            color = MaterialTheme.colorScheme.onSurface,
//                                            fontSize = 12.sp
//                                        )
//                                    }

                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
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


@Preview(showBackground = true)
@Composable
fun PrevReplyScreen() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val values =
        listOf(
            Reserve(
                reserveId = 1,
                materialId = 1,
                materialName = "LED 120W",
                materialQuantity = 17.0,
                reserveStatus = "PENDING",
                streetId = 1
            )
        )


    ContentReply(
        onNavigateToHome = { },
        onNavigateToMenu = { },
        onNavigateToProfile = { },
        onNavigateToNotifications = { },
        context = fakeContext,
        navController = rememberNavController(),
        notificationsBadge = "12",
        loading = false,
        pSelected = BottomBar.HOME.value,
        reply = { _, _ -> },
        reservations = values,
    )
}