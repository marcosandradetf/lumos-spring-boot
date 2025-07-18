package com.lumos.ui.maintenance

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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Maintenance
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NoInternet
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.MaintenanceViewModel
import com.lumos.ui.viewmodel.StockViewModel
import com.lumos.utils.Utils
import com.lumos.utils.Utils.abbreviate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
    var screenState by remember { mutableStateOf<MaintenanceUIState?>(null) }

    val alertMessage = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem", "body" to "Você está na rua da execução neste momento?"
        )
    }

    var alertModal by remember { mutableStateOf(false) }

    val maintenanceMap by remember(maintenances) {
        derivedStateOf { maintenances.associateBy { it.maintenanceId } }
    }

    val maintenance by remember(maintenanceId) {
        derivedStateOf {
            maintenanceId?.toString()?.let { maintenanceMap[it] }
        }
    }

    val filteredStreets by remember(maintenanceId, streets) {
        derivedStateOf {
            val id = maintenanceId.toString()
            streets.filter { it.maintenanceId == id }
        }
    }

    val loading by remember(
        maintenanceLoading, forceLoading
    ) {
        derivedStateOf {
            maintenanceLoading || forceLoading
        }
    }

    LaunchedEffect(Unit) {
        val loadedMaintenances = maintenanceViewModel.maintenances
            .first()

        screenState = when {
            loadedMaintenances.size == 1 -> {
                forceLoading = true
                maintenanceViewModel.setMaintenanceId(UUID.fromString(loadedMaintenances.first().maintenanceId))
                MaintenanceUIState.HOME
            }

            else -> MaintenanceUIState.LIST
        }
    }

    LaunchedEffect(resync) {
        if (resync < 10) contractViewModel.syncContracts()
    }

    LaunchedEffect(contractSelected) {
        if (contractSelected) {
            screenState = MaintenanceUIState.STREET
            forceLoading = false
        }
    }

    when (screenState) {
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
                            status = "IN_PROGRESS"
                        )
                    )
                },
                back = {
                    maintenanceViewModel.clearViewModel()
                    screenState = if (maintenances.size == 1) {
                        forceLoading = true
                        maintenanceViewModel.setMaintenanceId(UUID.fromString(maintenances.first().maintenanceId))
                        MaintenanceUIState.HOME
                    } else {
                        MaintenanceUIState.LIST
                    }
                }
            )
        }

        MaintenanceUIState.STREET -> {
            maintenanceId?.let {
                StreetMaintenanceContent(
                    maintenanceId = it,
                    contractor = maintenance?.contractor,
                    navController = navController,
                    loading = loading,
                    back = {
                        maintenanceViewModel.clearViewModelPartial()
                        screenState = MaintenanceUIState.HOME
                    },
                    saveStreet = { street, items ->
                        maintenanceViewModel.insertMaintenanceStreet(street, items)
                    },
                    lastRoute = lastRoute,
                    streetCreated = streetCreated,
                    newStreet = {
                        maintenanceViewModel.clearViewModelPartial()
                    },
                    stockData = stock
                )
            }
        }

        MaintenanceUIState.LIST -> {
            MaintenanceListContent(
                maintenances = maintenances,
                navController = navController,
                loading = loading,
                selectMaintenance = { id ->
                    forceLoading = true
                    maintenanceViewModel.clearViewModel()
                    maintenanceViewModel.setMaintenanceId(UUID.fromString(id))
                    screenState = MaintenanceUIState.HOME
                },
                newMaintenance = {
                    maintenanceViewModel.clearViewModel()
                    screenState = MaintenanceUIState.NEW
                },
            )
        }

        MaintenanceUIState.HOME -> {
            LaunchedEffect(Unit) {
                if (maintenanceId != null) {
                    delay(300L) // um pequeno tempo para garantir que carregou
                    forceLoading = false
                }
            }

            if (finish) {
                AppLayout(
                    title = "Gerenciar manutenção",
                    selectedIcon = BottomBar.MAINTENANCE.value,
                    navigateBack = {
                        forceLoading = false
                        maintenanceViewModel.clearViewModel()
                        screenState = MaintenanceUIState.LIST
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
                        navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
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
                                    maintenanceViewModel.clearViewModel()
                                    screenState = MaintenanceUIState.LIST
                                }
                            ) {
                                Text("Voltar")
                            }
                        }
                    }
                }
            } else {
                maintenance?.let { maintenanceNonNull ->
                    MaintenanceHomeContent(
                        maintenance = maintenanceNonNull,
                        streets = filteredStreets,
                        maintenanceSize = maintenances.size,
                        navController = navController,
                        loading = loading,
                        newStreet = {
                            screenState = MaintenanceUIState.STREET
                        },
                        newMaintenance = {
                            maintenanceViewModel.clearViewModel()
                            screenState = MaintenanceUIState.NEW
                        },
                        finishMaintenance = {
                            if (it != null)
                                maintenanceViewModel.finishMaintenance(it)
                        },
                        back = {
                            forceLoading = false
                            maintenanceViewModel.clearViewModel()
                            screenState = MaintenanceUIState.LIST
                        }
                    )
                }
            }
        }

        else -> {
            AppLayout(
                title = "Manutenções",
                selectedIcon = BottomBar.MAINTENANCE.value,
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
                Loading()
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