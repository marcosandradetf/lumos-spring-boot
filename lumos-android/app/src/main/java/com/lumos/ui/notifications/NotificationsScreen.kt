package com.lumos.ui.notifications

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout

@Composable
fun NotificationsScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController,
    context: Context
) {
    AppLayout(
        title = "Notificações",
        pSelected = BottomBar.NOTIFICATIONS.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        context = context
    ) { modifier ->
        Column(
            modifier = modifier
        ) {
            Text("Notifications Screen")
        }
    }
}

@Preview
@Composable
fun PrevNotifications() {
    NotificationsScreen(
        {},
        {},
        {},
        rememberNavController(),
        LocalContext.current
    )
}