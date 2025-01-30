package com.lumos.ui.measurement

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Material
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.domain.service.SyncStock
import com.lumos.navigation.Routes
import com.lumos.ui.viewmodel.MeasurementViewModel
import com.lumos.ui.viewmodel.StockViewModel
import java.util.concurrent.TimeUnit

@Composable
fun MeasurementScreen(
    onNavigateToHome: () -> Unit,
    navController: NavHostController,
    context: Context,
    stockViewModel: StockViewModel,
    measurementViewModel: MeasurementViewModel
) {
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
    val coord = CoordinatesService(context, fusedLocationProvider)

    var initMeasurement by remember { mutableStateOf(false) }
    var exitMeasurement by remember { mutableStateOf(false) }

    var vLatitude by remember { mutableStateOf<Double?>(null) }
    var vLongitude by remember { mutableStateOf<Double?>(null) }
    var address by remember { mutableStateOf<String>("") }

    // Obtém o estado atual dos depósitos
    val deposits by stockViewModel.deposits
    val materials by stockViewModel.materials

    var selectedDeposit by remember { mutableStateOf<Deposit?>(null) }
    var showModal by remember { mutableStateOf(false) }

    // Execute a função assíncrona
    LaunchedEffect(Unit) {
        coord.execute { latitude, longitude ->
            if (latitude != null && longitude != null) {
                vLatitude = latitude
                vLongitude = longitude
                val addr = AddressService(context)
                address = addr.execute(latitude, longitude)?.get(0).toString()
            } else {
                Log.e("GET Address", "Latitude ou Longitude são nulos.")
            }
        }

        // Agendar o Worker assim que a tela for aberta
        val workRequest = OneTimeWorkRequestBuilder<SyncStock>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_stock", // Nome único para o trabalho
            ExistingWorkPolicy.REPLACE, // Pode substituir o trabalho se já estiver agendado
            workRequest
        )

        stockViewModel.loadDeposits()
    }



    if (vLatitude == null || vLongitude == null) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color(0xFFF5F5F7)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(32.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = "Carregando coordenadas...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            )
        }
    } else {
        Scaffold(
            containerColor = Color(0xFFF5F5F7),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Fechar",
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { exitMeasurement = true },
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
                    onClick = { navController.navigate(Routes.MEASUREMENT_SCREEN) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(8.dp) // Botão com cantos menos arredondados
                ) {
                    Text(
                        text = "Salvar",
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

                    if (exitMeasurement) {
                        DialogExit(
                            onDismissRequest = {
                                exitMeasurement = false
                            }, // Fecha o diálogo ao cancelar
                            onConfirmation = {
                                onNavigateToHome()
                            }, // Ação ao confirmar
                            dialogTitle = "Confirmação de saída", // Título do diálogo
                            dialogText = "Você tem certeza que deseja cancelar a pré-medição atual?", // Texto do diálogo
                            icon = Icons.Filled.Warning // Ícone do Material Design
                        )
                    }

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

                        OutlinedTextField(
                            textStyle = TextStyle(Color.Black),
                            value = address,
                            onValueChange = { address = it },
                            label = {
                                Text(
                                    text = "Endereço:",
                                    color = Color.Black
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Exibe um indicador de carregamento se os depósitos ainda não foram carregados
                        if (deposits.isEmpty()) {
                            OutlinedTextField(
                                textStyle = TextStyle(Color.Black),
                                value = "Nenhum Almoxarifado Encontrado",
                                onValueChange = { },
                                label = {
                                    Text(
                                        text = "Almoxarifado:",
                                        color = Color.Black
                                    )
                                },
                                readOnly = true,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 15.dp),
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.ArrowDropDown, "contentDescription",
                                    )
                                }
                            )
                        } else {
                            // Exibe o Dropdown com os depósitos
                            Column {

                                var expanded by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.fillMaxWidth()) {

                                    OutlinedTextField(
                                        textStyle = TextStyle(Color.Black),
                                        value = selectedDeposit?.depositName
                                            ?: "Escolha um almoxarifado",
                                        onValueChange = { },
                                        label = {
                                            Text(
                                                text = "Almoxarifado:",
                                                color = Color.Black
                                            )
                                        },
                                        readOnly = true,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 15.dp),
                                        trailingIcon = {
                                            Icon(Icons.Filled.ArrowDropDown, "contentDescription",
                                                Modifier.clickable { expanded = true })
                                        }
                                    )

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth(),

                                        ) {
                                        deposits.forEach { deposit ->
                                            DropdownMenuItem(
                                                onClick = {
                                                    selectedDeposit = deposit
                                                    expanded = false
                                                    stockViewModel.loadMaterials(deposit.depositId)
                                                },
                                                text = { Text(text = deposit.depositName) }, // Nome do depósito
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp), // Modificador para o item
                                                leadingIcon = {
                                                    // Se desejar um ícone à esquerda, pode ser adicionado aqui
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Ícone do Depósito"
                                                    )
                                                },
                                                trailingIcon = {
                                                    // Se desejar um ícone à direita, pode ser adicionado aqui
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowForward,
                                                        contentDescription = "Seta"
                                                    )
                                                },
                                                enabled = true, // Se você deseja desabilitar o item, altere para 'false'
                                                colors = MenuItemColors(
                                                    textColor = Color.Black,
                                                    leadingIconColor = Color.Black,
                                                    trailingIconColor = Color.Black,
                                                    disabledTextColor = Color.Black,
                                                    disabledLeadingIconColor = Color.Gray,
                                                    disabledTrailingIconColor = Color.White,
                                                ),
                                                contentPadding = PaddingValues(
                                                    horizontal = 16.dp,
                                                    vertical = 12.dp
                                                ) // Espaçamento do conteúdo dentro do item
                                            )
                                        }

                                        if (showModal) {
                                            ItemSelectionModal(
                                                onDismiss = { showModal = false },
                                                materials,
                                                onConfirm = { item, quantity ->
                                                    // Lógica para confirmar a seleção do item e quantidade
                                                    println("Item selecionado: $item, Quantidade: $quantity")
                                                    showModal =
                                                        false // Fecha o modal após a confirmação
                                                }
                                            )
                                        }


                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(250.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Card redondo para o botão
                            Card(
                                modifier = Modifier
                                    .size(50.dp) // Tamanho maior para um botão redondo
                                    .clickable { showModal = true }, // Adiciona interação de clique
                                shape = CircleShape, // Formato circular
                                elevation = CardDefaults.cardElevation(10.dp), // Sombra
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2E8146) // Cor de fundo do botão
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Adicionar",
                                        tint = Color.White, // Cor do ícone
                                        modifier = Modifier.size(30.dp) // Tamanho do ícone
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp)) // Espaço entre o botão e o texto
                            Text(
                                text = "Adicionar Item",
                                color = Color(0xFF1E1F22), // Cor do texto
                                fontSize = 16.sp, // Tamanho da fonte
                                fontWeight = FontWeight.Medium // Peso da fonte
                            )
                        }


                    }

                }
            }


        )
    }


}

