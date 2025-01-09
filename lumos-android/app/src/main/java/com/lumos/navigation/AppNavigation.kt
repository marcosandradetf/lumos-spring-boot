package com.lumos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lumos.ui.auth.Login
import com.lumos.ui.details.DetailScreen
import com.lumos.ui.home.HomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val isLoggedIn = remember { mutableStateOf(true) } // Simula a autenticação

    NavHost(navController = navController, startDestination = if (isLoggedIn.value) "home" else "login") {
        // Tela de Login
        composable("login") {
            Login(
                sendCred = { _, _ -> /* Mock do login */ },
                signUp = { /* Mock da navegação para criar conta */ }
            )
        }

        // Tela Home (protegida por autenticação)
        composable("home") {
            if (isLoggedIn.value) {
                HomeScreen(
                    onLogout = {
                        isLoggedIn.value = false
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            } else {
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }

        // Exemplo de Tela Detalhes com parâmetro
        composable(
            route = "details/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            DetailScreen(itemId = itemId)
        }

    }
}
