package com.lumos.ui.sync

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.Alert
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.SyncViewModel
import com.lumos.worker.SyncTypes

@Composable
fun SyncDetailsScreen(
    applicationContext: Context,
    context: Context,
    navController: NavHostController,
    currentNotifications: String,
    syncViewModel: SyncViewModel,
    type: String,
) {
    var syncItems by remember { mutableStateOf<List<SyncQueueEntity>>(emptyList()) }
    var streets by remember { mutableStateOf<List<DirectExecutionStreet>>(emptyList()) }

    val loading by syncViewModel.loading.collectAsState()
    val message by syncViewModel.message.collectAsState()

    LaunchedEffect(Unit) {
        syncItems = syncViewModel.getItems(type)
        if (type == SyncTypes.POST_DIRECT_EXECUTION) {
            val streetIds = syncItems.map { it.relatedId!! }
            streets = syncViewModel.getStreets(streetIds)
        }
    }

    SyncDetailsScreenContent(
        streets,
        syncItems,
        type,
        loading,
        message,
        currentNotifications,
        context,
        navController,
        retry = {
            syncViewModel.retry(
                relatedId = it,
                type = type,
                context = applicationContext,
            )

            syncItems = syncItems.filter { s -> s.relatedId != it }
            streets = streets.filter { s -> s.directStreetId != it }
        },
        cancel = {
            syncViewModel.cancel(
                relatedId = it,
                type = type,
            )

            syncItems = syncItems.filter { s -> s.relatedId != it }
            streets = streets.filter { s -> s.directStreetId != it }
        }
    )

}

@Composable
fun SyncDetailsScreenContent(
    streets: List<DirectExecutionStreet>,
    syncItems: List<SyncQueueEntity>,
    syncType: String,
    loading: Boolean,
    message: String,
    currentNotifications: String,
    context: Context,
    navController: NavHostController,
    retry: (Long) -> Unit,
    cancel: (Long) -> Unit
) {
    val type = when (syncType) {
        SyncTypes.POST_DIRECT_EXECUTION -> "Sincronizaçoes de execuções"
        else -> syncType
    }
    var expandedItemId by remember { mutableStateOf<Long?>(null) }
    var syncItem by remember { mutableStateOf<SyncQueueEntity?>(null) }
    var lastMessageShown by remember { mutableStateOf("") }

    AppLayout(
        title = type,
        notificationsBadge = currentNotifications,
        context = context,
        navController = navController,
        navigateBack = { navController.navigate(Routes.PROFILE) },
        sliderNavigateToHome = { navController.navigate(Routes.HOME) },
        sliderNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
        sliderNavigateToMenu = { navController.navigate(Routes.MENU) },
        pSelected = BottomBar.PROFILE.value
    ) { modifier, snackBar ->

        if (message.isNotBlank() && message != lastMessageShown) {
            snackBar(message, null)
            lastMessageShown = message
        }

        if (loading) Loading("Carregando")
        else if (streets.isEmpty()) NothingData("Nenhuma rua encontrada")
        else {
            if (syncItem != null) {
                Alert(
                    title = "Motivo da falha",
                    body = syncItem?.errorMessage ?: "Motivo não registrado",
                    confirm = {
                        syncItem = null
                    }
                )
            }
            LazyColumn(
                modifier = modifier,
            ) {
                items(
                    items = streets,
                    key = { it.directStreetId }
                ) { street ->

                    Box(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        ListItem(
                            headlineContent = { Text(street.address) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.SyncProblem,
                                    contentDescription = "Rua"
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    expandedItemId =
                                        if (expandedItemId == street.directStreetId) null else street.directStreetId
                                }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent // importante se já tiver background no Box
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = expandedItemId == street.directStreetId,
                            onDismissRequest = { expandedItemId = null }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    expandedItemId = null
                                    retry(street.directStreetId)
                                },
                                text = { Text("Tentar Novamente") },
                                leadingIcon = {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expandedItemId = null
                                    syncItem =
                                        syncItems.find { it.relatedId == street.directStreetId }
                                },
                                text = { Text("Exibir motivo da falha") },
                                leadingIcon = {
                                    Icon(Icons.Default.Error, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expandedItemId = null
                                    cancel(street.directStreetId)
                                },
                                text = { Text("Cancelar o envio") },
                                leadingIcon = {
                                    Icon(Icons.Default.Cancel, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expandedItemId = null
                                    Toast.makeText(
                                        context,
                                        "Recurso não implementado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                text = { Text("Editar rua") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        }

    }

}

@Preview
@Composable
fun PrevSyncDetails() {
    SyncDetailsScreenContent(
        streets = listOf(
            DirectExecutionStreet(
                directStreetId = 1,
                address = "Rua 41",
                deviceId = "",
                directExecutionId = 1,
                description = "",
            )
        ),
        syncItems = emptyList(),
        syncType = SyncTypes.POST_DIRECT_EXECUTION,
        loading = false,
        message = "",
        currentNotifications = "10",
        context = LocalContext.current,
        navController = rememberNavController(),
        retry = {},
        cancel = {}
    )
}
