package com.lumos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.lumos.navigation.AppNavigation
import com.lumos.ui.theme.LumosTheme

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Instancie o FusedLocationProviderClient corretamente
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Solicitar permissão
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissão Necessária")
            .setMessage("A localização é essencial para o funcionamento do aplicativo. O aplicativo será encerrado caso você não permita o acesso à localização.")
            .setPositiveButton("Tentar Novamente") { _, _ ->
                // Re-solicite a permissão
                checkAndRequestPermissions()
            }
            .setNegativeButton("Sair") { _, _ ->
                // Feche o app
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permissão de Localização Necessária")
            .setMessage("O aplicativo utiliza sua localização para realizar medições com precisão. Por favor, permita o acesso à localização.")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setCancelable(false)
            .show()
    }

}
