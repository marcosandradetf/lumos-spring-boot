package com.lumos.ui.components


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FireTruck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FireTruck
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumos.navigation.BottomBar
import kotlinx.coroutines.launch

@Composable
fun AppLayout(
    title: String,
    selectedIcon: Int = 0,
    notificationsBadge: String = "0",
    navigateBack: (() -> Unit)? = null,
    navigateToHome: (() -> Unit?)? = null,
    navigateToMore: (() -> Unit?)? = null,
    navigateToStock: (() -> Unit?)? = null,
    navigateToMaintenance: (() -> Unit?)? = null,
    navigateToExecutions: (() -> Unit?)? = null,
    navigateToNotifications: (() -> Unit)? = null,
    content: @Composable (Modifier, showSnackBar: (String, String?, (() -> Unit)?) -> Unit) -> Unit,
) {
    val selectedItem by remember { mutableIntStateOf(selectedIcon) }
    val items = listOf("Início", "Estoque", "Manuten.", "Instalação", "Mais")
    val selectedIcons =
        listOf(
            Icons.Filled.Home,
            Icons.Filled.FireTruck,
            Icons.Filled.Build,
            Icons.Filled.Lightbulb,
            Icons.Filled.MoreHoriz,
        )
    val unselectedIcons =
        listOf(
            Icons.Outlined.Home,
            Icons.Outlined.FireTruck,
            Icons.Outlined.Build,
            Icons.Outlined.Lightbulb,
            Icons.Outlined.MoreHoriz,
        )

    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    val showSnackBar: (String, String?, (() -> Unit)?) -> Unit = { message, label, action ->
        scope.launch {
            val result = snackBarHostState.showSnackbar(
                message = message,
                actionLabel = label,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed && label != null) {
                action?.invoke()
            }
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
                    title = title,
                    notificationsBadge = notificationsBadge,
                    navigateToNotifications = navigateToNotifications,
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
                            if (index in selectedIcons.indices && index in unselectedIcons.indices) {
                                Icon(
                                    imageVector = if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                    contentDescription = item,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        label = { Text(text = item, fontSize = 8.sp) },
                        selected = selectedIcon == index,
                        onClick = {
                            handleNavigation(
                                index,
                                navigateToHome,
                                navigateToStock,
                                navigateToMaintenance,
                                navigateToExecutions,
                                navigateToMore,
                            )
                        },
                    )
                }
            }
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                content(
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
    navigateToHome: (() -> Unit?)?,
    navigateToStock: (() -> Unit?)?,
    navigateToMaintenance: (() -> Unit?)?,
    navigateToExecutions: (() -> Unit?)?,
    navigateToMore: (() -> Unit?)?,
) {
    when (index) {

        BottomBar.HOME.value -> if (navigateToHome != null) {
            navigateToHome()
        }

        BottomBar.STOCK.value -> if (navigateToStock != null) {
            navigateToStock()
        }

        BottomBar.MAINTENANCE.value -> if (navigateToMaintenance != null) {
            navigateToMaintenance()
        }

        BottomBar.EXECUTIONS.value -> if (navigateToExecutions != null) {
            navigateToExecutions()
        }


        BottomBar.MORE.value -> if (navigateToMore != null) {
            navigateToMore()
        }

        else -> println("Ação desconhecida")
    }
}