@Composable
fun DialogExit(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Example Icon")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ItemSelectionModal(
    onDismiss: () -> Unit, // Função para fechar o modal
    materials: List<Material>,
    onConfirm: (String, Int) -> Unit // Função para confirmar a seleção
) {
    var selectedItem by remember { mutableStateOf("") } // Item selecionado
    var quantity by remember { mutableStateOf(1) } // Quantidade

    Dialog(
        onDismissRequest = {},
        DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(10F),
            contentAlignment = Alignment.Center
        ) {
            Dialog(onDismissRequest = onDismiss) {
                Column {
                    materials.forEach { m ->
                        ListItem(
                            leadingContent = {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "Localized description",
                                )
                            },
                            overlineContent = {
                                Text("Estoque: ${m.stockQt ?: ""} - ${m.requestUnit ?: ""}")
                            },
                            headlineContent = {
                                Text(m.materialName ?: "")
                            },
                            supportingContent = {
                                if (m.materialPower != null) {
                                    Text("Potência: ${m.materialPower}")
                                } else if (m.materialLength != null) {
                                    Text("Tamanho ${m.materialLength}")
                                } else if (m.materialAmps != null) {
                                    Text("Corrente ${m.materialAmps}")
                                }

                            },
                            trailingContent = {
                                Icon(Icons.Filled.MoreVert, "")
                            },

                            )
                        HorizontalDivider()
                    }
                }
            }
        }
    }


}


//@Preview(showBackground = true)
//@Composable
//fun PrevMeasurementScreen() {
//    ItemSelectionModal({})
//}
