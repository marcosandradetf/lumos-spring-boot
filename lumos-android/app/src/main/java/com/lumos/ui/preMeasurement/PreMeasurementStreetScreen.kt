package com.lumos.ui.preMeasurement

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.PhotoCamera
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.lumos.data.api.UserExperience
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Material
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.domain.service.SyncStock
import com.lumos.ui.components.NetworkStatusBar
import com.lumos.ui.components.TopBar
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.StockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun PreMeasurementStreetScreen(
    back: (Long) -> Unit,
    context: Context,
    stockViewModel: StockViewModel,
    preMeasurementViewModel: PreMeasurementViewModel,
    contractId: Long,
    contractViewModel: ContractViewModel,
) {
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
    val coord = CoordinatesService(context, fusedLocationProvider)
    val lifecycleOwner = LocalLifecycleOwner.current

    var vLatitude by remember { mutableStateOf<Double?>(null) }
    var vLongitude by remember { mutableStateOf<Double?>(null) }
    var showModal by remember { mutableStateOf(false) }
    var finishMeasurement by remember { mutableStateOf(false) }
    var contract by remember { mutableStateOf<Contract?>(null) }

    // Obtém o estado atual dos depósitos
    val materials by stockViewModel.materials.collectAsState()

    val preMeasurementStreet by remember {
        mutableStateOf(
            PreMeasurementStreet(
                contractId = contractId,
                lastPower = "",
                latitude = 0.0,
                longitude = 0.0,
                street = "",
                neighborhood = "",
                number = "",
                city = "",
                state = "",
                photoUri = ""
            )
        )
    }

    var preMeasurementStreetItems by remember {
        mutableStateOf<List<PreMeasurementStreetItem>>(
            emptyList()
        )
    }

    LaunchedEffect(contractId) {
        contract = contractViewModel.getContract(contractId)
        preMeasurementViewModel.loadStreets(contractId)

        contract?.let { loadedContract ->
            val powersList = loadedContract.powers
                ?.split("#")?.map { it.trim() } ?: emptyList()
            val lengthsList = loadedContract.lengths
                ?.split("#")?.map { it.trim() } ?: emptyList()

            stockViewModel.loadMaterialsOfContract(powersList, lengthsList)

            Log.e("DEBUG", "contract.powers: ${loadedContract.powers}")
            Log.e("powersList", powersList.toString())
            Log.e("lengthsList", lengthsList.toString())

            // Agendar o Worker assim que a tela for aberta
            val workRequest = OneTimeWorkRequestBuilder<SyncStock>()
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.MINUTES
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_stock", // Nome único para o trabalho
                ExistingWorkPolicy.REPLACE, // Pode substituir o trabalho se já estiver agendado
                workRequest
            )

            withContext(Dispatchers.Main) {
                val workManager = WorkManager.getInstance(context)
                workManager.getWorkInfoByIdLiveData(workRequest.id)
                    .observe(lifecycleOwner) { workInfo ->
                        if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                            Log.e("syncStock screen street", "carregando materiais pos-worker")
                            stockViewModel.loadMaterialsOfContract(powersList, lengthsList)
                        }
                    }
            }
        }
    }

    // Execute a função assíncrona
    LaunchedEffect(Unit) {
        coord.execute { latitude, longitude ->
            if (latitude != null && longitude != null) {
                vLatitude = latitude
                vLongitude = longitude
                val addr = AddressService(context).execute(latitude, longitude)

                val street = addr?.get(0).toString()
                val neighborhood = addr?.get(1).toString()
                val city = addr?.get(2).toString()
                val state = addr?.get(3).toString()

                preMeasurementStreet.street = street
                preMeasurementStreet.neighborhood = neighborhood
                preMeasurementStreet.city = city
                preMeasurementStreet.state = state
                preMeasurementStreet.latitude = vLatitude ?: 0.0
                preMeasurementStreet.longitude = vLongitude ?: 0.0

            } else {
                Log.e("GET Address", "Latitude ou Longitude são nulos.")
            }
        }
    }





    if (vLatitude == null || vLongitude == null) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background),
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
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    } else {
        PMSContent(
            context = context,
            preMeasurementStreetItems = preMeasurementStreetItems,
            saveStreet = {
                preMeasurementViewModel.saveStreetOffline(
                    preMeasurementStreet,
                    callback = { preMeasurementStreetId ->
                        if (preMeasurementStreetId !== null)
                            try {
                                preMeasurementViewModel.saveItemsOffline(
                                    preMeasurementStreetItems,
                                    preMeasurementStreetId
                                )
                                finishMeasurement = true
                            } catch (e: Exception) {
                                Log.e("SaveError", "Erro ao salvar itens offline: ${e.message}")
                            }
                    }
                )

            },
            back = {
                back(it)
            },
            onValueChange = { field, value ->
                when (field) {
                    "street" -> preMeasurementStreet.street = value
                    "number" -> preMeasurementStreet.number = value
                    "neighborhood" -> preMeasurementStreet.neighborhood = value
                    "city" -> preMeasurementStreet.city = value
                    "state" -> preMeasurementStreet.state = value
                    "lastPower" -> preMeasurementStreet.lastPower = value
                }

            },
            showModal = {
                showModal = true
            },
            pStreet = preMeasurementStreet,
            takePhoto = {
                preMeasurementStreet.photoUri = it.toString()
            },
        )
    }

    // Abrir o BottomSheetDialog quando isDialogOpen for true
    if (showModal) {
        BottomSheetDialog(
            onDismissRequest = { selected ->
                preMeasurementStreetItems = selected
                showModal = false
            },
            preList = preMeasurementStreetItems,
            materials = materials,
            preMeasurementStreetId = preMeasurementStreet.preMeasurementStreetId,
            contractId = contractId,
            context = context
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


@Composable
fun PMSContent(
    context: Context,
    preMeasurementStreetItems: List<PreMeasurementStreetItem>,
    saveStreet: () -> Unit,
    back: (Long) -> Unit,
    onValueChange: (String, String) -> Unit,
    showModal: () -> Unit,
    pStreet: PreMeasurementStreet,
    takePhoto: (uri: Uri) -> Unit,
) {


    val keyboardController = LocalSoftwareKeyboardController.current
    var exitMeasurement by remember { mutableStateOf(false) }

    var street by remember { mutableStateOf(pStreet.street) }
    var number by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf(pStreet.neighborhood) }
    var city by remember { mutableStateOf(pStreet.city) }
    var state by remember { mutableStateOf(pStreet.state ?: "") }
    var lastPower by remember { mutableStateOf("") }

    val fileUri =
        remember { mutableStateOf<Uri?>(Uri.parse("content://com.thryon.lumos.provider/my_images/photo_1743345161984.jpg")) }
    val imageSaved = remember { mutableStateOf(false) }
    val createFile: () -> Uri = {
        val file = File(context.filesDir, "photo_${System.currentTimeMillis()}.jpg")
        file.createNewFile() // Garante que o arquivo seja criado
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        Log.d("ImageDebug", "URI criada: $uri") // 📌 Adiciona log aqui

        uri
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                Log.d(
                    "ImageDebug",
                    "Foto tirada com sucesso! URI: ${fileUri.value}"
                ) // 🔍 Verifica se foi salvo
                fileUri.value?.let { uri ->
                    takePhoto(uri)
                    imageSaved.value = true
                }
            } else {
                Log.e("ImageDebug", "Erro ao tirar foto.")
            }
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                NetworkStatusBar(context = context)
                TopBar(
                    navigateBack = {
                        exitMeasurement = true
                    },
                    title = "Voltar"
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
                        saveStreet()
                        back(pStreet.contractId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(8.dp) // Botão com cantos menos arredondados
            ) {
                Text(
                    text = "Salvar",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }
        }, content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(10.dp)
                    .fillMaxSize()
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
                            back(pStreet.contractId)
                        }, // Ação ao confirmar
                        dialogTitle = "Confirmação de saída", // Título do diálogo
                        dialogText = "Você tem certeza que deseja cancelar a pré-medição atual?", // Texto do diálogo
                        icon = Icons.Filled.Warning // Ícone do Material Design
                    )
                }

                Column(
                    Modifier.fillMaxWidth(),
                ) {


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
                                    color = MaterialTheme.colorScheme.primary // Azul escuro
                                )
                            )
                            Text(
                                text = "${pStreet.latitude}, ${pStreet.longitude}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary // Azul escuro
                                )
                            )
                            Text(
                                text = city,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary // Azul escuro
                                )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, // Alinha o conteúdo da coluna no centro
                            modifier = Modifier.clickable {
                                showModal()
                            }
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color.Red,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ) {
                                        Text(
                                            text = preMeasurementStreetItems.size.toString(),
                                            color = Color.White
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = "Shopping cart",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Text(
                                text = "Itens adicionados",
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(24.dp))

                    Row {
                        OutlinedTextField(
                            textStyle = TextStyle(MaterialTheme.colorScheme.onBackground),
                            value = street,
                            onValueChange = {
                                street = it
                                onValueChange("street", street)
                            },
                            label = {
                                Text(
                                    text = "Rua:",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth(0.80f)
                                .padding(end = 10.dp)
                        )

                        OutlinedTextField(
                            textStyle = TextStyle(MaterialTheme.colorScheme.onBackground),
                            value = number,
                            onValueChange = {
                                number = it
                                onValueChange("number", number)
                            },
                            label = {
                                Text(
                                    text = "№:",
                                    color = MaterialTheme.colorScheme.onBackground
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
                            textStyle = TextStyle(MaterialTheme.colorScheme.onBackground),
                            value = neighborhood,
                            onValueChange = {
                                neighborhood = it
                                onValueChange("neighborhood", neighborhood)
                            },
                            label = {
                                Text(
                                    text = "Bairro:",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .padding(end = 10.dp)

                        )

                        OutlinedTextField(
                            textStyle = TextStyle(MaterialTheme.colorScheme.onBackground),
                            value = city,
                            onValueChange = {
                                city = it
                                onValueChange("city", city)
                            },
                            label = {
                                Text(
                                    text = "Cidade:",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row {
                        pStreet.lastPower?.isEmpty()?.let { empty ->
                            OutlinedTextField(
                                textStyle = TextStyle(MaterialTheme.colorScheme.onBackground),
                                value = lastPower,
                                onValueChange = {
                                    lastPower = if (it.endsWith("W")) it else it + "W"
                                    onValueChange("lastPower", lastPower)
                                },
                                label = {
                                    Text(
                                        text = "Potência atual:",
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                                isError = empty
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    ) {

                        Card(
                            modifier = Modifier.padding(end = 5.dp)
                                .clickable {
                                    val newUri = createFile() // Gera um novo Uri
                                    fileUri.value = newUri // Atualiza o estado
                                    launcher.launch(newUri) // Usa a variável temporária, garantindo que o valor correto seja usado
                                },
                            shape = RoundedCornerShape(10.dp), // Formato
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer // Cor de fundo do botão
                            )
                        ) {

                            AnimatedVisibility(visible = imageSaved.value) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(fileUri.value)
                                            .crossfade(true) // Para um fade suave
                                            .build()
                                    ),
                                    contentDescription = "Imagem da foto",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .padding(0.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            AnimatedVisibility(visible = !imageSaved.value) {
                                Icon(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(30.dp),
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = "Foto",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(
                            modifier = Modifier.padding(top = 5.dp),
                            text = "Tirar foto",
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            fontStyle = MaterialTheme.typography.bodyLarge.fontStyle
                        )
                    }


                    Spacer(modifier = Modifier.height(120.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Card redondo para o botão
                        Card(
                            modifier = Modifier
                                .size(50.dp) // Tamanho maior para um botão redondo
                                .clickable {
                                    showModal()
                                }, // Adiciona interação de clique
                            shape = RoundedCornerShape(10.dp), // Formato circular
                            elevation = CardDefaults.cardElevation(5.dp), // Sombra
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer // Cor de fundo do botão
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Adicionar",
                                    tint = MaterialTheme.colorScheme.onBackground, // Cor do ícone
                                    modifier = Modifier.size(30.dp) // Tamanho do ícone
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp)) // Espaço entre o botão e o texto
                        Text(
                            text = "Adicionar Itens",
                            color = MaterialTheme.colorScheme.onBackground, // Cor do texto
                            fontSize = 16.sp, // Tamanho da fonte
                            fontWeight = FontWeight.Medium // Peso da fonte
                        )
                    }

                }

            }

        }


    )
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
            Icon(icon, contentDescription = "Icon")
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
    preList: List<PreMeasurementStreetItem>,
    preMeasurementStreetId: Long,
    contractId: Long,
    context: Context
) {
    var searchQuery by remember { mutableStateOf("") }
    val preMeasurementStreetItems = remember { mutableStateListOf<PreMeasurementStreetItem>() }
    val filteredList = materials.filter {
        it.materialName?.contains(searchQuery, ignoreCase = true) ?: false ||
                it.materialPower?.contains(searchQuery, ignoreCase = true) ?: false
    }

    LaunchedEffect(preList) {
        if (preList.isNotEmpty()) {
            preMeasurementStreetItems.clear()
            // Adiciona os itens de preList à lista reativa
            preMeasurementStreetItems.addAll(preList)
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = {
            it != SheetValue.Hidden
        }
    )

    ModalBottomSheet(
        onDismissRequest = { onDismissRequest(preMeasurementStreetItems) },
        dragHandle = null,
        modifier = Modifier.fillMaxSize(),
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Selecione os itens", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(
                    onClick = {
                        onDismissRequest(
                            preMeasurementStreetItems
                        )
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Concluir", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            modifier = Modifier.padding(end = 5.dp)
                        )
                        Icon(
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "Icone Concluir"
                        )
                    }

                }
            }

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
                        contentDescription = "Pesquisar"
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(filteredList) { material ->
                    val selectedItem = preMeasurementStreetItems.find { it.materialId == material.materialId }
                    // Passando diretamente o estado de 'selected' e 'quantity' do Composable 1
                    ItemLightRow(
                        material = material,
                        preSelected = selectedItem != null,
                        preQuantity = selectedItem?.materialQuantity
                            ?: 0,
                        onItemSelected = { m ->
                            if (preMeasurementStreetItems.find { item -> item.materialId == m.materialId } == null) {
                                preMeasurementStreetItems.add(
                                    PreMeasurementStreetItem(
                                        preMeasurementStreetId = preMeasurementStreetId,
                                        materialId = m.materialId,
                                        materialQuantity = 0,
                                        contractId = contractId
                                    )
                                )
                            } else {
                                preMeasurementStreetItems.removeAll { item -> item.materialId == m.materialId }
                            }
                        },
                        onQuantityChange = { materialId, quantity ->
                            preMeasurementStreetItems.replaceAll {
                                if (it.materialId == materialId) it.copy(
                                    materialQuantity = quantity
                                ) else it
                            }
                        },
                        context = context
                    )
                }
            }
        }
    }

}

@Composable
fun ItemLightRow(
    material: Material,
    onItemSelected: (Material) -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
    context: Context,
    preSelected: Boolean,
    preQuantity: Int
) {
    var selected = preSelected
    var quantity = preQuantity

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
        targetValue = if (selected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.background,
        label = ""
    )

    AnimatedVisibility(!selected) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            UserExperience.vibrate(context = context, 10)
                            selected = !selected
                            onItemSelected(material)
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardColors(
                contentColor = MaterialTheme.colorScheme.onSecondary,
                containerColor = backgroundColor,
                disabledContainerColor = MaterialTheme.colorScheme.onSecondary,
                disabledContentColor = MaterialTheme.colorScheme.onSecondary
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
                        color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground,
                    )
                    if (materialChar.isNotEmpty()) {
                        Text(
                            text = materialChar,
                            fontSize = 14.sp,
                            color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    // Só mostra o controle de quantidade se o item estiver selecionado

                }
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Pressione e segure",
                    modifier = Modifier.padding(top = 8.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

        }
    }

    AnimatedVisibility(selected) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            UserExperience.vibrate(context = context, 10)
                            selected = !selected
                            onItemSelected(material)
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardColors(
                contentColor = MaterialTheme.colorScheme.onError,
                containerColor = backgroundColor,
                disabledContainerColor = MaterialTheme.colorScheme.onError,
                disabledContentColor = MaterialTheme.colorScheme.onError
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
                    color = MaterialTheme.colorScheme.onSecondary
                )

                Spacer(modifier = Modifier.height(8.dp)) // Espaçamento entre os itens

                // Linha com os botões de aumentar e diminuir a quantidade
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center // Centraliza os itens
                ) {
                    IconButton(
                        onClick = {
                            if (quantity > 0) {
                                quantity -= 1
                                onQuantityChange(
                                    material.materialId,
                                    quantity
                                )
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onError, shape = CircleShape)
                            .padding(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Diminuir",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp)) // Espaçamento entre os ícones

                    Text(
                        text = quantity.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            quantity += 1
                            onQuantityChange(material.materialId, quantity)
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onError, shape = CircleShape)
                            .padding(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Aumentar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Quantidade",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

}


@Preview
@Composable
fun PrevStreet() {

    val list = listOf(
        PreMeasurementStreetItem(
            preMeasurementStreetId = 1,
            materialId = 1,
            materialQuantity = 1,
            contractId = 1
        )
    )

//    val materials = listOf(
//        Material(
//            materialId = 1,
//            materialName = "LED",
//            materialPower = "100W",
//            materialAmps = null,
//            materialLength = null
//        )
//    )

    PMSContent(
        context = LocalContext.current,
        preMeasurementStreetItems = list,
        saveStreet = { },
        back = { _ -> },
        onValueChange = { _, _ -> },
        showModal = { },
        pStreet = PreMeasurementStreet(
            preMeasurementStreetId = 1,
            contractId = 1,
            lastPower = "",
            latitude = 1.1,
            longitude = 2.2,
            street = "",
            neighborhood = "",
            number = "",
            city = "",
            state = "",
            photoUri = ""
        ),
        takePhoto = { },
    )

//    BottomSheetDialog(
//        onDismissRequest = { },
//        materials = materials,
//        preMeasurementStreetId = 1,
//        LocalContext.current,
//    )


}