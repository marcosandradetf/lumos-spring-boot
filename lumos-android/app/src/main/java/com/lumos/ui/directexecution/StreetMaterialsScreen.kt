package com.lumos.ui.directexecution

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.ReserveMaterialJoin
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.Alert
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.ui.components.Tag
import com.lumos.utils.Utils
import com.lumos.utils.Utils.sanitizeDecimalInput
import com.lumos.viewmodel.DirectExecutionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.util.UUID

@Composable
fun StreetMaterialScreen(
    directExecutionViewModel: DirectExecutionViewModel,
    context: Context,
    navController: NavHostController,
) {
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
    val coordinates = CoordinatesService(context, fusedLocationProvider)
    var currentAddress by remember { mutableStateOf("") }
    val contractor = directExecutionViewModel.contractor
    val scope = rememberCoroutineScope()

    val message = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem",
            "body" to "Você está na rua da execução neste momento?"
        )
    }

    LaunchedEffect(directExecutionViewModel.reserves.size) {
        if (directExecutionViewModel.sameStreet) {
            directExecutionViewModel.street =
                directExecutionViewModel.street?.copy(
                    address = currentAddress
                )

            coordinates.execute { latitude, longitude ->
                if (latitude != null && longitude != null) {
                    directExecutionViewModel.street =
                        directExecutionViewModel.street?.copy(
                            latitude = latitude,
                            longitude = longitude
                        )
                }
            }

        } else if (directExecutionViewModel.reserves.isNotEmpty()) {
            directExecutionViewModel.loadingCoordinates = true
            coordinates.execute { latitude, longitude ->
                if (latitude != null && longitude != null) {
                    val addr = AddressService(context).execute(latitude, longitude)

                    if (addr != null && addr.size >= 4) {
                        val streetName = addr[0]
                        val neighborhood = addr[1]
                        val city = addr[2]

                        currentAddress = "$streetName, $neighborhood, $city"

                        directExecutionViewModel.street =
                            directExecutionViewModel.street?.copy(
                                address = currentAddress,
                                latitude = latitude,
                                longitude = longitude
                            )
                    }
                    directExecutionViewModel.loadingCoordinates = false
                } else {
                    Log.e("GET Address", "Latitude ou Longitude são nulos.")
                    directExecutionViewModel.loadingCoordinates = false
                }
            }
        }
    }

    if (directExecutionViewModel.loadingCoordinates) {
        Loading("Tentando carregar as coordenadas...")
    } else if (directExecutionViewModel.hasPosted) {
        AppLayout(
            title = Utils.abbreviate(contractor ?: ""),
            selectedIcon = BottomBar.EXECUTIONS.value,
            navigateBack = {
                navController.popBackStack()
            },
            navigateToHome = {
                navController.navigate(Routes.HOME)
            },
            navigateToMore = {
                navController.navigate(Routes.MORE)
            },
            navigateToStock = {
                navController.navigate(Routes.STOCK)
            },
            navigateToExecutions = {
                navController.navigate(Routes.INSTALLATION_HOLDER)
            }
        ) { _, _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.TaskAlt,
                        contentDescription = "Tarefa concluída",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Ótimo Trabalho!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Esta rua está concluída.\nFinalize todas as ruas e depois toque em Gerenciar Instalação para enviar a instalação.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // 🧭 Grupo de ações
                Column(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 🔹 Ação principal
                    OutlinedButton(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text(
                            "Gerenciar Instalação"
                        )
                    }

                    Button(
                        onClick = {
                            directExecutionViewModel.sameStreet = true
                            directExecutionViewModel.loadExecutionData()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text(
                            "Nova Instalação Nessa Rua"
                        )
                    }
                }

            }
        }
    } else
        StreetMaterialsContent(
            isLoading = directExecutionViewModel.isLoading,
            description = contractor ?: "",
            reserves = directExecutionViewModel.reserves,
            street = directExecutionViewModel.street,
            context = context,
            navController = navController,
            takePhoto = { uri ->
                directExecutionViewModel.street =
                    directExecutionViewModel.street?.copy(photoUri = uri.toString())
            },
            changeStreet = {
                directExecutionViewModel.street =
                    directExecutionViewModel.street?.copy(address = it)
            },
            confirmModal = { action ->
                if (action == "SEND") {
                    directExecutionViewModel.confirmModal = false
                    val address = directExecutionViewModel.street?.address.orEmpty()
                    val hasNumber = Regex("""\d+""").containsMatchIn(address)
                    val hasSN = Regex("""(?i)\bS[\./\\]?\s?N\b""").containsMatchIn(address)
                    val invalidQuantity =
                        directExecutionViewModel.streetItems.map { it.quantityExecuted }
                            .any { BigDecimal(it) == BigDecimal.ZERO }

                    val balanceByContractId =
                        directExecutionViewModel.reserves
                            .groupBy { it.contractItemId }
                            .mapValues { (_, items) ->
                                if(items.first().currentBalance != null) {
                                    BigDecimal(items.first().currentBalance)
                                } else null
                            }

                    val contractItemQuantities =
                        directExecutionViewModel.streetItems
                            .groupBy { it.contractItemId }
                            .mapValues { (_, items) ->
                                items.fold(BigDecimal.ZERO) { acc, item ->
                                    acc + BigDecimal(item.quantityExecuted)
                                }
                            }

                    val exceededBalance = contractItemQuantities.any { (id, exec) ->
                        val balance = balanceByContractId[id]
                        balance != null && exec > balance
                    }


                    if (!hasNumber && !hasSN) {
                        message["title"] = "Número do endereço ausente"
                        message["body"] =
                            "Por favor, informe o número do endereço ou indique que é 'S/N'."
                        directExecutionViewModel.alertModal = true
                    } else if (directExecutionViewModel.street?.photoUri == null) {
                        message["title"] = "Você esqueceu da foto"
                        message["body"] = "Antes de finalizar, tire uma foto."
                        directExecutionViewModel.alertModal = true
                    } else if (directExecutionViewModel.street?.address == "") {
                        message["title"] = "Aviso Importante"
                        message["body"] =
                            "Você esqueceu de preencher o endereço, o envio é obrigatório."
                        directExecutionViewModel.alertModal = true
                    } else if (directExecutionViewModel.streetItems.isEmpty()) {
                        message["title"] = "Aviso Importante"
                        message["body"] = "Selecione os itens executados para continuar."
                        directExecutionViewModel.alertModal = true
                    } else if (exceededBalance) {
                        val itemId = contractItemQuantities
                            .filter { (id, exec) ->
                                exec > (balanceByContractId[id] ?: BigDecimal.ZERO)
                            }.keys.firstOrNull()

                        val itemName = directExecutionViewModel.reserves.find { it.contractItemId == itemId }?.itemName

                        message["title"] = "Saldo atual: ${balanceByContractId[itemId]}"
                        message["body"] =
                            """
                                Não há saldo suficiente para o item $itemName.
                        
                                Para evitar estouro de saldo, a soma de todos os materiais derivados deste item
                                não pode ultrapassar o saldo disponível no sistema.
                        
                                Saldo disponível atualmente: ${balanceByContractId[itemId]}
                        
                                Por favor, revise as quantidades derivadas para este item. 
                                Caso precise de ajuste de saldo ou tenha dúvidas, entre em contato com o setor administrativo.
                            """.trimIndent()


                        directExecutionViewModel.alertModal = true

                    } else if (invalidQuantity) {
                        message["title"] = "Quantidade inválida"
                        message["body"] =
                            "Não é permitido salvar itens com quantidade igual a 0."
                        directExecutionViewModel.alertModal = true
                    } else {
                        directExecutionViewModel.saveAndPost()
                    }
                } else if (action == "CLOSE") {
                    directExecutionViewModel.confirmModal = false
                } else {
                    navController.navigate(action)
                }
            },
            openConfirmModal = directExecutionViewModel.confirmModal,
            openModal = { action ->
                if (action == "SEND") {
                    message["body"] =
                        "Deseja confirmar o envio dessa execução?"
                } else {
                    message["body"] =
                        "Deseja sair?"
                }
                directExecutionViewModel.confirmModal = true
            },
            alertModal = directExecutionViewModel.alertModal,
            closeAlertModal = {
                directExecutionViewModel.alertModal = false
            },
            changeMaterial = { selected, reserveId, contractItemId, materialStockId, materialName, stockAvailable ->
                if (selected) {
                    if (stockAvailable == BigDecimal.ZERO) {
                        message["title"] = "Quantidade indisponível"
                        message["body"] =
                            "Esse material está sem estoque"
                        directExecutionViewModel.alertModal = true
                    } else {
                        val newItem = DirectExecutionStreetItem(
                            reserveId = reserveId,
                            materialStockId = materialStockId,
                            contractItemId = contractItemId,
                            quantityExecuted = "0",
                            materialName = materialName
                        )
                        directExecutionViewModel.streetItems += newItem
                    }

                } else {
                    directExecutionViewModel.streetItems =
                        directExecutionViewModel.streetItems.filterNot {
                            it.reserveId == reserveId
                        }
                }
            },


            changeQuantity = { reserveId, quantityExecuted, requestQuantity, stockAvailable ->

                fun resetQuantityWithMessage(title: String, body: String) {
//                    message["title"] = title
//                    message["body"] = body
//                    directExecutionViewModel.alertModal = true
                    directExecutionViewModel.errorMessage = "$title - $body"
                    directExecutionViewModel.streetItems =
                        directExecutionViewModel.streetItems.map {
                            if (it.reserveId == reserveId) {
                                it.copy(quantityExecuted = "0")
                            } else it
                        }

                }

                when {
                    quantityExecuted < BigDecimal.ZERO -> {
                        resetQuantityWithMessage(
                            title = "Quantidade inválida",
                            body = "Não é possível registrar uma quantidade negativa."
                        )
                    }

//                    quantityExecuted > requestQuantity -> {
//                        resetQuantityWithMessage(
//                            title = "Quantidade solicitada excedida",
//                            body = "O máximo permitido é $requestQuantity."
//                        )
//                    }

                    stockAvailable < quantityExecuted -> {
                        resetQuantityWithMessage(
                            title = "Estoque insuficiente",
                            body = "Há apenas $stockAvailable disponíveis em estoque."
                        )
                    }

                    else -> {
                        directExecutionViewModel.streetItems =
                            directExecutionViewModel.streetItems.map {
                                if (it.reserveId == reserveId) {
                                    it.copy(quantityExecuted = quantityExecuted.toString())
                                } else it
                            }
                    }
                }
            },
            errorMessage = directExecutionViewModel.errorMessage,
            alertMessage = message,
            streetItems = directExecutionViewModel.streetItems,
            clearMessage = {
                directExecutionViewModel.errorMessage = null
            }
        )

}

