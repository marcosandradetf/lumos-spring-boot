package com.lumos.ui.preMeasurement

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.data.repository.Status
import com.lumos.domain.model.Contract
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.utils.ConnectivityUtils
import com.lumos.utils.Utils
import java.time.Instant

@SuppressLint("HardwareIds")
@Composable
fun ContractsScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPreMeasurement: (Long) -> Unit,
    context: Context,
    contractViewModel: ContractViewModel,
    connection: ConnectivityUtils,
    navController: NavHostController,
    notificationsBadge: String,
    ) {
    val contracts by contractViewModel.contracts.collectAsState()
    var internet by remember { mutableStateOf(true) }
    val deviceId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    LaunchedEffect(Unit) {
        if (connection.isConnectedToInternet(context)) contractViewModel.syncContracts()
        else internet = false

        contractViewModel.loadFlowContracts(Status.PENDING)
    }

    ContractsScreenContent(
        contracts = contracts,
        onNavigateToHome = onNavigateToHome,
        onNavigateToMenu = onNavigateToMenu,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToNotifications = onNavigateToNotifications,
        context = context,
        navController = navController,
        notificationsBadge = notificationsBadge,
        start = {
            contractViewModel.startPreMeasurement(it, deviceId)
            onNavigateToPreMeasurement(it)
        },
        download = {
            contractViewModel.downloadContract(it)
            Toast.makeText(context, "Recurso de download não implementado", Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
fun ContractsScreenContent(
    contracts: List<Contract>,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    start: (Long) -> Unit,
    download: (Long) -> Unit
) {
    AppLayout(
        title = "Contratos",
        pSelected = BottomBar.MENU.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        navigateBack = onNavigateToMenu,
        context = context,
        notificationsBadge = notificationsBadge
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp) // Espaço entre os cards
        ) {
            items(contracts) { contract -> // Iteração na lista
                val createdAt = "Criado por ${contract.createdBy} há ${
                    Utils.timeSinceCreation(
                        Instant.parse(contract.createdAt)
                    )
                }"
                val expand = remember { mutableStateOf(false) }
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expand.value = !expand.value },
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
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Expandir",
                                    tint = MaterialTheme.colorScheme.primary
                                )

                            }

                            // Informação extra
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTimeFilled,
                                    contentDescription = "Horário",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp) // Ajuste do tamanho do ícone
                                )
                                Text(
                                    modifier = Modifier.padding(start = 5.dp),
                                    text = createdAt,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        AnimatedVisibility(visible = expand.value) {
                            // Linha inferior (Contrato + Ações)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { }
                                    .padding(top = 25.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally, // Centraliza os itens horizontalmente
                                    verticalArrangement = Arrangement.Center, // Mantém o alinhamento vertical
                                    modifier = Modifier.clickable {
                                        download(contract.contractId)
                                    }
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


                                TextButton(onClick = {
                                    start(contract.contractId)
                                }) {
                                    Text(
                                        text = "Iniciar Pré-Medição",
                                        color = Color(0xFFFF2F55),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


    }
}


@Preview(showBackground = true)
@Composable
fun PrevContract() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val values =
        listOf(
            Contract(
                contractId = 1,
                contractor = "Prefeitura Municipal de Belo Horizonte",
                contractFile = "arquivo.pdf",
                createdBy = "Gabriela",
                createdAt = Instant.parse("2025-03-20T20:00:50.765Z").toString(),
                status = ""
            ),
            Contract(
                contractId = 1,
                contractor = "Prefeitura Municipal de Ibirité",
                contractFile = "arquivo.pdf",
                createdBy = "Renato",
                createdAt = Instant.parse("2025-03-19T23:29:50.765Z").toString(),
                status = ""
            ),
            Contract(
                contractId = 1,
                contractor = "Prefeitura Municipal de Poté",
                contractFile = "arquivo.pdf",
                createdBy = "Daniela",
                createdAt = Instant.parse("2025-03-18T23:29:50.765Z").toString(),
                status = ""
            )
        )


    ContractsScreenContent(
        contracts = values,
        onNavigateToHome = { },
        onNavigateToMenu = { },
        onNavigateToProfile = { },
        onNavigateToNotifications = { },
        context = fakeContext,
        navController = rememberNavController(),
        "12",
        start = {},
        download = {}
    )
}

