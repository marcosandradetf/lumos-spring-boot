package com.lumos.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.lumos.service.AuthViewModel
import com.lumos.ui.menu.MenuScreen
import com.lumos.auth.ui.Login
import com.lumos.ui.home.HomeScreen
import com.lumos.ui.notifications.NotificationsScreen
import com.lumos.ui.profile.ProfileScreen

enum class BottomBar(val value: Int) {
    MENU(0),
    HOME(1),
    NOTIFICATIONS(2),
    PROFILE(3)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Simulação do estado de autenticação (usaria algo mais real em um app real)
    val authViewModel: AuthViewModel = viewModel()
    val isAuthenticated by authViewModel.isAuthenticated
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        authViewModel.authenticate(context)
    }


    // Rotas protegidas
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
            )
        }

        navigation(startDestination = Routes.HOME, route = Routes.MAIN) {
            composable(Routes.MENU,
                enterTransition = { slideInHorizontally(initialOffsetX = { 25000 }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -25000 }) }
            ) {
                MenuScreen(
                    onNavigateToHome = {
                        navController.navigate("homeFromMenu") {
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
                    navController = navController
                )
            }

            composable(Routes.HOME,
                enterTransition = { slideInHorizontally(initialOffsetX = { 25000 }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -25000 }) }
            ) {
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
                    navController = navController
                )
            }

            composable("homeFromMenu",
                enterTransition = { slideInHorizontally(initialOffsetX = { -25000 }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { 25000 }) }
            ) {
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
                    navController = navController
                )
            }

            composable(Routes.NOTIFICATIONS,
                enterTransition = { slideInHorizontally(initialOffsetX = { -25000 }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { 25000 }) }
            ) {
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
                    navController = navController
                )
            }

            composable("notificationsFromProfile",
                enterTransition = { slideInHorizontally(initialOffsetX = { 25000 }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -25000 }) }
            ) {
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
                    navController = navController
                )
            }

            composable(Routes.PROFILE,
                enterTransition = { slideInHorizontally(initialOffsetX = { -25000 }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { 25000 }) }
            ) {
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
                        navController.navigate("notificationsFromProfile") {
                            popUpTo(Routes.PROFILE) { inclusive = true }
                        }
                    },
                    navController = navController
                )
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
}
