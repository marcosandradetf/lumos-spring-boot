package com.lumos.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.lumos.MyApp
import com.lumos.api.ContractApi
import com.lumos.api.DirectExecutionApi
import com.lumos.api.PreMeasurementApi
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.AppInitCoordinator
import com.lumos.domain.service.CoordinatesService
import com.lumos.domain.service.DefaultRemoteActionExecutor
import com.lumos.midleware.SecureStorage
import com.lumos.notifications.FCMService
import com.lumos.notifications.FCMService.FCMBus
import com.lumos.notifications.NotificationManager
import com.lumos.notifications.NotificationsBadge
import com.lumos.repository.AuthRepository
import com.lumos.repository.ContractRepository
import com.lumos.repository.DirectExecutionRepository
import com.lumos.repository.MaintenanceRepository
import com.lumos.repository.NotificationRepository
import com.lumos.repository.PreMeasurementInstallationRepository
import com.lumos.repository.PreMeasurementRepository
import com.lumos.repository.RemoteConfigRepository
import com.lumos.repository.StockRepository
import com.lumos.repository.TeamRepository
import com.lumos.repository.ViewRepository
import com.lumos.ui.auth.LoginScreen
import com.lumos.ui.components.SplashScreen
import com.lumos.ui.directexecution.DirectExecutionHomeScreen
import com.lumos.ui.directexecution.StreetMaterialScreen
import com.lumos.ui.home.HomeScreen
import com.lumos.ui.installationholder.InstallationHolderScreen
import com.lumos.ui.lockscreen.LockedAppScreen
import com.lumos.ui.maintenance.MaintenanceScreen
import com.lumos.ui.menu.MenuScreen
import com.lumos.ui.noAccess.NoAccessScreen
import com.lumos.ui.notifications.NotificationsScreen
import com.lumos.ui.premeasurement.ContractsScreen
import com.lumos.ui.premeasurement.PreMeasurementProgressScreen
import com.lumos.ui.premeasurement.PreMeasurementScreen
import com.lumos.ui.premeasurement.PreMeasurementStreetScreen
import com.lumos.ui.premeasurementinstallation.PreMeasurementInstallationStreetsScreen
import com.lumos.ui.premeasurementinstallation.onstreet.MaterialScreen
import com.lumos.ui.profile.ProfileScreen
import com.lumos.ui.stock.CheckStockScreen
import com.lumos.ui.sync.SyncDetailsScreen
import com.lumos.ui.sync.SyncScreen
import com.lumos.ui.team.CheckTeamScreen
import com.lumos.ui.updater.ApkUpdateDownloader
import com.lumos.utils.NavEvents
import com.lumos.utils.SessionManager
import com.lumos.viewmodel.AuthViewModel
import com.lumos.viewmodel.ContractViewModel
import com.lumos.viewmodel.DirectExecutionViewModel
import com.lumos.viewmodel.HomeViewModel
import com.lumos.viewmodel.MaintenanceViewModel
import com.lumos.viewmodel.NotificationViewModel
import com.lumos.viewmodel.PreMeasurementInstallationViewModel
import com.lumos.viewmodel.PreMeasurementViewModel
import com.lumos.viewmodel.StockViewModel
import com.lumos.viewmodel.SyncViewModel
import com.lumos.viewmodel.TeamViewModel
import com.lumos.worker.SyncManager.enqueueSync
import com.lumos.worker.SyncManager.schedulePeriodicSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BottomBar(val value: Int) {
    HOME(0),
    STOCK(1),
    MAINTENANCE(2),
    EXECUTIONS(3),
    MORE(4),
}

