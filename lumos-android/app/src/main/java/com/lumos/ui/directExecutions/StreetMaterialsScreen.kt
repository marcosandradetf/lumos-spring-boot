package com.lumos.ui.directExecutions

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.lumos.domain.model.DirectReserve
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.Alert
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NothingData
import com.lumos.ui.viewmodel.DirectExecutionViewModel
import com.lumos.utils.ConnectivityUtils
import com.lumos.utils.Utils.formatDouble
import com.lumos.utils.Utils.sanitizeDecimalInput
import java.io.File

@SuppressLint("HardwareIds")
@Composable
fun StreetMaterialScreen(
    directExecutionId: Long,
    description: String,
    directExecutionViewModel: DirectExecutionViewModel,
    context: Context,
    pSelected: Int,
    navController: NavHostController,
    notificationsBadge: String,
) {
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
    val coordinates = CoordinatesService(context, fusedLocationProvider)

    var street by remember {
        mutableStateOf(
            DirectExecutionStreet(
                address = "",
                latitude = null,
                longitude = null,
                photoUri = null,
                deviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ),
                directExecutionId = directExecutionId,
                description = description,
            )
        )
    }

    var streetItems by remember { mutableStateOf(listOf<DirectExecutionStreetItem>()) }
    var hasPosted by remember { mutableStateOf(false) }
    var alertModal by remember { mutableStateOf(false) }
    var confirmModal by remember { mutableStateOf(false) }

    var locationModal by remember { mutableStateOf(true) }
    var confirmLocation by remember { mutableStateOf(false) }
    var loadingCoordinates by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var reserves by remember { mutableStateOf<List<DirectReserve>>(emptyList()) }

    val message = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem",
            "body" to "Você está na rua da execução neste momento?"
        )
    }


    LaunchedEffect(Unit) {
        isLoading = true
        try {
            reserves = directExecutionViewModel.getReservesOnce(directExecutionId)
        } finally {
            isLoading = false
        }
    }


    LaunchedEffect(confirmLocation) {
        if (confirmLocation) {
            loadingCoordinates = true
            coordinates.execute { latitude, longitude ->
                if (latitude != null && longitude != null) {
                    val addr = AddressService(context).execute(latitude, longitude)

                    if (addr != null && addr.size >= 4) {
                        val streetName = addr[0]
                        val neighborhood = addr[1]
                        val city = addr[2]
                        val state = addr[3]

                        street.address =
                            "$streetName, [nº não informado] - $neighborhood, $city - $state"
                    }
                    loadingCoordinates = false
                } else {
                    Log.e("GET Address", "Latitude ou Longitude são nulos.")
                    loadingCoordinates = false
                }
            }
        }
    }

    if (loadingCoordinates) {
        Loading("Tentando carregar as coordenadas...")
    } else
        StreetMaterialsContent(
            isLoading = isLoading,
            description = description,
            reserves = reserves,
            street = street,
            pSelected = pSelected,
            context = context,
            navController = navController,
            notificationsBadge = notificationsBadge,
            takePhoto = { uri ->
                street = street.copy(photoUri = uri.toString())
            },
            changeStreet = {
                street = street.copy(address = it)
            },
            openModal = { action ->
                if (action == "SEND") {
                    message["body"] =
                        "Deseja confirmar o envio dessa execução?"
                } else {
                    message["body"] =
                        "Deseja sair?"
                }
                confirmModal = true
            },
            confirmModal = { action ->
                if (action == "SEND") {
                    confirmModal = false
                    if (street.address.contains("[nº não informado]")) {
                        message["title"] = "Número do endereço não preenchido"
                        message["body"] =
                            "Por favor, informe o número do endereço antes de finalizar."
                        alertModal = true
                    } else if (street.photoUri == null) {
                        message["title"] = "Você esqueceu da foto"
                        message["body"] = "Antes de finalizar, tire uma foto."
                        alertModal = true
                    } else if (street.address.isEmpty()) {
                        message["title"] = "Aviso Importante"
                        message["body"] =
                            "Você esqueceu de preencher o endereço, o envio é obrigatório."
                        alertModal = true
                    } else if (streetItems.isEmpty()) {
                        message["title"] = "Aviso Importante"
                        message["body"] = "Selecione os itens executados para continuar."
                        alertModal = true
                    } else {
                        val quantities = streetItems.map { it.quantityExecuted }
                        if (quantities.any { it == 0.0 }) {
                            message["title"] = "Quantidade inválida"
                            message["body"] =
                                "Não é permitido salvar itens com quantidade igual a 0."
                            alertModal = true
                        } else {
                            directExecutionViewModel.saveAndPost(
                                street = street,
                                items = streetItems,
                                onPostExecuted = { hasPosted = true },
                                onError = {
                                    errorMessage = it
                                }
                            )
                        }
                    }
                } else if (action == "CLOSE") {
                    confirmModal = false
                } else {
                    navController.navigate(action)
                }
            },
            alertModal = alertModal,
            closeAlertModal = {
                alertModal = false
            },
            changeMaterial = { materialStockId, contractItemId, selected, reserveId ->
                if (selected) {
                    val newItem = DirectExecutionStreetItem(
                        reserveId = reserveId,
                        materialStockId = materialStockId,
                        contractItemId = contractItemId,
                        quantityExecuted = 0.0
                    )
                    streetItems = streetItems + newItem
                } else {
                    streetItems = streetItems.filterNot {
                        it.reserveId == reserveId
                    }
                }
            },
            changeQuantity = { quantityExecuted, materialQuantity, reserveId ->
                if (quantityExecuted > materialQuantity) {
                    message["title"] = "Quantidade inválida"
                    message["body"] =
                        "A quantidade disponível desse material é $materialQuantity."
                    alertModal = true
                } else if (quantityExecuted < 0) {
                    message["title"] = "Quantidade inválida"
                    message["body"] = "Não é possível registrar uma quantidade negativa."
                    alertModal = true
                } else {
                    streetItems = streetItems.map {
                        if (it.reserveId == reserveId) {
                            it.copy(quantityExecuted = quantityExecuted)
                        } else it
                    }
                }
            },
            hasPosted = hasPosted,
            errorMessage = errorMessage,
            alertMessage = message,
            locationModal = locationModal,
            confirmLocation = {
                if (it) {
                    confirmLocation = true
                    locationModal = false
                } else {
                    locationModal = false
                }
            },
            streetItems = streetItems,
            openConfirmModal = confirmModal,
            markAsFinish = {
                directExecutionViewModel.markAsFinished(directExecutionId)
            }
        )

}

