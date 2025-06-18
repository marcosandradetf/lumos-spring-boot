package com.lumos.navigation

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
import com.lumos.data.api.ContractApi
import com.lumos.data.api.ExecutionApi
import com.lumos.data.api.PreMeasurementApi
import com.lumos.data.repository.AuthRepository
import com.lumos.data.repository.ContractRepository
import com.lumos.data.repository.ExecutionRepository
import com.lumos.data.repository.NotificationRepository
import com.lumos.data.repository.PreMeasurementRepository
import com.lumos.midleware.SecureStorage
import com.lumos.notifications.FCMService
import com.lumos.notifications.FCMService.FCMBus
import com.lumos.notifications.NotificationManager
import com.lumos.notifications.NotificationsBadge
import com.lumos.ui.auth.Login
import com.lumos.ui.executions.CitiesScreen
import com.lumos.ui.executions.MaterialScreen
import com.lumos.ui.executions.StreetsScreen
import com.lumos.ui.home.HomeScreen
import com.lumos.ui.menu.MenuScreen
import com.lumos.ui.noAccess.NoAccessScreen
import com.lumos.ui.notifications.NotificationsScreen
import com.lumos.ui.preMeasurement.ContractsScreen
import com.lumos.ui.preMeasurement.MeasurementHome
import com.lumos.ui.preMeasurement.PreMeasurementProgressScreen
import com.lumos.ui.preMeasurement.PreMeasurementScreen
import com.lumos.ui.preMeasurement.PreMeasurementStreetScreen
import com.lumos.ui.profile.ProfileScreen
import com.lumos.ui.viewmodel.AuthViewModel
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.ExecutionViewModel
import com.lumos.ui.viewmodel.NotificationViewModel
import com.lumos.ui.viewmodel.PreMeasurementViewModel
import com.lumos.worker.SyncManager.enqueueSync
import com.lumos.worker.SyncManager.schedulePeriodicSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class BottomBar(val value: Int) {
    MENU(0),
    HOME(1),
    NOTIFICATIONS(2),
    PROFILE(3)
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

    val isLoading by authViewModel.isLoading.collectAsState()


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

    val executionViewModel: ExecutionViewModel = viewModel {
        val api = app.retrofit.create(ExecutionApi::class.java)

        val contractRepository = ExecutionRepository(
            db = app.database,
            api = api,
            secureStorage = secureStorage,
            app = app
        )
        ExecutionViewModel(
            repository = contractRepository
        )
    }

    val isAuthenticated by authViewModel.isAuthenticated

    val notificationViewModel: NotificationViewModel = viewModel {
        val notificationDao = app.database.notificationDao()

        val notificationRepository = NotificationRepository(
            dao = notificationDao,
        )
        NotificationViewModel(
            repository = notificationRepository
        )
    }

    val notificationManager = NotificationManager(app.applicationContext, secureStorage)

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            NotificationsBadge._notificationBadge.value = notificationViewModel.countNotifications()
            Log.e("n", "Antes de entrar no notification manager")
            notificationManager.subscribeToSavedTopics()

        } else {
            authViewModel.authenticate()
        }
    }

    LaunchedEffect(Unit) {
        if (isAuthenticated) {
            enqueueSync(app.applicationContext)
            schedulePeriodicSync(app.applicationContext)
        }
    }

    LaunchedEffect(isAuthenticated, actionState.value) {
        if (isAuthenticated && actionState.value != null) {
            withContext(Dispatchers.Main) {
                when (actionState.value) {
                    Routes.CONTRACT_SCREEN -> navController.navigate(Routes.CONTRACT_SCREEN)
                    Routes.NOTIFICATIONS -> navController.navigate(Routes.NOTIFICATIONS)
                    Routes.PROFILE -> navController.navigate(Routes.PROFILE)
                    Routes.EXECUTION_SCREEN -> navController.navigate(Routes.EXECUTION_SCREEN)
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
    if (isLoading) {
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
            startDestination = if (isAuthenticated) Routes.MAIN else Routes.LOGIN
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
                composable(Routes.MENU) {
                    MenuScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.MENU) { inclusive = true }
                            }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS) {
                                popUpTo(Routes.MENU) { inclusive = true }
                            }
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.MENU) { inclusive = true }
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
                            navController.navigate(Routes.MENU) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        executionViewModel = executionViewModel,
                        contractViewModel = contractViewModel,
                        roles = secureStorage.getRoles()
                    )
                }

                composable(Routes.NO_ACCESS + "/{screenOrigin}") { backStackEntry ->
                    val screenOrigin =
                        backStackEntry.arguments?.getString("screenOrigin") ?: ""
                    NoAccessScreen(
                        onNavigateToMenu = {
                            navController.navigate(Routes.MENU) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.NOTIFICATIONS) { inclusive = true }
                            }
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        screen = screenOrigin,
                    )
                }


                composable(Routes.NOTIFICATIONS) {
                    NotificationsScreen(
                        onNavigateToMenu = {
                            navController.navigate(Routes.MENU) {
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
                            navController.navigate(Routes.MENU) {
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

                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.PROFILE) { inclusive = true }
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
                            navController.navigate(Routes.MENU)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
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

                composable(Routes.PRE_MEASUREMENT_PROGRESS + "/{contractId}") { backStackEntry ->
                    val contractId =
                        backStackEntry.arguments?.getString("contractId")?.toLongOrNull() ?: 0
                    PreMeasurementProgressScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MENU)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        onNavigateToPreMeasurements = {
                            navController.navigate(Routes.PRE_MEASUREMENTS)
                        },
                        onNavigateToStreet = {
                            navController.navigate(Routes.PRE_MEASUREMENT_STREET + "/$it")
                        },
                        context = LocalContext.current,
                        contractViewModel = contractViewModel,
                        preMeasurementViewModel = preMeasurementViewModel,
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        contractId = contractId,
                    )
                }

                composable(Routes.PRE_MEASUREMENTS) {
                    PreMeasurementScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MENU)
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
                    MeasurementHome(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        navController = navController,
                        context = LocalContext.current
                    )
                }

                composable(Routes.PRE_MEASUREMENT_STREET + "/{contractId}") { backStackEntry ->
                    val contractId =
                        backStackEntry.arguments?.getString("contractId")?.toLongOrNull() ?: 0
                    PreMeasurementStreetScreen(
                        back = {
                            navController.navigate(Routes.PRE_MEASUREMENT_PROGRESS + "/$it")
                        },
                        context = LocalContext.current,
                        preMeasurementViewModel = preMeasurementViewModel,
                        contractId = contractId,
                        contractViewModel = contractViewModel,
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MENU)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString()
                    )
                }

                //

                composable(Routes.EXECUTION_SCREEN) {
                    CitiesScreen(
                        executionViewModel = executionViewModel,
                        context = LocalContext.current,
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MENU)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        pSelected = 0,
                        onNavigateToStreetScreen = { contractId, contractor ->
                            navController.navigate(Routes.EXECUTION_SCREEN_STREETS + "/$contractId/$contractor")
                        },
                        roles = secureStorage.getRoles(),
                        directExecution = false
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
                        executionViewModel = executionViewModel,
                        context = LocalContext.current,
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MENU)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        pSelected = 0,
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
                        executionViewModel = executionViewModel,
                        context = LocalContext.current,
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        onNavigateToMenu = {
                            navController.navigate(Routes.MENU)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Routes.PROFILE)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        pSelected = 0,
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        onNavigateToExecutions = {
                            navController.navigate(Routes.EXECUTION_SCREEN)
                        }
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
    const val MENU = "menu"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val CONTRACT_SCREEN = "contract-screen"
    const val PRE_MEASUREMENTS = "pre-measurements"
    const val PRE_MEASUREMENT_PROGRESS = "pre-measurement-progress"
    const val PRE_MEASUREMENT_STREET_HOME = "pre-measurement-home"
    const val PRE_MEASUREMENT_STREET = "pre-measurement-street"
    const val PRE_MEASUREMENT_STREET_PROGRESS = "pre-measurement-street"
    const val EXECUTION_SCREEN = "execution-screen"
    const val EXECUTION_SCREEN_STREETS = "execution-screen-streets"
    const val EXECUTION_SCREEN_MATERIALS = "execution-screen-materials"
}
