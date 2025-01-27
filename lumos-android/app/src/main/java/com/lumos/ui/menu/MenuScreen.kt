package com.lumos.ui.menu

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout

@Composable
fun MenuScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController,
    context: Context
) {
    AppLayout(
        title = "Menu",
        pSelected = BottomBar.MENU.value,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        context = context
    ) { modifier ->
        Column(
            modifier = modifier
        ) {

            ListItem(
                colors = ListItemColors(
                    containerColor = Color.White,
                    headlineColor =  Color.Black,
                    leadingIconColor =  Color.Black,
                    overlineColor =  Color.Black,
                    supportingTextColor =  Color.Black,
                    trailingIconColor =  Color.Black,
                    disabledHeadlineColor =  Color.Black,
                    disabledLeadingIconColor =  Color.Black,
                    disabledTrailingIconColor =  Color.Black
                ),
                headlineContent = { Text("Pré-Medição") },
                leadingContent = {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = "Pré-Medição",
                    )
                },
                shadowElevation = 10.dp,
                modifier = Modifier.padding(bottom = 10.dp)
                    .clickable {
                        navController.navigate(Routes.MEASUREMENT_HOME)
                    }
            )

            ListItem(
                colors = ListItemColors(
                    containerColor = Color.White,
                    headlineColor =  Color.Black,
                    leadingIconColor =  Color.Black,
                    overlineColor =  Color.Black,
                    supportingTextColor =  Color.Black,
                    trailingIconColor =  Color.Black,
                    disabledHeadlineColor =  Color.Black,
                    disabledLeadingIconColor =  Color.Black,
                    disabledTrailingIconColor =  Color.Black
                ),
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
                colors = ListItemColors(
                    containerColor = Color.White,
                    headlineColor =  Color.Black,
                    leadingIconColor =  Color.Black,
                    overlineColor =  Color.Black,
                    supportingTextColor =  Color.Black,
                    trailingIconColor =  Color.Black,
                    disabledHeadlineColor =  Color.Black,
                    disabledLeadingIconColor =  Color.Black,
                    disabledTrailingIconColor =  Color.Black
                ),
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
        LocalContext.current
    )
}