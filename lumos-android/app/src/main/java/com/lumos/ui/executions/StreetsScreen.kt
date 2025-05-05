package com.lumos.ui.executions

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCell
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.work.impl.utils.forAll
import com.lumos.data.repository.Status
import com.lumos.domain.model.Execution
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import com.lumos.ui.viewmodel.ExecutionViewModel
import com.lumos.utils.ConnectivityUtils

@Composable
fun StreetsScreen(
    executionViewModel: ExecutionViewModel,
    connection: ConnectivityUtils,
    context: Context
) {
    val executions by executionViewModel.executions.collectAsState()
    var internet by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (connection.isConnectedToInternet(context)) executionViewModel.syncExecutions()
        else internet = false

        executionViewModel.loadFlowExecutions(Status.PENDING)
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
                modifier = Modifier.fillMaxWidth()
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
                executionStatus = "PENDING"
            ),
            Execution(
                streetId = 2,
                streetName = "Rua Marcos Coelho Neto, 960",
                teamId = 12,
                teamName = "Norte",
                executionStatus = "PENDING"
            ),
            Execution(
                streetId = 3,
                streetName = "Rua Chopin, 35",
                teamId = 12,
                teamName = "Norte",
                executionStatus = "PENDING"
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
        start = {},
        download = {}
    )
}