package com.lumos.ui.preMeasurement

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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.lumos.domain.model.Item
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.ConfirmNavigation
import com.lumos.ui.components.CurrentScreenLoading
import com.lumos.ui.components.Tag
import com.lumos.utils.Utils
import com.lumos.utils.Utils.sanitizeDecimalInput
import com.lumos.viewmodel.ContractViewModel
import com.lumos.viewmodel.PreMeasurementViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.util.UUID

@Composable
fun PreMeasurementStreetScreen(
    context: Context,
    preMeasurementViewModel: PreMeasurementViewModel,
    preMeasurementId: String,
    contractViewModel: ContractViewModel,
    navController: NavHostController,
) {
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
    val coordinates = CoordinatesService(context, fusedLocationProvider)

    var currentAddress by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val message = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem",
            "body" to "Você está na rua da execução neste momento?"
        )
    }

    val items by contractViewModel.items.collectAsState()
    val measurement = preMeasurementViewModel.measurement


    LaunchedEffect(Unit) {
        preMeasurementViewModel.loading = true

        val contract = contractViewModel.getContract(measurement?.contractId!!)

        contract?.let { loadedContract ->
            val itemsIdsList = loadedContract.itemsIds
                ?.split("#")
                ?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()

            contractViewModel.loadItemsFromContract(itemsIdsList)

            contractViewModel.syncContractItems()

            preMeasurementViewModel.newPreMeasurementStreet()
        }

        preMeasurementViewModel.loading = false

        preMeasurementViewModel.locationLoading = true

        coordinates.execute { latitude, longitude ->
            if (latitude != null && longitude != null) {
                preMeasurementViewModel.latitude = latitude
                preMeasurementViewModel.longitude = longitude
                val addr = AddressService(context).execute(latitude, longitude)

                val street = addr?.get(0).toString()
                val neighborhood = addr?.get(1).toString()
                val city = addr?.get(2).toString()

                currentAddress = "$street, $neighborhood, $city"

                preMeasurementViewModel.street = preMeasurementViewModel.street?.copy(
                    latitude = latitude,
                    longitude = longitude,
                    address = currentAddress
                )
            }
            preMeasurementViewModel.locationLoading = false
        }

    }



    if (preMeasurementViewModel.locationLoading) {
        CurrentScreenLoading(
            navController,
            Utils.abbreviate(preMeasurementViewModel.measurement?.contractor.toString()),
            "Tentando carregar as coordenadas...",
            BottomBar.MORE.value
        )
    } else if (preMeasurementViewModel.loading) {
        CurrentScreenLoading(
            navController,
            Utils.abbreviate(preMeasurementViewModel.measurement?.contractor.toString()),
            "Carregando...",
            BottomBar.MORE.value
        )
    } else if (preMeasurementViewModel.nextStep) {
        AppLayout(
            title = Utils.abbreviate(preMeasurementViewModel.measurement?.contractor.toString()),
            selectedIcon = BottomBar.MORE.value,
            navigateBack = {
                navController.popBackStack()
            },
            navigateToMaintenance = {
                navController.navigate(Routes.MAINTENANCE)
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
                navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
            }
        ) { _, showSnackBar ->
            var triedToSubmit by remember { mutableStateOf(false) }

            if (preMeasurementViewModel.message != null) {
                showSnackBar(preMeasurementViewModel.message!!, null, null)
            }

            if (preMeasurementViewModel.hasPosted) {
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
                            text = "Ponto adicionado!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Escolha a sua próxima ação.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            navController.popBackStack()
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
                            "Voltar a tela anterior"
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                preMeasurementViewModel.preMeasurementStreetId = UUID.randomUUID()
                                preMeasurementViewModel.street =
                                    preMeasurementViewModel.street?.copy(
                                        preMeasurementStreetId = preMeasurementViewModel.preMeasurementStreetId.toString(),
                                        address = currentAddress,
                                        photoUri = null
                                    )
                                preMeasurementViewModel.streetItems = emptyList()
                                preMeasurementViewModel.hasPosted = false
                                preMeasurementViewModel.nextStep = false
                            }
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
                            "Adicionar ponto nessa rua"
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
                        text = "Dados da Pré-medição",
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
                        value = preMeasurementViewModel.street?.lastPower ?: "",
                        onValueChange = {
                            triedToSubmit = false
                            preMeasurementViewModel.street =
                                preMeasurementViewModel.street?.copy(lastPower = it)
                        },
                        isError = triedToSubmit && preMeasurementViewModel.street?.lastPower.isNullOrBlank(),
                        singleLine = true,
                        label = { Text("Potência anterior") },
                        supportingText = {
                            if (triedToSubmit && preMeasurementViewModel.street?.lastPower.isNullOrBlank()) {
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

                            val isLastPowerValid =
                                !preMeasurementViewModel.street?.lastPower.isNullOrBlank()

                            if (isLastPowerValid) {
                                preMeasurementViewModel.street?.let { street ->
                                    preMeasurementViewModel.save()
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
                            "Salvar",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
        }
    } else
        StreetItemsContent(
            description = Utils.abbreviate(preMeasurementViewModel.measurement?.contractor.toString()),
            preMeasurementViewModel = preMeasurementViewModel,
            context = context,
            navController = navController,
            items = items
        )

}

@Composable
fun StreetItemsContent(
    description: String,
    preMeasurementViewModel: PreMeasurementViewModel,
    context: Context,
    navController: NavHostController,
    items: List<Item>,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val fileUri: MutableState<Uri?> = remember {
        mutableStateOf(
            preMeasurementViewModel.street?.photoUri?.toUri()
        )
    }

    val imageSaved = remember { mutableStateOf(preMeasurementViewModel.street?.photoUri != null) }
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
                    preMeasurementViewModel.street = preMeasurementViewModel.street?.copy(
                        photoUri = uri.toString()
                    )
                    imageSaved.value = true
                }
            } else {
                Log.e("ImageDebug", "Erro ao tirar foto.")
            }
        }

    var action by remember { mutableStateOf<String?>(null) }

    val inputRequester = remember { FocusRequester() }

    AppLayout(
        title = description,
        selectedIcon = BottomBar.MORE.value,
        navigateToMore = {
            action = Routes.MORE
        },
        navigateToHome = {
            action = Routes.HOME
        },
        navigateBack = {
            action = "back"
        },

        navigateToStock = {
            action = Routes.STOCK
        },
        navigateToExecutions = {
            action = Routes.DIRECT_EXECUTION_SCREEN
        },
        navigateToMaintenance = {
            action = Routes.MAINTENANCE
        }

    ) { _, showSnackBar ->
        if (preMeasurementViewModel.message != null) {
            showSnackBar(preMeasurementViewModel.message!!, null) {
                preMeasurementViewModel.message = null
            }
            preMeasurementViewModel.message = null
        }

        if (action == "SEND") {
            Confirm(
                title = "Confirme a ação",
                body = "Deseja finalizar o ponto atual?",
                confirm = {
                    val hasNumber = Regex("""\d+""").containsMatchIn(
                        preMeasurementViewModel.street?.address ?: ""
                    )
                    val hasSN =
                        Regex("""(?i)\bS[\./\\]?\s?N\b""").containsMatchIn(
                            preMeasurementViewModel.street?.address ?: ""
                        )

                    val invalidItems = preMeasurementViewModel.streetItems
                        .filter { listOf("0", "0.0").contains(it.measuredQuantity) }

                    if (preMeasurementViewModel.street?.address?.isBlank() == true) {
                        preMeasurementViewModel.message =
                            "Você esqueceu de preencher o endereço! Por favor, informe a Rua, Nº - Bairro atual"
                        action = null
                        inputRequester.requestFocus()
                        return@Confirm
                    } else if (!hasNumber && !hasSN) {
                        preMeasurementViewModel.message =
                            "Número do endereço ausente! Por favor, informe o número do endereço ou indique que é 'S/N'."
                        inputRequester.requestFocus()
                        action = null
                        return@Confirm
                    } else if (preMeasurementViewModel.streetItems.isEmpty()) {
                        preMeasurementViewModel.message =
                            "Nenhum item selecionado! Por favor, selecione os itens."
                        action = null
                        return@Confirm
                    } else if (invalidItems.isNotEmpty()) {
                        val invalidContractReferenceItemId =
                            invalidItems.first().contractReferenceItemId
                        val invalidItemName =
                            items.find { it.contractReferenceItemId == invalidContractReferenceItemId }?.description

                        preMeasurementViewModel.message =
                            "o item $invalidItemName está com o valor igual a zero, corrija para prosseguir"
                        action = null
                        return@Confirm
                    } else if (preMeasurementViewModel.street?.photoUri == null) {
                        preMeasurementViewModel.message =
                            "Antes de continuar é necessário tirar uma foto"
                        action = null
                        return@Confirm
                    }

                    preMeasurementViewModel.nextStep = true
                    action = null
                },
                cancel = {
                    action = null
                },
            )
        } else if (action != null) {
            ConfirmNavigation(
                action!!,
                navController
            ) {
                action = null
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 150.dp)// deixa espaço pros botões
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
                        value = preMeasurementViewModel.street?.address ?: "",
                        onValueChange = {
                            preMeasurementViewModel.street = preMeasurementViewModel.street?.copy(
                                address = it
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .focusRequester(inputRequester)
                            .focusable()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Selecione os itens da Pré-medição",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Switch(
                                modifier = Modifier.size(40.dp),
                                checked = preMeasurementViewModel.autoCalculate,
                                onCheckedChange = {
                                    preMeasurementViewModel.toggleAutoCalculate(items)
                                }
                            )
                            Text(
                                text = "Auto-calcular",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }
                items(
                    items = items,
                    key = { it.contractReferenceItemId }
                ) {
                    ContractItem(
                        item = it,
                        preMeasurementViewModel = preMeasurementViewModel,
                        items = items
                    )
                }
            }
            var searchQuery by remember { mutableStateOf("") }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Pesquisar Item...",
                        style = MaterialTheme.typography.bodySmall.copy( // Texto menor
                            fontSize = 13.sp
                        )
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter) // <-- Aqui dentro de um Box
                    .padding(bottom = 90.dp)
                    .fillMaxWidth(0.9f) // ajusta a largura
                    .height(48.dp),     // ajusta a altura

                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy( // Texto menor
                    fontSize = 13.sp
                ),
            )

            FloatingActionButton(
                onClick = {
                    val newUri = createFile() // Gera um novo Uri
                    fileUri.value = newUri // Atualiza o estado
                    launcher.launch(newUri) // Usa a variável temporária, garantindo que o valor correto seja usado
                },
                modifier = Modifier
                    .align(Alignment.BottomStart) // <-- Aqui dentro de um Box
                    .padding(8.dp)
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
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // <-- Aqui dentro de um Box
                    .padding(10.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.inverseSurface)
                        .padding(20.dp)
                ) {
                    Text(
                        "CONTINUAR",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

            }

//            if (alertModal) {
//                Alert(
//                    title = alertMessage["title"] ?: "",
//                    body = alertMessage["body"] ?: "",
//                    confirm = {
//                        closeAlertModal()
//                    })
//            }
//
//            if (openConfirmModal) {
//                Confirm(
//                    body = alertMessage["body"] ?: "",
//                    confirm = {
//                        confirmModal(action)
//                    },
//                    cancel = {
//                        confirmModal("CLOSE")
//                    }
//                )
//            }


        }
    }
}

