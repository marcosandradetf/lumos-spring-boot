package com.lumos.ui.maintenance

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.domain.model.MaintenanceStreetItem
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.service.AddressService
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.Alert
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.utils.Utils
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

@Composable
fun StreetMaintenanceContent(
    maintenanceId: UUID,
    navController: NavHostController,
    loading: Boolean,
    lastRoute: String?,
    back: () -> Unit,
    saveStreet: (MaintenanceStreet, List<MaintenanceStreetItem>, CoordinatesService) -> Unit,
    streetCreated: Boolean,
    newStreet: () -> Unit,
    stockData: List<MaterialStock>,
    contractor: String?,
    message: String?,
    setMessage: (String) -> Unit,
    coordinates: CoordinatesService,
    addressService: AddressService
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val navigateBack: (() -> Unit) =
        if (lastRoute == Routes.HOME) {
            {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.MAINTENANCE) { inclusive = true }
                }
            }
        } else {
            back
        }

    var onlyHasStock by remember { mutableStateOf(true) }

    val types = stockData
        .filter {
            !onlyHasStock || BigDecimal(it.stockAvailable) > BigDecimal.ZERO
        }
        .distinctBy { it.type }
        .map { it.type }
        .sortedBy { it }
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

    var maintenanceStreetId by remember { mutableStateOf(UUID.randomUUID()) }
    var alertModal by remember { mutableStateOf(false) }
    var confirmModal by remember { mutableStateOf(false) }
    val alertMessage = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem", "body" to "Você está na rua da execução neste momento?"
        )
    }

    var street by remember {
        mutableStateOf(
            MaintenanceStreet(
                maintenanceStreetId = maintenanceStreetId.toString(),
                maintenanceId = maintenanceId.toString(),
                address = "",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            )
        )
    }

    var items by remember {
        mutableStateOf<List<MaintenanceStreetItem>>(emptyList())
    }

    val selectedIds = remember(items) { items.map { it.materialStockId }.toSet() }

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
    var reasonError by remember { mutableStateOf<String?>(null) }
    var loadingCoordinates by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadingCoordinates = true
        val (lat, long) = coordinates.execute()
        if (lat != null && long != null) {
            val addr = addressService.execute(lat, long)

            val streetName = addr?.get(0)
            val neighborhood = addr?.get(1)
            val city = addr?.get(2)

            if (streetName != null) {
                street.address = "$streetName, $neighborhood, $city"
                address = street.address
            } else {
                setMessage("Geolocalização salva! Não foi possível identificar o endereço. Insira manualmente.")
            }

            street.latitude = lat
            street.longitude = lat
        }
        loadingCoordinates = false
    }

    AppLayout(
        title = "Manutenção em ${Utils.abbreviate(contractor.toString())}",
        selectedIcon = BottomBar.MAINTENANCE.value,
        navigateBack = navigateBack,
        navigateToHome = {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.MAINTENANCE) { inclusive = true }
            }
        },
        navigateToMore = {
            navController.navigate(Routes.MORE) {
                popUpTo(Routes.MAINTENANCE) { inclusive = true }
            }
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK) {
                popUpTo(Routes.MAINTENANCE) { inclusive = true }
            }
        },
        navigateToExecutions = {
            navController.navigate(Routes.INSTALLATION_HOLDER) {
                popUpTo(Routes.MAINTENANCE) { inclusive = true }
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
            Confirm(body = "Deseja finalizar essa manutenção?", confirm = {
                confirmModal = false
                saveStreet(street, items, coordinates)
            }, cancel = {
                confirmModal = false
            })
        }

        if (loading) {
            Loading()
        } else if (loadingCoordinates) {
            Loading("Tentando carregar coordenadas")
        } else if (streetCreated) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth(fraction = 0.5f),
                    onClick = {
                        maintenanceStreetId = UUID.randomUUID()
                        items = emptyList()
                        street = MaintenanceStreet(
                            maintenanceStreetId = maintenanceStreetId.toString(),
                            maintenanceId = maintenanceId.toString(),
                            address = address,
                            latitude = null,
                            longitude = null,
                            comment = null,
                            lastPower = null,
                            lastSupply = null,
                            currentSupply = null,
                            reason = null
                        )
                        newStreet()

                        scope.launch {
                            val (lat, long) = coordinates.execute()
                            if (lat != null && long != null) {
                                street = street.copy(
                                    latitude = lat,
                                    longitude = long,
                                )
                            }
                        }
                    }
                ) {
                    Text("Inserir outro ponto")
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth(fraction = 0.5f),
                    onClick = {
                        back()
                    }
                ) {
                    Text("Voltar")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus() // ⌨️ Fecha o teclado
                        })
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = street.address,
                    onValueChange = {
                        street = street.copy(address = it)
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
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
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

                Box {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(16.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.background,
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(16.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }
                Text(
                    text = "Deslize horizontalmente para ver mais",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Selecione os materiais",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        filteredStock,
                        key = { it.materialStockId }
                    ) { material ->

                        val checked =
                            items.any { it.materialStockId == material.materialStockId }

                        OutlinedCard(
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    if (checked)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    else
                                        MaterialTheme.colorScheme.surface,
                                contentColor = if (checked)
                                    Color.White
                                else
                                    MaterialTheme.colorScheme.onBackground,

                                ),
                            border = if (checked)
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(.9f))
                            else
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onBackground.copy(.5f)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(350.dp)
                                .height(110.dp)
                                .animateContentSize()
                                .clickable {
                                    // espelha exatamente o toggle atual
                                    if (BigDecimal(material.stockAvailable) == BigDecimal.ZERO &&
                                        material.truckStockControl
                                    ) {
                                        alertMessage["title"] =
                                            "Material sem estoque disponível"
                                        alertMessage["body"] =
                                            "Para selecionar esse material é necessário haver estoque disponível."
                                        alertModal = true
                                        return@clickable
                                    }

                                    val isChecked = !checked
                                    val type = material.type

                                    items = if (isChecked) {
                                        val isScrew =
                                            material.materialName.trim()
                                                .contains("parafuso", ignoreCase = true)

                                        val filteredItems = if (!isScrew) {
                                            items.filterNot {
                                                stockData.find { stock ->
                                                    stock.materialStockId == it.materialStockId
                                                }?.type == type &&
                                                        it.maintenanceStreetId == maintenanceStreetId.toString() &&
                                                        it.maintenanceId == maintenanceId.toString()
                                            }
                                        } else items

                                        if (material.materialName
                                                .contains("led", ignoreCase = true)
                                        ) {
                                            street = street.copy(
                                                currentSupply = material.materialBrand,
                                                lastPower = material.materialPower
                                            )
                                        }

                                        filteredItems + MaintenanceStreetItem(
                                            maintenanceId = maintenanceId.toString(),
                                            maintenanceStreetId = maintenanceStreetId.toString(),
                                            materialStockId = material.materialStockId,
                                            quantityExecuted = BigDecimal.ONE.toString(),
                                            truckStockControl = material.truckStockControl
                                        )
                                    } else {
                                        items.filterNot {
                                            it.materialStockId == material.materialStockId &&
                                                    it.maintenanceStreetId == maintenanceStreetId.toString() &&
                                                    it.maintenanceId == maintenanceId.toString()
                                        }
                                    }
                                },

                            ) {

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {

                                Column(
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxSize()
                                ) {

                                    // Nome
                                    Text(
                                        text = material.materialName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        minLines = 2,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // KPI
                                    if (checked) {
                                        Text(
                                            text = "Selecionado",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    } else if (material.truckStockControl) {
                                        Text(
                                            text = "Estoque: ${material.stockQuantity} ${material.requestUnit}",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    } else {
                                        Text(
                                            text = "N/A",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }

                                // Check overlay
                                if (checked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                if (hasLed) {
                    Text(
                        text = "Informações referentes a LED",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column {
                        OutlinedTextField(
                            value = street.lastSupply ?: "",
                            onValueChange = {
                                street = street.copy(lastSupply = it)
                            },
                            label = {
                                Text(
                                    "Fabricante anterior",
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
                            supportingText = {}
                        )

                        OutlinedTextField(
                            isError = lastPowerError != null,
                            value = street.lastPower ?: "",
                            onValueChange = {
                                street = street.copy(lastPower = it.uppercase())
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
                                        modifier = Modifier.padding(start = 16.dp, top = 7.dp)
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
                            value = street.currentSupply ?: "",
                            onValueChange = {
                                street = street.copy(currentSupply = it)
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
                                        modifier = Modifier.padding(start = 16.dp, top = 7.dp)
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            isError = reasonError != null,
                            value = street.reason ?: "",
                            onValueChange = {
                                street = street.copy(reason = it)
                                reasonError = null
                            },
                            label = {
                                Text(
                                    "Motivo da troca",
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
                                if (reasonError != null) {
                                    Text(
                                        text = reasonError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 7.dp)
                                    )
                                }
                            }
                        )
                    }
                } else {
                    Text(
                        text = "Informação referente a potência",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        isError = lastPowerError != null,
                        value = street.lastPower ?: "",
                        onValueChange = {
                            street = street.copy(lastPower = it.uppercase())
                            lastPowerError = null
                        },
                        label = {
                            Text(
                                "Potência atual (W)",
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
                                    modifier = Modifier.padding(start = 16.dp, top = 7.dp)
                                )
                            }
                        }
                    )
                }

                Text(
                    text = "Comentários adiconais",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = street.comment ?: "",
                    onValueChange = {
                        street = street.copy(comment = it)
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
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        val hasNumber = Regex("""\d+""").containsMatchIn(street.address)
                        val hasSN =
                            Regex("""(?i)\bS[\./\\]?\s?N\b""").containsMatchIn(street.address)

                        var error = false
                        if (street.address.isBlank()) {
                            alertMessage["title"] = "Você esqueceu de preencher o endereço"
                            alertMessage["body"] = "Por favor, informe a Rua, Nº - Bairro atual"
                            alertModal = true
                            return@Button
                        } else if (!hasNumber && !hasSN) {
                            alertMessage["title"] = "Número do endereço ausente"
                            alertMessage["body"] =
                                "Por favor, informe o número do endereço ou indique que é 'S/N'."
                            alertModal = true
                            return@Button
                        } else if (items.isEmpty()) {
                            alertMessage["title"] = "Nenhum material selecionado"
                            alertMessage["body"] = "Por favor, selecione os materiais."
                            alertModal = true
                            return@Button
                        }

                        if (hasLed) { // verificar se selecionou led e validar campos
                            if (street.lastPower.isNullOrBlank()) {
                                lastPowerError = "Informe a potência anterior."
                                error = true
                            }
                            if (street.currentSupply.isNullOrBlank()) {
                                currentSupplyError = "Informe o fabricante atual."
                                error = true
                            }
                            if (street.reason.isNullOrBlank()) {
                                reasonError = "Informe o motivo da troca."
                                error = true
                            }
                        } else if (street.lastPower.isNullOrBlank()) {
                            lastPowerError = "Informe a potência atual."
                            error = true
                        }


                        if (error) return@Button

                        confirmModal = true
                    }
                ) {
                    Text("Salvar manutenção")
                }
            }
        }

    }

}

@Composable
fun StockStatusChip(material: MaterialStock) {
    when {
        BigDecimal(material.stockAvailable) == BigDecimal.ZERO -> {
            AssistChip(
                onClick = {},
                label = { Text("Sem estoque") },
                leadingIcon = {
                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color.Red.copy(alpha = 0.1f),
                    labelColor = Color.Red
                )
            )
        }

        BigDecimal(material.stockAvailable) <= BigDecimal.TEN -> {
            AssistChip(
                onClick = {},
                label = { Text("Baixo estoque") },
                leadingIcon = {
                    Icon(Icons.Default.Warning, null, Modifier.size(16.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                    labelColor = Color(0xFFFF9800)
                )
            )
        }

        else -> {
            AssistChip(
                onClick = {},
                label = { Text("Disponível") },
                leadingIcon = {
                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                }
            )
        }
    }
}


@Preview
@Composable
fun PrevStreetMaintenance() {
    StreetMaintenanceContent(
        maintenanceId = UUID.randomUUID(),
        navController = rememberNavController(),
        loading = false,
        lastRoute = null,
        back = {

        },
        saveStreet = { _: MaintenanceStreet, _: List<MaintenanceStreetItem>, _ -> },
        streetCreated = false,
        newStreet = {},
        stockData = listOf(
            MaterialStock(
                materialId = 1,
                materialStockId = 11,
                materialName = "LUMINÁRIA LED",
                stockQuantity = "12",
                stockAvailable = "12",
                requestUnit = "UN",
                type = "LED",
                truckStockControl = false,
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
        ),
        contractor = "",
        message = null,
        setMessage = {},
        coordinates = CoordinatesService(
            LocalContext.current,
            locationProvider = LocationServices.getFusedLocationProviderClient(LocalContext.current)
        ),
        addressService = AddressService(LocalContext.current),
    )
}