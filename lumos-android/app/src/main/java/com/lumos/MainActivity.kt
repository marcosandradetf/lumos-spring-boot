package com.lumos

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lumos.navigation.AppNavigation
import com.lumos.ui.theme.LumosTheme
import kotlinx.coroutines.launch


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

//        val currentVersion = packageManager.getPackageInfo(packageName, 0).longVersionCode

        // Suponha que fetchMinVersion() seja uma função suspensa que chama sua API
//        lifecycleScope.launch {
//            val minVersion = fetchMinVersionFromApi()
//            if (currentVersion < minVersion) {
//                // Abre tela obrigatória para atualizar
//                startActivity(Intent(this@MainActivity, UpdateRequiredActivity::class.java))
//                finish() // Fecha MainActivity para não continuar
//            } else {
//                // Continua normalmente
//                setContent {
//                    val actionState =
//                        remember { mutableStateOf<String?>(intent?.getStringExtra("action")) }
//
//                    LumosTheme {
//                        AppNavigation(
//                            db = app.database,
//                            retrofit = app.retrofit,
//                            secureStorage = app.secureStorage,
//                            context = this as Context,
//                            actionState
//                        )
//                    }
//                }
//            }
//        }

        setContent {
            val actionState =
                remember { mutableStateOf<String?>(intent?.getStringExtra("action")) }

            LumosTheme {
                AppNavigation(
                    db = app.database,
                    retrofit = app.retrofit,
                    secureStorage = app.secureStorage,
                    context = this as Context,
                    actionState
                )
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        // Verifica se a permissão de Localização foi concedida
        if (permissionsResult[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // A permissão de localização foi concedida
            println("Permissão de localização concedida.")
        } else {
            // A permissão de localização não foi concedida
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // O usuário marcou "Não perguntar novamente"
                showSettingsDialog()
            } else {
                // Solicita novamente a permissão de localização
                showPermissionRationale("LOCATION")
            }
        }

        // Verifica se a permissão de Notificações (Android 13+) foi concedida
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (permissionsResult[Manifest.permission.POST_NOTIFICATIONS] == true) {
                // A permissão de notificações foi concedida
                println("Permissão de notificações concedida.")
            } else {
                // A permissão de notificações não foi concedida
                if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    // O usuário marcou "Não perguntar novamente"
                    showSettingsDialog()
                } else {
                    // Solicita novamente a permissão de notificações
                    showPermissionRationale("NOTIFICATIONS")
                }
            }
        }

        // Verifica se a permissão de Estado do Telefone foi concedida
        if (permissionsResult[Manifest.permission.READ_PHONE_STATE] == true) {
            // A permissão de estado do telefone foi concedida
            println("Permissão de estado do telefone concedida.")
        } else {
            // A permissão de estado do telefone não foi concedida
            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {
                // O usuário marcou "Não perguntar novamente"
                showSettingsDialog()
            } else {
                // Solicita novamente a permissão de estado do telefone
                showPermissionRationale("READ_PHONE_STATE")
            }
        }

        if (permissionsResult[Manifest.permission.CAMERA] == false) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // O usuário marcou "Não perguntar novamente"
                showSettingsDialog()
            } else {
                // Solicita novamente a permissão de estado do telefone
                showPermissionRationale("CAMERA")
            }
        }
    }


    private fun checkAndRequestPermissions() {
        // Lista de permissões a serem solicitadas
        val permissions = mutableListOf<String>()

        // Adiciona a permissão de localização
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Adiciona a permissão de notificações (para Android 13 ou superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Adiciona a permissão de estado do telefone
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        // Se houver permissões para solicitar, chama o launcher
        if (permissions.isNotEmpty()) {
            // Use o RequestMultiplePermissions para várias permissões
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }


    private fun showPermissionRationale(type: String) {
        val message: String
        val permission: String
        val title: String

        // Define título, mensagem e permissão conforme o tipo de permissão
        when (type) {
            "LOCATION" -> {
                title = "Permissão Necessária"
                message =
                    "O aplicativo precisa de permissão de localização para funcionar corretamente. Por favor, permita o acesso."
                permission = Manifest.permission.ACCESS_FINE_LOCATION
            }

            "NOTIFICATIONS" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    title = "Permissão de Notificações"
                    message =
                        "Para receber notificações, o app precisa da sua permissão. Deseja permitir?"
                    permission = Manifest.permission.POST_NOTIFICATIONS
                } else {
                    return
                }
            }

            "READ_PHONE_STATE" -> {
                title = "Permissão de Rede"
                message =
                    "Para evitar instabilidade durante a sincronização, o app precisa dessa permissão. Por favor, permita o acesso."
                permission = Manifest.permission.READ_PHONE_STATE
            }

            "CAMERA" -> {
                title = "Permissão de Câmera"
                message =
                    "Para tirar fotos das pré-medições, o app precisa dessa permissão. Por favor, permita o acesso."
                permission = Manifest.permission.READ_PHONE_STATE
            }

            else -> throw IllegalArgumentException("Tipo de permissão inválido: $type")
        }

        // Exibe o diálogo
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Permitir") { _, _ ->
                requestPermissionLauncher.launch(arrayOf(permission)) // Solicita a permissão
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Toast.makeText(
                    this,
                    "Permissão crucial negada, fechando o app.",
                    Toast.LENGTH_SHORT
                ).show()
                finish() // Fecha o app se a permissão for crucial
            }
            .setCancelable(false)
            .show()
    }


    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissão Necessária")
            .setMessage("Alguma permissão foi negada permanentemente. Para continuar, na tela de configurações acesse a opção permissões e habilite as pendências.")
            .setPositiveButton("Abrir Configurações") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Toast.makeText(
                    this,
                    "App fechado. Até logo!",
                    Toast.LENGTH_SHORT
                ).show()
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
