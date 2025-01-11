package com.lumos.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout

@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController,
) {
    AppLayout(
        title = "Início",
        pSelected = BottomBar.HOME.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
    ) { modifier ->
        Column(
            modifier = modifier
        ) {
            Text("Home Screen")
        }
    }
}

@Preview
@Composable
fun PrevHome() {
    HomeScreen(
        {},
        {},
        {},
        rememberNavController(),
    )
}