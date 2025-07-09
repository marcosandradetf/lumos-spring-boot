package com.lumos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navigateBack: (() -> Unit)? = null,
    title: String = "Navigation example",
    notificationsBadge: String = "10",
    navigateToNotifications: (() -> Unit)? = null,
) {
    TopAppBar(
        modifier = Modifier.height(70.dp),
        colors = TopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            navigationIconContentColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.secondary,
            actionIconContentColor = MaterialTheme.colorScheme.secondary,
        ),
        title = {
            Row(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                if (navigateToNotifications != null)
                    BadgedBox(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clickable {
                                navigateToNotifications()
                            },
                        badge = {
                            Badge(
                                containerColor = Color(0xFFF55159),
                                contentColor = Color.White,
                            ) { Text(notificationsBadge, fontSize = 10.sp) }
                        } // Verifique se o Badge é suportado
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notificações",
                            modifier = Modifier
                                .size(30.dp)
                        )
                    }
            }

        },
        navigationIcon = {
            if (navigateBack != null) {
                Row(
                    Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                        )
                    }
                }

            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TopBar(
        navigateBack = {}
    )
}