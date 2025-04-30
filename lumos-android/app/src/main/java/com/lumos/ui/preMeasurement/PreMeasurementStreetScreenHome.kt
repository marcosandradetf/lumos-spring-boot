package com.lumos.ui.preMeasurement

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.navigation.Routes

@Composable
fun MeasurementHome(
    onNavigateToHome: () -> Unit,
    navController: NavHostController,
    context: Context
) {
    Scaffold(
        containerColor = Color(0xFFF5F5F7),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(10.dp)
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Fechar",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onNavigateToHome() },
                    tint = Color(0xFF757575) // Cor moderna para o ícone
                )
            }
        },
        bottomBar = {
            // Botão fixado no fim da tela
            ElevatedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .padding(bottom = 50.dp)
                    .height(48.dp),
                onClick = { navController.navigate(Routes.PRE_MEASUREMENT_STREET) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(8.dp) // Botão com cantos menos arredondados
            ) {
                Text(
                    text = "Iniciar Pré-Medição",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }, content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(10.dp)
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)) // Fundo moderno, cinza claro
            ) {
                // Conteúdo principal (coluna no topo)
                Column(
                    Modifier.fillMaxWidth()
                ) {


                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pré-Medição",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00308F) // Azul escuro
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Lembre-se de que a pré-medição exige total atenção e exclusividade no uso desta funcionalidade.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF616161), // Cinza médio
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Ao iniciar a pré-medição usaremos suas coordenadas para definir a rua, número e bairro. Tenha atenção com as informações.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF616161), // Cinza médio
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Quando estiver pronto, clique no botão abaixo para começar a pré-medição. Este processo permitirá capturar dados precisos e essenciais para o seu trabalho.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF616161),
                            lineHeight = 20.sp
                        )
                    )
                }


            }
        }


    )

}


@Preview
@Composable
fun PrevMeasurement() {
    MeasurementHome(
        {},
        rememberNavController(),
        LocalContext.current
    )
}