object Routes {
    const val AUTH_FLOW = "auth-flow"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val HOME = "home"
    const val NO_ACCESS = "no-access"
    const val MORE = "more"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val CONTRACT_SCREEN = "contract-screen"
    const val PRE_MEASUREMENT_FLOW = "pre-measurement-flow"
    const val PRE_MEASUREMENTS = "pre-measurements"
    const val PRE_MEASUREMENT_PROGRESS = "pre-measurement-progress"
    const val PRE_MEASUREMENT_STREET = "pre-measurement-street"
    const val INSTALLATION_HOLDER = "installation-holder-screen"
    const val MAINTENANCE = "maintenance"
    const val STOCK = "stock"
    const val ORDER = "order"

    // -> pre-measurement-installations
    const val PRE_MEASUREMENT_INSTALLATION_FLOW = "pre-measurement-installation-flow"
    const val PRE_MEASUREMENT_INSTALLATION_STREETS = "pre-measurement-installation-streets"
    const val PRE_MEASUREMENT_INSTALLATION_MATERIALS = "pre-measurement-installation-materials"

    // -> direct-installations
    const val DIRECT_EXECUTION_FLOW = "direct-execution-flow"
    const val DIRECT_EXECUTION_HOME_SCREEN = "direct-execution-home-screen"
    const val DIRECT_EXECUTION_SCREEN_MATERIALS = "direct-execution-screen-materials"
    const val UPDATE = "update"
    const val SYNC_FLOW = "sync-flow"
    const val SYNC = "sync"
    const val TEAM_SCREEN = "team-screen"
}

