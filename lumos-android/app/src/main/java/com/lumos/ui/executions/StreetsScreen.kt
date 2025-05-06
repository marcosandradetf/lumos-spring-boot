package com.lumos.ui.executions

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCell
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    context: Context
) {
    val executions by executionViewModel.executions.collectAsState()
    var internet by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (connection.isConnectedToInternet(context)) executionViewModel.syncExecutions()
        else internet = false

        executionViewModel.loadFlowExecutions(Status.PENDING)
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
    start: (Long) -> Unit,
    download: (Long) -> Unit
) {
    AppLayout(
        title = "Execuções",
        pSelected = BottomBar.HOME.value,
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
                                Text(
                                    text = execution.streetName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Expandir",
                                    tint = MaterialTheme.colorScheme.primary
                                )

                            }

                            // Informação extra
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTimeFilled,
                                    contentDescription = "Horário",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp) // Ajuste do tamanho do ícone
                                )
//                                Text(
//                                    modifier = Modifier.padding(start = 5.dp),
//                                    text = createdAt,
//                                    style = MaterialTheme.typography.bodySmall,
//                                    fontWeight = FontWeight.Light,
//                                    color = MaterialTheme.colorScheme.onSurface
//                                )
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
                teamName = "Norte",
                executionStatus = "PENDING",
                priority = true,
                type = "INSTALLATION",
                itemsQuantity = 5,
                creationDate = ""
            ),
            Execution(
                streetId = 2,
                streetName = "Rua Marcos Coelho Neto, 960",
                teamId = 12,
                teamName = "Norte",
                executionStatus = "PENDING",
                priority = false,
                type = "INSTALLATION",
                itemsQuantity = 5,
                creationDate = ""
            ),
            Execution(
                streetId = 3,
                streetName = "Rua Chopin, 35",
                teamId = 12,
                teamName = "Norte",
                executionStatus = "PENDING",
                priority = false,
                type = "INSTALLATION",
                itemsQuantity = 5,
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
        start = {},
        download = {}
    )
}