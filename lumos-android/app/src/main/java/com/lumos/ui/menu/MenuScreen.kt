package com.lumos.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout

@Composable
fun MenuScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController) {
    AppLayout(
        title = "Menu",
        pSelected = BottomBar.MENU.value,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
    ) { modifier ->
        Column(
            modifier = modifier
        ) {
            ListItem(
                headlineContent = { Text("Execuções Pendentes") },
                leadingContent = {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Execuções Pendentes",
                    )
                },
                shadowElevation = 10.dp,
                modifier = Modifier.padding(bottom = 10.dp)
                    .clickable { println("teste") }

            )
            ListItem(
                headlineContent = { Text("Histórico de Execuções") },
                leadingContent = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Histórico",
                    )
                },
                shadowElevation = 10.dp,
                modifier = Modifier.padding(bottom = 10.dp)
                    .clickable { println("teste") }

            )
            ListItem(
                headlineContent = { Text("Desconectar") },
                leadingContent = {
                    Icon(
                        Icons.Filled.ExitToApp,
                        contentDescription = "Desconectar",
                    )
                },
                shadowElevation = 10.dp,
            )
        }
    }
}

@Preview
@Composable
fun PrevMenuScreen() {
    MenuScreen(
        {},
        {},
        {},
        rememberNavController(),
    )
}