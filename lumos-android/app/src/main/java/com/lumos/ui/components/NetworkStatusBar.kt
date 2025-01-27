package com.lumos.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NetworkStatusBar(context: Context) {
    var noNetwork by remember { mutableStateOf(false) }

    // Registre o callback para mudanças de rede
    DisposableEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                noNetwork = false
            }

            override fun onLost(network: Network) {
                noNetwork = true
            }
        }

        // Registra o callback
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Cancela o callback quando o Composable é destruído
        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    // Exibe a barra de aviso
    if (noNetwork) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)

                .padding(top = 30.dp)
        ) {
            Row(modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Info,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp, 20.dp),

                    contentDescription = "Informação de rede"
                )
                Text(
                    text = "Sem conexão com a internet",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }

        }
    }
}
