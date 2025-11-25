package com.lumos.ui.sync

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val syncItems by syncViewModel.syncItemsTypes.collectAsState()
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
        title = "Fila de sincronização",
        selectedIcon = BottomBar.MORE.value,
        notificationsBadge = currentNotifications,
        navigateToMore = { navController.navigate(Routes.MORE) },
        navigateToHome = { navController.navigate(Routes.HOME) },
        navigateBack = { navController.navigate(Routes.PROFILE) },
        navigateToStock = { navController.navigate(Routes.STOCK) },
        navigateToMaintenance = { navController.navigate(Routes.MAINTENANCE) },
        navigateToExecutions = { navController.navigate(Routes.INSTALLATION_HOLDER) }
    ) { modifier, snackBar ->

        if (error.isNotEmpty()) snackBar(error, null, null)

        if (loading)
            Box {
                Loading("Carregando")
            }
        else if (syncItems.isEmpty()) NothingData("Nenhuma sincronização pendente")
        else
            LazyColumn(
                modifier = modifier
            ) {
                item {
                    Text(
                        text = "Selecione uma categoria abaixo",
                        modifier = Modifier
                            .padding(
                                bottom = 20.dp,
                                start = 10.dp
                            )
                            .fillMaxWidth(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp

                    )
                }

                items(syncItems) { syncType ->
                    val type = when (syncType) {
                        SyncTypes.POST_DIRECT_EXECUTION,
                        SyncTypes.FINISHED_DIRECT_EXECUTION,
                        SyncTypes.POST_INDIRECT_EXECUTION -> "Sincronização - Instalação de Led"

                        SyncTypes.POST_MAINTENANCE, SyncTypes.POST_MAINTENANCE_STREET -> "Sincronização de Manutenção"

                        SyncTypes.POST_PRE_MEASUREMENT -> "Sincronização de Pré-medição"

                        SyncTypes.SYNC_STOCK, SyncTypes.POST_ORDER -> "Sincronização de Estoque"

                        SyncTypes.UPDATE_TEAM -> "Sincronização de Equipe"

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
                            headlineContent = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(type)
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = type,
                                    )
                                }
                            },
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
                                    navController
                                        .navigate("${Routes.SYNC}/${syncType}")
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
