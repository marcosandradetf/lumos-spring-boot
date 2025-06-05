package com.lumos.ui.profile

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import com.lumos.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    AppLayout(
        title = "Perfil",
        pSelected = BottomBar.PROFILE.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        navController = navController,
        context = context,
        notificationsBadge = notificationsBadge
    ) { modifier, snackBar ->
        Column(
            modifier = modifier
        ) {

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
                        coroutineScope.launch {
                            authViewModel.logout(
                                onLogoutSuccess
                            )
                        }

                    }
            )
        }
    }
}

