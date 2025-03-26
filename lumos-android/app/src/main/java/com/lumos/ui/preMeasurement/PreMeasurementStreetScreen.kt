package com.lumos.ui.preMeasurement

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.Material
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.domain.service.SyncStock
import com.lumos.ui.viewmodel.StockViewModel
import java.util.concurrent.TimeUnit

@SuppressLint("HardwareIds")
@Composable
fun PreMeasurementStreetScreen(
    onNavigateToHome: () -> Unit,
    navController: NavHostController,
    context: Context,
    stockViewModel: StockViewModel,
    preMeasurementViewModel: PreMeasurementViewModel
) {
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
    val coord = CoordinatesService(context, fusedLocationProvider)

    var finishMeasurement by remember { mutableStateOf(false) }
    var exitMeasurement by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    var vLatitude by remember { mutableStateOf<Double?>(null) }
    var vLongitude by remember { mutableStateOf<Double?>(null) }
    var address by remember { mutableStateOf<String>("") }
    var street by remember { mutableStateOf<String>("") }
    var number by remember { mutableStateOf<String>("") }
    var neighborhood by remember { mutableStateOf<String>("") }
    var city by remember { mutableStateOf<String>("") }
    var lastPower by remember { mutableStateOf<String>("") }
    var state by remember { mutableStateOf<String>("") }

    // Obtém o estado atual dos depósitos
    val materials by stockViewModel.materials
    var showModal by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val preMeasurementStreet by remember {
        mutableStateOf<PreMeasurementStreet>(
            PreMeasurementStreet(
                preMeasurementId = 0,
                lastPower = "",
                latitude = 0.0,
                longitude = 0.0,
                address = "",
                number = "",
                city = "",
                deviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ),
            )
        )
    }
    var preMeasurementStreetItems by remember { mutableStateOf<List<PreMeasurementStreetItem>>(emptyList()) }

    // Execute a função assíncrona
    LaunchedEffect(Unit) {
        coord.execute { latitude, longitude ->
            if (latitude != null && longitude != null) {
                vLatitude = latitude
                vLongitude = longitude
                val addr = AddressService(context).execute(latitude, longitude)

                street = addr?.get(0).toString()
                neighborhood = addr?.get(1).toString()
                city = addr?.get(2).toString()
                state = addr?.get(3).toString()

                preMeasurementStreet.address = "$street, $number - $neighborhood, $city - $state"
                preMeasurementStreet.city = city
                preMeasurementStreet.latitude = vLatitude ?: 0.0
                preMeasurementStreet.longitude = vLongitude ?: 0.0

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

        stockViewModel.loadMaterials()

        val workManager = WorkManager.getInstance(context)
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(lifecycleOwner) { workInfo ->
            if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                // Somente chama loadMaterials() quando o Worker terminar com sucesso
                stockViewModel.loadMaterials()
            }
        }
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
                        .padding(top = 20.dp),
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
                    onClick = {
                        if (preMeasurementStreetItems.isEmpty()) {
                            Toast
                                .makeText(
                                    context,
                                    "Adicione os itens",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        } else if (!validFields(street, number, neighborhood, city)) {
                            Toast
                                .makeText(
                                    context,
                                    "Todos os campos são obrigatórios",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        } else {
                            preMeasurementViewModel.saveStreetOffline(preMeasurementStreet) { measurementId ->
                                if (measurementId != null) {
                                    try {
                                        preMeasurementViewModel.saveItemsOffline(preMeasurementStreetItems, measurementId)
                                        finishMeasurement = true
                                    } catch (e: Exception) {
                                        finishMeasurement = false
                                        Log.e(
                                            "SaveError",
                                            "Erro ao salvar itens offline: ${e.message}"
                                        )
                                    }

                                }
                            }

                        }
                    },
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
                        .background(Color(0xFFF5F5F5)) // Fundo cinza claro
                        .pointerInput(Unit) {
                            detectTapGestures {
                                // Fechar o teclado ao tocar em qualquer lugar da tela
                                keyboardController?.hide()
                            }
                        }
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
                        Modifier.fillMaxWidth(),
                    ) {

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically // Certificando que os itens dentro da Row ficam alinhados verticalmente
                        ) {
                            Column {
                                Text(
                                    text = "Pré-Medição",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00308F) // Azul escuro
                                    )
                                )
                                Text(
                                    text = "$vLatitude, $vLongitude",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00308F) // Azul escuro
                                    )
                                )
                                Text(
                                    text = city,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00308F) // Azul escuro
                                    )
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally // Alinha o conteúdo da coluna no centro
                            ) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Color.Red,
                                            contentColor = Color.White
                                        ) {
                                            Text(preMeasurementStreetItems.size.toString()) // Evitei o uso de `items.size.toString() ?: ""`, já que `items.size.toString()` é sempre seguro
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lightbulb,
                                        contentDescription = "Shopping cart",
                                    )
                                }

                                Text(
                                    text = "Itens adicionados",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    color = Color.Black,
                                )
                            }
                        }


                        Spacer(modifier = Modifier.height(24.dp))

                        Row {
                            OutlinedTextField(
                                textStyle = TextStyle(Color.Black),
                                value = street,
                                onValueChange = {
                                    street = it
                                    preMeasurementStreet.address =
                                        "$street, $number - $neighborhood, $city - $state"
                                },
                                label = {
                                    Text(
                                        text = "Rua:",
                                        color = Color.Black
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth(0.80f)
                                    .padding(end = 10.dp)
                            )

                            OutlinedTextField(
                                textStyle = TextStyle(Color.Black),
                                value = number,
                                onValueChange = {
                                    number = it
                                    preMeasurementStreet.address =
                                        "$street, $number - $neighborhood, $city - $state"
                                },
                                label = {
                                    Text(
                                        text = "№:",
                                        color = Color.Black
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                                isError = number.isEmpty()
                            )
                        }

                        Row {
                            OutlinedTextField(
                                textStyle = TextStyle(Color.Black),
                                value = neighborhood,
                                onValueChange = {
                                    neighborhood = it
                                    preMeasurementStreet.address =
                                        "$street, $number - $neighborhood, $city - $state"
                                },
                                label = {
                                    Text(
                                        text = "Bairro:",
                                        color = Color.Black
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .padding(end = 10.dp)

                            )

                            OutlinedTextField(
                                textStyle = TextStyle(Color.Black),
                                value = city,
                                onValueChange = {
                                    city = it
                                    preMeasurementStreet.city = it
                                    preMeasurementStreet.address =
                                        "$street, $number - $neighborhood, $it - $state"
                                },
                                label = {
                                    Text(
                                        text = "Cidade:",
                                        color = Color.Black
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row {
                            preMeasurementStreet.lastPower?.isEmpty()?.let { empty ->
                                OutlinedTextField(
                                    textStyle = TextStyle(Color.Black),
                                    value = lastPower,
                                    onValueChange = {
                                        lastPower = if (it.endsWith("W")) it else it + "W"
                                        preMeasurementStreet.lastPower = lastPower
                                    },
                                    label = {
                                        Text(
                                            text = "Potência anterior:",
                                            color = Color.Black
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = empty
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(200.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Card redondo para o botão
                            Card(
                                modifier = Modifier
                                    .size(50.dp) // Tamanho maior para um botão redondo
                                    .clickable {
                                        preMeasurementStreetItems = emptyList()
                                        showModal = true
                                    }, // Adiciona interação de clique
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
                                text = "Adicionar Itens",
                                color = Color(0xFF1E1F22), // Cor do texto
                                fontSize = 16.sp, // Tamanho da fonte
                                fontWeight = FontWeight.Medium // Peso da fonte
                            )
                        }


                    }

                }

                // Abrir o BottomSheetDialog quando isDialogOpen for true
                if (showModal) {
                    BottomSheetDialog(
                        onDismissRequest = { selected ->
                            preMeasurementStreetItems = selected
                            showModal = false
                        },
                        materials = materials,
                        preMeasurementStreetId = preMeasurementStreet.preMeasurementStreetId
                    )
                }

                if (finishMeasurement) {
                    preMeasurementStreetItems = emptyList()
                    Toast
                        .makeText(
                            context,
                            "Medição salva com sucesso!",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                    finishMeasurement = false
                }

            }


        )
    }


}

fun validFields(street: String, number: String, neighborhood: String, city: String): Boolean {
    return !(street.isEmpty() || number.isEmpty() || neighborhood.isEmpty() || city.isEmpty())
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialog(
    onDismissRequest: (List<PreMeasurementStreetItem>) -> Unit,
    materials: List<Material>,
    preMeasurementStreetId: Long
) {
    var searchQuery by remember { mutableStateOf("") }

    val preMeasurementStreetItems = remember { mutableStateListOf<PreMeasurementStreetItem>() }
    val filteredList = materials.filter {
        it.materialName?.contains(searchQuery, ignoreCase = true) ?: false ||
                it.materialPower?.contains(searchQuery, ignoreCase = true) ?: false
    }

    MaterialTheme(
        colorScheme = lightColorScheme() // Força o modo claro dentro do BottomSheet
    ) {
        ModalBottomSheet(
            onDismissRequest = { onDismissRequest(preMeasurementStreetItems) },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Selecione os itens:", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Spacer(modifier = Modifier.height(8.dp))

                // Barra de pesquisa
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Pesquiar"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(filteredList) { material ->
                        ItemIluminacaoRow(
                            material = material,
                            onItemSelected = { m ->

                                if (preMeasurementStreetItems.find { item -> item.materialId == m.materialId } == null) {
                                    preMeasurementStreetItems.add(
                                        PreMeasurementStreetItem(
                                            preMeasurementStreetId = preMeasurementStreetId,
                                            materialId = m.materialId,
                                            materialQuantity = 0,
                                        )
                                    )
                                } else {
                                    preMeasurementStreetItems.removeAll { item -> item.materialId == m.materialId }
                                }
                            },
                            onQuantidadeChange = { materialId, quantity ->
                                preMeasurementStreetItems.replaceAll {
                                    if (it.materialId == materialId) it.copy(
                                        materialQuantity = quantity
                                    ) else it
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemIluminacaoRow(
    material: Material,
    onItemSelected: (Material) -> Unit,
    onQuantidadeChange: (Long, Int) -> Unit
) {

    var selected by rememberSaveable { mutableStateOf(false) }
    var quantity by rememberSaveable { mutableIntStateOf(0) }
    val materialChar = if (material.materialLength != null) {
        "Tamanho: ${material.materialLength}"
    } else if (material.materialAmps != null) {
        "Corrente: ${material.materialAmps}"
    } else if (material.materialPower != null) {
        "Potência: ${material.materialPower}"
    } else {
        ""
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF6BA3F2) else Color(0xFFFFFFFF),
        animationSpec = tween(durationMillis = 300)
    )


    if (!selected) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    selected = !selected
                    onItemSelected(material)
                },
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardColors(
                contentColor = Color.White,
                containerColor = backgroundColor,
                disabledContainerColor = Color.White,
                disabledContentColor = Color.White
            )
        ) {
            Row(
                modifier = if (materialChar.isNotEmpty()) Modifier.padding(16.dp)
                else Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.materialName ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (selected) Color.White else Color.Black,
                    )
                    if (materialChar.isNotEmpty()) {
                        Text(
                            text = materialChar,
                            fontSize = 14.sp,
                            color = if (selected) Color.White else Color.Black
                        )
                    }
                    // Só mostra o controle de quantidade se o item estiver selecionado

                }
            }

        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    selected = !selected
                    onItemSelected(material)
                },

            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardColors(
                contentColor = Color.White,
                containerColor = backgroundColor,
                disabledContainerColor = Color.White,
                disabledContentColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally, // Centraliza tudo dentro do Card
                verticalArrangement = Arrangement.Center // Centraliza verticalmente
            ) {
                // Texto para o nome do material ou outros detalhes, se necessário
                Text(
                    text = "${material.materialName ?: ""} -  $materialChar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp)) // Espaçamento entre os itens

                // Linha com os botões de aumentar e diminuir a quantidade
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center // Centraliza os itens
                ) {
                    IconButton(
                        onClick = {
                            quantity -= 1
                            if (quantity > 0) onQuantidadeChange(
                                material.materialId,
                                quantity
                            )
                        },
                        modifier = Modifier
                            .background(Color(0xFFE0E0E0), shape = CircleShape)
                            .padding(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Diminuir",
                            tint = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp)) // Espaçamento entre os ícones

                    Text(
                        text = quantity.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            quantity += 1
                            onQuantidadeChange(material.materialId, quantity)
                        },
                        modifier = Modifier
                            .background(Color(0xFFE0E0E0), shape = CircleShape)
                            .padding(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Aumentar",
                            tint = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Quantidade",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

}

