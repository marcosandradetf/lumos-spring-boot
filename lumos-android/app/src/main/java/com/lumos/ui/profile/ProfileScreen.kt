package com.lumos.ui.profile

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    navController: NavHostController,
    context: Context,
    onLogoutSuccess: () -> Unit,
    authViewModel: AuthViewModel,
    notificationsBadge: String
) {

    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val currentVersionName =
        packageInfo.versionName

    val currentVersionCode =
        packageInfo.longVersionCode


    AppLayout(
        title = "Perfil",
        selectedIcon = BottomBar.MORE.value,
        navigateToMore = onNavigateToMenu,
        navigateToHome = onNavigateToHome,
        navigateBack = {
            navController.navigate(Routes.MORE)
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK)
        },
        navigateToExecutions = {
            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
        },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE)
        }

    ) { modifier, snackBar ->
        Column(
            modifier = modifier
        ) {

            ListItem(
                colors = ListItemColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurface,
                    overlineColor = MaterialTheme.colorScheme.surface,
                    supportingTextColor = MaterialTheme.colorScheme.surface,
                    trailingIconColor = MaterialTheme.colorScheme.surface,
                    disabledHeadlineColor = MaterialTheme.colorScheme.surface,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.surface,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.surface
                ),
                headlineContent = { Text("Versão: $currentVersionName (${currentVersionCode}) ") },
                shadowElevation = 0.dp,
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            ListItem(
                colors = ListItemColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurface,
                    overlineColor = MaterialTheme.colorScheme.surface,
                    supportingTextColor = MaterialTheme.colorScheme.surface,
                    trailingIconColor = MaterialTheme.colorScheme.surface,
                    disabledHeadlineColor = MaterialTheme.colorScheme.surface,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.surface,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.surface
                ),
                headlineContent = { Text("Tarefas em Sincronização") },
                leadingContent = {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "Tarefas em Sincronização",
                    )
                },
                shadowElevation = 10.dp,
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        navController.navigate(Routes.SYNC)
                    }
            )

            ListItem(
                colors = ListItemColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurface,
                    overlineColor = MaterialTheme.colorScheme.surface,
                    supportingTextColor = MaterialTheme.colorScheme.surface,
                    trailingIconColor = MaterialTheme.colorScheme.surface,
                    disabledHeadlineColor = MaterialTheme.colorScheme.surface,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.surface,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.surface
                ),
                headlineContent = { Text("Desconectar") },
                leadingContent = {
                    Icon(
                        Icons.Filled.ExitToApp,
                        contentDescription = "Desconectar",
                    )
                },
                shadowElevation = 10.dp,
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {

                        authViewModel.logout(
                            onLogoutSuccess
                        )

                    }
            )
        }
    }
}

