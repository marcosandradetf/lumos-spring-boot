package com.lumos.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.lumos.R
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.maintenance.MaintenanceUIState

@Composable
fun Loading(label: String? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.width(34.dp),
            color = MaterialTheme.colorScheme.onBackground,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        if (label != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(label, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun NothingData(description: String) {
    // micro animação de entrada (fade + leve scale)
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.98f),
            exit = fadeOut()
        ) {
            // cartão bem clean, só com borda suave
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .widthIn(max = 420.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 340.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun Confirm(
    title: String = "Confirmação",
    body: String = "Você tem certeza?",
    icon: ImageVector? = null,
    confirm: () -> Unit,
    cancel: () -> Unit,
    textAlign: TextAlign = TextAlign.Center
) {
    AlertDialog(
        shape = RoundedCornerShape(12.dp),
        onDismissRequest = cancel,
        icon = {
            icon?.let {
                Icon(it, contentDescription = "Icon")
            }
        },
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = body,
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = textAlign,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        },

        confirmButton = {},
        dismissButton = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp)
            ) {


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { confirm() }
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sim",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }




                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { cancel() }
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Não",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },

        )
}

@Composable
fun Alert(
    title: String = "Atenção",
    body: String = "Você tem certeza?",
    icon: ImageVector? = null,
    confirm: () -> Unit,
) {
    AlertDialog(
        shape = RoundedCornerShape(12.dp),
        onDismissRequest = confirm,
        icon = {
            icon?.let {
                Icon(it, contentDescription = "Icon")
            }
        },
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = body,
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        },

        confirmButton = {},
        dismissButton = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp)
            ) {


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { confirm() }
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Ok",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                }
            }
        },

        )
}


@Composable
fun Option(
    title: String = "Confirmação",
    body: String = "Você tem certeza?",
    icon: ImageVector? = null,
    confirm: () -> Unit,
    cancel: () -> Unit,
) {
    AlertDialog(
        shape = RoundedCornerShape(12.dp),
        onDismissRequest = cancel,
        icon = {
            icon?.let {
                Icon(it, contentDescription = "Icon")
            }
        },
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = body,
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        },

        confirmButton = {},
        dismissButton = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp)
            ) {


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { confirm() }
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sim",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }




                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { cancel() }
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Não",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },

        )
}

@Composable
fun NoInternet(message: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Você está offline",
            modifier = Modifier
                .fillMaxWidth(fraction = 0.9f)
                .padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Left,
            color = MaterialTheme.colorScheme.error
        )
        Box(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                text = message
                    ?: "Estamos com problemas para conectar aos servidores. Utilizando dados em cache.",
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.9f)
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun Tag(text: String, color: Color, icon: ImageVector? = null) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            if (icon != null) Icon(imageVector = icon, contentDescription = null)
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color,
            leadingIconContentColor = color
        ),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun CurrentScreenLoading(
    navController: NavHostController,
    currentScreenName: String,
    loadingLabel: String? = null,
    selectedIcon: Int = BottomBar.MAINTENANCE.value
) {
    AppLayout(
        title = currentScreenName,
        selectedIcon = selectedIcon,
        navigateToHome = {
            navController.navigate(Routes.HOME)
        },
        navigateToMore = {
            navController.navigate(Routes.MORE)
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK)
        },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE)
        },
        navigateToExecutions = {
            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
        }
    ) { _, _ ->
        Loading(loadingLabel)
    }
}


@Composable
fun UpdateModal(
    context: Context,
    title: String = "Atualização em andamento",
    body: String = "Por favor, aguarde enquanto o app está sendo atualizado...",
    progress: Int = 0,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* bloquear dismiss para não fechar sozinho */ },
        shape = RoundedCornerShape(12.dp),
        icon = {
            Image(
                painter = painterResource(id = R.drawable.ic_lumos), // Agora no drawable
                contentDescription = "Ícone do App",
                modifier = Modifier.size(50.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$body $progress%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
            }
        },
        confirmButton = {
            if (progress >= 100) {
                TextButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reiniciar App",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        dismissButton = {
            if (progress < 100) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancelar",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@Composable
fun ConfirmNavigation(route: String, navController: NavHostController, body: String = "Deseja sair?", onDismiss: () -> Unit) {
    Confirm(
        title = "Confirme sua ação",
        body = body,
        confirm = {
            if (route == "back") {
                navController.popBackStack()
            } else {
                navController.navigate(route)
            }

        },
        cancel = {
            onDismiss()
        }
    )
}

@Composable
fun UserAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = remember(name) {
        name.trim()
            .split(Regex("\\s+"))
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .ifEmpty { "?" }
    }
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun FinishScreen(
    screenTitle: String,

    navigateBack: (() -> Unit)? = null,

    messageTitle: String = "Missão cumprida!",
    messageBody: String = "Os dados serão enviados para o sistema.",
    navController: NavHostController,
    clickBack: () -> Unit
) {

    AppLayout(
        title = screenTitle,
        selectedIcon = BottomBar.MAINTENANCE.value,
        navigateBack = {
            navigateBack
        },
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
    ) { _, _ ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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
                    text = messageTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = messageBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth(fraction = 0.5f),
                    onClick = {
                        clickBack()
                    }
                ) {
                    Text("Voltar")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrevComponents() {
    UpdateModal(
        context = LocalContext.current,
        progress = 100,
        onRestart = {

        },
        onDismiss = {

        }
    )

}