@Composable
fun AppNavigation(
    app: MyApp,
    secureStorage: SecureStorage,
    actionState: MutableState<String?>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationItem by FCMBus.notificationItem.collectAsState()
    val navController = rememberNavController()
    val loggedIn by SessionManager.loggedIn.collectAsState()
    val loggedOut by SessionManager.loggedOut.collectAsState()
    val sessionExpired by SessionManager.sessionExpired.collectAsState()
    val checkingSession by SessionManager.checkingSession.collectAsState()
    val notificationManager = remember { NotificationManager() }
    val isLocked by secureStorage.appLocked.collectAsState()

    val viewRepository = remember { ViewRepository(app.database) }
    val authRepository =
        remember { AuthRepository(app.retrofit, secureStorage, app, notificationManager) }
    val preMeasurementRepository = remember {
        PreMeasurementRepository(
            app.database,
            app.retrofit.create(PreMeasurementApi::class.java),
            app
        )
    }
    val fusedLocationProvider =
        remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    val coordinatesService =
        remember(context) { CoordinatesService(context, fusedLocationProvider) }
    val addressService = remember(context) { AddressService(context) }

    val contractRepository = remember {
        ContractRepository(
            db = app.database,
            api = app.retrofit.create(ContractApi::class.java),
            app = app
        )
    }

    val preMeasurementInstallationRepository = remember {
        PreMeasurementInstallationRepository(
            db = app.database,
            retrofit = app.retrofit,
            secureStorage = secureStorage,
            app = app
        )
    }

    val directExecutionRepository = remember {
        DirectExecutionRepository(
            db = app.database,
            api = app.retrofit.create(DirectExecutionApi::class.java),
            secureStorage = secureStorage,
            app = app
        )
    }

    val notificationRepository = remember {
        NotificationRepository(
            db = app.database,
            app = app
        )
    }

    val maintenanceRepository = remember {
        MaintenanceRepository(
            db = app.database,
            api = app.retrofit,
            app = app,
            secureStorage = secureStorage
        )
    }

    val stockRepository = remember {
        StockRepository(
            db = app.database,
            api = app.retrofit,
            secureStorage = secureStorage,
            app = app
        )
    }

    val teamRepository = remember {
        TeamRepository(
            db = app.database,
            api = app.retrofit,
            secureStorage = secureStorage,
            app = app,
            notificationManager = notificationManager
        )
    }

    // 👇 AQUI você monta (uma vez só)
    val remoteConfigRepository = remember {
        RemoteConfigRepository(
            retrofit = app.remoteConfigRetrofit,
        )
    }

    val actionExecutor = remember {
        DefaultRemoteActionExecutor(
            db = app.database,
            remoteConfigRepository = remoteConfigRepository,
            secureStorage = secureStorage,
            appContext = app.applicationContext
        )
    }

    val appInitCoordinator = remember {
        AppInitCoordinator(
            remoteConfigRepository,
            actionExecutor,
            secureStorage = secureStorage
        )
    }


    val notificationViewModel: NotificationViewModel = viewModel {

        NotificationViewModel(
            repository = notificationRepository
        )
    }

    val notifications by notificationViewModel.notifications

    LaunchedEffect(Unit) {
        if (secureStorage.getAccessToken() != null) {
            appInitCoordinator.onAppStart()

            SessionManager.setSessionExpired(false)
            SessionManager.setLoggedOut(false)
            SessionManager.setLoggedIn(true)
            enqueueSync(app.applicationContext)
            schedulePeriodicSync(app.applicationContext)

            delay(2200)
            SessionManager.setCheckingSession(false)
        } else {
            SessionManager.setLoggedIn(false)
            SessionManager.setSessionExpired(false)
            SessionManager.setLoggedOut(false)

            delay(2200)
            SessionManager.setCheckingSession(false)
        }
    }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            SessionManager.setSessionExpired(false)
            SessionManager.setLoggedOut(false)
            SessionManager.setCheckingSession(false)
            NotificationsBadge._notificationBadge.value = notificationViewModel.countNotifications()
        }
    }

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            SessionManager.setLoggedIn(false)
            SessionManager.setSessionExpired(false)
            SessionManager.setCheckingSession(false)
            SessionManager.setLoggedOut(false)
            secureStorage.clearAll()
            notificationManager.unsubscribeFromAllTopics()
        }
    }

    LaunchedEffect(loggedOut) {
        if (loggedOut) {
            SessionManager.setLoggedIn(false)
            SessionManager.setLoggedOut(false)
            SessionManager.setSessionExpired(false)
            secureStorage.clearAll()
            notificationManager.unsubscribeFromAllTopics()
        }
    }

    LaunchedEffect(checkingSession) {
        if ((loggedIn || loggedOut || sessionExpired) && checkingSession) {
            SessionManager.setCheckingSession(false)
        }
    }

    LaunchedEffect(actionState.value) {
        if (loggedIn && actionState.value != null) {
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
        if (loggedIn) {
            notificationItem?.let {
                NotificationsBadge._notificationBadge.value = notificationViewModel.insert(it)
                FCMService.clearNotification()
            }
        }
    }

    LaunchedEffect(Unit, notifications) {
        if (loggedIn) {
            notificationViewModel.loadNotifications()
        }
    }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            NavEvents.publish(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                lifecycleOwner.lifecycleScope.launch {
                    appInitCoordinator.onForeground()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (sessionExpired) {
        Toast.makeText(
            context,
            "Sua sessão expirou, por favor faça login novamente!",
            Toast.LENGTH_LONG
        ).show()
    }


    // Rotas protegidas
    if (checkingSession) {
        SplashScreen()

    } else if (isLocked && loggedIn) {
        LockedAppScreen(
            reason = null,
            appInitCoordinator,
            authRepository
        )
        return
    } else {

        NavHost(
            navController = navController,
            startDestination = if (loggedIn) Routes.HOME else Routes.AUTH_FLOW
        ) {

            navigation(
                route = Routes.AUTH_FLOW,
                startDestination = Routes.LOGIN
            ) {

                composable(Routes.LOGIN) { backStackEntry ->

                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.AUTH_FLOW)
                    }

                    val vm: AuthViewModel = viewModel(parentEntry) {
                        AuthViewModel(authRepository, parentEntry.savedStateHandle)
                    }

                    LoginScreen(
                        viewModel = vm,
                        context = context,
                        onTestClick = {}
                    )
                }

                composable(Routes.PROFILE) { backStackEntry ->

                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.AUTH_FLOW)
                    }

                    val vm: AuthViewModel = viewModel(parentEntry) {
                        AuthViewModel(authRepository, parentEntry.savedStateHandle)
                    }

                    ProfileScreen(
                        authViewModel = vm,
                        navController = navController,
                        context = context,
                        notificationsBadge = "",

                        onNavigateToHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.AUTH_FLOW) { inclusive = true }
                            }
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE) {
                                popUpTo(Routes.AUTH_FLOW) { inclusive = true }
                            }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS) {
                                popUpTo(Routes.AUTH_FLOW) { inclusive = true }
                            }
                        },
                        onLogoutSuccess = {}
                    )
                }

            }

            navigation(
                route = Routes.PRE_MEASUREMENT_FLOW,
                startDestination = Routes.CONTRACT_SCREEN
            ) {

                // pre-measurement
                composable(Routes.CONTRACT_SCREEN) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.PRE_MEASUREMENT_FLOW)
                    }

                    val cvm: ContractViewModel = viewModel(parentEntry) {
                        ContractViewModel(
                            repository = contractRepository,
                            parentEntry.savedStateHandle
                        )
                    }

                    val pvm: PreMeasurementViewModel = viewModel(parentEntry) {
                        PreMeasurementViewModel(
                            preMeasurementRepository,
                            parentEntry.savedStateHandle
                        )
                    }


                    ContractsScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.PRE_MEASUREMENT_FLOW) { inclusive = true }
                            }
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE) {
                                popUpTo(Routes.PRE_MEASUREMENT_FLOW) { inclusive = true }
                            }
                        },
                        context = context,
                        contractViewModel = cvm,
                        navController = navController,
                        notificationsBadge = "",
                        roles = secureStorage.getRoles(),
                        pvm
                    )
                }

                composable(Routes.PRE_MEASUREMENT_STREET + "/{preMeasurementId}") { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.PRE_MEASUREMENT_FLOW)
                    }

                    val cvm: ContractViewModel = viewModel(parentEntry) {
                        ContractViewModel(
                            repository = contractRepository,
                            parentEntry.savedStateHandle
                        )
                    }

                    val pvm: PreMeasurementViewModel = viewModel(parentEntry) {
                        PreMeasurementViewModel(
                            preMeasurementRepository,
                            parentEntry.savedStateHandle
                        )
                    }

                    PreMeasurementStreetScreen(
                        context = context,
                        preMeasurementViewModel = pvm,
                        contractViewModel = cvm,
                        navController = navController,
                        coordinates = coordinatesService,
                        addressService = addressService
                    )
                }

                composable(Routes.PRE_MEASUREMENT_PROGRESS) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.PRE_MEASUREMENT_FLOW)
                    }

                    val pvm: PreMeasurementViewModel = viewModel(parentEntry) {
                        PreMeasurementViewModel(
                            preMeasurementRepository,
                            parentEntry.savedStateHandle
                        )
                    }

                    PreMeasurementProgressScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.AUTH_FLOW) { inclusive = true }
                            }
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE) {
                                popUpTo(Routes.PRE_MEASUREMENT_FLOW) { inclusive = true }
                            }
                        },
                        onNavigateToPreMeasurements = {
                            navController.navigate(Routes.PRE_MEASUREMENTS)
                        },
                        context = context,
                        preMeasurementViewModel = pvm,
                        navController = navController,
                    )
                }

                composable(Routes.PRE_MEASUREMENTS) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.PRE_MEASUREMENT_FLOW)
                    }

                    val pvm: PreMeasurementViewModel = viewModel(parentEntry) {
                        PreMeasurementViewModel(
                            preMeasurementRepository,
                            parentEntry.savedStateHandle
                        )
                    }

                    PreMeasurementScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.PRE_MEASUREMENT_FLOW) { inclusive = true }
                            }
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MORE) {
                                popUpTo(Routes.PRE_MEASUREMENT_FLOW) { inclusive = true }
                            }
                        },
                        preMeasurementViewModel = pvm,
                        navController = navController,
                        roles = secureStorage.getRoles()
                    )
                }

            }

            composable(Routes.INSTALLATION_HOLDER) {

                InstallationHolderScreen(
                    directExecutionRepository = directExecutionRepository,
                    preMeasurementInstallationRepository = preMeasurementInstallationRepository,
                    viewRepository = viewRepository,
                    navController = navController,
                    roles = secureStorage.getRoles(),
                    secureStorage = secureStorage
                )
            }

            navigation(
                route = Routes.PRE_MEASUREMENT_INSTALLATION_FLOW,
                startDestination = Routes.PRE_MEASUREMENT_INSTALLATION_STREETS
            ) {
                composable(
                    route = "${Routes.PRE_MEASUREMENT_INSTALLATION_STREETS}/{id}/{contractor}"
                            + "?contractId={contractId}&instructions={instructions}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType },
                        navArgument("contractor") { type = NavType.StringType },
                        navArgument("contractId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("instructions") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.PRE_MEASUREMENT_INSTALLATION_FLOW)
                    }

                    val vm: PreMeasurementInstallationViewModel = viewModel(
                        parentEntry
                    ) {
                        PreMeasurementInstallationViewModel(
                            repository = preMeasurementInstallationRepository,
                            contractRepository = contractRepository,
                            savedStateHandle = parentEntry.savedStateHandle
                        )
                    }

                    PreMeasurementInstallationStreetsScreen(
                        viewModel = vm,
                        navController = navController,
                    )
                }

                composable(Routes.PRE_MEASUREMENT_INSTALLATION_MATERIALS) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.PRE_MEASUREMENT_INSTALLATION_FLOW)
                    }

                    val vm: PreMeasurementInstallationViewModel = viewModel(
                        parentEntry
                    ) {
                        PreMeasurementInstallationViewModel(
                            repository = preMeasurementInstallationRepository,
                            contractRepository = contractRepository,
                            savedStateHandle = parentEntry.savedStateHandle
                        )
                    }

                    MaterialScreen(
                        viewModel = vm,
                        context = context,
                        navController = navController,
                        coordinatesService = coordinatesService,
                    )
                }
            }

            navigation(
                route = Routes.DIRECT_EXECUTION_FLOW,
                startDestination = Routes.DIRECT_EXECUTION_HOME_SCREEN
            ) {
                composable(
                    route = "${Routes.DIRECT_EXECUTION_HOME_SCREEN}/{id}/{contractor}/{creationDate}" +
                            "?contractId={contractId}&instructions={instructions}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("contractor") { type = NavType.StringType },
                        navArgument("creationDate") { type = NavType.StringType },
                        navArgument("contractId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("instructions") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.DIRECT_EXECUTION_FLOW)
                    }

                    val vm: DirectExecutionViewModel = viewModel(parentEntry) {
                        DirectExecutionViewModel(
                            repository = directExecutionRepository,
                            contractRepository = contractRepository,
                            parentEntry.savedStateHandle
                        )
                    }

                    DirectExecutionHomeScreen(
                        viewModel = vm,
                        navController = navController
                    )
                }

                composable(Routes.DIRECT_EXECUTION_SCREEN_MATERIALS) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.DIRECT_EXECUTION_FLOW)
                    }

                    val vm: DirectExecutionViewModel = viewModel(parentEntry) {
                        DirectExecutionViewModel(
                            repository = directExecutionRepository,
                            contractRepository = contractRepository,
                            parentEntry.savedStateHandle
                        )
                    }

                    StreetMaterialScreen(
                        directExecutionViewModel = vm,
                        context = context,
                        navController = navController,
                        coordinates = coordinatesService,
                        addressService = addressService,
                    )
                }

            }

            composable(Routes.MORE) {
                MenuScreen(
                    navController = navController,
                    context = context
                )
            }

            composable(Routes.HOME) { backStackEntry ->

                val vm: HomeViewModel = viewModel(backStackEntry) {
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
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Routes.NOTIFICATIONS)
                    },
                    navController = navController,
                    notificationsBadge = notifications.size.toString(),
                    homeViewModel = vm,
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
                            popUpTo(Routes.NO_ACCESS) { inclusive = true }
                        }
                    },
                    navController = navController,
                    notificationsBadge = "",
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
                    context = context,
                    notificationViewModel = notificationViewModel,
                )
            }


            composable(Routes.UPDATE + "/{apk}") { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("apk") ?: ""
                val apkUrl = Uri.decode(encodedUrl)
                ApkUpdateDownloader(
                    apkUrl = apkUrl,
                    context = context,
                    navController,
                    "",
                )
            }


            navigation(
                route = Routes.SYNC_FLOW,
                startDestination = Routes.SYNC
            ) {
                composable(Routes.SYNC) { backStackEntry ->

                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.SYNC_FLOW)
                    }

                    val vm: SyncViewModel = viewModel(backStackEntry) {
                        SyncViewModel(
                            db = app.database,
                            parentEntry.savedStateHandle
                        )
                    }

                    SyncScreen(
                        navController,
                        vm,
                    )
                }

                composable(Routes.SYNC + "/{type}?lastRoute={lastRoute}") { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.SYNC_FLOW)
                    }
                    val type = backStackEntry.arguments?.getString("type") ?: ""
                    val lastRoute =
                        backStackEntry.arguments?.getString("lastRoute")

                    val vm: SyncViewModel = viewModel(backStackEntry) {
                        SyncViewModel(
                            db = app.database,
                            parentEntry.savedStateHandle
                        )
                    }

                    SyncDetailsScreen(
                        applicationContext = app.applicationContext,
                        context = context,
                        navController,
                        "",
                        vm,
                        type,
                        lastRoute = lastRoute
                    )
                }
            }

            // MAINTENANCE

            composable(Routes.STOCK + "?lastRoute={lastRoute}") { backStackEntry ->
                val lastRoute =
                    backStackEntry.arguments?.getString("lastRoute")

                val stockViewModel: StockViewModel = viewModel(backStackEntry) {
                    StockViewModel(
                        repository = stockRepository
                    )
                }

                CheckStockScreen(
                    navController = navController,
                    lastRoute = lastRoute,
                    stockViewModel = stockViewModel,
                    connectivityGate = app.connectivityGate
                )
            }

            composable(Routes.MAINTENANCE + "?lastRoute={lastRoute}?maintenanceId={maintenanceId}") { backStackEntry ->
                val lastRoute =
                    backStackEntry.arguments?.getString("lastRoute")

                val maintenanceId =
                    backStackEntry.arguments?.getString("maintenanceId")

                val maintenanceViewModel: MaintenanceViewModel = viewModel(backStackEntry) {
                    MaintenanceViewModel(
                        repository = maintenanceRepository,
                        contractRepository = contractRepository,
                        stockRepository = stockRepository,
                    )
                }

                MaintenanceScreen(
                    maintenanceViewModel = maintenanceViewModel,
                    navController = navController,
                    lastRoute = lastRoute,
                    secureStorage = secureStorage,
                    coordinatesService = coordinatesService,
                    addressService = addressService
                )
            }

            composable(Routes.TEAM_SCREEN + "/{currentScreen}") { backStackEntry ->
                val currentScreen =
                    backStackEntry.arguments?.getString("currentScreen")?.toInt()

                val teamViewModel: TeamViewModel = viewModel(backStackEntry) {
                    TeamViewModel(
                        repository = teamRepository
                    )
                }

                CheckTeamScreen(
                    viewModel = teamViewModel,
                    navController = navController,
                    currentScreen = currentScreen!!,
                    secureStorage = secureStorage,
                    connectivityGate = app.connectivityGate
                )

            }

        }
    }

}