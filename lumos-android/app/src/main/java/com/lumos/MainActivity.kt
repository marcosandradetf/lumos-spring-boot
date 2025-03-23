package com.lumos

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.lumos.navigation.AppNavigation
import com.lumos.ui.theme.LumosTheme


class MainActivity : ComponentActivity() {
    //    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Instancie o FusedLocationProviderClient corretamente
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Solicite as permissões de localização
        checkAndRequestPermissions()

        enableEdgeToEdge()
        val app = application as MyApp

        FirebaseMessaging.getInstance().subscribeToTopic("RESPONSAVEL_TECNICO")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("✅ Inscrição no tópico realizada com sucesso!")
                } else {
                    println("❌ Falha ao se inscrever no tópico.")
                }
            }

        setContent {
            val actionState = remember { mutableStateOf<String?>(intent?.getStringExtra("action")) }

            // Observa mudanças de intent (caso o app já esteja aberto)
            LaunchedEffect(Unit) {
                intent?.getStringExtra("action")?.let {
                    actionState.value = it
                }
            }

            LumosTheme {
                AppNavigation(
                    database = app.database,
                    retrofit = app.retrofit,
                    secureStorage = app.secureStorage,
                    context = this,
                    actionState = actionState
                )
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // O usuário marcou "Não perguntar novamente"
                showSettingsDialog()
            } else {
                showPermissionRationale("LOCATION")
            }

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
                showPermissionRationale("LOCATION")
            }

            else -> {
                showSettingsDialog()
            }

        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                return
            }

            (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) -> {
                showPermissionRationale("NOTIFICATIONS")
            }

        }
    }

    private fun showPermissionRationale(type: String) {
        when (type) {
            "LOCATION" -> {
                AlertDialog.Builder(this) // Se for Fragment, use requireContext()
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

            "NOTIFICATIONS" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
                    AlertDialog.Builder(this) // Se for Fragment, use requireContext()
                        .setTitle("Permissão de Notificações")
                        .setMessage("Para receber notificações, o app precisa da sua permissão. Deseja permitir?")
                        .setPositiveButton("Permitir") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }

            else -> throw IllegalArgumentException("Tipo de permissão inválido: $type")
        }
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
