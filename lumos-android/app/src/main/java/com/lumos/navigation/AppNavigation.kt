package com.lumos.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.lumos.MyApp
import com.lumos.R
import com.lumos.api.ApiService
import com.lumos.api.ContractApi
import com.lumos.api.ExecutionApi
import com.lumos.api.PreMeasurementApi
import com.lumos.repository.AuthRepository
import com.lumos.repository.ContractRepository
import com.lumos.repository.DirectExecutionRepository
import com.lumos.repository.IndirectExecutionRepository
import com.lumos.repository.MaintenanceRepository
import com.lumos.repository.StockRepository
import com.lumos.repository.NotificationRepository
import com.lumos.repository.PreMeasurementRepository
import com.lumos.midleware.SecureStorage
import com.lumos.notifications.FCMService
import com.lumos.notifications.FCMService.FCMBus
import com.lumos.notifications.NotificationManager
import com.lumos.notifications.NotificationsBadge
import com.lumos.repository.TeamRepository
import com.lumos.ui.auth.Login
import com.lumos.ui.directExecutions.StreetMaterialScreen
import com.lumos.ui.indirectExecutions.CitiesScreen
import com.lumos.ui.indirectExecutions.MaterialScreen
import com.lumos.ui.indirectExecutions.StreetsScreen
import com.lumos.ui.home.HomeScreen
import com.lumos.ui.maintenance.MaintenanceScreen
import com.lumos.ui.menu.MenuScreen
import com.lumos.ui.noAccess.NoAccessScreen
import com.lumos.ui.notifications.NotificationsScreen
import com.lumos.ui.preMeasurement.ContractsScreen
import com.lumos.ui.preMeasurement.PreMeasurementProgressScreen
import com.lumos.ui.preMeasurement.PreMeasurementScreen
import com.lumos.ui.preMeasurement.PreMeasurementStreetScreen
import com.lumos.ui.profile.ProfileScreen
import com.lumos.ui.stock.CheckStockScreen
import com.lumos.ui.sync.SyncDetailsScreen
import com.lumos.ui.sync.SyncScreen
import com.lumos.ui.team.CheckTeamScreen
import com.lumos.ui.updater.ApkUpdateDownloader
import com.lumos.viewmodel.AuthViewModel
import com.lumos.viewmodel.ContractViewModel
import com.lumos.viewmodel.DirectExecutionViewModel
import com.lumos.viewmodel.IndirectExecutionViewModel
import com.lumos.viewmodel.MaintenanceViewModel
import com.lumos.viewmodel.StockViewModel
import com.lumos.viewmodel.NotificationViewModel
import com.lumos.viewmodel.PreMeasurementViewModel
import com.lumos.viewmodel.SyncViewModel
import com.lumos.viewmodel.TeamViewModel
import com.lumos.worker.SyncManager.enqueueSync
import com.lumos.worker.SyncManager.schedulePeriodicSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BottomBar(val value: Int) {
    HOME(0),
    STOCK(1),
    MAINTENANCE(2),
    EXECUTIONS(3),
    MORE(4),
}

