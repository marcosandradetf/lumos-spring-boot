package com.lumos.ui.sync

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.Alert
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.FinishScreen
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NothingData
import com.lumos.viewmodel.SyncViewModel
import com.lumos.worker.SyncTypes
import kotlinx.coroutines.delay

@Composable
fun SyncDetailsScreen(
    applicationContext: Context,
    context: Context,
    navController: NavHostController,
    currentNotifications: String,
    syncViewModel: SyncViewModel,
    type: String,
    lastRoute: String? = null
) {
    val syncItems by syncViewModel.syncItems.collectAsState()
    var streets by remember { mutableStateOf<List<DirectExecutionStreet>>(emptyList()) }

    val loading by syncViewModel.loading.collectAsState()
    val message by syncViewModel.message.collectAsState()

    LaunchedEffect(Unit) {
        when (type) {
            SyncTypes.POST_DIRECT_EXECUTION -> {
                syncViewModel.getItems(listOf(type))
                val streetIds = syncItems.map { it.relatedId!! }
                streets = syncViewModel.getStreets(streetIds)
            }

            SyncTypes.POST_MAINTENANCE -> {
                syncViewModel.getItems(
                    listOf(
                        SyncTypes.POST_MAINTENANCE,
                        SyncTypes.SYNC_STOCK,
                        SyncTypes.POST_ORDER,
                        SyncTypes.POST_MAINTENANCE_STREET,
                        SyncTypes.POST_DIRECT_EXECUTION,
                        SyncTypes.FINISHED_DIRECT_EXECUTION,
                        SyncTypes.POST_INDIRECT_EXECUTION
                    )
                )
            }

            else -> {
                syncViewModel.getItems(
                    listOf(type)
                )
            }
        }
    }

    if (type == SyncTypes.POST_DIRECT_EXECUTION) {
        SyncDetailsStreetsContent(
            streets,
            syncItems,
            type,
            loading,
            message,
            currentNotifications,
            context,
            navController,
            retry = {
                syncViewModel.retryDirectExecution(
                    relatedId = it,
                    type = type,
                    context = applicationContext,
                )

                streets = streets.filter { s -> s.directStreetId != it }
            },
            cancel = {
                syncViewModel.cancelDirectExecution(
                    relatedId = it,
                    type = type,
                    context = applicationContext,
                )


                streets = streets.filter { s -> s.directStreetId != it }
            }
        )
    } else {
        SyncDetailsMaintenanceContent(
            syncItems,
            type,
            loading,
            message,
            currentNotifications,
            navController,
            lastRoute = lastRoute,
            retry = { id ->
                syncViewModel.retryById(
                    id = id,
                    context = applicationContext,
                )

            },
            cancel = { id, count ->
                if (count == 0) {
                    syncViewModel.setMessage("Não é permitido cancelar itens sem falha, no menu clique em tentar novamente.")
                } else {
                    syncViewModel.cancelById(
                        id = id,
                        context = applicationContext,
                    )

                }
            }
        )
    }

}

