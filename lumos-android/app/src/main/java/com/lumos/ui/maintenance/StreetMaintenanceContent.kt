package com.lumos.ui.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.Contract
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.domain.model.MaintenanceStreetItem
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import java.util.UUID

@Composable
fun StreetMaintenanceContent(
    maintenanceId: UUID?,
    navController: NavHostController,
    loading: Boolean,
    lastRoute: String?,
    back: () -> Unit,
    saveStreet: (MaintenanceStreet, List<MaintenanceStreetItem>) -> Unit,
    streetCreated: Boolean,
    newStreet: () -> Unit
) {
    val navigateBack: (() -> Unit) =
        if (lastRoute == Routes.HOME) {
            { navController.navigate(Routes.HOME) }
        } else {
            back
        }

    AppLayout(
        title = "Manutenção em andamento",
        selectedIcon = BottomBar.MAINTENANCE.value,
        navigateBack = navigateBack,
        navigateToHome = {
            navController.navigate(Routes.HOME)
        },
        navigateToMore = {
            navController.navigate(Routes.MORE)
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK)
        },
        navigateToExecutions = {
            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
        }
    ) { modifier, showSnackBar ->

        if (streetCreated) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .padding(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Missão cumprida!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Os dados serão enviados para o sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth(fraction = 0.5f),
                    onClick = {

                    }
                ) {
                    Text("Inserir outro ponto")
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth(fraction = 0.5f),
                    onClick = {
                        back()
                    }
                ) {
                    Text("Voltar")
                }
            }
        }

    }

}


@Preview
@Composable
fun PrevStreetMaintenance() {
    StreetMaintenanceContent(
        navController = rememberNavController(),
        lastRoute = null,
        loading = false,
        maintenanceId = UUID.randomUUID(),
        back = {

        },
        saveStreet = { _: MaintenanceStreet, _: List<MaintenanceStreetItem> -> },
        streetCreated = false,
        newStreet = {},
    )
}