@Composable
fun StreetMaterialsContent(
    isLoading: Boolean,
    description: String,
    reserves: List<ReserveMaterialJoin>,
    street: DirectExecutionStreet?,
    context: Context,
    navController: NavHostController,
    takePhoto: (uri: Uri) -> Unit,
    changeStreet: (address: String) -> Unit,
    confirmModal: (String) -> Unit,
    openConfirmModal: Boolean,
    openModal: (String) -> Unit,
    alertModal: Boolean,
    closeAlertModal: () -> Unit,
    changeMaterial: (Boolean, Long, Long, Long, String, BigDecimal) -> Unit,
    changeQuantity: (Long, BigDecimal, BigDecimal, BigDecimal) -> Unit,
    errorMessage: String?,
    alertMessage: MutableMap<String, String>,
    streetItems: List<DirectExecutionStreetItem>,
    clearMessage: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val fileUri: MutableState<Uri?> = remember {
        mutableStateOf(
            street?.photoUri?.toUri()
        )
    }

    val imageSaved = remember { mutableStateOf(street?.photoUri != null) }
    val createFile: () -> Uri = {
        val file = File(context.filesDir, "photo_${System.currentTimeMillis()}.jpg")
        file.createNewFile() // Garante que o arquivo seja criado
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        uri
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                fileUri.value?.let { uri ->
                    takePhoto(uri)
                    imageSaved.value = true
                }
            } else {
                Log.e("ImageDebug", "Erro ao tirar foto.")
            }
        }

    var action by remember { mutableStateOf("") }


    AppLayout(
        title = description,
        selectedIcon = BottomBar.EXECUTIONS.value,
        navigateToMore = {
            action = Routes.MORE
            openModal(Routes.MORE)
        },
        navigateToHome = {
            action = Routes.HOME
            openModal(Routes.HOME)
        },
        navigateBack = {
            action = Routes.INSTALLATION_HOLDER
            openModal(Routes.INSTALLATION_HOLDER)
        },

        navigateToStock = {
            action = Routes.STOCK
            openModal(Routes.STOCK)
        },
        navigateToExecutions = {
            action = Routes.INSTALLATION_HOLDER
            openModal(Routes.INSTALLATION_HOLDER)
        },
        navigateToMaintenance = {
            action = Routes.MAINTENANCE
            openModal(Routes.MAINTENANCE)
        }

    ) { _, showSnackBar ->

        if (errorMessage != null) {
            showSnackBar(errorMessage, null, null)
            clearMessage()
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            if (isLoading) {
                Loading("Carregando materiais")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 90.dp)// deixa espaço pros botões
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus() // ⌨️ Fecha o teclado
                            })
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp) // Espaço entre os cards

                ) {
                    item {
                        TextField(
                            value = street?.address ?: "",
                            onValueChange = { changeStreet(it) },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                                disabledContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            placeholder = {
                                Text(
                                    text = "Qual o endereço atual?",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Localização",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            singleLine = true,
                            shape = CircleShape,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                }
                            )
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Selecione os itens executados na rua",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    items(
                        items = reserves,
                        key = { it.reserveId }
                    ) {
                        MaterialItem(
                            material = it,
                            changeQuantity = { reserveId, quantityExecuted, materialQuantity, stockAvailable ->
                                changeQuantity(
                                    reserveId,
                                    quantityExecuted,
                                    materialQuantity,
                                    stockAvailable
                                )
                            },
                            changeMaterial = { selected, reserveId, contractItemId, materialStockId, materialName, stockAvailable ->
                                changeMaterial(
                                    selected,
                                    reserveId,
                                    contractItemId,
                                    materialStockId,
                                    materialName,
                                    stockAvailable,
                                )
                            },
                            streetItems = streetItems,
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        val newUri = createFile() // Gera um novo Uri
                        fileUri.value = newUri // Atualiza o estado
                        launcher.launch(newUri) // Usa a variável temporária, garantindo que o valor correto seja usado
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart) // <-- Aqui dentro de um Box
                        .padding(16.dp)
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
                                .size(70.dp)
                                .padding(0.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    AnimatedVisibility(visible = !imageSaved.value) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    contentDescription = null,
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Tirar Foto",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 12.sp
                                )
                            }

                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        action = "SEND"
                        openModal("SEND")
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // <-- Aqui dentro de um Box
                        .fillMaxWidth(0.6f)
                        .padding(25.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.inverseSurface)
                            .padding(10.dp)
                    ) {
                        Text(
                            "Finalizar",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                if (alertModal) {
                    Alert(
                        title = alertMessage["title"] ?: "",
                        body = alertMessage["body"] ?: "",
                        confirm = {
                            closeAlertModal()
                        })
                }

                if (openConfirmModal) {
                    Confirm(
                        body = alertMessage["body"] ?: "",
                        confirm = {
                            confirmModal(action)
                        },
                        cancel = {
                            confirmModal("CLOSE")
                        }
                    )
                }


            }
        }

    }
}

