package com.lumos.ui.lockscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lumos.R
import com.lumos.domain.service.AppInitCoordinator
import com.lumos.repository.AuthRepository
import com.lumos.repository.RemoteConfigRepository
import com.lumos.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun LockedAppScreen(
    reason: String? = null,
    appInitCoordinator: AppInitCoordinator? = null,
    authRepository: AuthRepository
) {
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(R.drawable.ic_lumos),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Acesso bloqueado temporariamente",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = reason
                        ?: "Fale com seu administrador de TI para obter mais informações.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                if (appInitCoordinator != null) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(.5f),
                        onClick = {
                            scope.launch {
                                appInitCoordinator.onAppStart()
                            }
                        }
                    ) {
                        Text("Verificar acesso")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(.5f),
                    onClick = {
                        scope.launch {
                            authRepository.logout {
                                SessionManager.setLoggedOut(true)
                            }
                        }
                    }
                ) {
                    Text("Desconectar")
                }
            }
        }
    }
}
