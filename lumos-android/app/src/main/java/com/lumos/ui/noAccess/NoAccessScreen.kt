package com.lumos.ui.noAccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout

@Composable
fun NoAccessScreen(
    onNavigateToMenu: () -> Unit,
    navController: NavHostController,
    notificationsBadge: String,
    selectedIcon: Int,
    title: String
) {

    AppLayout(
        title = title,
        selectedIcon = selectedIcon,
        notificationsBadge = notificationsBadge,
        navigateToHome = {
            navController.navigate(Routes.HOME){
                popUpTo(Routes.NO_ACCESS) { inclusive = true }
            }
        },
        navigateToMore = onNavigateToMenu,
        navigateBack = {
            navController.navigate(Routes.HOME){
                popUpTo(Routes.NO_ACCESS) { inclusive = true }
            }
        },
        navigateToExecutions = {
            navController.navigate(Routes.INSTALLATION_HOLDER){
                popUpTo(Routes.NO_ACCESS) { inclusive = true }
            }
        },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE){
                popUpTo(Routes.NO_ACCESS) { inclusive = true }
            }
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK){
                popUpTo(Routes.NO_ACCESS) { inclusive = true }
            }
        }
    ) { modifier, snackBar ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone com fundo circular e destaque
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Sem permissão",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Acesso negado",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Você não tem permissão para acessar esta funcionalidade.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Em caso de dúvida, contate o administrador.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    navController.navigate(Routes.HOME){
                        popUpTo(Routes.NO_ACCESS) { inclusive = true }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Voltar")
            }

        }
    }

}

@Preview
@Composable
fun PrevNoAccess(){
    NoAccessScreen(
        {},
        rememberNavController(),
        "10",
        BottomBar.EXECUTIONS.value,
        title = "Instalações"
    )
}