package com.lumos.ui.maintenance

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Maintenance
import com.lumos.midleware.SecureStorage
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.CurrentScreenLoading
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NoInternet
import com.lumos.ui.components.NothingData
import com.lumos.viewmodel.ContractViewModel
import com.lumos.viewmodel.MaintenanceViewModel
import com.lumos.viewmodel.StockViewModel
import com.lumos.utils.Utils
import com.lumos.utils.Utils.abbreviate
import kotlinx.coroutines.delay
import java.util.UUID

enum class MaintenanceUIState {
    NEW,
    STREET,
    LIST,
    HOME
}

@Composable
fun MaintenanceScreen(
    maintenanceViewModel: MaintenanceViewModel,
    contractViewModel: ContractViewModel,
    stockViewModel: StockViewModel,
    navController: NavHostController,
    lastRoute: String?,
    secureStorage: SecureStorage
) {
    val viewModelAux: MaintenanceHomeViewModel = viewModel()
    val uiState by maintenanceViewModel.uiState.collectAsState()

    val stock by stockViewModel.stock.collectAsState()
    val contractLoading by contractViewModel.isSyncing.collectAsState()
    val contractMessage by contractViewModel.syncError.collectAsState()
    val hasInternet by contractViewModel.hasInternet.collectAsState()
    var forceLoading by remember { mutableStateOf(false) }
    val contracts by contractViewModel.contracts.collectAsState()

    var resync by remember { mutableIntStateOf(0) }

    val maintenanceMap by remember(uiState.maintenances) {
        Log.e("maintenances", uiState.maintenances.toString())

        derivedStateOf { uiState.maintenances.associateBy { it.maintenanceId } }
    }

    val maintenance by remember(uiState.maintenanceId, maintenanceMap) {
        Log.e("Maintenance", maintenanceMap.toString())

        derivedStateOf {
            uiState.maintenanceId?.toString()?.let { maintenanceMap[it] }
        }
    }

    val filteredStreets by remember(uiState.maintenanceId, uiState.maintenanceStreets) {
        derivedStateOf {
            uiState.maintenanceId?.toString()?.let { idString ->
                uiState.maintenanceStreets.filter { it.maintenanceId == idString }
            } ?: emptyList()
        }
    }

    val loading by remember(uiState.loading, forceLoading) {
        derivedStateOf {
            uiState.loading || forceLoading
        }
    }

    LaunchedEffect(Unit) {
        if(uiState.screenState != MaintenanceUIState.HOME) {
                val loadedMaintenances = uiState.maintenances

                val screenState = when {
                    loadedMaintenances.size == 1 -> {
                        forceLoading = true
                        maintenanceViewModel.setMaintenanceId(UUID.fromString(loadedMaintenances.first().maintenanceId))
                        MaintenanceUIState.HOME
                    }

                    else -> MaintenanceUIState.LIST
                }
                maintenanceViewModel.setScreenState(screenState)
            }
    }

    LaunchedEffect(resync) {
        if(uiState.screenState != MaintenanceUIState.HOME) {
            if (resync < 10) contractViewModel.syncContracts()
        }
    }

    if (uiState.contractSelected) {
        Log.e("entrou no contract", "contract")
        maintenanceViewModel.setScreenState(MaintenanceUIState.STREET)
        maintenanceViewModel.setContractSelected(false)
    }

    when (uiState.screenState) {
        MaintenanceUIState.NEW -> {
            NewMaintenanceContent(
                navController = navController,
                contracts = contracts,
                loading = contractLoading,
                resync = {
                    resync += 1
                },
                hasInternet = hasInternet,
                createMaintenance = { contractId ->
                    val newId = UUID.randomUUID()
                    maintenanceViewModel.insertMaintenance(
                        Maintenance(
                            maintenanceId = newId.toString(),
                            contractId = contractId,
                            pendingPoints = false,
                            quantityPendingPoints = null,
                            dateOfVisit = Utils.dateTime.toString(),
                            type = "",
                            status = "IN_PROGRESS",
                            executorsIds = null
                        )
                    )
                },
                back = {
                    maintenanceViewModel.resetFormState()
                    val screenState = if (uiState.maintenances.size == 1) {
                        forceLoading = true
                        maintenanceViewModel.setMaintenanceId(UUID.fromString(uiState.maintenances.first().maintenanceId))
                        MaintenanceUIState.HOME
                    } else {
                        MaintenanceUIState.LIST
                    }
                    maintenanceViewModel.setScreenState(screenState)
                }
            )
        }

        MaintenanceUIState.STREET -> {
            val maintenanceNonNull = maintenance
            if (uiState.maintenanceId != null && maintenanceNonNull?.contractor != null) {
                StreetMaintenanceContent(
                    maintenanceId = uiState.maintenanceId!!,
                    contractor = maintenanceNonNull.contractor,
                    navController = navController,
                    loading = loading,
                    back = {
                        val currentId = uiState.maintenanceId
                        maintenanceViewModel.resetFormState()
                        maintenanceViewModel.setMaintenanceId(currentId)
                        maintenanceViewModel.setScreenState(MaintenanceUIState.HOME)
                    },
                    saveStreet = { street, items ->
                        maintenanceViewModel.insertMaintenanceStreet(street, items)
                    },
                    lastRoute = lastRoute,
                    streetCreated = uiState.streetCreated,
                    newStreet = {
                        val currentMaintenanceId = uiState.maintenanceId
                        maintenanceViewModel.resetFormState()
                        maintenanceViewModel.setMaintenanceId(currentMaintenanceId)
                    },
                    stockData = stock,
                    message = uiState.message
                )
            } else {
                CurrentScreenLoading(
                    navController,
                    "Manutenções",
                    "Carregando dados da manutenção..."
                )
            }
        }


        MaintenanceUIState.LIST -> {
            MaintenanceListContent(
                stockSize = stock.size,
                maintenances = uiState.maintenances,
                navController = navController,
                loading = loading,
                selectMaintenance = { id ->
                    forceLoading = true
                    maintenanceViewModel.resetFormState()
                    maintenanceViewModel.setMaintenanceId(UUID.fromString(id))
                    maintenanceViewModel.setScreenState(MaintenanceUIState.HOME)
                },
                newMaintenance = {
                    maintenanceViewModel.resetFormState()
                    maintenanceViewModel.setScreenState(MaintenanceUIState.NEW)
                },
                secureStorage = secureStorage
            )
        }

        MaintenanceUIState.HOME -> {
            LaunchedEffect(uiState.maintenanceId) {
                if (uiState.maintenanceId != null) {
                    delay(300L) // um pequeno tempo para garantir que carregou
                    forceLoading = false
                }
            }

            if (uiState.finish) {
                AppLayout(
                    title = "Gerenciar manutenção",
                    selectedIcon = BottomBar.MAINTENANCE.value,
                    navigateBack = {
                        forceLoading = false
                        maintenanceViewModel.resetFormState()
                        viewModelAux.clear()
                        maintenanceViewModel.setScreenState(MaintenanceUIState.LIST)
                    },
                    navigateToHome = {
                        navController.navigate(Routes.HOME)
                    },
                    navigateToMore = {
                        navController.navigate(Routes.MORE)
                    },
                    navigateToStock = {
                        navController.navigate(Routes.STOCK)
                    },
                    navigateToExecutions = {
                        navController.navigate(Routes.INSTALLATION_HOLDER)
                    }
                ) { _, _ ->

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.8f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TaskAlt,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                    )
                                    .padding(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Missão cumprida!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Os dados serão enviados para o sistema.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = 0.5f),
                                onClick = {
                                    forceLoading = false
                                    maintenanceViewModel.resetFormState()
                                    viewModelAux.clear()
                                    maintenanceViewModel.setScreenState(MaintenanceUIState.LIST)
                                }
                            ) {
                                Text("Voltar")
                            }
                        }
                    }
                }
            } else {
                if (maintenance != null) {
                    MaintenanceHomeContent(
                        viewModel = viewModelAux,
                        maintenance = maintenance!!,
                        streets = filteredStreets,
                        maintenanceSize = uiState.maintenances.size,
                        navController = navController,
                        loading = loading,
                        newStreet = {
                            maintenanceViewModel.setScreenState(MaintenanceUIState.STREET)
                        },
                        newMaintenance = {
                            maintenanceViewModel.resetFormState()
                            maintenanceViewModel.setScreenState(
                                MaintenanceUIState.NEW
                            )
                        },
                        finishMaintenance = {
                            if (it != null)
                                maintenanceViewModel.finishMaintenance(it)
                        },
                        back = {
                            forceLoading = false
                            maintenanceViewModel.resetFormState()
                            maintenanceViewModel.setScreenState(
                                MaintenanceUIState.LIST
                            )
                        }
                    )
                } else {
                    CurrentScreenLoading(
                        navController,
                        "Manutenções",
                        "Carregando dados da manutenção..."
                    )
                }
            }
        }

        else -> {
            CurrentScreenLoading(navController, "Manutenções", "Carregando dados da manutenção...")
        }
    }

}

