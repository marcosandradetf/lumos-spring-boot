package com.lumos.ui.menu

import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
    navController: NavHostController,
    context: Context
) {
    AppLayout(
        title = "Mais Opções",
        selectedIcon = BottomBar.MORE.value,
        navigateToHome = {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.MORE) { inclusive = true }
            }
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK) {
                popUpTo(Routes.MORE) { inclusive = true }
            }
        },
        navigateToExecutions = {
            navController.navigate(Routes.INSTALLATION_HOLDER) {
                popUpTo(Routes.MORE) { inclusive = true }
            }
        },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE) {
                popUpTo(Routes.MORE) { inclusive = true }
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }


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
            icons = listOf(Icons.AutoMirrored.Filled.Assignment, Icons.Default.Map)
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
        rememberNavController(),
        LocalContext.current
    )
}