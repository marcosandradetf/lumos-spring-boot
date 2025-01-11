package com.lumos.ui.profile

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
fun ProfileScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    navController: NavHostController) {
    AppLayout(
        title = "Minha Conta",
        pSelected = BottomBar.PROFILE.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        navController = navController,
    ) { modifier ->
        Column(
            modifier = modifier
        ) {
            Text("Minha Conta")
        }
    }
}

@Preview
@Composable
fun PrevProfileScreen() {
    ProfileScreen(
        {},
        {},
        {},
        rememberNavController(),
    )
}