package com.lumos.ui.components


import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.lumos.navigation.BottomBar
import kotlinx.coroutines.launch

@Composable
fun AppLayout(
    title: String,
    pSelected: Int = 1,
    notificationsBadge: String,
    sliderNavigateToMenu: (() -> Unit?)? = null,
    sliderNavigateToHome: (() -> Unit?)? = null,
    sliderNavigateToNotifications: (() -> Unit?)? = null,
    sliderNavigateToProfile: (() -> Unit?)? = null,
    navController: NavHostController,
    navigateBack: (() -> Unit)? = null,
    context: Context, // Adicione o contexto como parâmetro para passar para o NetworkStatusBar
    pContent: @Composable (Modifier, showSnackBar: (String, String?) -> Unit) -> Unit,
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

    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    // Função simples que você vai expor pro conteúdo
    val showSnackBar: (String, String?) -> Unit = { message, label ->
        scope.launch {
            snackBarHostState.showSnackbar(
                message = message,
                actionLabel = label,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        },
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                TopBar(
                    navigateBack = navigateBack,
                    title = title
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            if (item == "Notificações")
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Color(0xFFF55159),
                                            contentColor = Color.White
                                        ) { Text(notificationsBadge) }
                                    } // Verifique se o Badge é suportado
                                ) {
                                    if (index in selectedIcons.indices && index in unselectedIcons.indices) {
                                        Icon(
                                            imageVector = if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                            contentDescription = item
                                        )
                                    }
                                } else
                                if (index in selectedIcons.indices && index in unselectedIcons.indices) {
                                    Icon(
                                        imageVector = if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                        contentDescription = item
                                    )
                                }
                        },
                        label = { Text(item) },
                        selected = pSelected == index,
                        onClick = {
                            handleNavigation(
                                index,
                                sliderNavigateToMenu,
                                sliderNavigateToHome,
                                sliderNavigateToNotifications,
                                sliderNavigateToProfile
                            )
                        },
                    )
                }
            }
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                pContent(
                    Modifier
                        .padding(10.dp)
                        .fillMaxSize(),
                    showSnackBar
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