@Composable
fun NewMaintenanceContent(
    navController: NavHostController,
    contracts: List<Contract>,
    loading: Boolean,
    resync: () -> Unit,
    hasInternet: Boolean,
    createMaintenance: (Long) -> Unit,
    back: () -> Unit
) {

    LaunchedEffect(Unit) {
        resync()
    }

    AppLayout(
        title = "Nova Manutenção",
        selectedIcon = BottomBar.MAINTENANCE.value,
        navigateBack = back,
        navigateToHome = {
            navController.navigate(Routes.HOME)
        },
        navigateToMore = {
            navController.navigate(Routes.MORE)
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK)
        },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE)
        },
        navigateToExecutions = {
            navController.navigate(Routes.INSTALLATION_HOLDER)
        }
    ) { _, _ ->

        if (loading) {
            Loading("Carregando")
        } else {

            LazyColumn {

                if (!hasInternet) {
                    item {
                        Column(
                            Modifier.clickable {
                                resync()
                            }
                        ) {
                            NoInternet("Se o contrato que procura não aparecer, verifique a internet e clique para tentar de novo.")
                        }
                    }
                }

                if (contracts.isEmpty()) {
                    item {
                        Spacer(Modifier.height(50.dp))
                        NothingData("Nenhum contrato encontrado")
                    }
                } else {
                    item {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            text = "Selecione o contrato para continuar",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    items(
                        contracts,
                        key = { it.contractId }
                    ) { contract ->
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    createMaintenance(contract.contractId)
                                }
                                .background(MaterialTheme.colorScheme.surface)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            headlineContent = {
                                Text(
                                    text = abbreviate(contract.contractor),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            },
                            leadingContent = {
                                Icon(
                                    contentDescription = null,
                                    imageVector = Icons.Default.Build
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

            }


        }

    }

}

@Preview
@Composable
fun PrevMaintenance() {
    NewMaintenanceContent(
        navController = rememberNavController(),
        contracts = listOf(
            Contract(
                contractId = 1,
                contractor = "PREFEITURA ITAMBÉ DO MATO DENTRO",
                contractFile = null,
                createdBy = "",
                createdAt = "",
                status = "",
                startAt = "",
                deviceId = "",
                itemsIds = "",
                hasMaintenance = true
            ),

            Contract(
                contractId = 2,
                contractor = "PREFEITURA DE MIRAÍ",
                contractFile = null,
                createdBy = "",
                createdAt = "",
                status = "",
                startAt = "",
                deviceId = "",
                itemsIds = "",
                hasMaintenance = true
            )
        ),
        loading = false,
        resync = { },
        hasInternet = false,
        createMaintenance = {},
        back = {

        }
    )
}