package com.lumos.ui.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.utils.UpdateManager.downloadApk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ApkUpdateDownloader(
    apkUrl: String,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
) {
    var progress by remember { mutableIntStateOf(0) }
    var downloadComplete by remember { mutableStateOf(false) }
    var exit by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    AppLayout(
        title = "Atualização",
        pSelected = BottomBar.HOME.value,
        sliderNavigateToMenu = {
            if (exit) navController.navigate(Routes.MENU)
            else Toast.makeText(context, "Aguarde o término da atualização", Toast.LENGTH_SHORT)
                .show()
        },
        sliderNavigateToHome = {
            if (exit) navController.navigate(Routes.HOME)
            else Toast.makeText(context, "Aguarde o término da atualização", Toast.LENGTH_SHORT)
                .show()
        },
        sliderNavigateToNotifications = {
            if (exit) navController.navigate(Routes.NOTIFICATIONS)
            else Toast.makeText(context, "Aguarde o término da atualização", Toast.LENGTH_SHORT)
                .show()
        },
        sliderNavigateToProfile = {
            if (exit) navController.navigate(Routes.PROFILE)
            else Toast.makeText(context, "Aguarde o término da atualização", Toast.LENGTH_SHORT)
                .show()
        },
        navController = navController,
        navigateBack = {
            if (exit) navController.navigate(Routes.HOME)
            else Toast.makeText(context, "Aguarde o término da atualização", Toast.LENGTH_SHORT)
                .show()
        },
        context = context,
        notificationsBadge = notificationsBadge,
    ) { _, snackBar ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Atualização disponível",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Estamos baixando a nova versão para você. Por favor, aguarde...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$progress%")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (downloadComplete && downloadedFile != null) {
                        exit = true

                        if (!context.packageManager.canRequestPackageInstalls()) {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = "package:${context.packageName}".toUri()
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } else {

                            val apkUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                downloadedFile!!
                            )

                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(apkUri, "application/vnd.android.package-archive")
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }

                            // 🔒 Garantir que o instalador tenha permissão de leitura do arquivo
                            val resInfoList = context.packageManager.queryIntentActivities(
                                intent,
                                PackageManager.MATCH_DEFAULT_ONLY
                            )
                            for (resolveInfo in resInfoList) {
                                val packageName = resolveInfo.activityInfo.packageName
                                context.grantUriPermission(
                                    packageName,
                                    apkUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }

                            context.startActivity(intent)
                        }
                    } else {
                        Toast.makeText(context, "APK não encontrado", Toast.LENGTH_SHORT).show()
                        exit = true
                    }
                },
                enabled = downloadComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (downloadComplete) "INSTALAR" else "Baixando...")
            }
        }
    }

    // Inicia download assim que o Composable entra na composição
    LaunchedEffect(apkUrl) {
        try {
            val file = downloadApk(
                context = context,
                url = Uri.decode(apkUrl),
                onProgress = { p -> progress = p }
            )
            downloadedFile = file
            downloadComplete = true
        } catch (e: Exception) {
            exit = true
            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

}

@Preview
@Composable
fun PrevUpdate() {
    ApkUpdateDownloader(
        "",
        LocalContext.current,
        rememberNavController(),
        "1"
    )
}