package com.lumos.ui.sync

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NothingData
import com.lumos.viewmodel.SyncViewModel
import com.lumos.worker.SyncTypes

@Composable
fun SyncScreen(
    context: Context,
    navController: NavHostController,
    currentNotifications: String,
    syncViewModel: SyncViewModel
) {
    val syncItems by syncViewModel.syncItems.collectAsState()
    val loading by syncViewModel.loading.collectAsState()
    val error by syncViewModel.message.collectAsState()

    LaunchedEffect(Unit) {
        syncViewModel.syncFlowItems()
    }

    SyncScreenContent(
        syncItems,
        loading,
        error,
        currentNotifications,
        navController
    )

}

@Composable
fun SyncScreenContent(
    syncItems: List<String>,
    loading: Boolean,
    error: String,
    currentNotifications: String,
    navController: NavHostController
) {

    AppLayout(
        title = "Sincronizações com falha",
        selectedIcon = BottomBar.MORE.value,
        notificationsBadge = currentNotifications,
        navigateToMore = { navController.navigate(Routes.MORE) },
        navigateToHome = { navController.navigate(Routes.HOME) },
        navigateBack = { navController.navigate(Routes.PROFILE) },
        navigateToStock = { navController.navigate(Routes.STOCK) },
        navigateToMaintenance = { navController.navigate(Routes.MAINTENANCE) },
        navigateToExecutions = { navController.navigate(Routes.DIRECT_EXECUTION_SCREEN) }
    ) { modifier, snackBar ->

        if (error.isNotEmpty()) snackBar(error, null)

        if (loading)
            Box {
                Loading("Carregando")
            }
        else if (syncItems.isEmpty()) NothingData("Nenhuma sincronização pendente")
        else
            LazyColumn(
                modifier = modifier
            ) {
                items(syncItems) { syncType ->
                    val type = when (syncType) {
                        SyncTypes.POST_DIRECT_EXECUTION -> "Execução (sem pré-medição) - Registro em campo"
                        SyncTypes.POST_MAINTENANCE -> "Manutenção - Envio"
                        SyncTypes.SYNC_STOCK -> "Sincronização de dados de estoque"
                        SyncTypes.POST_ORDER -> "Requisição de materiais"
                        SyncTypes.FINISHED_DIRECT_EXECUTION -> "Execução (sem pré-medição) - Finalização"
                        else -> syncType
                    }


                    Box(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {

                        ListItem(
                            colors = ListItemColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                headlineColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurface,
                                overlineColor = MaterialTheme.colorScheme.surface,
                                supportingTextColor = MaterialTheme.colorScheme.surface,
                                trailingIconColor = MaterialTheme.colorScheme.surface,
                                disabledHeadlineColor = MaterialTheme.colorScheme.surface,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.surface,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.surface
                            ),
                            headlineContent = { Text(type) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.SyncDisabled,
                                    contentDescription = type,
                                )
                            },
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    when (syncType) {
                                        SyncTypes.POST_DIRECT_EXECUTION ->
                                            navController
                                                .navigate("${Routes.SYNC}/${SyncTypes.POST_DIRECT_EXECUTION}")

                                        SyncTypes.POST_MAINTENANCE, SyncTypes.SYNC_STOCK, SyncTypes.POST_ORDER, SyncTypes.POST_PRE_MEASUREMENT,
                                        SyncTypes.POST_GENERIC, SyncTypes.POST_MAINTENANCE_STREET, SyncTypes.FINISHED_DIRECT_EXECUTION
                                            ->
                                            navController
                                                .navigate("${Routes.SYNC}/${SyncTypes.POST_MAINTENANCE}")
                                    }
                                }
                        )

                    }

                }
            }

    }

}

@Preview
@Composable
fun PrevSyncScreen() {
    SyncScreenContent(
        syncItems = listOf(SyncTypes.POST_DIRECT_EXECUTION),
        loading = false,
        error = "",
        currentNotifications = "10",
        navController = rememberNavController()
    )
}
