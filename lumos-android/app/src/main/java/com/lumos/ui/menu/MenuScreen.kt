package com.lumos.ui.menu

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        title = "Menu",
        pSelected = BottomBar.MENU.value,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        context = context,
        notificationsBadge = notificationsBadge
    ) { modifier, snackBar ->
        CategoryMenu(navController = navController)
    }
}

@Composable
fun CategoryMenu(navController: NavHostController) {
    // Supondo que você tenha uma lista de categorias com títulos e seus respectivos cards
    val categories = listOf(
        Category(
            "Pré-medição",
            listOf("Contratos", "Pré-medições em andamento"),
            action = listOf(Routes.CONTRACT_SCREEN, Routes.PRE_MEASUREMENTS),
            icons = listOf(Icons.Default.Mail, Icons.Default.Map)
        ),
        Category(
            "Execução",
            listOf("Execuções"),
            action = listOf(Routes.EXECUTION_SCREEN),
            icons = listOf(Icons.Default.Build)
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp) // Espaço entre os títulos e os cards
    ) {
        categories.forEach { category ->
            // Item para o título
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp), // Espaço abaixo do título
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.title, // Texto do título
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, // Aumenta a força do texto
                            color = MaterialTheme.colorScheme.primary, // Cor do título
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Espaço entre o ícone e o título
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown, // Exemplo de ícone
                        contentDescription = "Icone Titulo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp) // Ajusta o tamanho do ícone
                    )
                }
            }

            // Itens dos cards
            items(category.cards.size) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(category.action[index]) },
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = category.icons[index],
                            contentDescription = "Descrição",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = category.cards[index], // Exemplo de texto dinâmico para cada item
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
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