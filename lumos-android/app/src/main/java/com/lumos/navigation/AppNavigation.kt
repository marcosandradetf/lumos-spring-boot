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
import androidx.compose.runtime.DisposableEffect
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.lumos.MyApp
import com.lumos.R
import com.lumos.api.ContractApi
import com.lumos.api.DirectExecutionApi
import com.lumos.api.PreMeasurementApi
import com.lumos.repository.AuthRepository
import com.lumos.repository.ContractRepository
import com.lumos.repository.DirectExecutionRepository
import com.lumos.repository.MaintenanceRepository
import com.lumos.repository.StockRepository
import com.lumos.repository.NotificationRepository
import com.lumos.repository.PreMeasurementRepository
import com.lumos.midleware.SecureStorage
import com.lumos.notifications.FCMService
import com.lumos.notifications.FCMService.FCMBus
import com.lumos.notifications.NotificationManager
import com.lumos.notifications.NotificationsBadge
import com.lumos.repository.PreMeasurementInstallationRepository
import com.lumos.repository.TeamRepository
import com.lumos.repository.ViewRepository
import com.lumos.ui.auth.Login
import com.lumos.ui.directexecution.DirectExecutionHomeScreen
import com.lumos.ui.directexecution.StreetMaterialScreen
import com.lumos.ui.installationholder.InstallationHolderScreen
import com.lumos.ui.premeasurementinstallation.onstreet.MaterialScreen
import com.lumos.ui.premeasurementinstallation.PreMeasurementInstallationStreetsScreen
import com.lumos.ui.home.HomeScreen
import com.lumos.ui.maintenance.MaintenanceScreen
import com.lumos.ui.menu.MenuScreen
import com.lumos.ui.noAccess.NoAccessScreen
import com.lumos.ui.notifications.NotificationsScreen
import com.lumos.ui.premeasurement.ContractsScreen
import com.lumos.ui.premeasurement.PreMeasurementProgressScreen
import com.lumos.ui.premeasurement.PreMeasurementScreen
import com.lumos.ui.premeasurement.PreMeasurementStreetScreen
import com.lumos.ui.profile.ProfileScreen
import com.lumos.ui.stock.CheckStockScreen
import com.lumos.ui.sync.SyncDetailsScreen
import com.lumos.ui.sync.SyncScreen
import com.lumos.ui.team.CheckTeamScreen
import com.lumos.ui.updater.ApkUpdateDownloader
import com.lumos.utils.NavEvents
import com.lumos.viewmodel.AuthViewModel
import com.lumos.viewmodel.ContractViewModel
import com.lumos.viewmodel.DirectExecutionViewModel
import com.lumos.viewmodel.HomeViewModel
import com.lumos.viewmodel.PreMeasurementInstallationViewModel
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

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            NavEvents.publish(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }


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

    val contractRepository = ContractRepository(
        db = app.database,
        api = app.retrofit.create(ContractApi::class.java),
        app = app
    )

    val contractViewModel: ContractViewModel = viewModel {
        ContractViewModel(
            repository = contractRepository
        )
    }


    val preMeasurementInstallationRepository = PreMeasurementInstallationRepository(
        db = app.database,
        retrofit = app.retrofit,
        secureStorage = secureStorage,
        app = app
    )

    val preMeasurementInstallationViewModel: PreMeasurementInstallationViewModel = viewModel {
        PreMeasurementInstallationViewModel(
            repository = preMeasurementInstallationRepository,
            contractRepository = contractRepository
        )
    }

    val directExecutionRepository = DirectExecutionRepository(
        db = app.database,
        api = app.retrofit.create(DirectExecutionApi::class.java),
        secureStorage = secureStorage,
        app = app
    )

    val directExecutionViewModel: DirectExecutionViewModel = viewModel {
        DirectExecutionViewModel(
            repository = directExecutionRepository,
            contractRepository = contractRepository,
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
            api = app.retrofit,
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
            api = app.retrofit,
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
            api = app.retrofit,
            secureStorage = secureStorage,
            app = app
        )

        TeamViewModel(
            repository = teamRepository
        )
    }

    val viewRepository = ViewRepository(app.database)

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
                    Routes.INSTALLATION_HOLDER -> navController.navigate(Routes.INSTALLATION_HOLDER)
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
                        navController = navController,
                        context = LocalContext.current
                    )
                }

                composable(Routes.HOME) {
                    val homeViewModel: HomeViewModel = viewModel {
                        HomeViewModel(
                            repository = contractRepository,
                            directExecutionRepository = directExecutionRepository,
                            preMeasurementInstallationRepository = preMeasurementInstallationRepository,
                            viewRepository = viewRepository
                        )
                    }

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
                        homeViewModel = homeViewModel,
                        roles = secureStorage.getRoles(),
                        secureStorage = secureStorage
                    )
                }

                composable(Routes.NO_ACCESS + "/{icon}/{title}") { backStackEntry ->

                    val icon = backStackEntry.arguments?.getInt("icon") ?: 1
                    val title = backStackEntry.arguments?.getString("title") ?: ""

                    NoAccessScreen(
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        selectedIcon = icon,
                        title = title,
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
                        context = LocalContext.current,
                        contractViewModel = contractViewModel,
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        roles = secureStorage.getRoles(),
                        preMeasurementViewModel
                    )
                }

                composable(Routes.PRE_MEASUREMENT_PROGRESS) {

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
                        context = LocalContext.current,
                        preMeasurementViewModel = preMeasurementViewModel,
                        navController = navController,
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
                        preMeasurementViewModel = preMeasurementViewModel,
                        navController = navController,
                        roles = secureStorage.getRoles()
                    )
                }


                composable(Routes.PRE_MEASUREMENT_STREET + "/{preMeasurementId}") { backStackEntry ->
                    val preMeasurementId =
                        backStackEntry.arguments?.getString("preMeasurementId") ?: ""
                    PreMeasurementStreetScreen(
                        context = LocalContext.current,
                        preMeasurementViewModel = preMeasurementViewModel,
                        preMeasurementId = preMeasurementId,
                        contractViewModel = contractViewModel,
                        navController = navController
                    )
                }

                //


                composable(Routes.INSTALLATION_HOLDER) {
                    InstallationHolderScreen(
                        directExecutionViewModel = directExecutionViewModel,
                        preMeasurementInstallationViewModel = preMeasurementInstallationViewModel,
                        viewRepository = viewRepository,
                        navController = navController,
                        roles = secureStorage.getRoles(),
                        secureStorage = secureStorage
                    )
                }

                composable(Routes.PRE_MEASUREMENT_INSTALLATION_STREETS) {
                    PreMeasurementInstallationStreetsScreen(
                        viewModel = preMeasurementInstallationViewModel,
                        navController = navController,
                    )
                }

                composable(Routes.PRE_MEASUREMENT_INSTALLATION_MATERIALS) {
                    MaterialScreen(
                        viewModel = preMeasurementInstallationViewModel,
                        context = LocalContext.current,
                        navController = navController,
                    )
                }


                // Direct Execution
                composable(Routes.DIRECT_EXECUTION_HOME_SCREEN) {
                    DirectExecutionHomeScreen(
                        viewModel = directExecutionViewModel,
                        navController = navController
                    )
                }

                composable(Routes.DIRECT_EXECUTION_SCREEN_MATERIALS) {
                    StreetMaterialScreen(
                        directExecutionViewModel = directExecutionViewModel,
                        context = LocalContext.current,
                        navController = navController,
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
                        secureStorage = secureStorage
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
    const val PRE_MEASUREMENT_STREET = "pre-measurement-street"

    const val INSTALLATION_HOLDER = "installation-holder-screen"
    const val MAINTENANCE = "maintenance"
    const val STOCK = "stock"
    const val ORDER = "order"

    // -> pre-measurement-installations
    const val PRE_MEASUREMENT_INSTALLATION_STREETS = "pre-measurement-installation-streets"
    const val PRE_MEASUREMENT_INSTALLATION_MATERIALS = "pre-measurement-installation-materials"

    // -> direct-installations

    const val DIRECT_EXECUTION_HOME_SCREEN = "direct-execution-home-screen"
    const val DIRECT_EXECUTION_SCREEN_MATERIALS = "direct-execution-screen-materials"
    const val UPDATE = "update"
    const val SYNC = "sync"

    const val TEAM_SCREEN = "team-screen"
}