@Composable
fun MaterialItem(
    material: ReserveMaterialJoin,
    changeMaterial: (Boolean, Long, Long, Long, String, BigDecimal) -> Unit,
    changeQuantity: (Long, BigDecimal, BigDecimal, BigDecimal) -> Unit,
    streetItems: List<DirectExecutionStreetItem>,
) {
    val quantity = remember(streetItems, material) {
        derivedStateOf {
            streetItems.find {
                it.reserveId == material.reserveId
            }?.quantityExecuted ?: BigDecimal.ZERO.toString()
        }
    }

    var text by remember(material.reserveId) {
        mutableStateOf(
            TextFieldValue(
                quantity.value
            )
        )
    }

    val selected = remember(streetItems, material) {
        derivedStateOf {
            streetItems.any {
                it.reserveId == material.reserveId
            }
        }
    }


    ListItem(
        headlineContent = {
            Text(
                text = material.materialName,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis
            )
        },
        overlineContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tag de disponibilidade
                when {
                    BigDecimal(material.stockAvailable) == BigDecimal.ZERO -> {
                        Tag(
                            text = "Sem estoque disponível",
                            color = Color.Red,
                            icon = Icons.Default.Close
                        )
                    }

                    BigDecimal(material.stockAvailable) <= BigDecimal.TEN -> {
                        Tag(
                            text = "Estoque: ${material.stockAvailable} ${material.requestUnit}",
                            color = Color(0xFFFF9800),
                            icon = Icons.Default.Warning
                        )
                    }

                    else -> {
                        Tag(
                            text = "Estoque: ${material.stockAvailable} ${material.requestUnit}",
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.Check
                        )
                    }
                }

                // Quantidade restante
                Text(
                    text = "Saldo a executar: ${material.materialQuantity} ${material.requestUnit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingContent = {
            AnimatedVisibility(visible = selected.value) {
                OutlinedTextField(
                    value = TextFieldValue(quantity.value, TextRange(quantity.value.length)),
                    onValueChange = { newValue ->
                        val sanitized = sanitizeDecimalInput(newValue.text)
                        text = TextFieldValue(sanitized, TextRange(sanitized.length))

                        changeQuantity(
                            material.reserveId,
                            BigDecimal(text.text),
                            BigDecimal(material.materialQuantity),
                            BigDecimal(material.stockAvailable)
                        )
                    },
                    label = { Text("Quantidade") },
                    placeholder = { Text("0.00") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(0.5f)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            IconToggleButton(
                checked = selected.value,
                onCheckedChange = { isChecked ->
                    changeMaterial(
                        isChecked,
                        material.reserveId,
                        material.contractItemId,
                        material.materialStockId,
                        material.materialName,
                        BigDecimal(material.stockAvailable)
                    )
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (selected.value)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (selected.value)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selecionar",
                    tint = if (selected.value)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        },
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .shadow(3.dp, RoundedCornerShape(12.dp)),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )

}


@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun PrevMStreetScreen() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current

    val mockItems = listOf(
        ReserveMaterialJoin(
            materialStockId = 10,
            materialName = "LED 120W",
            materialQuantity = "12",
            requestUnit = "UN",
            reserveId = 1,
            contractItemId = -1,
            directExecutionId = -1,
            stockAvailable = "100"
        ),
        ReserveMaterialJoin(
            materialStockId = 2,
            materialName = "BRAÇO DE PROJEÇÃO HORIZONTAL DE 3,5M",
            materialQuantity = "16",
            requestUnit = "UN",
            reserveId = 2,
            contractItemId = -1,
            directExecutionId = -1,
            stockAvailable = "100"
        ),
        ReserveMaterialJoin(
            materialStockId = 3,
            materialName = "BRAÇO DE 3,5",
            materialQuantity = "16",
            requestUnit = "UN",
            reserveId = 3,
            contractItemId = -1,
            directExecutionId = -1,
            stockAvailable = "100"
        ),
        ReserveMaterialJoin(
            materialStockId = 4,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
            requestUnit = "UN",
            reserveId = 4,
            contractItemId = -1,
            directExecutionId = -1,
            stockAvailable = "100"
        ),
        ReserveMaterialJoin(
            materialStockId = 5,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
            requestUnit = "UN",
            reserveId = 5,
            contractItemId = -1,
            directExecutionId = -1,
            stockAvailable = "100"
        ),
        ReserveMaterialJoin(
            materialStockId = 6,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1,
            stockAvailable = "100"
        ),
        ReserveMaterialJoin(
            materialStockId = 7,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1,
            stockAvailable = "100"
        ),
        ReserveMaterialJoin(
            materialStockId = 8,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1,
            stockAvailable = "100"
        )
    )

    StreetMaterialScreen(
        directExecutionViewModel = DirectExecutionViewModel(
            null, null,
            mockItems = mockItems,
            mockStreetItems = listOf(
                DirectExecutionStreetItem(
                    directStreetItemId = 1,
                    reserveId = 1,
                    materialStockId = 10,
                    materialName = "",
                    contractItemId = -1,
                    directStreetId = 1,
                    quantityExecuted = "12"
                )
            )
        ),
        context = LocalContext.current,
        navController = rememberNavController()
    )
}