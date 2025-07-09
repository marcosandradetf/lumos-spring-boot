package com.lumos.ui.components

import android.graphics.drawable.Icon
import android.graphics.fonts.Font
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.UpdateDisabled
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun NoInternet() {
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
                text = "Estamos com problemas para conectar aos servidores. Por favor, reconecte.",
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
        }
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}



@Preview(showBackground = true)
@Composable
fun PrevComponents() {
    NoInternet()
}