@Composable
fun ContractItem(
    item: Item,
    preMeasurementViewModel: PreMeasurementViewModel,
    items: List<Item>
) {
    val quantity by remember(preMeasurementViewModel.streetItems, item) {
        derivedStateOf {
            preMeasurementViewModel.streetItems.find {
                it.contractReferenceItemId == item.contractReferenceItemId
            }?.measuredQuantity ?: BigDecimal.ZERO.toString()
        }
    }

//    var text by remember(item.contractReferenceItemId) {
//        mutableStateOf(
//            TextFieldValue(
//                quantity
//            )
//        )
//    }

    val selected by remember(preMeasurementViewModel.streetItems, item) {
        derivedStateOf {
            preMeasurementViewModel.streetItems.any {
                it.contractReferenceItemId == item.contractReferenceItemId
            }
        }
    }

    ListItem(
        headlineContent = {
            Text(
                text = item.description,
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
                    item.type?.lowercase() == "serviço" -> {
                        Tag(
                            text = "Serviço",
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.Info
                        )
                    }

                    item.type?.lowercase() == "projeto" -> {
                        Tag(
                            text = "Projeto",
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.Info
                        )
                    }

                }

            }
        },
        supportingContent = {
            AnimatedVisibility(visible = selected) {
                OutlinedTextField(
                    value = TextFieldValue(quantity, TextRange(quantity.length + 1)),
                    onValueChange = { newValue ->
                        val sanitized = sanitizeDecimalInput(newValue.text)

                        preMeasurementViewModel.setQuantity(
                            items,
                            item,
                            TextFieldValue(sanitized, TextRange(sanitized.length)).text
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
                imageVector = Icons.Default.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            IconToggleButton(
                checked = selected,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        preMeasurementViewModel.addItem(
                            item,
                            items
                        )
                    } else {
                        preMeasurementViewModel.removeItem(
                            items,
                            item
                        )
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (selected)
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
                    tint = if (selected)
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
fun PrevPMStreet() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val viewModel = PreMeasurementViewModel()

    StreetItemsContent(
        description = "Pré-mediçao",
        preMeasurementViewModel = viewModel,
        context = fakeContext,
        navController = rememberNavController(),
        items = listOf(
            Item(
                contractReferenceItemId = 1,
                description = "BRAÇO DE 3,5",
                nameForImport = "BRAÇO DE 3,5",
                type = "BRAÇO",
                linking = null,
                itemDependency = null
            ),
            Item(
                contractReferenceItemId = 2,
                description = "BRAÇO DE 3,5",
                nameForImport = "BRAÇO DE 3,5",
                type = "SERVIÇO",
                linking = null,
                itemDependency = null
            )
        )
    )

}
