package com.lumos.ui.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Maintenance
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NoInternet
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.MaintenanceViewModel
import com.lumos.ui.viewmodel.StockViewModel
import com.lumos.utils.Utils
import com.lumos.utils.Utils.abbreviate
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
) {
    val stock by stockViewModel.stock.collectAsState()
    val contractSelected by maintenanceViewModel.contractSelected.collectAsState()
    val streetCreated by maintenanceViewModel.streetCreated.collectAsState()
    val finish by maintenanceViewModel.finish.collectAsState()

    val maintenanceLoading by maintenanceViewModel.loading.collectAsState()
    val maintenanceMessage by maintenanceViewModel.message.collectAsState()
    val contractLoading by contractViewModel.isSyncing.collectAsState()
    val contractMessage by contractViewModel.syncError.collectAsState()
    val hasInternet by contractViewModel.hasInternet.collectAsState()
    var forceLoading by remember { mutableStateOf(false) }

    val maintenanceId by maintenanceViewModel.maintenanceId.collectAsState()
    val maintenances by maintenanceViewModel.maintenances.collectAsState()
    val streets by maintenanceViewModel.maintenanceStreets.collectAsState()
    val contracts by contractViewModel.contracts.collectAsState()
    val message = maintenanceMessage ?: contractMessage

    var resync by remember { mutableIntStateOf(0) }
    var screenState by remember { mutableStateOf(MaintenanceUIState.NEW) }


    val alertMessage = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem", "body" to "Você está na rua da execução neste momento?"
        )
    }
    var alertModal by remember { mutableStateOf(false) }

    val maintenanceMap by remember(maintenances) {
        mutableStateOf(maintenances.associateBy { it.maintenanceId })
    }

    val contractMap by remember(contracts) {
        mutableStateOf(contracts.associateBy { it.contractId })
    }

    val maintenance by remember(maintenanceId, maintenanceMap) {
        derivedStateOf {
            maintenanceMap[maintenanceId.toString()]
        }
    }

    val contractor by remember(maintenance?.contractId, contractMap) {
        derivedStateOf {
            maintenance?.contractId?.let { contractMap[it] }?.contractor
        }
    }

    val filteredStreets by remember(maintenanceId, streets) {
        derivedStateOf {
            streets.filter { it.maintenanceId == maintenanceId.toString() }
        }
    }


    val loading = maintenanceLoading || contractLoading ||
            forceLoading || contractor == null || maintenance == null



    LaunchedEffect(Unit) {
        maintenanceViewModel.loadMaintenances("IN_PROGRESS")
        maintenanceViewModel.loadMaintenanceStreets(maintenances.map { it.maintenanceId })

        contractViewModel.syncContracts()
        contractViewModel.loadFlowContractsForMaintenance()
        stockViewModel.loadStockFlow()
    }

    LaunchedEffect(resync) {
        if (resync > 0) contractViewModel.syncContracts()
    }

    LaunchedEffect(maintenances) {
        screenState = if (maintenances.isNotEmpty()) {
            when {
                maintenances.size == 1 -> {
                    maintenanceViewModel.setMaintenanceId(UUID.fromString(maintenances.first().maintenanceId))
                    MaintenanceUIState.HOME
                }

                else -> MaintenanceUIState.LIST
            }
        } else {
            MaintenanceUIState.NEW
        }
    }

    LaunchedEffect(contractSelected) {
        if (contractSelected) screenState = MaintenanceUIState.STREET
    }

    LaunchedEffect(screenState) {
        forceLoading = false
    }

    when (screenState) {
        MaintenanceUIState.NEW -> {
            NewMaintenanceContent(
                navController = navController,
                maintenancesSize = maintenances.size,
                contracts = contracts,
                loading = loading,
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
                            status = "IN_PROGRESS"
                        )
                    )
                },
                back = {
                    maintenanceViewModel.clearViewModel()
                    screenState = if (maintenances.size > 1) {
                        MaintenanceUIState.LIST
                    } else {
                        MaintenanceUIState.HOME
                    }
                }
            )
        }

        MaintenanceUIState.STREET -> {
            maintenanceId?.let {
                StreetMaintenanceContent(
                    maintenanceId = it,
                    contractor = contractor,
                    navController = navController,
                    loading = loading,
                    back = {
                        forceLoading = true
                        maintenanceViewModel.clearViewModel()
                        screenState = MaintenanceUIState.HOME
                    },
                    saveStreet = { street, items ->
                        maintenanceViewModel.insertMaintenanceStreet(street, items)
                    },
                    lastRoute = lastRoute,
                    streetCreated = streetCreated,
                    newStreet = {
                        maintenanceViewModel.clearViewModel()
                    },
                    stockData = stock
                )
            }
        }

        MaintenanceUIState.LIST -> {
            MaintenanceListContent(
                maintenances = maintenances,
                contracts = contracts,
                navController = navController,
                loading = loading,
                selectMaintenance = { id ->
                    forceLoading = true
                    maintenanceViewModel.clearViewModel()
                    maintenanceViewModel.setMaintenanceId(UUID.fromString(id))
                    screenState = MaintenanceUIState.HOME
                },
                newMaintenance = {
                    forceLoading = true
                    maintenanceViewModel.clearViewModel()
                    screenState = MaintenanceUIState.NEW
                },
            )
        }

        MaintenanceUIState.HOME -> {
            maintenance?.let { maintenanceNonNull ->
                MaintenanceHomeContent(
                    maintenance = maintenanceNonNull,
                    contractor = contractor,
                    streets = filteredStreets,
                    maintenanceSize = maintenances.size,
                    navController = navController,
                    loading = loading,
                    finish = finish,
                    newStreet = {
                        forceLoading = true
                        maintenanceViewModel.clearViewModel()
                        screenState = MaintenanceUIState.STREET
                    },
                    newMaintenance = {
                        forceLoading = true
                        maintenanceViewModel.clearViewModel()
                        screenState = MaintenanceUIState.NEW
                    },
                    finishMaintenance = {
                        maintenanceViewModel.finishMaintenance(it)
                    },
                    back = {
                        forceLoading = true
                        maintenanceViewModel.clearViewModel()
                        screenState = MaintenanceUIState.LIST
                    }
                )
            }
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
    back: () -> Unit,
    maintenancesSize: Int
) {
    val navigateBack: (() -> Unit)? =
        if (maintenancesSize > 0) {
            back
        } else {
            null
        }

    AppLayout(
        title = "Nova Manutenção",
        selectedIcon = BottomBar.MAINTENANCE.value,
        navigateBack = navigateBack,
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
            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
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
                                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
                            headlineContent = {
                                Text(
                                    text = abbreviate(contract.contractor),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
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

        },
        maintenancesSize = 1
    )
}