@Composable
fun AppNavigation(
    app: MyApp,
    secureStorage: SecureStorage,
    actionState: MutableState<String?>
) {
    val notificationItem by FCMBus.notificationItem.collectAsState()

    val navController = rememberNavController()

    // Usar viewModel para armazenar o ViewModel corretamente
    val authViewModel: AuthViewModel = viewModel {
        val authRepository = AuthRepository(app.retrofit, secureStorage, app)
        AuthViewModel(authRepository, secureStorage)
    }

    val preMeasurementViewModel: PreMeasurementViewModel = viewModel {
        val api = app.retrofit.create(PreMeasurementApi::class.java)

        val preMeasurementRepository = PreMeasurementRepository(app.database, api, app)
        PreMeasurementViewModel(preMeasurementRepository)
    }

    val contractViewModel: ContractViewModel = viewModel {
        val api = app.retrofit.create(ContractApi::class.java)

        val contractRepository = ContractRepository(
            db = app.database,
            api = api,
            app = app
        )
        ContractViewModel(
            repository = contractRepository
        )
    }

    val indirectExecutionViewModel: IndirectExecutionViewModel = viewModel {
        val api = app.retrofit.create(ExecutionApi::class.java)

        val contractRepository = IndirectExecutionRepository(
            db = app.database,
            api = api,
            secureStorage = secureStorage,
            app = app
        )
        IndirectExecutionViewModel(
            repository = contractRepository
        )
    }

    val directExecutionViewModel: DirectExecutionViewModel = viewModel {
        val api = app.retrofit.create(ExecutionApi::class.java)

        val repository = DirectExecutionRepository(
            db = app.database,
            api = api,
            secureStorage = secureStorage,
            app = app
        )
        DirectExecutionViewModel(
            repository = repository
        )
    }

    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    val notificationViewModel: NotificationViewModel = viewModel {
        val notificationRepository = NotificationRepository(
            db = app.database,
            app = app
        )
        NotificationViewModel(
            repository = notificationRepository
        )
    }

    val notificationManager = NotificationManager(app.applicationContext, secureStorage)

    val syncViewModel: SyncViewModel = viewModel {
        SyncViewModel(
            db = app.database
        )
    }

    val stockViewModel: StockViewModel = viewModel {
        val stockRepository = StockRepository(
            db = app.database,
            api = ApiService(app.applicationContext, secureStorage),
            secureStorage = secureStorage,
            app = app
        )
        StockViewModel(
            repository = stockRepository
        )
    }

    val maintenanceViewModel: MaintenanceViewModel = viewModel {
        val maintenanceRepository = MaintenanceRepository(
            db = app.database,
            api = ApiService(app.applicationContext, secureStorage),
            app = app,
            secureStorage = secureStorage
        )

        MaintenanceViewModel(
            repository = maintenanceRepository
        )
    }

    val teamViewModel: TeamViewModel = viewModel {
        val teamRepository = TeamRepository(
            db = app.database,
            api = ApiService(app.applicationContext, secureStorage),
            secureStorage = secureStorage,
            app = app
        )

        TeamViewModel(
            repository = teamRepository
        )
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == true) {
            NotificationsBadge._notificationBadge.value = notificationViewModel.countNotifications()
            Log.e("n", "Antes de entrar no notification manager")
            notificationManager.subscribeToSavedTopics()

        } else {
            authViewModel.authenticate()
        }
    }

    LaunchedEffect(Unit) {

        if (isAuthenticated == true) {
            enqueueSync(app.applicationContext)
            schedulePeriodicSync(app.applicationContext)
        }
    }

    LaunchedEffect(isAuthenticated, actionState.value) {
        if (isAuthenticated == true && actionState.value != null) {
            withContext(Dispatchers.Main) {
                when (actionState.value) {
                    Routes.CONTRACT_SCREEN -> navController.navigate(Routes.CONTRACT_SCREEN)
                    Routes.NOTIFICATIONS -> navController.navigate(Routes.NOTIFICATIONS)
                    Routes.PROFILE -> navController.navigate(Routes.PROFILE)
                    Routes.EXECUTION_SCREEN -> navController.navigate(Routes.EXECUTION_SCREEN)
                    Routes.DIRECT_EXECUTION_SCREEN -> navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
                    // Adicione mais cases conforme necessário

                }
            }

            // Zerar para evitar reexecução desnecessária
            actionState.value = null
        }
    }

    LaunchedEffect(notificationItem) {
        notificationItem?.let {
            NotificationsBadge._notificationBadge.value = notificationViewModel.insert(it)
            FCMService.clearNotification()
        }
    }

    val notifications by notificationViewModel.notifications

    LaunchedEffect(Unit, notifications) {
        notificationViewModel.loadNotifications()
    }

    // Rotas protegidas
    // Tela preta enquanto a autenticação está sendo verificada
    if (isAuthenticated == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_lumos), // Agora no drawable
                contentDescription = "Ícone do App",
                modifier = Modifier.size(50.dp)
            )

        }
    } else {
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated == true) Routes.MAIN else Routes.LOGIN
        ) {

            // tela de Login
            composable(Routes.LOGIN) {
                Login(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }

            navigation(startDestination = Routes.HOME, route = Routes.MAIN) {
                composable(Routes.MORE) {
                    MenuScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.MORE) { inclusive = true }
                            }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS) {
                                popUpTo(Routes.MORE) { inclusive = true }
                            }
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.MORE) { inclusive = true }
                            }
                        },
                        navController = navController,
                        context = LocalContext.current,
                        notificationsBadge = notifications.size.toString()
                    )
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        indirectExecutionViewModel = indirectExecutionViewModel,
                        directExecutionViewModel = directExecutionViewModel,
                        contractViewModel = contractViewModel,
                        roles = secureStorage.getRoles(),
                        secureStorage = secureStorage
                    )
                }

                composable(Routes.NO_ACCESS + "/{lastRoute}") { backStackEntry ->
                    val lastRoute =
                        backStackEntry.arguments?.getString("lastRoute") ?: ""

                    NoAccessScreen(
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        lastRoute = lastRoute,
                    )
                }


                composable(Routes.NOTIFICATIONS) {
                    NotificationsScreen(
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        navController = navController,
                        context = LocalContext.current,
                        notificationViewModel = notificationViewModel,

                        )
                }


                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE) {
                                popUpTo(Routes.PROFILE) { inclusive = true }
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.PROFILE) { inclusive = true }
                            }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS) {
                                popUpTo(Routes.PROFILE) { inclusive = true }
                            }
                        },
                        navController = navController,
                        context = LocalContext.current,
                        onLogoutSuccess = {
                            notificationManager.unsubscribeFromSavedTopics()
                            secureStorage.clearAll()

                            CoroutineScope(Dispatchers.Main).launch {
                                navController.navigate(Routes.LOGIN)
                            }
                        },
                        authViewModel = authViewModel,
                        notificationsBadge = notifications.size.toString()
                    )
                }

                // pre-measurement
                composable(Routes.CONTRACT_SCREEN) {
                    ContractsScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE)
                        },
                        onNavigateToPreMeasurement = {
                            navController.navigate(Routes.PRE_MEASUREMENT_PROGRESS + "/$it")
                        },
                        context = LocalContext.current,
                        contractViewModel = contractViewModel,
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        roles = secureStorage.getRoles()
                    )
                }

                composable(Routes.PRE_MEASUREMENT_PROGRESS + "/{preMeasurementId}") { backStackEntry ->
                    val preMeasurementId =
                        backStackEntry.arguments?.getString("preMeasurementId")

                    PreMeasurementProgressScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE)
                        },
                        onNavigateToPreMeasurements = {
                            navController.navigate(Routes.PRE_MEASUREMENTS)
                        },
                        onNavigateToStreet = {
                            navController.navigate(Routes.PRE_MEASUREMENT_STREET + "/$it")
                        },
                        context = LocalContext.current,
                        preMeasurementViewModel = preMeasurementViewModel,
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        preMeasurementId = preMeasurementId!!,
                    )
                }

                composable(Routes.PRE_MEASUREMENTS) {
                    PreMeasurementScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        context = LocalContext.current,
                        contractViewModel = contractViewModel,
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        roles = secureStorage.getRoles()
                    )
                }

                composable(Routes.PRE_MEASUREMENT_STREET_HOME) {
//                    MeasurementHome(
//                        onNavigateToHome = {
//                            navController.navigate(Routes.HOME)
//                        },
//                        navController = navController
//                    )
                }

                composable(Routes.PRE_MEASUREMENT_STREET + "/{contractId}") { backStackEntry ->
                    val contractId =
                        backStackEntry.arguments?.getString("contractId")?.toLongOrNull() ?: 0
                    PreMeasurementStreetScreen(
                        context = LocalContext.current,
                        preMeasurementViewModel = preMeasurementViewModel,
                        contractId = contractId,
                        contractViewModel = contractViewModel,
                        navController = navController
                    )
                }

                //

                composable(Routes.EXECUTION_SCREEN+ "?lastRoute={lastRoute}") { backStackEntry ->
                    val lastRoute =
                        backStackEntry.arguments?.getString("lastRoute")

                    CitiesScreen(
                        lastRoute = lastRoute,
                        indirectExecutionViewModel = indirectExecutionViewModel,
                        directExecutionViewModel = directExecutionViewModel,
                        context = LocalContext.current,
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE)
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        onNavigateToStreetScreen = { contractId, contractor ->
                            navController.navigate(Routes.EXECUTION_SCREEN_STREETS + "/$contractId/$contractor")
                        },
                        roles = secureStorage.getRoles(),
                        directExecution = false,
                        secureStorage = secureStorage
                    )
                }

                composable(Routes.EXECUTION_SCREEN_STREETS + "/{contractId}/{contractor}") { backStackEntry ->
                    val contractId =
                        backStackEntry.arguments?.getString("contractId")?.toLongOrNull() ?: 0
                    val contractor =
                        backStackEntry.arguments?.getString("contractor") ?: ""
                    StreetsScreen(
                        contractId = contractId,
                        contractor = contractor,
                        indirectExecutionViewModel = indirectExecutionViewModel,
                        context = LocalContext.current,
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        pSelected = BottomBar.MORE.value,
                        onNavigateToExecution = {
                            navController.navigate(Routes.EXECUTION_SCREEN_MATERIALS + "/$it")
                        }
                    )
                }

                composable(Routes.EXECUTION_SCREEN_MATERIALS + "/{streetId}") { backStackEntry ->
                    val streetId =
                        backStackEntry.arguments?.getString("streetId")?.toLongOrNull() ?: 0
                    MaterialScreen(
                        streetId = streetId,
                        indirectExecutionViewModel = indirectExecutionViewModel,
                        context = LocalContext.current,
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        pSelected = BottomBar.MORE.value,
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        onNavigateToExecutions = {
                            navController.navigate(Routes.EXECUTION_SCREEN)
                        }
                    )
                }

                // direct executions
                composable(Routes.DIRECT_EXECUTION_SCREEN+ "?lastRoute={lastRoute}") { backStackEntry ->
                    val lastRoute =
                        backStackEntry.arguments?.getString("lastRoute")

                    CitiesScreen(
                        lastRoute = lastRoute,
                        indirectExecutionViewModel = indirectExecutionViewModel,
                        directExecutionViewModel = directExecutionViewModel,
                        context = LocalContext.current,
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE)
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        onNavigateToStreetScreen = { contractId, contractor ->
                            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN_MATERIALS + "/$contractId/$contractor")
                        },
                        roles = secureStorage.getRoles(),
                        directExecution = true,
                        secureStorage = secureStorage
                    )
                }

                composable(Routes.DIRECT_EXECUTION_SCREEN_MATERIALS + "/{directExecutionId}/{description}?lastRoute={lastRoute}") { backStackEntry ->
                    val directExecutionId =
                        backStackEntry.arguments?.getString("directExecutionId")?.toLongOrNull()
                            ?: 0
                    val description =
                        backStackEntry.arguments?.getString("description") ?: ""

                    val lastRoute =
                        backStackEntry.arguments?.getString("lastRoute")

                    StreetMaterialScreen(
                        directExecutionId = directExecutionId,
                        description = description,
                        directExecutionViewModel = directExecutionViewModel,
                        context = LocalContext.current,
                        lastRoute = lastRoute,
                        navController = navController,
                        notificationsBadge = notifications.size.toString()
                    )
                }

                composable(Routes.UPDATE + "/{apk}") { backStackEntry ->
                    val encodedUrl = backStackEntry.arguments?.getString("apk") ?: ""
                    val apkUrl = Uri.decode(encodedUrl)
                    ApkUpdateDownloader(
                        apkUrl = apkUrl,
                        context = LocalContext.current,
                        navController,
                        notifications.size.toString()
                    )
                }

                composable(Routes.SYNC) {
                    SyncScreen(
                        context = LocalContext.current,
                        navController,
                        notifications.size.toString(),
                        syncViewModel,
                    )
                }

                composable(Routes.SYNC + "/{type}?lastRoute={lastRoute}") { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: ""
                    val lastRoute =
                        backStackEntry.arguments?.getString("lastRoute")

                    SyncDetailsScreen(
                        applicationContext = app.applicationContext,
                        context = LocalContext.current,
                        navController,
                        notifications.size.toString(),
                        syncViewModel,
                        type,
                        lastRoute = lastRoute
                    )
                }

                // MAINTENANCE

                composable(Routes.STOCK + "?lastRoute={lastRoute}") { backStackEntry ->
                    val lastRoute =
                        backStackEntry.arguments?.getString("lastRoute")

                    CheckStockScreen(
                        navController = navController,
                        lastRoute = lastRoute,
                        stockViewModel = stockViewModel
                    )
                }

                composable(Routes.MAINTENANCE + "?lastRoute={lastRoute}?maintenanceId={maintenanceId}") { backStackEntry ->
                    val lastRoute =
                        backStackEntry.arguments?.getString("lastRoute")

                    val maintenanceId =
                        backStackEntry.arguments?.getString("maintenanceId")

                    MaintenanceScreen(
                        maintenanceViewModel = maintenanceViewModel,
                        contractViewModel = contractViewModel,
                        stockViewModel = stockViewModel,
                        navController = navController,
                        lastRoute = lastRoute,
                        secureStorage = secureStorage
                    )
                }

                composable(Routes.TEAM_SCREEN + "/{currentScreen}") { backStackEntry ->
                    val currentScreen =
                        backStackEntry.arguments?.getString("currentScreen")?.toInt()

                    CheckTeamScreen(
                        viewModel = teamViewModel,
                        navController = navController,
                        currentScreen = currentScreen!!,
                        secureStorage= secureStorage
                    )

                }

            }

        }
    }
}


object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val HOME = "home"
    const val NO_ACCESS = "no-access"
    const val MORE = "more"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val CONTRACT_SCREEN = "contract-screen"
    const val PRE_MEASUREMENTS = "pre-measurements"
    const val PRE_MEASUREMENT_PROGRESS = "pre-measurement-progress"
    const val PRE_MEASUREMENT_STREET_HOME = "pre-measurement-home"
    const val PRE_MEASUREMENT_STREET = "pre-measurement-street"
    const val PRE_MEASUREMENT_STREET_PROGRESS = "pre-measurement-street"

    const val EXECUTION_SCREEN = "execution-screen"
    const val MAINTENANCE = "maintenance"
    const val STOCK = "stock"
    const val ORDER = "order"

    const val EXECUTION_SCREEN_STREETS = "execution-screen-streets"
    const val EXECUTION_SCREEN_MATERIALS = "execution-screen-materials"

    const val DIRECT_EXECUTION_SCREEN = "direct-execution-screen"
    const val DIRECT_EXECUTION_SCREEN_MATERIALS = "direct-execution-screen-materials"
    const val UPDATE = "update"
    const val SYNC = "sync"

    const val TEAM_SCREEN = "team-screen"
}
