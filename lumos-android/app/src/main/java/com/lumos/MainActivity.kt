package com.lumos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lumos.domain.service.SyncMeasurement
import com.lumos.navigation.AppNavigation
import com.lumos.ui.theme.LumosTheme
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
//    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Instancie o FusedLocationProviderClient corretamente
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Solicite as permissões de localização
        checkAndRequestPermissions()

        // Ative o Edge-to-Edge (ou outra funcionalidade relacionada)
        enableEdgeToEdge()

        // Configure o tema e a navegação do Jetpack Compose
        setContent {
            LumosTheme {
                AppNavigation()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Permissão negada, exiba um alerta e feche o app
            showPermissionRationale()
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            // Caso a permissão já tenha sido concedida
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                return
            }

            (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) -> {
                showPermissionRationale()
            }

            else -> {
                showSettingsDialog()
            }

        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permissão Necessária")
            .setMessage("O aplicativo precisa de permissão de localização para funcionar corretamente. Por favor, permita o acesso.")
            .setPositiveButton("Permitir") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish() // Feche o app se a permissão for crucial
            }
            .setCancelable(false)
            .show()
    }


    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissão Bloqueada")
            .setMessage("A permissão de localização foi permanentemente negada. Por favor, habilite a permissão manualmente nas configurações do aplicativo.")
            .setPositiveButton("Abrir Configurações") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish() // Feche o app se a permissão for crucial
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }


}
