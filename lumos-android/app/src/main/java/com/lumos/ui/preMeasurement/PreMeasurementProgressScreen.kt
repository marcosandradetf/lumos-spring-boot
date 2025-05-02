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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.Contract
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.PreMeasurementViewModel
import java.time.Instant

@Composable
fun PreMeasurementProgressScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPreMeasurements: () -> Unit,
    onNavigateToStreet: (Long) -> Unit,
    context: Context,
    contractViewModel: ContractViewModel,
    preMeasurementViewModel: PreMeasurementViewModel,
    navController: NavHostController,
    notificationsBadge: String,
    contractId: Long,

    ) {
    var contract by remember { mutableStateOf<Contract?>(null) }
    val streets by preMeasurementViewModel.streets

    LaunchedEffect(contractId) {
        contract = contractViewModel.getContract(contractId)
        preMeasurementViewModel.loadStreets(contractId)
    }

    if (contract != null)
        PMPContent(
            contract = contract!!,
            onNavigateToHome = onNavigateToHome,
            onNavigateToMenu = onNavigateToMenu,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToPreMeasurements = onNavigateToPreMeasurements,
            onNavigateToStreet = {
                onNavigateToStreet(it)
            },
            context = context,
            navController = navController,
            notificationsBadge = notificationsBadge,
            streets = streets,
            sendPreMeasurement = {
                if (streets.isNotEmpty()) {
                    Toast
                        .makeText(
                            context,
                            "Pré-medição enviada com sucesso!",
                            Toast.LENGTH_SHORT
                        )
                        .show()
//                    preMeasurementViewModel.
                    onNavigateToHome()
                    preMeasurementViewModel.sendPreMeasurementSync(contractId)
                } else {
                    Toast
                        .makeText(
                            context,
                            "Não é permitido enviar sem adicionar pelo menos uma rua!",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }

            }
        )
    else
        Loading(
            context,
            navController,
            notificationsBadge
        )
}

@Composable
fun Loading(
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
) {
    AppLayout(
        title = "Pré-medição",
        pSelected = BottomBar.MENU.value,
        navController = navController,
        context = context,
        notificationsBadge = notificationsBadge
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                Image(
//                    bitmap = ImageBitmap.imageResource(id = R.mipmap.ic_lumos),
//                    contentDescription = null
//                )
//                Spacer(modifier = Modifier.height(16.dp)) // Dá um espaço entre os elementos
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp), // Use size() ao invés de width()
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}


@Composable
fun PMPContent(
    contract: Contract,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPreMeasurements: () -> Unit,
    onNavigateToStreet: (Long) -> Unit,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    streets: List<PreMeasurementStreet>,
    sendPreMeasurement: () -> Unit
) {
    AppLayout(
        title = "Pré-medição",
        pSelected = BottomBar.MENU.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        navigateBack = onNavigateToPreMeasurements,
        context = context,
        notificationsBadge = notificationsBadge
    ) {
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
                                containerColor = MaterialTheme.colorScheme.onSecondary,
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
                                        text = contract.contractor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally, // Centraliza os itens horizontalmente
                                        verticalArrangement = Arrangement.Center // Mantém o alinhamento vertical
                                    ) {
                                        Text(
                                            text = "Contrato",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 2.dp) // Pequeno espaço entre o texto e o ícone
                                        )

                                        Icon(
                                            imageVector = Icons.Default.Downloading,
                                            contentDescription = "Baixar Contrato",
                                            tint = Color(0xFF007AFF),
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
                                onNavigateToStreet(contract.contractId)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary) // Azul
                        ) {
                            Text(
                                text = "Adicionar rua",
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Adicionar",
                                tint = MaterialTheme.colorScheme.onSecondary
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
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary
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
                                text = street.street,
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
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer) // Azul
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
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                color = MaterialTheme.colorScheme.secondary,
                text = "Finalizar Pré-Medição",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}


@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Light Mode", showBackground = true)
@Composable
fun PrevPMP() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val value =
        Contract(
            contractId = 1,
            contractor = "Prefeitura Municipal de Belo Horizonte",
            contractFile = "arquivo.pdf",
            createdBy = "Gabriela",
            createdAt = Instant.parse("2025-03-20T20:00:50.765Z").toString(),
            status = ""
        )

    val streets =
        listOf(
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
            PreMeasurementStreet(
                preMeasurementStreetId = 1,
                contractId = 1,
                lastPower = "",
                latitude = 1.9,
                longitude = 2.2,
                street = "Rua D, 12 - Jardim tal, Belo Horizonte - MG",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                photoUri = ""
            ),
        )


    PMPContent(
        contract = value,
        onNavigateToHome = { },
        onNavigateToMenu = { },
        onNavigateToProfile = { },
        onNavigateToNotifications = { },
        onNavigateToPreMeasurements = { },
        onNavigateToStreet = { },
        context = fakeContext,
        navController = rememberNavController(),
        "12",
        streets,
        { }
    )

//    Loading(
//        fakeContext,
//        rememberNavController(),
//        "12"
//    )
}

