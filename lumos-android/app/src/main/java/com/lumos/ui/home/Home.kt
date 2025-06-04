package com.lumos.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat

@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController,
    notificationsBadge: String
) {
    val context = LocalContext.current


    AppLayout(
        title = "Início",
        pSelected = BottomBar.HOME.value,
        notificationsBadge = notificationsBadge,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        context = context
    ) { modifier ->
        Column(
            modifier = modifier
                .padding(2.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Cards Minimalistas
            MaintenanceStatusCard()
            Spacer(modifier = Modifier.height(16.dp))
            AlertsCard()

            // Botão de Ação Principal
            Spacer(modifier = Modifier.height(24.dp))
            ReportProblemButton()

            // Lista de Atividades Recentes
            Spacer(modifier = Modifier.height(24.dp))
            RecentActivitiesList()
        }
    }
}
@Composable
fun MaintenanceStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone ilustrativo
            Icon(
                imageVector = Icons.Outlined.Assignment, // Ícone de "tarefa"
                contentDescription = "Execuçoes ícone",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Título
            Text(
                text = "Status das Execuções",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Mensagem informativa
            Text(
                text = "Nenhuma execução alocada no momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AlertsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Alertas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nenhum alerta no momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun ReportProblemButton() {
    Button(
        onClick = { /* Ação para reportar problema */ },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer) // Azul
    ) {
        Text(text = "Reportar Problema", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun RecentActivitiesList() {


    Column {
        Text(
            text = "Atividades Recentes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

    }
}


fun openGoogleMaps(latitude: Double, longitude: Double, context: Context) {
    // Verifica se o Google Maps está instalado
    val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps") // Especifica o pacote do Google Maps
    }

    try {
        // Tenta abrir o Google Maps com o intent
        ContextCompat.startActivity(context, mapIntent, null)
    } catch (e: Exception) {
        // Caso não consiga abrir o Google Maps, mostre uma mensagem de erro
        Toast.makeText(context, "Erro ao abrir o Google Maps", Toast.LENGTH_SHORT).show()
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
        "12"
    )
}