package com.lumos.ui.directexecution

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.model.ReserveMaterialJoin
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.Alert
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.utils.Utils
import com.lumos.viewmodel.DirectExecutionViewModel
import java.io.File
import java.math.BigDecimal

@Composable
fun StreetInstallationNoWorkOrder(
    navController: NavHostController,
    coordinates: CoordinatesService,
    addressService: AddressService,
    viewModel: DirectExecutionViewModel,
    context: Context
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val stockData by viewModel.stock.collectAsState(emptyList())

    var onlyHasStock by remember { mutableStateOf(true) }

    val types = remember(stockData, onlyHasStock) {
        stockData
            .filter { !onlyHasStock || BigDecimal(it.stockAvailable) > BigDecimal.ZERO }
            .map { it.type }
            .distinct()
            .sortedBy { it.uppercase() }
    }

    val (selectedOption, onOptionSelected) = remember {
        mutableStateOf(types.firstOrNull() ?: "")
    }

    var searchQuery by remember { mutableStateOf("") }
    val normalizedQuery = searchQuery.replace("\\s".toRegex(), "").lowercase()

    val filteredStock = stockData.filter { item ->
        val name = item.materialName.replace("\\s".toRegex(), "").lowercase()

        (selectedOption.isBlank() || item.type == selectedOption) &&
                (!onlyHasStock || BigDecimal(item.stockAvailable) > BigDecimal.ZERO) &&
                (normalizedQuery.isBlank() || name.contains(normalizedQuery))
    }.distinctBy { it.materialStockId }

    var alertModal by remember { mutableStateOf(false) }
    var confirmModal by remember { mutableStateOf(false) }
    val alertMessage = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem", "body" to "Você está na rua da execução neste momento?"
        )
    }

    val selectedIds = remember(
        viewModel.streetItems
    ) { viewModel.streetItems.map { it.materialStockId }.toSet() }

    val hasLed by remember(selectedIds, stockData) {
        derivedStateOf {
            stockData.any {
                it.materialStockId in selectedIds &&
                        it.materialName.contains("led", ignoreCase = true)
            }
        }
    }

    var lastPowerError by remember { mutableStateOf<String?>(null) }
    var currentSupplyError by remember { mutableStateOf<String?>(null) }
    var loadingCoordinates by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    val message = viewModel.errorMessage
    val loading = viewModel.isLoading

    val fileUri: MutableState<Uri?> = remember {
        mutableStateOf(
            viewModel.street?.photoUri?.toUri()
        )
    }
    val imageSaved = remember {
        mutableStateOf(
            viewModel.street?.photoUri != null
        )
    }
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
                    viewModel.street =
                        viewModel.street?.copy(photoUri = uri.toString())
                    imageSaved.value = true
                }
            } else {
                Log.e("ImageDebug", "Erro ao tirar foto.")
            }
        }

    var nextStep by rememberSaveable { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        viewModel.onExecutionScreen()

        loadingCoordinates = true
        val (lat, long) = coordinates.execute()
        if (lat != null && long != null) {
            val addr = addressService.execute(lat, long)

            val streetName = addr?.get(0)
            val neighborhood = addr?.get(1)
            val city = addr?.get(2)

            if (streetName != null) {
                viewModel.street =
                    viewModel.street?.copy(
                        address = "$streetName, $neighborhood, $city"
                    )
                address = viewModel.street?.address ?: ""
            } else {
                viewModel.errorMessage =
                    "Geolocalização salva! Não foi possível identificar o endereço. Insira manualmente."
            }

            viewModel.street =
                viewModel.street?.copy(
                    latitude = lat,
                    longitude = long
                )
        }
        loadingCoordinates = false
    }

    // Sempre que a lista 'types' for recalculada...
    LaunchedEffect(types) {
        // Se a lista tem dados E o que está selecionado não existe mais na lista (ou está vazio)
        if (types.isNotEmpty() && !types.contains(selectedOption)) {
            onOptionSelected(types.first())
        }
    }

    BackHandler {
        nextStep = false
    }

    AppLayout(
        title = "Nova Rua",
        selectedIcon = BottomBar.EXECUTIONS.value,
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
        navigateToExecutions = {
            navController.navigate(Routes.INSTALLATION_HOLDER) {
                popUpTo(Routes.DIRECT_EXECUTION_FLOW) { inclusive = true }
            }
        }
    ) { _, showSnackBar ->

        if (message != null) {
            showSnackBar(message, null, null)
        }

        if (alertModal) {
            Alert(
                title = alertMessage["title"] ?: "", body = alertMessage["body"] ?: "",
                confirm = {
                    alertModal = false
                })
        }

        if (confirmModal) {
            Confirm(body = "Deseja finalizar essa rua?", confirm = {
                confirmModal = false
                viewModel.saveAndPost(coordinates)
            }, cancel = {
                confirmModal = false
            })
        }

        if (loading) {
            Loading()
        } else if (loadingCoordinates) {
            Loading("Tentando carregar coordenadas")
        } else if (viewModel.hasPosted) {
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
                        text = "Essa rua está concluído.\nFinalize todos os pontos e depois toque em Gerenciar Instalação para enviar a instalação.",
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
                            nextStep = false
                            imageSaved.value = false
                            viewModel.startNewExecution(address)
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

                    Button(
                        onClick = {
                            nextStep = false
                            imageSaved.value = false
                            viewModel.startNewExecution(address)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text(
                            "Nova Rua"
                        )
                    }

                }

            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            // 2. Esconde o teclado e remove o foco de qualquer TextField
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                    }
            ) {

                if (!viewModel.acceptedResponsibilityTerm) {
                    LegalResponsibilityDialog(
                        onAccept = {
                            viewModel.acceptedResponsibilityTerm = true
                        }
                    )
                }

                TextField(
                    value = viewModel.street?.address ?: "",
                    onValueChange = {
                        viewModel.street =
                            viewModel.street?.copy(address = it)
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

                Text(
                    text = Utils.abbreviate(
                        viewModel.contractor ?: "Sem Contrato"
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )

                AnimatedVisibility(
                    visible = !nextStep,
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Filtre os materiais",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Switch(
                                    modifier = Modifier.size(40.dp),
                                    checked = onlyHasStock,
                                    onCheckedChange = {
                                        onlyHasStock = !onlyHasStock
                                    })

                                Text(
                                    text =
                                        "Somente com estoque",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }


                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0.0f to Color.Transparent, // Opcional: fade na esquerda
                                            0.05f to Color.Black,      // Fica sólido rápido
                                            0.85f to Color.Black,      // Começa a sumir no final
                                            1.0f to Color.Transparent  // Fica transparente na borda direita
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                },
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(types) { type ->
                                FilterChip(
                                    selected = type == selectedOption,
                                    onClick = { onOptionSelected(type) },
                                    label = { Text(type) },
                                    leadingIcon = if (type == selectedOption) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(
                                            .9f
                                        ),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
                            }
                        }

                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            text = "Selecione os materiais e quantidades",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(2.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                filteredStock,
                                key = { it.materialStockId }
                            ) { material ->

                                val isChecked =
                                    viewModel.streetItems
                                        .any { it.materialStockId == material.materialStockId }

                                MaterialSelectionCard(
                                    material = material,
                                    checked = isChecked,
                                    currentQuantity = viewModel.streetItems.find { it.materialStockId == material.materialStockId }
                                        ?.quantityExecuted ?: if (material.truckStockControl) "" else "1",
                                    onCheckChange = { checked ->
                                        if (checked) {
                                            if (BigDecimal(material.stockAvailable) == BigDecimal.ZERO &&
                                                material.truckStockControl
                                            ) {
                                                alertMessage["title"] =
                                                    "Material sem estoque disponível"

                                                alertMessage["body"] =
                                                    "Para selecionar esse material é necessário haver estoque disponível."

                                                alertModal = true
                                                return@MaterialSelectionCard
                                            }

                                            if (material.materialName
                                                    .contains("led", ignoreCase = true)
                                            ) {
                                                viewModel.street =
                                                    viewModel.street?.copy(
                                                        currentSupply = material.materialBrand,
                                                        lastPower = material.materialPower
                                                    )
                                            }

                                            viewModel.streetItems += DirectExecutionStreetItem(
                                                materialStockId = material.materialStockId,
                                                materialName = material.materialName,
                                                directStreetId = viewModel.street?.directStreetId
                                                    ?: -1,
                                                quantityExecuted = if (material.truckStockControl) "" else "1",
                                                reserveId = -1,
                                                contractItemId = -1
                                            )
                                        } else {
                                            viewModel.streetItems =
                                                viewModel.streetItems.filterNot {
                                                    it.materialStockId == material.materialStockId
                                                }
                                        }
                                    },
                                    onQuantityChange = { novaQtd ->
                                        val updatedItem =
                                            viewModel.streetItems.find { it.materialStockId == material.materialStockId }

                                        viewModel.streetItems = viewModel.streetItems.filterNot {
                                            it.materialStockId == material.materialStockId
                                        }

                                        updatedItem?.let {
                                            viewModel.streetItems += it.copy(quantityExecuted = novaQtd)
                                        }
                                    }
                                )
                            }
                        }
                    }

                }

                AnimatedVisibility(
                    visible = nextStep,
                    modifier = Modifier.weight(1f)
                ) {
                    Column {


                        if (hasLed) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Informações referentes a LED",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                OutlinedTextField(
                                    isError = lastPowerError != null,
                                    value = viewModel.street?.lastPower ?: "",
                                    onValueChange = {
                                        viewModel.street =
                                            viewModel.street?.copy(lastPower = it.uppercase())
                                        lastPowerError = null
                                    },
                                    label = {
                                        Text(
                                            "Potência anterior (W)",
                                            style = MaterialTheme.typography.bodySmall.copy( // Texto menor
                                                fontSize = 14.sp
                                            )
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall.copy( // Texto menor
                                        fontSize = 14.sp
                                    ),
                                    supportingText = {
                                        if (lastPowerError != null) {
                                            Text(
                                                text = lastPowerError ?: "",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(
                                                    start = 16.dp,
                                                    top = 7.dp
                                                )
                                            )
                                        }
                                    }
                                )

                                OutlinedTextField(
                                    label = {
                                        Text(
                                            "Fabricante atual (Novo)",
                                            style = MaterialTheme.typography.bodySmall.copy( // Texto menor
                                                fontSize = 14.sp
                                            )
                                        )
                                    },
                                    isError = currentSupplyError != null,
                                    value = viewModel.street?.currentSupply ?: "",
                                    onValueChange = {
                                        viewModel.street =
                                            viewModel.street?.copy(currentSupply = it)
                                        currentSupplyError = null
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall.copy( // Texto menor
                                        fontSize = 14.sp
                                    ),
                                    supportingText = {
                                        if (currentSupplyError != null) {
                                            Text(
                                                text = currentSupplyError ?: "",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(
                                                    start = 16.dp,
                                                    top = 7.dp
                                                )
                                            )
                                        }
                                    }
                                )

                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Comentários adiconais",
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedTextField(
                                value = viewModel.street?.comment ?: "",
                                onValueChange = {
                                    viewModel.street =
                                        viewModel.street?.copy(comment = it)
                                },
                                label = {
                                    Text(
                                        "Campo para observações",
                                        style = MaterialTheme.typography.bodySmall.copy( // Texto menor
                                            fontSize = 14.sp
                                        )
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                textStyle = MaterialTheme.typography.bodySmall.copy( // Texto menor
                                    fontSize = 14.sp
                                ),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        modifier = Modifier
                            .width(200.dp)
                            .height(70.dp)
                            .padding(10.dp),
                        onClick = {
                            if (!nextStep) {
                                if (viewModel.streetItems.isEmpty()) {
                                    showSnackBar("Nenhum material selecionado. Por favor, selecione os materiais.", null, null)
                                    return@Button // Para aqui! Não faz mais nada.
                                }

                                val invalidItem = viewModel.streetItems.find {
                                    it.quantityExecuted == "0" || it.quantityExecuted.isBlank()
                                }

                                if (invalidItem != null) {
                                    showSnackBar("Quantidade inválida. O material '${invalidItem.materialName}' não pode ter quantidade zerada ou vazia.", null, null)
                                    return@Button
                                }

                                nextStep = true
                                return@Button
                            }

                            val hasNumber = Regex("""\d+""").containsMatchIn(
                                viewModel.street?.address ?: ""
                            )
                            val hasSN = Regex(
                                """(?i)\bS[\./\\]?\s?N\b"""
                            ).containsMatchIn(
                                viewModel.street?.address ?: ""
                            )

                            var error = false
                            if (viewModel.street?.address?.isBlank() == true) {
                                showSnackBar("Você esqueceu de preencher o endereço. Por favor, informe a Rua, Nº - Bairro atual", null, null)
                                return@Button
                            } else if (!hasNumber && !hasSN) {
                                showSnackBar("Número do endereço ausente. Por favor, informe o número do endereço ou indique que é 'S/N'.", null, null)
                                return@Button
                            }

                            if (hasLed) { // verificar se selecionou led e validar campos
                                if (viewModel.street?.lastPower.isNullOrBlank()) {
                                    lastPowerError = "Informe a potência anterior."
                                    error = true
                                }
                                if (viewModel.street?.currentSupply.isNullOrBlank()) {
                                    currentSupplyError = "Informe o fabricante atual."
                                    error = true
                                }
                            }

                            if(viewModel.street?.photoUri == null) {
                                showSnackBar("Foto ausente. Por favor, tire uma foto da instalação.", null, null)
                                error = true
                            }

                            if (error) return@Button

                            confirmModal = true
                        }
                    ) {
                        Text(if (!nextStep) "Continuar" else "Salvar rua")
                    }

                    FloatingActionButton(
                        onClick = {
                            val newUri = createFile() // Gera um novo Uri
                            fileUri.value = newUri // Atualiza o estado
                            launcher.launch(newUri) // Usa a variável temporária, garantindo que o valor correto seja usado
                        },
                        modifier = Modifier
                            .padding(10.dp)
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
                }
            }

        }
    }

}

@Composable
fun MaterialSelectionCard(
    material: MaterialStock,
    currentQuantity: String,
    checked: Boolean,
    onCheckChange: (Boolean) -> Unit,
    onQuantityChange: (String) -> Unit,
) {
    val hasStock =
        !material.truckStockControl || BigDecimal(material.stockAvailable) > BigDecimal.ZERO
    val cardAlpha = if (hasStock) 1f else 0.5f

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = if (checked) 2.dp else 1.dp,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth() // Adaptável a telas landscape/portrait
            .height(130.dp)
            .alpha(cardAlpha)
            .animateContentSize()
            .clickable(enabled = hasStock) { onCheckChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Título do Material
                Text(
                    text = material.materialName.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.85f) // Espaço para o ícone de check
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Informação de Estoque ou Status
                    Column {
                        if (material.truckStockControl) {
                            Text(
                                text = "DISPONÍVEL: ${material.stockAvailable} ${material.requestUnit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text(
                                text = "CONTROLE AUTOMÁTICO",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Input de Quantidade (Só aparece se checked e controlado)
                    if (checked && material.truckStockControl) {
                        val isQuantityError = currentQuantity.isBlank() || currentQuantity == "0"

                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isQuantityError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            ),
                            onClick = { /* Opcional: Abrir um seletor numérico */ }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar quantidade",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.width(8.dp))

                                // Usando um BasicTextField para que ele possa digitar a quantidade
                                BasicTextField(
                                    value = currentQuantity,
                                    onValueChange = { novaQtd ->
                                        // Filtra para aceitar apenas números e ponto/vírgula
                                        if (novaQtd.all { it.isDigit() }) {
                                            onQuantityChange(novaQtd)
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.width(IntrinsicSize.Min) // Ajusta ao tamanho do número
                                )

                                Spacer(Modifier.width(4.dp))

                                Text(
                                    text = material.requestUnit, // Ou material.unitMeasurement se você tiver essa info
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Indicador de Seleção no Canto Superior
            if (checked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                )
            }

            // Alerta visual se não houver estoque
            if (!hasStock) {
                Text(
                    text = "SEM ESTOQUE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun LegalResponsibilityDialog(
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = onAccept
            ) {
                Text("LI E ESTOU CIENTE")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Termo de Responsabilidade",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Text(
                text = "Ao prosseguir, você assume responsabilidade pelo correto registro das informações no sistema e pela instalação adequada dos materiais elétricos em campo. " +
                        "A utilização ou instalação de materiais com especificações técnicas divergentes das orientações da empresa, sem validação do responsável técnico, é de responsabilidade da equipe executora. " +
                        "Em caso de dúvida quanto às especificações, consulte o responsável técnico antes de prosseguir.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    )
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun PrevStreetMaintenance() {
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
    val mockStock = listOf(
        MaterialStock(
            materialId = 1,
            materialStockId = 11,
            materialName = "LUMINÁRIA LED HGE 100W",
            stockQuantity = "12",
            stockAvailable = "12",
            requestUnit = "UN",
            type = "LED",
            truckStockControl = true,
            parentMaterialId = 3,
            materialBaseName = "LUMINÁRIA",
            materialPower = "",
            materialBrand = "",
        ),
        MaterialStock(
            materialId = 2,
            materialStockId = 22,
            materialName = "LÂMPADA DE SÓDIO TUBULAR",
            stockQuantity = "15",
            stockAvailable = "10",
            requestUnit = "UN",
            type = "LÂMPADA",
            truckStockControl = false,
            parentMaterialId = 2,
            materialBaseName = "LÂMPADA",
            materialPower = "",
            materialBrand = "",
        ),
        MaterialStock(
            materialId = 3,
            materialStockId = 33,
            materialName = "LÂMPADA DE MERCÚRIO",
            stockQuantity = "62",
            stockAvailable = "62",
            requestUnit = "UN",
            type = "LÂMPADA",
            truckStockControl = true,
            parentMaterialId = 1,
            materialBaseName = "LÂMPADA",
            materialPower = "",
            materialBrand = "",
        ),
    )

    StreetInstallationNoWorkOrder(
        navController = rememberNavController(),
        coordinates = CoordinatesService(
            LocalContext.current,
            locationProvider = LocationServices.getFusedLocationProviderClient(LocalContext.current)
        ),
        addressService = AddressService(LocalContext.current),
        viewModel = DirectExecutionViewModel(
            null,
            null,
            null,
            null,
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
            ),
            mockStockData = mockStock
        ),
        context = LocalContext.current,
    )
}