package com.lumos.ui.maintenance

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
import com.lumos.ui.components.Tag
import com.lumos.utils.Utils
import com.lumos.utils.Utils.sanitizeDecimalInput
import com.lumos.viewmodel.MaintenanceUiState
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Composable
fun StreetMaintenanceContent(
    maintenanceId: UUID,
    navController: NavHostController,
    loading: Boolean,
    lastRoute: String?,
    back: () -> Unit,
    saveStreet: (MaintenanceStreet, List<MaintenanceStreetItem>) -> Unit,
    streetCreated: Boolean,
    newStreet: () -> Unit,
    stockData: List<MaterialStock>,
    contractor: String?,
    message: String?,
) {
    val context = LocalContext.current
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
    val coordinates = CoordinatesService(context, fusedLocationProvider)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val navigateBack: (() -> Unit) =
        if (lastRoute == Routes.HOME) {
            { navController.navigate(Routes.HOME) }
        } else {
            back
        }

    val types = stockData.distinctBy { it.type }.map { it.type }.sortedBy { it }
    val (selectedOption, onOptionSelected) = remember {
        mutableStateOf(types.firstOrNull() ?: "")
    }

    var searchQuery by remember { mutableStateOf("") }
    val normalizedQuery = searchQuery.replace("\\s".toRegex(), "").lowercase()

    val filteredStock = stockData.filter { item ->
        val name = item.materialName.replace("\\s".toRegex(), "").lowercase()
        val specs = item.specs?.replace("\\s".toRegex(), "")?.lowercase()
        val combined = name + (specs ?: "")

        (selectedOption.isBlank() || item.type == selectedOption) &&
                (normalizedQuery.isBlank() || name.contains(normalizedQuery) || combined.contains(
                    normalizedQuery
                ))
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

    val cableItem by remember(selectedIds, stockData) {
        derivedStateOf {
            stockData
                .find {
                    it.materialStockId in selectedIds && it.materialName.contains(
                        "cabo",
                        ignoreCase = true
                    )
                }
                ?.materialStockId
                ?.let { id -> items.find { it.materialStockId == id } }
        }
    }

    val screws by remember(selectedIds, stockData) {
        derivedStateOf {
            val screwIds = stockData
                .filter {
                    it.materialStockId in selectedIds &&
                            it.materialName.contains("parafuso", ignoreCase = true)
                }
                .map { it.materialStockId }

            items.filter { it.materialStockId in screwIds }
        }
    }

    var lastPowerError by remember { mutableStateOf<String?>(null) }
    var currentSupplyError by remember { mutableStateOf<String?>(null) }
    var reasonError by remember { mutableStateOf<String?>(null) }
    var cableError by remember { mutableStateOf<String?>(null) }
    val screwErrors = remember { mutableStateMapOf<Long, String?>() }
    var loadingCoordinates by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loadingCoordinates = true
        coordinates.execute { latitude, longitude ->
            if (latitude != null && longitude != null) {
                val addr = AddressService(context).execute(latitude, longitude)

                if (addr != null && addr.size >= 4) {
                    val streetName = addr[0]
                    val neighborhood = addr[1]
                    val city = addr[2]

                    street.address =
                        "$streetName, $neighborhood, $city"
                    street.latitude = latitude
                    street.longitude = longitude

                    address = street.address
                }
                loadingCoordinates = false
            } else {
                Log.e("GET Address", "Latitude ou Longitude são nulos.")
                loadingCoordinates = false
            }
        }

    }

    AppLayout(
        title = "Manutenção em ${Utils.abbreviate(contractor.toString())}",
        selectedIcon = BottomBar.MAINTENANCE.value,
        navigateBack = navigateBack,
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
    ) { _, showSnackBar ->

        if(message != null) {
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
                saveStreet(street, items)
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
                Text(
                    text = "Selecione os materiais trocados",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow {
                    items(
                        types
                    ) { type ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (type == selectedOption),
                                    onClick = { onOptionSelected(type) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (type == selectedOption),
                                onClick = null // null recommended for accessibility with screen readers
                            )
                            Text(
                                text = type,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow {
                    items(
                        filteredStock,
                        key = { it.materialStockId }
                    ) { material ->

                        val checked = items.any { it.materialStockId == material.materialStockId }

                        ListItem(
                            headlineContent = {
                                Text(
                                    text = material.materialName,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            },
                            overlineContent = {
                                Column {
                                    // Tag de disponibilidade
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        when {
                                            BigDecimal(material.stockAvailable) == BigDecimal.ZERO -> {
                                                Tag(
                                                    "Sem estoque disponível",
                                                    Color.Red,
                                                    Icons.Default.Close
                                                )
                                            }

                                            BigDecimal(material.stockAvailable) <= BigDecimal.TEN -> {
                                                Tag(
                                                    "Disponível: ${material.stockAvailable} ${material.requestUnit}",
                                                    Color(0xFFFF9800),
                                                    Icons.Default.Warning
                                                )
                                            }

                                            else -> {
                                                Tag(
                                                    "Disponível: ${material.stockAvailable} ${material.requestUnit}",
                                                    MaterialTheme.colorScheme.primary,
                                                    Icons.Default.Check
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    // Quantidade total, mais discreto
                                    Text(
                                        text = "Total em estoque: ${material.stockQuantity} ${material.requestUnit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            supportingContent = {
                                material.specs?.let {
                                    Tag(
                                        text = it, color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            trailingContent = {
                                IconToggleButton(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (BigDecimal(material.stockAvailable) == BigDecimal.ZERO) {
                                            alertMessage["title"] =
                                                "Material sem estoque disponível"
                                            alertMessage["body"] =
                                                "Para selecionar esse material é necessário haver estoque disponível."
                                            alertModal = true
                                            return@IconToggleButton
                                        }

                                        val type = material.type

                                        items = if (isChecked) {
                                            val isScrew =
                                                material.type.equals("parafuso", ignoreCase = true)

                                            val filteredItems = if (!isScrew) {
                                                // Remove qualquer outro material desse tipo (exceto parafuso)
                                                items.filterNot {
                                                    stockData.find { stock -> stock.materialStockId == it.materialStockId }?.type == type &&
                                                            it.maintenanceStreetId == maintenanceStreetId.toString() &&
                                                            it.maintenanceId == maintenanceId.toString()
                                                }
                                            } else {
                                                // Mantém todos os outros materiais
                                                items
                                            }

                                            filteredItems + MaintenanceStreetItem(
                                                maintenanceId = maintenanceId.toString(),
                                                maintenanceStreetId = maintenanceStreetId.toString(),
                                                materialStockId = material.materialStockId,
                                                quantityExecuted = if (
                                                    material.type.equals(
                                                        "cabo",
                                                        ignoreCase = true
                                                    ) || isScrew
                                                ) BigDecimal.ZERO.toString() else BigDecimal.ONE.toString()
                                            )
                                        } else {
                                            // Apenas remove esse item
                                            items.filterNot {
                                                it.materialStockId == material.materialStockId &&
                                                        it.maintenanceStreetId == maintenanceStreetId.toString() &&
                                                        it.maintenanceId == maintenanceId.toString()
                                            }
                                        }
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
                                                if (checked) 2.dp else 0.dp,
                                                MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 0.6f
                                                )
                                            ), shape = CircleShape
                                        )
                                        .size(30.dp)
                                ) {
                                    if (checked) Icon( // true = selected
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Check",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            },
                            modifier = Modifier
//                                .height(160.dp)
                                .padding(bottom = 10.dp)
                                .padding(end = 10.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            shadowElevation = 10.dp,
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Pesquisar ${selectedOption.lowercase()}...",
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
                                street = street.copy(lastPower = it)
                                lastPowerError = null
                            },
                            label = {
                                Text(
                                    "Potência anterior",
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
                }
                if (cableItem != null) {
                    var text by remember {
                        mutableStateOf(TextFieldValue("0"))
                    }
                    Text(
                        text = "Informações referente a cabo",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        isError = cableError != null,
                        supportingText = {
                            if (cableError != null) {
                                Text(
                                    text = cableError ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        trailingIcon = { Text("cm") },
                        value = text,
                        onValueChange = { newValue ->
                            cableError = null
                            val sanitized = sanitizeDecimalInput(newValue.text)
                            val quantityInMeters: BigDecimal = try {
                                BigDecimal(sanitized).divide(
                                    BigDecimal(100),
                                    2,
                                    RoundingMode.HALF_UP
                                )
                            } catch (e: NumberFormatException) {
                                BigDecimal.ZERO
                            }
                            text = TextFieldValue(sanitized, TextRange(sanitized.length))

                            items = items.map {
                                if (it.materialStockId == cableItem?.materialStockId)
                                    it.copy(quantityExecuted = quantityInMeters.toString())
                                else it
                            }
                        },
                        label = { Text("Quantidade de cabo (cm)", fontSize = 14.sp) },
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                    )
                }
                if (screws.isNotEmpty()) {
                    Text(
                        text = "Informações referente a parafuso",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    screws.forEach { screw ->
                        var text by remember(screw.materialStockId) {
                            mutableStateOf(TextFieldValue("0"))
                        }

                        val reference =
                            stockData.find { it.materialStockId == screw.materialStockId }

                        OutlinedTextField(
                            isError = screwErrors[screw.materialStockId] != null,
                            supportingText = {
                                screwErrors[screw.materialStockId]?.let { errorMsg ->
                                    Text(
                                        text = errorMsg,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            trailingIcon = { Text(reference?.requestUnit?.lowercase() ?: "un") },
                            value = text,
                            onValueChange = { newValue ->
                                val sanitized = sanitizeDecimalInput(newValue.text)
                                text = TextFieldValue(sanitized, TextRange(sanitized.length))

                                if (sanitized.toDoubleOrNull() == null || (sanitized.toDoubleOrNull()
                                        ?: 0.0) <= 0.0
                                ) {
                                    screwErrors[screw.materialStockId] =
                                        "Informe uma quantidade válida"
                                } else {
                                    screwErrors[screw.materialStockId] = null
                                }

                                items = items.map {
                                    if (it.materialStockId == screw.materialStockId)
                                        it.copy(
                                            quantityExecuted = BigDecimal(sanitized).toString()
                                        )
                                    else it
                                }
                            },
                            label = {
                                Text(
                                    "Quantidade de parafuso de ${reference?.specs}",
                                    fontSize = 14.sp
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                        )
                    }
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
                        }

                        if (cableItem != null) {
                            val material =
                                stockData.find { it.materialStockId == cableItem?.materialStockId }

                            if (BigDecimal(cableItem?.quantityExecuted) == BigDecimal.ZERO) {
                                cableError = "Informe a quantidade"
                                error = true
                            } else if (BigDecimal(material?.stockAvailable) < BigDecimal(cableItem?.quantityExecuted)) {
                                cableError = "Quantidade informada indisponível"
                                error = true
                            }
                        }

                        screws.forEach { screw ->
                            val item = items.find { it.materialStockId == screw.materialStockId }
                            val material =
                                stockData.find { it.materialStockId == screw.materialStockId }

                            if (item?.quantityExecuted == null || BigDecimal(item.quantityExecuted) == BigDecimal.ZERO) {
                                screwErrors[screw.materialStockId] = "Informe a quantidade"
                                error = true
                            } else if (BigDecimal(material?.stockAvailable) < BigDecimal(screw.quantityExecuted)) {
                                cableError = "Quantidade informada indisponível"
                                error = true
                            } else {
                                screwErrors[screw.materialStockId] = null
                            }
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
        saveStreet = { _: MaintenanceStreet, _: List<MaintenanceStreetItem> -> },
        streetCreated = false,
        newStreet = {},
        stockData = listOf(
            MaterialStock(
                materialId = 1,
                materialStockId = 11,
                materialName = "LUMINÁRIA LED",
                specs = "120W",
                stockQuantity = "12.0",
                stockAvailable = "0.0",
                requestUnit = "UN",
                type = "LED"
            ),
            MaterialStock(
                materialId = 2,
                materialStockId = 22,
                materialName = "LÂMPADA DE SÓDIO TUBULAR",
                specs = "400W",
                stockQuantity = "15.0",
                stockAvailable = "10.0",
                requestUnit = "UN",
                type = "LÂMPADA"
            ),
            MaterialStock(
                materialId = 3,
                materialStockId = 33,
                materialName = "LÂMPADA DE MERCÚRIO",
                specs = "250W",
                stockQuantity = "62.0",
                stockAvailable = "48.0",
                requestUnit = "UN",
                type = "LÂMPADA"
            ),
        ),
        contractor = "",
        message =  null
    )
}