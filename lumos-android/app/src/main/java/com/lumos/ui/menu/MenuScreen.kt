package com.lumos.ui.menu

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    context: Context,
    notificationsBadge: String
) {
    AppLayout(
        title = "Mais Opções",
        selectedIcon = BottomBar.MORE.value,
        notificationsBadge = notificationsBadge,
        navigateToHome = onNavigateToHome,
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
        CategoryMenu(navController = navController, context = context)
    }
}

@Composable
fun CategoryMenu(navController: NavHostController, context: Context) {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val currentVersionName =
        packageInfo.versionName

    val currentVersionCode =
        packageInfo.longVersionCode


    // Supondo que você tenha uma lista de categorias com títulos e seus respectivos cards
    val categories = listOf(

        Category(
            "Perfil",
            listOf("Perfil"),
            action = listOf(Routes.PROFILE),
            icons = listOf(Icons.Default.Person)
        ),

        Category(
            "Pré-medição",
            listOf("Nova pré-medição", "Pré-medições em andamento"),
            action = listOf(Routes.CONTRACT_SCREEN, Routes.PRE_MEASUREMENTS),
            icons = listOf(Icons.Default.Mail, Icons.Default.Map)
        ),
        Category(
            "Execução",
            listOf("Execuções Com Pré-Medição"),
            action = listOf(Routes.NO_ACCESS + "/Execuções"),
            icons = listOf(Icons.Default.Build, Icons.Default.Start)
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp) // Espaço entre os títulos e os cards
    ) {

        item {
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
        }

        categories.forEach { category ->
            items(category.cards.size) { index ->

                ListItem(
                    colors = ListItemColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        headlineColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.primary,
                        overlineColor = MaterialTheme.colorScheme.onSurface,
                        supportingTextColor = MaterialTheme.colorScheme.onSurface,
                        trailingIconColor = MaterialTheme.colorScheme.primary,
                        disabledHeadlineColor = MaterialTheme.colorScheme.primary,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    headlineContent = { Text(category.cards[index]) },
                    leadingContent = {
                        Icon(
                            category.icons[index],
                            contentDescription = null,
                        )
                    },
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            navController.navigate(category.action[index])
                        }
                )
            }
        }
    }
}

data class Category(
    val title: String,
    val cards: List<String>,
    val action: List<String>,
    val icons: List<ImageVector>
)

@Preview
@Composable
fun PrevMenuScreen() {
    MenuScreen(
        {},
        {},
        {},
        rememberNavController(),
        LocalContext.current,
        "12"
    )
}