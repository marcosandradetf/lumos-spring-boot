package com.lumos.ui.profile

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.navigation.BottomBar
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
    authViewModel: AuthViewModel
) {
    AppLayout(
        title = "Perfil",
        pSelected = BottomBar.PROFILE.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        navController = navController,
        context = context
    ) { modifier ->
        Column(
            modifier = modifier
        ) {
            Text("Minha Conta")
            ListItem(
                colors = ListItemColors(
                    containerColor = Color.White,
                    headlineColor = Color.Black,
                    leadingIconColor = Color.Black,
                    overlineColor = Color.Black,
                    supportingTextColor = Color.Black,
                    trailingIconColor = Color.Black,
                    disabledHeadlineColor = Color.Black,
                    disabledLeadingIconColor = Color.Black,
                    disabledTrailingIconColor = Color.Black
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
                    .clickable {
                        authViewModel.logout(
                            onLogoutSuccess
                        )
                    }
            )
        }
    }
}

