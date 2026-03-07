package com.lumos.ui.installationholder

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NothingData
import com.lumos.utils.Utils
import com.lumos.utils.Utils.abbreviate
import com.lumos.viewmodel.DirectExecutionViewModel
import kotlinx.coroutines.launch

@Composable
fun NewInstallationScreen(
    navController: NavHostController,
    viewModel: DirectExecutionViewModel,
    coordinatesService: CoordinatesService,
    addressService: AddressService
) {
    val scope = rememberCoroutineScope()
    val contracts by viewModel.contracts.collectAsState(emptyList())

    var description by rememberSaveable { mutableStateOf("") }
    val localId = viewModel.installationId
    val loading = viewModel.isLoading
    val error = viewModel.errorMessage

    // Mantém sua lógica de sincronização inicial
    LaunchedEffect(Unit) {
        viewModel.syncContracts()

        val (lat, long) = coordinatesService.execute()
        if (lat != null && long != null) {
            val addr = addressService.execute(lat, long)
            if (addr != null && addr.size >= 4) {
                description = "INSTALAÇÃO EM ${addr[2].uppercase()} - SEM CONTRATO"
            }
        }
    }

    LaunchedEffect(localId) {
        if (localId != null) {
            val contractorEncoded = Uri.encode(viewModel.contractor)
            val creationDateEncoded = Uri.encode(viewModel.creationDate)

            val contractParam = viewModel.contractId?.let { "contractId=$it" }
            val instructionsParam = viewModel.instructions?.let { "instructions=${Uri.encode(it)}" }

            val query = listOfNotNull(contractParam, instructionsParam)
                .joinToString("&")
                .let { if (it.isNotEmpty()) "?$it" else "" }

            navController.getBackStackEntry(Routes.DIRECT_EXECUTION_FLOW)
                .savedStateHandle["route_event"] =
                Routes.DIRECT_EXECUTION_HOME_SCREEN

            navController.navigate(
                "${Routes.DIRECT_EXECUTION_HOME_SCREEN}/$localId/$contractorEncoded/$creationDateEncoded$query"
            )
        }
    }

    AppLayout(
        title = "Nova Instalação",
        selectedIcon = BottomBar.MAINTENANCE.value,
        navigateBack = {
            navController.popBackStack()
        },
        navigateToHome = {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.DIRECT_EXECUTION_FLOW) { inclusive = true }
            }
        },
        navigateToMore = {
            navController.navigate(Routes.MORE) {
                popUpTo(Routes.DIRECT_EXECUTION_FLOW) { inclusive = true }
            }
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK) {
                popUpTo(Routes.DIRECT_EXECUTION_FLOW) { inclusive = true }
            }
        },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE) {
                popUpTo(Routes.DIRECT_EXECUTION_FLOW) { inclusive = true }
            }
        },
        navigateToExecutions = {
            navController.navigate(Routes.INSTALLATION_HOLDER) {
                popUpTo(Routes.DIRECT_EXECUTION_FLOW) { inclusive = true }
            }
        }
    ) { _, _ ->

        Box(modifier = Modifier.fillMaxSize()) {
            if (loading) {
                // Centraliza o loading na tela
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Loading("Carregando contratos...")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 130.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Alerta de Internet estilizado como um Card de aviso
                    if (!error.isNullOrBlank()) {
                        item {
                            Surface(
                                onClick = {
                                    scope.launch {
                                        viewModel.syncContracts()
                                    }
                                },
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.WifiOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    if (contracts.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(16.dp))
                                NothingData("Nenhum contrato encontrado")
                                TextButton(onClick = {
                                    scope.launch {
                                        viewModel.syncContracts()
                                    }
                                }) {
                                    Text("Tentar atualizar agora")
                                }
                            }
                        }
                    } else {
                        // Cabeçalho da Lista
                        item {
                            Text(
                                text = "Selecione o contrato para continuar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }

                        // Lista de Contratos
                        items(
                            contracts,
                            key = { it.contractId }
                        ) { contract ->
                            ContractCard(
                                contractorName = abbreviate(contract.contractor),
                                onClick = {
                                    val externalId = System.currentTimeMillis() * 10_000 + (0..9999).random()
                                    val localId = -externalId
                                    viewModel.insertInstallation(
                                        DirectExecution(
                                            directExecutionId = localId,
                                            contractId = contract.contractId,
                                            description = contract.contractor,
                                            instructions = null,
                                            executionStatus = "IN_PROGRESS",
                                            type = "INSTALLATION",
                                            itemsQuantity = 0,
                                            creationDate = Utils.dateTime.toString(),
                                            responsible = null,
                                            signPath = null,
                                            signDate = null,
                                            executorsIds = null
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                                ),
                                startY = 0f
                            )
                        )
                        .padding(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            Text(
                                text = "A Instalação ainda não possui contrato?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 🔹 INPUT DE DESCRIÇÃO
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Descrição da Instalação") },
                                placeholder = { Text("Ex: Instalação padrão residencial") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                maxLines = 3
                            )

                            OutlinedButton(
                                onClick = {
                                    if (description.isNotBlank()) {
                                        val localId = -(System.currentTimeMillis())
                                        viewModel.insertInstallation(
                                            DirectExecution(
                                                directExecutionId = localId,
                                                contractId = null,
                                                description = description,
                                                instructions = null,
                                                executionStatus = "IN_PROGRESS",
                                                type = "INSTALLATION",
                                                itemsQuantity = 0,
                                                creationDate = Utils.dateTime.toString(),
                                                responsible = null,
                                                signPath = null,
                                                signDate = null,
                                                executorsIds = null
                                            )
                                        )

                                        description = "" // limpa após criar
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                enabled = description.isNotBlank() // desabilita se vazio
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Nova Instalação sem Contrato",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractCard(
    contractorName: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = contractorName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = "Toque para iniciar a instalação",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent // O Card já provê a cor
            )
        )
    }
}

//
//@Preview
//@Composable
//fun PrevNewInstallation() {
//    NewInstallationScreen(
//        navController = rememberNavController(),
//
//        loading = false,
//        resync = { },
//        error = null,
//    )
//}