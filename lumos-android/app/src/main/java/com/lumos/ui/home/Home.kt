package com.lumos.ui.home

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat

@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)

    // Defina o estado para latitude e longitude
    var vLatitude by remember { mutableStateOf<Double?>(null) }
    var vLongitude by remember { mutableStateOf<Double?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var btnClick by remember { mutableStateOf<Boolean>(false) }

    // Crie a instância do seu UseCase
    val coord = CoordinatesService(context, fusedLocationProvider)

    // Execute a função assíncrona
    LaunchedEffect(Unit) {

    }

    AppLayout(
        title = "Início",
        pSelected = BottomBar.HOME.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        context = context
    ) { modifier ->
        Column(
            modifier = modifier
        ) {

            Text("Home Screen")

            Button(onClick = {
                coord.execute { latitude, longitude ->
                    if (latitude != null && longitude != null) {
                        vLatitude = latitude
                        vLongitude = longitude
                        val addr = AddressService(context)
                        address = addr.execute(latitude, longitude)?.get(0).toString()
                        btnClick = true
                    } else {
                        btnClick = false
                        Log.e("GET Address", "Latitude ou Longitude são nulos.")
                    }
                }

            }) {
                Text("Buscar Localização")
            }

            if (btnClick) {

                Column {

                    Row {
                        Text(text = vLatitude.toString())
                        Text(text = vLongitude.toString())
                    }

                    Text(
                        text = address ?: "Endereço não disponível",
                        modifier = Modifier.padding(16.dp)
                    )

                    Button(
                        onClick = { openGoogleMaps(vLatitude!!, vLongitude!!, context) },
                        content = { Text("Abrir no Google Maps") }
                    )

                }


            }

        }
    }
}


fun openGoogleMaps(latitude: Double, longitude: Double, context: Context) {
    // Verifica se o Google Maps está instalado
    val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps") // Especifica o pacote do Google Maps
    }

    try {
        // Tenta abrir o Google Maps com o intent
        ContextCompat.startActivity(context, mapIntent, null)
    } catch (e: Exception) {
        // Caso não consiga abrir o Google Maps, mostre uma mensagem de erro
        Toast.makeText(context, "Erro ao abrir o Google Maps", Toast.LENGTH_SHORT).show()
    }
}

@Preview
@Composable
fun PrevHome() {
    HomeScreen(
        {},
        {},
        {},
        rememberNavController(),
    )
}