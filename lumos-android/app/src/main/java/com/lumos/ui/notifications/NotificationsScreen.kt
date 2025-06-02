package com.lumos.ui.notifications

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.data.api.NotificationType
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.notifications.NotificationItem
import com.lumos.ui.components.AppLayout
import com.lumos.ui.viewmodel.NotificationViewModel
import com.lumos.utils.Utils
import java.time.Instant

@Composable
fun NotificationsScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController,
    notificationViewModel: NotificationViewModel,
    context: Context,
) {
    val notifications by notificationViewModel.notifications

    LaunchedEffect(Unit, notifications) {
        notificationViewModel.loadNotifications()
    }

    NotificationsList(
        onNavigateToMenu = onNavigateToMenu,
        onNavigateToHome = onNavigateToHome,
        onNavigateToProfile = onNavigateToProfile,
        navController = navController,
        context = context,
        notifications = notifications,
        clear = {
            notificationViewModel.deleteAll()
        },
        onClick = {
            notificationViewModel.delete(it)
        },
        onNavigate = {
            navController.navigate(it)
        },
        notificationsBadge = notifications.size.toString()
    )

}

@Composable
fun NotificationsList(
    onNavigateToMenu: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController,
    context: Context,
    notifications: List<NotificationItem>,
    clear: () -> Unit,
    onClick: (Long) -> Unit,
    onNavigate: (String) -> Unit,
    notificationsBadge: String
) {
    AppLayout(
        title = "Notificações",
        pSelected = BottomBar.NOTIFICATIONS.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        notificationsBadge = notificationsBadge,
        context = context,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (notifications.isNotEmpty())
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                clear()
                            }) {
                            Row {
                                Icon(
                                    imageVector = Icons.Default.ClearAll,
                                    contentDescription = "Limpar Notificações",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Limpar Notificações",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                        }
                    }
                }
            else
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Notificações",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = "Nenhuma notificação no momento",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            items(notifications) { notification ->
                NotificationCard(
                    notification,
                    onClick = {
                        onClick(it)
                    },
                    onNavigate = {
                        onNavigate(it)
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    onClick: (Long) -> Unit,
    onNavigate: (String) -> Unit
) {
    val icon: ImageVector = when (notification.type) {
        NotificationType.CONTRACT -> Icons.Default.Mail
        NotificationType.UPDATE -> Icons.Default.Update
        NotificationType.EVENT -> Icons.Default.Event
        NotificationType.WARNING -> Icons.Default.Warning
        NotificationType.CASH -> Icons.Default.AttachMoney

        else -> Icons.Default.Texture
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(notification.id)
                onNavigate(notification.action)
            }
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSecondary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone da notificação
            Icon(
                imageVector = icon,
                contentDescription = "Ícone de Notificação",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                // Título da notificação
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Descrição da notificação
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Horário da notificação
            Text(
                text = "Há ${Utils.timeSinceCreation(Instant.parse(notification.time))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


@Preview
@Composable
fun PrevNotifications() {
    NotificationsList(
        {},
        {},
        {},
        rememberNavController(),
        LocalContext.current,
        listOf(
            NotificationItem(
                title = "Alerta de segurança",
                body = "Houve uma tentativa de login suspeita.",
                action = Routes.CONTRACT_SCREEN,
                time = Instant.parse("2025-03-20T20:00:50.765Z").toString(),
                type = "Alert"
            ),
            NotificationItem(
                title = "Alerta de segurança",
                body = "Houve uma tentativa de login suspeita.",
                action = Routes.CONTRACT_SCREEN,
                time = Instant.parse("2025-03-20T20:00:50.765Z").toString(),
                type = "Alert"
            ),
        ),
        {},
        {},
        {},
        "12"
    )
}