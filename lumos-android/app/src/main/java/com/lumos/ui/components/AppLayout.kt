package com.lumos.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.lumos.navigation.BottomBar

@Composable
fun AppLayout(
    title: String,
    pSelected: Int = 1,
    sliderNavigateToMenu: (() -> Unit?)? = null,
    sliderNavigateToHome: (() -> Unit?)? = null,
    sliderNavigateToNotifications: (() -> Unit?)? = null,
    sliderNavigateToProfile: (() -> Unit?)? = null,
    navController: NavHostController,
    navigateBack: (() -> Unit)? = null,
    pContent: @Composable (Modifier) -> Unit,
) {
    var selectedItem by remember { mutableIntStateOf(1) }
    val items = listOf("Menu", "Início", "Notificações", "Perfil")
    val selectedIcons =
        listOf(
            Icons.Filled.Menu,
            Icons.Filled.Home,
            Icons.Filled.Notifications,
            Icons.Filled.Person
        )
    val unselectedIcons =
        listOf(
            Icons.Outlined.Menu,
            Icons.Outlined.Home,
            Icons.Outlined.Notifications,
            Icons.Outlined.Person
        )

    Scaffold(
        containerColor = Color(0xFFF5F5F7),
        topBar = {
            TopBar(
                navigateBack = navigateBack,
                title = title
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = pSelected == index,
                        onClick = {
//                            selectedItem = index
                            handleNavigation(index, sliderNavigateToMenu, sliderNavigateToHome, sliderNavigateToNotifications, sliderNavigateToProfile)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White, // Cor do ícone selecionado
                            unselectedIconColor = Color(0xFF000000), // Cor do ícone não selecionado
                            selectedTextColor = Color(0xFF007AFF),  // Cor do texto selecionado
                            unselectedTextColor = Color(0xFF000000), // Cor do texto não selecionado
                            indicatorColor = Color(0xFF007AFF)       // Background do item selecionado
                        )
                    )
                }
            }
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                pContent(
                    Modifier
                        .padding(10.dp)
                        .background(Color(0xFFF5F5F7))
                )
            }
        },
    )
}

// Função para lidar com navegação ou ações específicas
fun handleNavigation(
    index: Int,
    sliderNavigateToMenu: (() -> Unit?)?,
    sliderNavigateToHome: (() -> Unit?)?,
    sliderNavigateToNotifications: (() -> Unit?)?,
    sliderNavigateToProfile: (() -> Unit?)?,
) {
    when (index) {
        BottomBar.MENU.value -> if (sliderNavigateToMenu != null) {
            sliderNavigateToMenu()
        }
        BottomBar.HOME.value -> if (sliderNavigateToHome != null) {
            sliderNavigateToHome()
        }
        BottomBar.NOTIFICATIONS.value -> if (sliderNavigateToNotifications != null) {
            sliderNavigateToNotifications()
        }
        BottomBar.PROFILE.value -> if (sliderNavigateToProfile != null) {
            sliderNavigateToProfile()
        }
        else -> println("Ação desconhecida")
    }
}
