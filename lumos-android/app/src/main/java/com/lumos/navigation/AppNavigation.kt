package com.lumos.navigation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.lumos.data.api.ContractApi
import com.lumos.data.api.MeasurementApi
import com.lumos.data.api.StockApi
import com.lumos.data.database.AppDatabase
import com.lumos.data.database.StockDao
import com.lumos.data.repository.AuthRepository
import com.lumos.data.repository.ContractRepository
import com.lumos.data.repository.MeasurementRepository
import com.lumos.data.repository.NotificationRepository
import com.lumos.data.repository.StockRepository
import com.lumos.midleware.SecureStorage
import com.lumos.service.DepositService
import com.lumos.service.FCMService
import com.lumos.service.NotificationsBadge
import com.lumos.ui.auth.Login
import com.lumos.ui.home.HomeScreen
import com.lumos.ui.preMeasurement.ContractsScreen
import com.lumos.ui.preMeasurement.MeasurementHome
import com.lumos.ui.preMeasurement.PreMeasurementStreetScreen
import com.lumos.ui.preMeasurement.PreMeasurementViewModel
import com.lumos.ui.menu.MenuScreen
import com.lumos.ui.notifications.NotificationsScreen
import com.lumos.ui.profile.ProfileScreen
import com.lumos.ui.viewmodel.AuthViewModel
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.NotificationViewModel
import com.lumos.ui.viewmodel.StockViewModel
import com.lumos.utils.ConnectivityUtils
import retrofit2.Retrofit

enum class BottomBar(val value: Int) {
    MENU(0),
    HOME(1),
    NOTIFICATIONS(2),
    PROFILE(3)
}

@Composable
fun AppNavigation(
    database: AppDatabase,
    retrofit: Retrofit,
    secureStorage: SecureStorage,
    context: Context
) {
    val notificationItem by FCMService.notificationItem.collectAsState()

    val navController = rememberNavController()
    var isLoading by remember { mutableStateOf(true) }

    val dao: StockDao = database.stockDao()

    // Usar viewModel para armazenar o ViewModel corretamente
    val authViewModel: AuthViewModel = viewModel {
        val authRepository = AuthRepository(retrofit, secureStorage, context)
        AuthViewModel(authRepository, secureStorage)
    }


    val stockViewModel: StockViewModel = viewModel {
        val api = retrofit.create(StockApi::class.java)
        val repository = StockRepository(dao, api)
        val service = DepositService(context, repository)

        StockViewModel(repository, service)
    }


    val preMeasurementViewModel: PreMeasurementViewModel = viewModel {
        val measurementDao = database.measurementDao()
        val api = retrofit.create(MeasurementApi::class.java)

        val measurementRepository = MeasurementRepository(measurementDao, api, context)
        PreMeasurementViewModel(measurementRepository)
    }

    val contractViewModel: ContractViewModel = viewModel {
        val contractDao = database.contractDao()
        val api = retrofit.create(ContractApi::class.java)

        val contractRepository = ContractRepository(
            dao = contractDao,
            api = api
        )
        ContractViewModel(
            repository = contractRepository
        )
    }


    val isAuthenticated by authViewModel.isAuthenticated

    val notificationViewModel: NotificationViewModel = viewModel {
        val notificationDao = database.notificationDao()

        val notificationRepository = NotificationRepository(
            dao = notificationDao,
        )
        NotificationViewModel(
            repository = notificationRepository
        )
    }



    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            isLoading = false
            NotificationsBadge._notificationBadge.value = notificationViewModel.countNotifications()
        } else {
            isLoading = false
            authViewModel.authenticate(context)
        }
    }

    LaunchedEffect(notificationItem) {
        if (notificationItem != null) {
            NotificationsBadge._notificationBadge.value = notificationViewModel.insert(notificationItem!!)
            FCMService._notificationItem.value = null
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
                .background(Color(0xFF1E1F22))
        ) {
            // Aqui você pode mostrar algo, como um ícone de carregamento, caso queira
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
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.PROFILE) { inclusive = true }
                            }
                        },
                        authViewModel = authViewModel,
                        notificationsBadge = notifications.size.toString()
                    )
                }

                // measurement
                composable(Routes.MEASUREMENT_HOME) {
                    MeasurementHome(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        navController = navController,
                        context = context
                    )
                }

                composable(Routes.MEASUREMENT_SCREEN) {
                    PreMeasurementStreetScreen(
                        onNavigateToHome = {
                            navController.navigate(Routes.HOME)
                        },
                        navController = navController,
                        context = context,
                        stockViewModel,
                        preMeasurementViewModel
                    )
                }

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
                        context = context,
                        contractViewModel = contractViewModel,
                        connection = ConnectivityUtils,
                        navController = navController,
                        notificationsBadge = notifications.size.toString()
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
    const val MEASUREMENT_HOME = "measurement-home"
    const val MEASUREMENT_SCREEN = "measurement-screen"
    const val CONTRACT_SCREEN = "contract-screen"

}
