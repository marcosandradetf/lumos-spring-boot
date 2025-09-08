package com.lumos.ui.preMeasurement

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lumos.domain.model.Contract
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.CurrentScreenLoading
import com.lumos.ui.components.FinishScreen
import com.lumos.utils.Utils
import com.lumos.viewmodel.PreMeasurementViewModel

@Composable
fun PreMeasurementProgressScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToPreMeasurements: () -> Unit,
    context: Context,
    preMeasurementViewModel: PreMeasurementViewModel,
    navController: NavHostController,
) {
    val streets by preMeasurementViewModel.streets
    val measurement = preMeasurementViewModel.measurement
    var currentContractor = ""


    LaunchedEffect(Unit) {
        currentContractor = measurement?.contractor ?: ""
        preMeasurementViewModel.loadStreets()
    }


    if (preMeasurementViewModel.loading) {
        CurrentScreenLoading(
            navController = navController,
            currentScreenName = Utils.abbreviate(measurement?.contractor!!),
            loadingLabel = "Loading",
            selectedIcon = BottomBar.MORE.value
        )
    } else if (measurement == null) {
        FinishScreen(
            screenTitle = Utils.abbreviate(currentContractor),
            navigateBack = {
                navController.popBackStack()
            },
            messageTitle = "Pré-medição finalizada!",
            messageBody = "Os dados serão enviados ao sistema.",
            navController = navController,
            clickBack = {
                navController.popBackStack()
            }
        )
    } else {
        PMPContent(
            measurement = measurement,
            onNavigateToHome = onNavigateToHome,
            onNavigateToMenu = onNavigateToMenu,
            onNavigateToPreMeasurements = onNavigateToPreMeasurements,
            onNavigateToStreet = {
                navController.navigate(Routes.PRE_MEASUREMENT_STREET + "/$it")
            },
            navController = navController,
            streets = streets,
            sendPreMeasurement = {
                if (streets.isNotEmpty()) {
                    preMeasurementViewModel.queueSendMeasurement()
                } else {
                    Toast
                        .makeText(
                            context,
                            "Não é permitido enviar sem finalizar pelo menos uma rua!",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }

            }
        )
    }
}

@Composable
fun PMPContent(
    measurement: PreMeasurement,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToPreMeasurements: () -> Unit,
    onNavigateToStreet: (String) -> Unit,
    navController: NavHostController,
    streets: List<PreMeasurementStreet>,
    sendPreMeasurement: () -> Unit
) {
    AppLayout(
        title = Utils.abbreviate(measurement.contractor),
        selectedIcon = BottomBar.MORE.value,
        navigateToMore = onNavigateToMenu,
        navigateToHome = onNavigateToHome,
        navigateBack = {
            navController.popBackStack()
        },
        navigateToExecutions = {
            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
        },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE)
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK)
        }
    ) { modifier, snackBar ->
        var expanded by remember { mutableStateOf(false) }

        // Box para garantir que o conteúdo fique acima e o botão no final
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp) // Espaço entre os cards
            ) {
                item {
                    AnimatedVisibility(visible = !expanded) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = measurement.contractor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally, // Centraliza os itens horizontalmente
                                        verticalArrangement = Arrangement.Center // Mantém o alinhamento vertical
                                    ) {
                                        Text(
                                            text = "Contrato",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(bottom = 2.dp) // Pequeno espaço entre o texto e o ícone
                                        )

                                        Icon(
                                            imageVector = Icons.Default.Downloading,
                                            contentDescription = "Baixar Contrato",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp) // Ajuste do tamanho do ícone
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Streets(streets = streets, expanded = expanded, callbackExpanded = {
                        expanded = it
                    })
                }

                item {
                    AnimatedVisibility(
                        visible = !expanded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Button(
                            onClick = {
                                onNavigateToStreet(measurement.preMeasurementId)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) // Azul
                        ) {
                            Text(
                                text = "Adicionar rua",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Adicionar",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                }
            }

            // Botão fixado no final da tela

            AnimatedVisibility(
                visible = !expanded,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(25.dp)
                    .align(Alignment.BottomCenter)
            ) {
                FinishPreMeasurementButton(
                    onClick = {
                        sendPreMeasurement()
                    }
                )
            }
        }
    }
}


@Composable
fun Streets(
    streets: List<PreMeasurementStreet>,
    expanded: Boolean,
    callbackExpanded: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .clickable {
                callbackExpanded(!expanded)
            },
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .padding(bottom = if (expanded) 20.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ruas Adicionadas",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expandir ou recolher",
                    modifier = Modifier.size(24.dp)
                )
            }


            // Lista de ruas com animação de visibilidade
            AnimatedVisibility(visible = expanded) {
                Column {
                    streets.forEachIndexed { index, street ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Localização",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = street.address ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun FinishPreMeasurementButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface) // Azul
        ,
        elevation = ButtonDefaults.elevatedButtonElevation(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Finalizar",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                color = MaterialTheme.colorScheme.onSurface,
                text = "Finalizar Pré-Medição",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}


@Preview()
@Composable
fun PrevPMP() {
    // Criando um contexto fake para a preview
//    val fakeContext = LocalContext.current
//    val value =
//        Contract(
//            preMeasurementId = 1,
//            contractor = "Prefeitura Municipal de Belo Horizonte",
//            contractFile = "arquivo.pdf",
//            createdBy = "Gabriela",
//            createdAt = Instant.parse("2025-03-20T20:00:50.765Z").toString(),
//            status = ""
//        )
//
//    val streets =
//        listOf(
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//            PreMeasurementStreet(
//                preMeasurementStreetId = 1,
//                preMeasurementId = 1,
//                lastPower = "",
//                latitude = 1.9,
//                longitude = 2.2,
//                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
//                number = "",
//                neighborhood = "",
//                city = "",
//                state = "",
//                deviceId = ""
//            ),
//        )
//
//
//    PMPContent(
//        contract = value,
//        onNavigateToHome = { },
//        onNavigateToMenu = { },
//        onNavigateToPreMeasurements = { },
//        onNavigateToStreet = { },
//        navController = rememberNavController(),
//        "12",
//        streets,
//        { }
//    )


}

