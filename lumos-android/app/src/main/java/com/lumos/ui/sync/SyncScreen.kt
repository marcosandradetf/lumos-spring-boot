package com.lumos.ui.sync

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.SyncViewModel

@Composable
fun SyncScreen(
    context: Context,
    navController: NavHostController,
    currentNotifications: String,
    syncViewModel: SyncViewModel
) {
    val syncItems by syncViewModel.syncItems.collectAsState()
    val loading by syncViewModel.loading.collectAsState()
    val error by syncViewModel.error.collectAsState()

    LaunchedEffect(Unit) {

    }

    SyncScreenContent(
        syncItems,
        loading,
        error,
        currentNotifications,
        context,
        navController
    )

}

@Composable
fun SyncScreenContent(
    syncItems: List<SyncQueueEntity>,
    loading: Boolean,
    error: String,
    currentNotifications: String,
    context: Context,
    navController: NavHostController
) {

    AppLayout(
        title = "Sincronizações pendentes",
        notificationsBadge = currentNotifications,
        context = context,
        navController = navController,
        navigateBack = { navController.navigate(Routes.PROFILE) },
        sliderNavigateToHome = { navController.navigate(Routes.HOME) },
        sliderNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
        sliderNavigateToMenu = { navController.navigate(Routes.MENU) },
        pSelected = BottomBar.PROFILE.value
    ) { _, snackBar ->

        if(error.isNotEmpty()) snackBar(error, null)

        if (loading)
            Box {
                Loading("Carregando")
            }
        else if (syncItems.isEmpty()) NothingData("Nenhuma sincronização pendente")
        else
            LazyColumn {
                items(syncItems) { syncItem ->
                    Column {
                        Row {
                            Text(syncItem.type)
                        }
                        Row {
                            Text(syncItem.relatedId.toString())
                        }

                        Button(
                            onClick = {

                            }
                        ) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            }

    }

}
