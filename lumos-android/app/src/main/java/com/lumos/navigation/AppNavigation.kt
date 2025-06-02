package com.lumos.navigation

import android.content.Context
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.lumos.R
import com.lumos.data.api.ContractApi
import com.lumos.data.api.PreMeasurementApi
import com.lumos.data.api.StockApi
import com.lumos.data.database.AppDatabase
import com.lumos.data.repository.AuthRepository
import com.lumos.data.repository.ContractRepository
import com.lumos.data.repository.NotificationRepository
import com.lumos.data.repository.PreMeasurementRepository
import com.lumos.data.repository.StockRepository
import com.lumos.midleware.SecureStorage
import com.lumos.notifications.FCMService
import com.lumos.notifications.FCMService.FCMBus
import com.lumos.notifications.NotificationManager
import com.lumos.notifications.NotificationsBadge
import com.lumos.ui.auth.Login
import com.lumos.ui.home.HomeScreen
import com.lumos.ui.menu.MenuScreen
import com.lumos.ui.notifications.NotificationsScreen
import com.lumos.ui.preMeasurement.ContractsScreen
import com.lumos.ui.preMeasurement.MeasurementHome
import com.lumos.ui.preMeasurement.PreMeasurementProgressScreen
import com.lumos.ui.preMeasurement.PreMeasurementScreen
import com.lumos.ui.preMeasurement.PreMeasurementStreetScreen
import com.lumos.ui.profile.ProfileScreen
import com.lumos.ui.viewmodel.AuthViewModel
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.NotificationViewModel
import com.lumos.ui.viewmodel.PreMeasurementViewModel
import com.lumos.ui.viewmodel.StockViewModel
import com.lumos.worker.SyncManager.enqueueSync
import com.lumos.worker.SyncManager.schedulePeriodicSync
import retrofit2.Retrofit

enum class BottomBar(val value: Int) {
    MENU(0),
    HOME(1),
    NOTIFICATIONS(2),
    PROFILE(3)
}

@Composable
fun AppNavigation(
    db: AppDatabase,
    retrofit: Retrofit,
    secureStorage: SecureStorage,
    context: Context,
    actionState: MutableState<String?>
) {
    val notificationItem by FCMBus.notificationItem.collectAsState()

    val navController = rememberNavController()
    var isLoading by remember { mutableStateOf(true) }

    // Usar viewModel para armazenar o ViewModel corretamente
    val authViewModel: AuthViewModel = viewModel {
        val authRepository = AuthRepository(retrofit, secureStorage, context)
        AuthViewModel(authRepository, secureStorage)
    }

    val stockViewModel: StockViewModel = viewModel {
        val api = retrofit.create(StockApi::class.java)
        val repository = StockRepository(db, api)

        StockViewModel(repository)
    }

    val preMeasurementViewModel: PreMeasurementViewModel = viewModel {
        val api = retrofit.create(PreMeasurementApi::class.java)

        val preMeasurementRepository = PreMeasurementRepository(db, api, context)
        PreMeasurementViewModel(preMeasurementRepository)
    }

    val contractViewModel: ContractViewModel = viewModel {
        val api = retrofit.create(ContractApi::class.java)

        val contractRepository = ContractRepository(
            db = db,
            api = api
        )
        ContractViewModel(
            repository = contractRepository
        )
    }

    val isAuthenticated by authViewModel.isAuthenticated

    val notificationViewModel: NotificationViewModel = viewModel {
        val notificationDao = db.notificationDao()

        val notificationRepository = NotificationRepository(
            dao = notificationDao,
        )
        NotificationViewModel(
            repository = notificationRepository
        )
    }

    val notificationManager = NotificationManager(context, secureStorage)

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            isLoading = false
            NotificationsBadge._notificationBadge.value = notificationViewModel.countNotifications()
            notificationManager.subscribeToSavedTopics()

        } else {
            isLoading = false
            authViewModel.authenticate(context)
        }
    }

    LaunchedEffect(Unit) {
        if (isAuthenticated) {
            enqueueSync(context)
            schedulePeriodicSync(context)
        }
    }

    LaunchedEffect(isAuthenticated, actionState.value) {
        if (isAuthenticated && actionState.value != null) {
            when (actionState.value) {
                Routes.CONTRACT_SCREEN -> navController.navigate(Routes.CONTRACT_SCREEN)
                Routes.NOTIFICATIONS -> navController.navigate(Routes.NOTIFICATIONS)
                Routes.PROFILE -> navController.navigate(Routes.PROFILE)
                Routes.EXECUTION_SCREEN -> null
                // Adicione mais cases conforme necessário
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
                        context = context,
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
                        notificationsBadge = notifications.size.toString()
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
                        context = context,
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
                        context = context,
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
                        context = context,
                        contractViewModel = contractViewModel,
                        navController = navController,
                        notificationsBadge = notifications.size.toString()
                    )
                }

                composable(Routes.PRE_MEASUREMENT_PROGRESS + "/{contractId}") { backStackEntry ->
                    val contractId = backStackEntry.arguments?.getString("contractId")?.toLongOrNull() ?: 0
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
                        context = context,
                        contractViewModel = contractViewModel,
                        preMeasurementViewModel = preMeasurementViewModel,
                        navController = navController,
                        notificationsBadge = notifications.size.toString(),
                        contractId = contractId
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
                        context = context,
                        contractViewModel = contractViewModel,
                        navController = navController,
                        notificationsBadge = notifications.size.toString()
                    )
                }

                composable(Routes.PRE_MEASUREMENT_STREET_HOME) {
                    MeasurementHome(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        navController = navController,
                        context = context
                    )
                }

                composable(Routes.PRE_MEASUREMENT_STREET+ "/{contractId}") { backStackEntry ->
                    val contractId = backStackEntry.arguments?.getString("contractId")?.toLongOrNull() ?: 0
                    PreMeasurementStreetScreen(
                        back = {
                            navController.navigate(Routes.PRE_MEASUREMENT_PROGRESS + "/$it")
                        },
                        context = context,
                        stockViewModel,
                        preMeasurementViewModel,
                        contractId = contractId,
                        contractViewModel
                    )
                }

                //


            }
        }
    }
}


object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val HOME = "home"
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
}
