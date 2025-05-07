package com.lumos.ui.executions

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.work.impl.utils.forAll
import com.lumos.data.repository.Status
import com.lumos.domain.model.Execution
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.ExecutionViewModel
import com.lumos.utils.ConnectivityUtils
import com.lumos.utils.Utils
import java.time.Instant

@Composable
fun StreetsScreen(
    executionViewModel: ExecutionViewModel,
    connection: ConnectivityUtils,
    context: Context,
    pSelected: Int,
) {
    val executions by executionViewModel.executions.collectAsState()
    var internet by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (connection.isConnectedToInternet(context)) executionViewModel.syncExecutions()
        else internet = false

        executionViewModel.loadFlowExecutions()
    }

    LaunchedEffect(executions) {
        if (executions.isNotEmpty() && internet) loading = false
    }

}

@Composable
fun Content(
    executions: List<Execution>,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    internet: Boolean,
    loading: Boolean,
    pSelected: Int,
    start: (Long) -> Unit,
    download: (Long) -> Unit
) {
    AppLayout(
        title = "Execuções",
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
        if (!internet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.NetworkCell, contentDescription = "Alert")
                Text(
                    text = "Conecte-se a internet para obter novas execuções",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        Loading(loading)
        if (!loading && internet)
            NothingData(
                executions.size,
                "Nenhuma execução disponível no momento, volte mais tarde!"
            )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp, top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp) // Espaço entre os cards
        ) {
            items(executions) { execution -> // Iteração na lista
//                val createdAt = "Criado por ${contract.createdBy} há ${
//                    Utils.timeSinceCreation(
//                        Instant.parse(contract.createdAt)
//                    )
//                }"
                val objective = if (execution.type == "INSTALLATION") "Instalação" else "Manutenção"
                val status = when (execution.executionStatus) {
                    Status.PENDING -> "Pendente"
                    Status.IN_PROGRESS -> "Em Progresso"
                    Status.FINISHED -> "Finalizado"
                    else -> "Status Desconhecido"
                }
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
                                        modifier = Modifier.clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFFEDEBF6))
                                            .padding(5.dp)
                                    ) {
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
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


@Preview(showBackground = true)
@Composable
fun PrevStreetsScreen() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val values =
        listOf(
            Execution(
                streetId = 1,
                streetName = "Rua Dona Tina, 251",
                teamId = 12,
                teamName = "Equipe Norte",
                executionStatus = "PENDING",
                priority = true,
                type = "INSTALLATION",
                itemsQuantity = 7,
                creationDate = ""
            ),
            Execution(
                streetId = 2,
                streetName = "Rua Marcos Coelho Neto, 960",
                teamId = 12,
                teamName = "Equipe Sul",
                executionStatus = Status.IN_PROGRESS,
                priority = false,
                type = "MAINTENANCE",
                itemsQuantity = 5,
                creationDate = ""
            ),
            Execution(
                streetId = 3,
                streetName = "Rua Chopin, 35",
                teamId = 12,
                teamName = "Equipe BH",
                executionStatus = Status.FINISHED,
                priority = false,
                type = "INSTALLATION",
                itemsQuantity = 12,
                creationDate = ""
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
        internet = false,
        loading = false,
        pSelected = BottomBar.HOME.value,
        start = {},
        download = {}
    )
}