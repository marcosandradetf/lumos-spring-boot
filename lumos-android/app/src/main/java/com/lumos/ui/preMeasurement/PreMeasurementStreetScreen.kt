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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
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
    contractId: Long,
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


    LaunchedEffect(contractId) {
        preMeasurementViewModel.loading = true

        val contract = contractViewModel.getContract(contractId)

        contract?.let { loadedContract ->
            val itemsIdsList = loadedContract.itemsIds
                ?.split("#")
                ?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()

            Log.e("IDS", itemsIdsList.toString())

            contractViewModel.loadItemsFromContract(itemsIdsList)

            contractViewModel.syncContractItems()

            preMeasurementViewModel.newPreMeasurement(contractId, loadedContract.contractor)
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

                currentAddress = "$street, - $neighborhood, $city"

                preMeasurementViewModel.street = preMeasurementViewModel.street?.copy(
                    latitude = latitude,
                    longitude = longitude,
                    address = currentAddress
                )
            }
        }
        preMeasurementViewModel.locationLoading = false
    }

    if (preMeasurementViewModel.locationLoading) {
        CurrentScreenLoading(
            navController,
            "Pré-medição - " + Utils.abbreviate(preMeasurementViewModel.measurement?.contractor.toString()),
            "Tentando carregar as coordenadas...",
            BottomBar.MORE.value
        )
    } else if (preMeasurementViewModel.loading || items.isEmpty()) {
        CurrentScreenLoading(
            navController,
            "Pré-medição - " + Utils.abbreviate(preMeasurementViewModel.measurement?.contractor.toString()),
            "Carregando...",
            BottomBar.MORE.value
        )
    } else if (preMeasurementViewModel.nextStep) {
        AppLayout(
            title = "Pré-medição - " + Utils.abbreviate(preMeasurementViewModel.measurement?.contractor.toString()),
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
                preMeasurementViewModel.clearViewModel()
                navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
            }
        ) { _, _ ->
            var triedToSubmit by remember { mutableStateOf(false) }

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
                            text = "Rua adicionada!",
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
                            preMeasurementViewModel.clearViewModel()
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
                            "Voltar a tela anterior"
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                preMeasurementViewModel.preMeasurementStreetId = UUID.randomUUID()
                                preMeasurementViewModel.street?.copy(
                                    preMeasurementStreetId = preMeasurementViewModel.preMeasurementStreetId.toString(),
                                    address = currentAddress
                                )
                                preMeasurementViewModel.streetItems.clear()
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
                            "Continuar pré-medição nessa rua"
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
                            "Enviar",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
        }
    } else
        StreetItemsContent(
            description = "Pré-medição - " + Utils.abbreviate(preMeasurementViewModel.measurement?.contractor.toString()),
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

    var action by remember { mutableStateOf("") }


    AppLayout(
        title = description,
        selectedIcon = BottomBar.MORE.value,
        navigateToMore = {
            action = Routes.MORE
//            openModal(Routes.MORE)
        },
        navigateToHome = {
            action = Routes.HOME
//            openModal(Routes.HOME)
        },
        navigateBack = {
            action = Routes.DIRECT_EXECUTION_SCREEN
//            openModal(Routes.DIRECT_EXECUTION_SCREEN)
        },

        navigateToStock = {
            action = Routes.STOCK
//            openModal(Routes.STOCK)
        },
        navigateToExecutions = {
            action = Routes.DIRECT_EXECUTION_SCREEN
//            openModal(Routes.DIRECT_EXECUTION_SCREEN)
        },
        navigateToMaintenance = {
            action = Routes.MAINTENANCE
//            openModal(Routes.MAINTENANCE)
        }

    ) { _, showSnackBar ->
        if (preMeasurementViewModel.message != null) {
            showSnackBar(preMeasurementViewModel.message!!, null, null)
            preMeasurementViewModel.message = null
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

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
                        "Selecione os itens da Pré-medição",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(10.dp))
                }
                items(
                    items = items,
                    key = { it.contractReferenceItemId }
                ) {
                    ContractItem(
                        item = it,
                        preMeasurementViewModel = preMeasurementViewModel
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
//                    openModal("SEND")
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
    preMeasurementViewModel: PreMeasurementViewModel
) {
    val quantity = remember(preMeasurementViewModel.streetItems, item) {
        derivedStateOf {
            preMeasurementViewModel.streetItems.find {
                it.contractReferenceItemId == item.contractReferenceItemId
            }?.measuredQuantity ?: BigDecimal.ZERO.toString()
        }
    }

    var text by remember(item.contractReferenceItemId) {
        mutableStateOf(
            TextFieldValue(
                quantity.value
            )
        )
    }

    val selected = remember(preMeasurementViewModel.streetItems, item) {
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
                    item.itemDependency != null -> {
                        Tag(
                            text = "Possuí serviço vínculado",
                            color = Color.Red,
                            icon = Icons.Default.Close
                        )
                    }

                    item.type?.lowercase() == "serviço" -> {
                        Tag(
                            text = "Serviço",
                            color = Color(0xFFFF9800),
                            icon = Icons.Default.Warning
                        )
                    }

                    item.type?.lowercase() == "projeto" -> {
                        Tag(
                            text = "Projeto",
                            color = Color(0xFFFF9800),
                            icon = Icons.Default.Warning
                        )
                    }

                }

            }
        },
        supportingContent = {
            AnimatedVisibility(visible = selected.value) {
                OutlinedTextField(
                    value = TextFieldValue(quantity.value, TextRange(quantity.value.length)),
                    onValueChange = { newValue ->
                        val sanitized = sanitizeDecimalInput(newValue.text)
                        text = TextFieldValue(sanitized, TextRange(sanitized.length))

                        preMeasurementViewModel.setQuantity(item.contractReferenceItemId, text.text)

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
                    if (isChecked) {
                        preMeasurementViewModel.addItem(item.contractReferenceItemId)
                    } else {
                        preMeasurementViewModel.removeItem(item.contractReferenceItemId)
                    }
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
fun PrevPMStreet() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val viewModel = PreMeasurementViewModel()

    StreetItemsContent(
        description =  "Pré-mediçao",
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
