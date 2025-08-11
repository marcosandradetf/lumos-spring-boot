package com.lumos.ui.components

import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes

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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Info, contentDescription = "Info")
        Text(
            description, Modifier
                .width(300.dp)
                .padding(top = 10.dp), textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Confirm(
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
                text = message ?: "Estamos com problemas para conectar aos servidores. Por favor, reconecte.",
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
fun Tag(text: String, color: Color, icon: ImageVector? = null ) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            if(icon!=null)Icon(imageVector = icon, contentDescription = null)
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
fun CurrentScreenLoading(navController: NavHostController, currentScreenName: String, loadingLabel: String? = null, selectedIcon: Int = BottomBar.MAINTENANCE.value) {
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
    val iconId = context.applicationInfo.icon
    val appIcon = if (iconId != 0) painterResource(id = iconId) else null

    AlertDialog(
        onDismissRequest = { /* bloquear dismiss para não fechar sozinho */ },
        shape = RoundedCornerShape(12.dp),
        icon = {
            appIcon?.let {
                Icon(
                    painter = it,
                    contentDescription = "App Icon",
                    modifier = Modifier.size(48.dp)
                )
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
fun ConfirmNavigation(route: String, navController: NavHostController, onDismiss: () -> Unit) {
    Confirm(
        title = "Confirme sua ação",
        body = "Deseja sair?",
        confirm = {
            navController.navigate(route)
        },
        cancel = {
            onDismiss()
        }
    )
}


@Preview(showBackground = true)
@Composable
fun PrevComponents() {
    NoInternet()
}