@Composable
fun SyncDetailsMaintenanceContent(
    syncItems: List<SyncQueueEntity>,
    syncType: String,
    loading: Boolean,
    message: String,
    currentNotifications: String,
    navController: NavHostController,
    lastRoute: String?,
    retry: (Long) -> Unit,
    cancel: (Long, Int) -> Unit
) {
    val type = when (syncType) {
        SyncTypes.POST_DIRECT_EXECUTION -> "Instalação (sem pré-medição) - Registro em campo"
        SyncTypes.FINISHED_DIRECT_EXECUTION -> "Execução (sem pré-medição) - Finalização"

        SyncTypes.POST_INDIRECT_EXECUTION -> "Instalação (com pré-medição) - Registro em campo"

        SyncTypes.POST_MAINTENANCE -> "Manutenção - Finalização"
        SyncTypes.POST_MAINTENANCE_STREET -> "Manutenção - Registro em campo"
        SyncTypes.POST_PRE_MEASUREMENT -> "Pré-medição"

        SyncTypes.SYNC_STOCK -> "Dados de estoque"
        SyncTypes.POST_ORDER -> "Requisição de materiais"

        SyncTypes.UPDATE_TEAM -> "Confirmação de Equipe"

        else -> syncType
    }

    var expandedItemId by remember { mutableStateOf<Long?>(null) }
    var syncItem by remember { mutableStateOf<SyncQueueEntity?>(null) }

    val selectedIcon =
        when (lastRoute) {
            Routes.MAINTENANCE -> BottomBar.MAINTENANCE.value
            Routes.STOCK -> BottomBar.STOCK.value
            Routes.HOME -> BottomBar.HOME.value
            else -> BottomBar.MORE.value
        }

    if (syncItems.isEmpty()) {
        FinishScreen(
            screenTitle = "Fila de sincronização",
            navigateBack = {
                navController.navigate(Routes.HOME)
            },
            messageTitle = "Nenhuma pendência",
            messageBody = "Nenhuma sincronização pendente",
            navController = navController,
            clickBack = {
                navController.navigate(Routes.HOME)
            }
        )
//            NothingData("Nenhuma pendência de sincronização")
    } else
        AppLayout(
            title = "Fila de sincronização",
            selectedIcon = selectedIcon,
            notificationsBadge = currentNotifications,
            navigateToMore = { navController.navigate(Routes.MORE) },
            navigateToHome = { navController.navigate(Routes.HOME) },
            navigateBack = {
                navController.popBackStack()
            },
            navigateToStock = {
                navController.navigate(Routes.STOCK)
            },
            navigateToMaintenance = {
                navController.navigate(Routes.MAINTENANCE)
            },
            navigateToExecutions = {
                navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
            }
        ) { modifier, snackBar ->

            if (message.isNotBlank()) {
                snackBar(message, null, null)
            }

            if (loading) Loading("Reprocessando fila")
            else {
                if (syncItem != null) {
                    Alert(
                        title = "Motivo da falha",
                        body = syncItem?.errorMessage
                            ?: "Não houve uma falha identificada, no menu anterior clique em tentar novamente.",
                        confirm = {
                            syncItem = null
                        }
                    )
                }
                LazyColumn(
                    modifier = modifier,
                ) {
                    item {
                        Column {

                            Text(
                                text = "Clique no menu para ver as opções",
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
                    }

                    items(
                        items = syncItems,
                        key = { it.id }
                    ) { m ->

                        val title = when (m.type) {
                            SyncTypes.POST_DIRECT_EXECUTION -> "Instalação (sem pré-medição) - Registro em campo"
                            SyncTypes.FINISHED_DIRECT_EXECUTION -> "Execução (sem pré-medição) - Finalização"

                            SyncTypes.POST_INDIRECT_EXECUTION -> "Instalação (com pré-medição) - Registro em campo"

                            SyncTypes.POST_MAINTENANCE -> "Manutenção - Finalização"
                            SyncTypes.POST_MAINTENANCE_STREET -> "Manutenção - Registro em campo"
                            SyncTypes.POST_PRE_MEASUREMENT -> "Pré-medição"

                            SyncTypes.SYNC_STOCK -> "Dados de estoque"
                            SyncTypes.POST_ORDER -> "Requisição de materiais"

                            SyncTypes.UPDATE_TEAM -> "Confirmação de Equipe"

                            else -> m.type
                        }

                        Box(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        "Enviar $title"
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.SyncProblem,
                                        contentDescription = "Rua"
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = {
                                        expandedItemId =
                                            if (expandedItemId == m.id) null else m.id
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
                                expanded = expandedItemId == m.id,
                                onDismissRequest = { expandedItemId = null }
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        expandedItemId = null
                                        retry(m.id)
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
                                            syncItems.find { it.id == m.id }
                                    },
                                    text = { Text("Exibir motivo da falha") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Error, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        expandedItemId = null
                                        cancel(m.id, m.attemptCount)
                                    },
                                    text = { Text("Cancelar o envio") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Cancel, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

        }
}

@Composable
fun SyncDetailsStreetsContent(
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
        selectedIcon = BottomBar.MORE.value,
        notificationsBadge = currentNotifications,
        navigateToMore = { navController.navigate(Routes.MORE) },
        navigateToHome = { navController.navigate(Routes.HOME) },
        navigateBack = { navController.popBackStack() }
    ) { modifier, snackBar ->

        if (message.isNotBlank() && message != lastMessageShown) {
            snackBar(message, null, null)
            lastMessageShown = message
        }

        if (loading) Loading("Reprocessando fila")
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
//
//@Preview
//@Composable
//fun PrevSyncDetails() {
//    SyncDetailsStreetsContent(
//        streets = listOf(
//            DirectExecutionStreet(
//                directStreetId = 1,
//                address = "Rua 41",
//                deviceId = "",
//                directExecutionId = 1,
//                description = "",
//            )
//        ),
//        syncItems = emptyList(),
//        syncType = SyncTypes.POST_DIRECT_EXECUTION,
//        loading = false,
//        message = "",
//        currentNotifications = "10",
//        context = LocalContext.current,
//        navController = rememberNavController(),
//        retry = {},
//        cancel = {}
//    )
//}
