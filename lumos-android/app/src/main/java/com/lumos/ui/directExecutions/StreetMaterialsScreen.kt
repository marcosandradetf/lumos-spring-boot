package com.lumos.ui.directExecutions

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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.lumos.domain.model.MaterialStock
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
import com.lumos.ui.viewmodel.StockViewModel
import com.lumos.utils.Utils.sanitizeDecimalInput
import java.io.File
import java.math.BigDecimal

@Composable
fun StreetMaterialScreen(
    directExecutionId: Long,
    description: String,
    directExecutionViewModel: DirectExecutionViewModel,
    stockViewModel: StockViewModel,
    context: Context,
    lastRoute: String?,
    navController: NavHostController,
    notificationsBadge: String,
) {
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
    val coordinates = CoordinatesService(context, fusedLocationProvider)
    var currentAddress by remember { mutableStateOf("") }
    val stockData by stockViewModel.stock.collectAsState()

    val message = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem",
            "body" to "Você está na rua da execução neste momento?"
        )
    }

    LaunchedEffect(Unit) {
        directExecutionViewModel.loadExecutionData(directExecutionId, description)
    }

    LaunchedEffect(directExecutionViewModel.reserves.size) {
        if (directExecutionViewModel.sameStreet) {
            directExecutionViewModel.street =
                directExecutionViewModel.street?.copy(
                    address = currentAddress
                )
        } else if (directExecutionViewModel.reserves.isNotEmpty()) {
            directExecutionViewModel.loadingCoordinates = true
            coordinates.execute { latitude, longitude ->
                if (latitude != null && longitude != null) {
                    val addr = AddressService(context).execute(latitude, longitude)

                    if (addr != null && addr.size >= 4) {
                        val streetName = addr[0]
                        val neighborhood = addr[1]
                        val city = addr[2]
                        val state = addr[3]

                        directExecutionViewModel.street =
                            directExecutionViewModel.street?.copy(
                                address = "$streetName, - $neighborhood, $city - $state"
                            )

                        currentAddress = "$streetName, - $neighborhood, $city - $state"
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
    } else if (directExecutionViewModel.nextStep) {
        AppLayout(
            title = description,
            selectedIcon = BottomBar.EXECUTIONS.value,
            navigateBack = {
                if (directExecutionViewModel.hasPosted) {
                    directExecutionViewModel.clearViewModel()
                    navController.popBackStack()
                } else {
                    directExecutionViewModel.nextStep = false
                }
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
                directExecutionViewModel.clearViewModel()
                navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
            }
        ) { _, _ ->
            var triedToSubmit by remember { mutableStateOf(false) }

            if (directExecutionViewModel.hasPosted) {
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
                            directExecutionViewModel.clearViewModel()
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

                    Button(
                        onClick = {
                            directExecutionViewModel.clearViewModel()
                            directExecutionViewModel.sameStreet = true
                            directExecutionViewModel.loadExecutionData(
                                directExecutionId,
                                description
                            )
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
                        Text(
                            "Iniciar nova instalação nessa rua"
                        )
                    }
                }
            } else
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "Dados da instalação",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )

                    Text(
                        text = "Preencha os dados abaixo",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = directExecutionViewModel.street?.currentSupply ?: "",
                        onValueChange = {
                            triedToSubmit = false
                            directExecutionViewModel.street =
                                directExecutionViewModel.street?.copy(currentSupply = it)
                        },
                        isError = triedToSubmit && directExecutionViewModel.street?.currentSupply.isNullOrBlank(),
                        singleLine = true,
                        label = { Text("Fornecedor atual") },
                        supportingText = {
                            if (triedToSubmit && directExecutionViewModel.street?.currentSupply.isNullOrBlank()) {
                                Text(
                                    "Informe o fornecedor atual",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )

                    OutlinedTextField(
                        value = directExecutionViewModel.street?.lastPower ?: "",
                        onValueChange = {
                            triedToSubmit = false
                            directExecutionViewModel.street =
                                directExecutionViewModel.street?.copy(lastPower = it)
                        },
                        isError = triedToSubmit && directExecutionViewModel.street?.lastPower.isNullOrBlank(),
                        singleLine = true,
                        label = { Text("Potência anterior") },
                        supportingText = {
                            if (triedToSubmit && directExecutionViewModel.street?.lastPower.isNullOrBlank()) {
                                Text(
                                    "Informe a potência anterior",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )

                    Button(
                        onClick = {
                            triedToSubmit = true

                            val isCurrentSupplyValid =
                                !directExecutionViewModel.street?.currentSupply.isNullOrBlank()
                            val isLastPowerValid =
                                !directExecutionViewModel.street?.lastPower.isNullOrBlank()

                            if (isCurrentSupplyValid && isLastPowerValid) {
                                directExecutionViewModel.street?.let { street ->
                                    directExecutionViewModel.saveAndPost(
                                        street = street,
                                        items = directExecutionViewModel.streetItems,
                                        onPostExecuted = {
                                            directExecutionViewModel.hasPosted = true
                                        },
                                        onError = {
                                            directExecutionViewModel.errorMessage = it
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Enviar",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
        }
    } else
        StreetMaterialsContent(
            isLoading = directExecutionViewModel.isLoading,
            description = description,
            reserves = directExecutionViewModel.reserves,
            street = directExecutionViewModel.street,
            lastRoute = lastRoute,
            context = context,
            navController = navController,
            notificationsBadge = notificationsBadge,
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
                    } else {
                        val quantities =
                            directExecutionViewModel.streetItems.map { it.quantityExecuted }
                        if (quantities.any { it == "0" }) {
                            message["title"] = "Quantidade inválida"
                            message["body"] =
                                "Não é permitido salvar itens com quantidade igual a 0."
                            directExecutionViewModel.alertModal = true
                        } else {
                            directExecutionViewModel.nextStep = true
                        }
                    }
                } else if (action == "CLOSE") {
                    directExecutionViewModel.confirmModal = false
                } else {
                    directExecutionViewModel.clearViewModel()
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
            changeMaterial = { materialStockId, contractItemId, selected, reserveId, materialName ->
               if (selected) {
                   val material = stockData.find { it.materialStockId == materialStockId }
                   if(BigDecimal(material?.stockAvailable) == BigDecimal.ZERO) {
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
            changeQuantity = { materialStockId, quantityExecuted, balanceLimit, reserveId ->
                val material = stockData.find { it.materialStockId == materialStockId }
                if(BigDecimal(material?.stockAvailable) > quantityExecuted) {
                    message["title"] = "Quantidade indisponível"
                    message["body"] =
                        "A quantidade em estoque desse material é ${material?.stockAvailable}."
                    directExecutionViewModel.alertModal = true
                }
                else if (quantityExecuted > balanceLimit) {
                    message["title"] = "Quantidade inválida"
                    message["body"] =
                        "O Saldo contratual desse material é $balanceLimit."
                    directExecutionViewModel.alertModal = true
                } else if (quantityExecuted <= BigDecimal.ZERO) {
                    message["title"] = "Quantidade inválida"
                    message["body"] = "Não é possível registrar uma quantidade zero ou negativa."
                    directExecutionViewModel.alertModal = true
                } else {
                    directExecutionViewModel.streetItems =
                        directExecutionViewModel.streetItems.map {
                            if (it.reserveId == reserveId) {
                                it.copy(quantityExecuted = quantityExecuted.toString())
                            } else it
                        }
                }
            },
            errorMessage = directExecutionViewModel.errorMessage,
            alertMessage = message,
            streetItems = directExecutionViewModel.streetItems
        )

}

@Composable
fun StreetMaterialsContent(
    isLoading: Boolean,
    description: String,
    reserves: List<DirectReserve>,
    street: DirectExecutionStreet?,
    lastRoute: String?,
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
    changeMaterial: (Long, Long, Boolean, Long, String) -> Unit,
    changeQuantity: (Long, BigDecimal, BigDecimal, Long) -> Unit,
    errorMessage: String?,
    alertMessage: MutableMap<String, String>,
    streetItems: List<DirectExecutionStreetItem>,
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

    val selectedIcon =
        if (lastRoute == Routes.DIRECT_EXECUTION_SCREEN) BottomBar.EXECUTIONS.value else BottomBar.EXECUTIONS.value

    AppLayout(
        title = description,
        selectedIcon = selectedIcon,
        notificationsBadge = notificationsBadge,
        navigateToMore = {
            action = Routes.MORE
            openModal(Routes.MORE)
        },
        navigateToHome = {
            action = Routes.HOME
            openModal(Routes.HOME)
        },
        navigateBack = {
            action = Routes.DIRECT_EXECUTION_SCREEN
            openModal(Routes.DIRECT_EXECUTION_SCREEN)
        },

        navigateToStock = {
            action = Routes.STOCK
            openModal(Routes.STOCK)
        },
        navigateToExecutions = {
            action = Routes.DIRECT_EXECUTION_SCREEN
            openModal(Routes.DIRECT_EXECUTION_SCREEN)
        },
        navigateToMaintenance = {
            action = Routes.MAINTENANCE
            openModal(Routes.MAINTENANCE)
        }

    ) { _, _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            if (isLoading) {
                Loading("Carregando materiais")
            } else if (reserves.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = 0.7f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Saldo igual a zero.\nExecução finalizada!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Nenhuma ação é necessária!\nEstamos processando o envio dos dados!!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = "Ok, voltar a tela anterior",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
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
                            changeQuantity = { materialStockId, quantityExecuted, materialQuantity, reserveId ->
                                changeQuantity(
                                    materialStockId,
                                    quantityExecuted,
                                    materialQuantity,
                                    reserveId
                                )
                            },
                            changeMaterial = { materialStockId, contractItemId, selected, reserveId, materialName ->
                                changeMaterial(
                                    materialStockId,
                                    contractItemId,
                                    selected,
                                    reserveId,
                                    materialName
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
    material: DirectReserve,
    changeMaterial: (Long, Long, Boolean, Long, String) -> Unit,
    changeQuantity: (Long, BigDecimal, BigDecimal, Long) -> Unit,
    streetItems: List<DirectExecutionStreetItem>
) {

    val quantity = streetItems.find {
        it.reserveId == material.reserveId
    }?.quantityExecuted ?: "0"

    var text by remember(material.reserveId) {
        mutableStateOf(
            TextFieldValue(
                if (BigDecimal(quantity) % BigDecimal.ONE == BigDecimal.ZERO) {
                    quantity.toInt().toString()
                } else {
                    quantity
                }
            )
        )
    }

    val quantityExecuted = BigDecimal(text.text)
    val selected = remember(streetItems, material) {
        derivedStateOf {
            streetItems.any {
                it.reserveId == material.reserveId
            }
        }
    }


    LaunchedEffect(quantityExecuted) {
        changeQuantity(
            material.materialStockId,
            quantityExecuted,
            BigDecimal(material.materialQuantity),
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
                                    text = "Quantidade disponível: ${material.materialQuantity}",
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
                                            TextFieldValue(
                                                sanitized,
                                                TextRange(sanitized.length)
                                            )
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
                                        material.reserveId,
                                        material.materialName
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
            materialQuantity = "12",
            requestUnit = "UN",
            reserveId = 1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 2,
            materialName = "BRAÇO DE 3,5",
            materialQuantity = "16",
            requestUnit = "UN",
            reserveId = 2,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 3,
            materialName = "BRAÇO DE 3,5",
            materialQuantity = "16",
            requestUnit = "UN",
            reserveId = 3,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 4,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
            requestUnit = "UN",
            reserveId = 4,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 5,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
            requestUnit = "UN",
            reserveId = 5,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 6,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 7,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
            requestUnit = "UN",
            reserveId = -1,
            contractItemId = -1,
            directExecutionId = -1
        ),
        DirectReserve(
            materialStockId = 8,
            materialName = "CABO 1.5MM",
            materialQuantity = "30.4",
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
            lastPower = "100W",
            finishAt = "",
            currentSupply = "",
        ),
        lastRoute = Routes.DIRECT_EXECUTION_SCREEN,
        context = fakeContext,
        navController = rememberNavController(),
        notificationsBadge = "12",
        takePhoto = { },
        changeStreet = {},
        confirmModal = { },
        openConfirmModal = false,
        openModal = {},
        alertModal = false,
        closeAlertModal = { },
        changeMaterial = { _, _, _, _, _ -> },
        changeQuantity = { _, _, _, _ -> },
        errorMessage = null,
        alertMessage = mutableMapOf(
            "title" to "Título da mensagem",
            "body" to "Conteúdo da mensagem"
        ),
        streetItems = emptyList()
    )
}