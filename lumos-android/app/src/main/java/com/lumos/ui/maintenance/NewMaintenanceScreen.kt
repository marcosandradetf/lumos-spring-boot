package com.lumos.ui.maintenance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout

@Composable
fun NewMaintenanceScreen(
    context: Context,
    navController: NavHostController,
    lastRoute: String?
) {

    NewMaintenanceContent(
        navController = navController,
        lastRoute = lastRoute
    )
}

@Composable
fun NewMaintenanceContent(
    navController: NavHostController,
    lastRoute: String?
) {
    val navigateBack: (() -> Unit)? =
        if (lastRoute == Routes.HOME) {
            { navController.navigate(Routes.HOME) }
        } else {
            null
        }

    AppLayout(
        title= "Nova Manutenção",
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
    ) {
        modifier, snackbar ->
    }
}