@Composable
fun StreetMaterialsContent(
    isLoading: Boolean,
    description: String,
    reserves: List<DirectReserve>,
    street: DirectExecutionStreet,
    pSelected: Int,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    takePhoto: (uri: Uri) -> Unit,
    changeStreet: (address: String) -> Unit,
    confirmModal: (String) -> Unit,
    openConfirmModal: Boolean,
    openModal: (String) -> Unit,
    alertModal: Boolean,
    closeAlertModal: () -> Unit,
    markAsFinish: () -> Unit,
    locationModal: Boolean,
    confirmLocation: (Boolean) -> Unit,
    changeMaterial: (Long, Long, Boolean, Long) -> Unit,
    changeQuantity: (Double, Double, Long) -> Unit,
    hasPosted: Boolean,
    errorMessage: String?,
    alertMessage: MutableMap<String, String>,
    streetItems: List<DirectExecutionStreetItem>
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val fileUri: MutableState<Uri?> = remember {
        mutableStateOf(
            street.photoUri?.toUri()
        )
    }

    val imageSaved = remember { mutableStateOf(street.photoUri != null) }
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
        pSelected = pSelected,
        sliderNavigateToMenu = {
            action = Routes.MENU
            openModal(Routes.MENU)
        },
        sliderNavigateToHome = {
            action = Routes.HOME
            openModal(Routes.HOME)
        },
        sliderNavigateToNotifications = {
            action = Routes.NOTIFICATIONS
            openModal(Routes.NOTIFICATIONS)
        },
        sliderNavigateToProfile = {
            action = Routes.PROFILE
            openModal(Routes.PROFILE)
        },
        navController = navController,
        navigateBack = {
            action = Routes.DIRECT_EXECUTION_SCREEN
            openModal(Routes.DIRECT_EXECUTION_SCREEN)
        },
        context = context,
        notificationsBadge = notificationsBadge,
    ) { _, _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            if (isLoading) {
                Loading("Carregando materiais")
            } else if (reserves.isEmpty()) {
                NothingData("Nenhum material pendente, execução finalizada.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        markAsFinish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Finalizar e enviar",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Finalizar e enviar",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else if (!hasPosted) {
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
                            value = street.address,
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
                            changeQuantity = { quantityExecuted, materialQuantity, reserveId ->
                                changeQuantity(
                                    quantityExecuted,
                                    materialQuantity,
                                    reserveId
                                )
                            },
                            changeMaterial = { materialStockId, contractItemId, selected, reserveId ->
                                changeMaterial(materialStockId, contractItemId, selected, reserveId)
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
                                    shape = RoundedCornerShape(8.dp)
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
                        .padding(20.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.inverseSurface)
                            .padding(20.dp)
                    ) {
                        Text(
                            "CONFIRMAR",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                }

                if (locationModal) {
                    Confirm(
                        body = "Você está na rua da execução nesse momento?",
                        confirm = {
                            confirmLocation(true)
                        },
                        cancel = {
                            confirmLocation(false)
                        }
                    )
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


            } else  {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.TaskAlt,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                                .padding(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Missão cumprida!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Os dados serão enviados para o sistema.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "Ok, voltar a tela anterior"
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun MaterialItem(
    material: DirectReserve,
    changeMaterial: (Long, Long, Boolean, Long) -> Unit,
    changeQuantity: (Double, Double, Long) -> Unit,
    streetItems: List<DirectExecutionStreetItem>
) {

    val quantity = streetItems.find {
        it.reserveId == material.reserveId
    }?.quantityExecuted ?: 0.0

    var text by remember(material.reserveId) {
        mutableStateOf(
            TextFieldValue(
                if (quantity % 1.0 == 0.0) {
                    quantity.toInt().toString()
                } else {
                    quantity.toString()
                }
            )
        )
    }

    val quantityExecuted = text.text.toDoubleOrNull() ?: 0.0

    val selected = remember(streetItems, material) {
        derivedStateOf {
            streetItems.any {
                it.reserveId == material.reserveId
            }
        }
    }


    LaunchedEffect(quantityExecuted) {
        changeQuantity(
            quantityExecuted,
            material.materialQuantity,
            material.reserveId
        )
    }

    Card(
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(3.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // Isso é o truque!
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(10.dp)
            ) {
                // Linha vertical com bolinha no meio
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.7f)
                        .padding(start = 20.dp)
                        .width(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary
                        )
                )

                // Bolinha com ícone (no meio da linha)
                Box(
                    modifier = Modifier
                        .offset(x = 10.dp) // posiciona sobre a linha
                        .size(24.dp) // tamanho do círculo
                        .clip(CircleShape)
                        .background(
                            color = MaterialTheme.colorScheme.primary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Local",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 📦 Nome e quantidade do material
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = material.materialName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AnimatedVisibility(!selected.value) {
                                Text(
                                    text = "Quantidade disponível: ${formatDouble(material.materialQuantity)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            AnimatedVisibility(visible = selected.value) {
                                OutlinedTextField(
                                    value = text,
                                    onValueChange = { newValue ->
                                        val sanitized = sanitizeDecimalInput(newValue.text)
                                        text =
                                            TextFieldValue(sanitized, TextRange(sanitized.length))
                                    },
                                    label = { Text("Qtde Exec") },
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .size(width = 90.dp, height = 60.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                )
                            }
                            IconToggleButton(
                                checked = selected.value,
                                onCheckedChange = { isChecked ->
                                    changeMaterial(
                                        material.materialStockId,
                                        material.contractItemId,
                                        isChecked,
                                        material.reserveId
                                    )
                                },
                                colors = IconToggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    contentColor = MaterialTheme.colorScheme.onBackground,
                                    disabledContentColor = MaterialTheme.colorScheme.background,
                                    disabledContainerColor = MaterialTheme.colorScheme.background,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedContainerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .border(
                                        border = BorderStroke(
                                            if (!selected.value) 2.dp else 0.dp,
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        ), shape = CircleShape
                                    )
                                    .size(30.dp)
                            ) {
                                if (selected.value)
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Check",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                            }
                        }


                    }
                }
            }
        }

    }
}


@Preview
@Composable
fun PrevMStreetScreen() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current

    val reserves = listOf(
        DirectReserve(
            materialStockId = 10,
            materialName = "LED 120W",
            materialQuantity = 12.0,
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 2,
            materialName = "BRAÇO DE 3,5",
            materialQuantity = 16.0,
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 3,
            materialName = "BRAÇO DE 3,5",
            materialQuantity = 16.0,
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 4,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 5,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 6,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 7,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 8,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        )
    )


    StreetMaterialsContent(
        isLoading = false,
        description = "Prefeitura de Belo Horizonte",
        reserves = reserves,
        street = DirectExecutionStreet(
            directStreetId = 1,
            address = "Rua Marcos Coelho Neto, 960 - Estrela Dalva",
            latitude = null,
            longitude = null,
            photoUri = null,
            deviceId = "",
            directExecutionId = 1,
            description = "",
        ),
        context = fakeContext,
        navController = rememberNavController(),
        notificationsBadge = "12",
        pSelected = BottomBar.HOME.value,
        takePhoto = { },
        confirmModal = { },
        alertModal = false,
        closeAlertModal = { },
        hasPosted = false,
        errorMessage = null,
        changeMaterial = { _, _, _, _ -> },
        openConfirmModal = false,
        changeQuantity = { _, _, _ -> },
        alertMessage = mutableMapOf(
            "title" to "Título da mensagem",
            "body" to "Conteúdo da mensagem"
        ),
        locationModal = true,
        confirmLocation = { },
        streetItems = emptyList(),
        changeStreet = {},
        openModal = {},
        markAsFinish = {}
    )
}