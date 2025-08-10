package com.lumos.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.model.Stockist
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.Alert
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NoInternet
import com.lumos.ui.components.NothingData
import com.lumos.ui.components.Tag
import com.lumos.viewmodel.StockViewModel
import com.lumos.utils.ConnectivityUtils
import com.lumos.worker.SyncTypes
import java.math.BigDecimal

@Composable
fun CheckStockScreen(
    navController: NavHostController, lastRoute: String? = null, stockViewModel: StockViewModel
) {
    val stock by stockViewModel.stock.collectAsState()
    val deposits by stockViewModel.deposits.collectAsState()
    val stockists by stockViewModel.stockists.collectAsState()

    val loading by stockViewModel.loading.collectAsState()
    val message by stockViewModel.message.collectAsState()
    val orderCode by stockViewModel.orderCode.collectAsState()

    var hasInternet by remember { mutableStateOf(true) }
    var alertModal by remember { mutableStateOf(false) }
    var next by remember { mutableStateOf(false) }
    var resync by remember { mutableIntStateOf(0) }

    var selectedMode by remember { mutableStateOf(false) }
    var selectedMaterials by remember { mutableStateOf<List<Long>>(emptyList()) }


    val alertMessage = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem", "body" to "Você está na rua da execução neste momento?"
        )
    }

    LaunchedEffect(Unit) {
        stockViewModel.loadStockFlow()
        stockViewModel.loadDepositsFlow()
        stockViewModel.loadStockistsFlow()

        if (ConnectivityUtils.hasRealInternetConnection()) {
            hasInternet = true

            stockViewModel.hasTypesInQueue(
                listOf(
                    SyncTypes.POST_MAINTENANCE_STREET,
                    SyncTypes.POST_DIRECT_EXECUTION,
                    SyncTypes.POST_INDIRECT_EXECUTION
                )
            )
        } else {
            hasInternet = false
        }

    }

    LaunchedEffect(resync) {
        if (resync > 0) {
            if (ConnectivityUtils.hasRealInternetConnection()) {
                hasInternet = true

                stockViewModel.hasTypesInQueue(
                    listOf(
                        SyncTypes.POST_MAINTENANCE_STREET,
                        SyncTypes.POST_DIRECT_EXECUTION,
                        SyncTypes.POST_INDIRECT_EXECUTION
                    )
                )
            } else {
                hasInternet = false
            }
        }
    }

    if (!next) {
        CheckStockContent(
            selectedMaterialsCopy = selectedMaterials,
            selectedModeCopy = selectedMode,

            navController = navController,
            lastRoute = lastRoute,
            hasInternet = hasInternet,
            message = message,
            loading = loading,
            stockData = stock,
            resync = {
                resync += 1
            },
            next = { materials ->
                if (materials.isEmpty()) {
                    alertMessage["title"] = "Nenhum material selecionado"
                    alertMessage["body"] = "Por favor, selecione os materiais."
                    alertModal = true
                } else {
                    selectedMaterials = materials
                    selectedMode = true
                    next = true
                }

            },
            alertMessage = alertMessage,
            alertModal = alertModal,
            closeAlertModal = {
                alertModal = false
            })
    } else {
        SelectDeposit(
            message = message,
            loading = loading,
            orderCode = orderCode,
            navController = navController,
            stockData = stock,

            selectedMaterials = selectedMaterials,
            deposits = deposits,
            stockists = stockists,

            finish = { selectedDepositId ->
                stockViewModel.saveOrder(
                    materials = selectedMaterials, depositId = selectedDepositId
                )
            },
            back = {
                if (orderCode.isNotBlank()) {
                    selectedMaterials = emptyList()
                    selectedMode = false
                    stockViewModel.clear()
                }
                next = false
            })
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckStockContent(
    selectedMaterialsCopy: List<Long>,
    selectedModeCopy: Boolean,
    navController: NavHostController,
    lastRoute: String?,
    hasInternet: Boolean,
    message: String,
    loading: Boolean,
    stockData: List<MaterialStock>,
    resync: () -> Unit,
    next: (List<Long>) -> Unit,
    alertModal: Boolean,
    alertMessage: Map<String, String>,
    closeAlertModal: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var lastMessage by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(selectedModeCopy) }

    val selectedMaterials =
        remember { mutableStateListOf<Long>().apply { addAll(selectedMaterialsCopy) } }

    var searchQuery by remember { mutableStateOf("") }

    val normalizedQuery = searchQuery.replace("\\s".toRegex(), "").lowercase()

    val filteredStock = stockData.filter {
        val name = it.materialName.replace("\\s".toRegex(), "").lowercase()
        val specs = it.specs?.replace("\\s".toRegex(), "")?.lowercase()

        name.contains(normalizedQuery) || (name + specs).contains(normalizedQuery) || (specs?.contains(
            normalizedQuery
        ) ?: false)
    }.distinctBy { it.materialId }

    val navigateBack: (() -> Unit)? = if (lastRoute == Routes.MAINTENANCE) {
        { navController.navigate(Routes.MAINTENANCE) }
    } else {
        null
    }

    if (alertModal) {
        Alert(
            title = alertMessage["title"] ?: "", body = alertMessage["body"] ?: "", confirm = {
                closeAlertModal()
            })
    }

    AppLayout(
        title = "Estoque do caminhão",
        selectedIcon = BottomBar.STOCK.value,
        navigateBack = navigateBack,
        navigateToHome = {
            navController.navigate(Routes.HOME)
        },
        navigateToMore = {
            navController.navigate(Routes.MORE)
        },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE)
        },
        navigateToExecutions = {
            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
        }) { _, showSnackBar ->

        if (message.isNotBlank() && message != lastMessage) {
            lastMessage = message
            showSnackBar(message, null, null)
        }

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = {
                resync()
            }, modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            if (loading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Carregando",
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Está demorando? Clique no botão",
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 50.dp, bottom = 10.dp),
                    )
                    Button(
                        onClick = {
                            navController.navigate("${Routes.SYNC}/${SyncTypes.POST_MAINTENANCE}?lastRoute=${Routes.STOCK}")
                        }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Verificar se existe alguma pendência")
                    }
                }
            } else if (!hasInternet) {
                NoInternet()
                NothingData("Arraste para baixo para tentar novamente.")
            } else if (stockData.isEmpty()) {
                NothingData("Nenhum material encontrado, Arraste para baixo para buscar novamente.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(bottom = 60.dp)
                        .nestedScroll(remember {
                            object : NestedScrollConnection {
                                override fun onPreScroll(
                                    available: Offset, source: NestedScrollSource
                                ): Offset {
                                    focusManager.clearFocus()
                                    return Offset.Zero
                                }
                            }
                        })
                ) {
                    items(
                        items = filteredStock,
                        key = { it.materialStockId },
                    ) { material ->

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

                                            BigDecimal(material.stockAvailable) <= BigDecimal(10)
                                                -> {
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
                                if (selectedMode) IconToggleButton(
                                    checked = selectedMaterials.contains(material.materialId),
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            selectedMaterials.add(material.materialId)
                                        } else {
                                            selectedMaterials.remove(material.materialId)
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
                                                if (!selectedMaterials.contains(material.materialId)) 2.dp else 0.dp,
                                                MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 0.6f
                                                )
                                            ), shape = CircleShape
                                        )
                                        .size(30.dp)
                                ) {
                                    if (selectedMaterials.contains(material.materialId)) Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Check",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            shadowElevation = 10.dp,
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )


                    }
                }

                FloatingActionButton(
                    onClick = {

                    },
                    modifier = Modifier.align(Alignment.BottomStart) // <-- Aqui dentro de um Box
                    ,
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    )
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Pesquisar...",
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
                            .fillMaxWidth(0.5f) // ajusta a largura
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

                }

                if (!selectedMode) FloatingActionButton(
                    onClick = {
                        selectedMode = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // <-- Aqui dentro de um Box
                        .padding(horizontal = 20.dp),
                    containerColor = MaterialTheme.colorScheme.background,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 5.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 5.dp
                    )
                ) {
                    Box {
                        Text(
                            "Solicitar Materiais",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                else FloatingActionButton(
                    onClick = {
                        next(selectedMaterials)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // <-- Aqui dentro de um Box
                        .padding(horizontal = 20.dp),
                    containerColor = MaterialTheme.colorScheme.background,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 5.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 5.dp
                    )
                ) {
                    Box {
                        Text(
                            "Continuar",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectDeposit(
    loading: Boolean,
    orderCode: String,
    message: String,
    navController: NavHostController,
    stockData: List<MaterialStock>,
    selectedMaterials: List<Long>,
    deposits: List<Deposit>,
    stockists: List<Stockist>,
    finish: (Long) -> Unit,
    back: () -> Unit,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var confirmModal by remember { mutableStateOf(false) }
    var selectedDepositId by remember { mutableLongStateOf(-1) }
    var lastMessage by remember { mutableStateOf("") }

    var showDetails by remember(orderCode) {
        mutableStateOf(orderCode.isBlank())
    }

    val filteredStock = stockData.filter {
        selectedMaterials.contains(it.materialId)
    }

    val filteredStockists = stockists.filter {
        it.depositId == selectedDepositId
    }

    if (confirmModal) {
        Confirm(body = "Deseja enviar essa requisição de materiais?", confirm = {
            confirmModal = false
            finish(selectedDepositId)
        }, cancel = {
            confirmModal = false
        })
    }

    AppLayout(
        title = "Requisição de materiais",
        selectedIcon = BottomBar.STOCK.value,
        navigateBack = { back() },
        navigateToHome = { navController.navigate(Routes.HOME) },
        navigateToMore = { navController.navigate(Routes.MORE) },
        navigateToMaintenance = { navController.navigate(Routes.MAINTENANCE) },
        navigateToExecutions = { navController.navigate(Routes.DIRECT_EXECUTION_SCREEN) }) { _, showSnackBar ->

        if (message.isNotBlank() && message != lastMessage) {
            showSnackBar(message, null, null)
            lastMessage = message
        }

        Box(modifier = Modifier.fillMaxSize()) {

            if (loading) {
                Loading("Processando requisição")
            } else if (showDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (orderCode.isBlank()) 80.dp else 0.dp)
                ) {

                    if (orderCode.isNotBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                showDetails = false
                            }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Código da solicitação:",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(Modifier.width(5.dp))
                                Tag(orderCode, MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(5.dp))
                            Tag("Pendente", MaterialTheme.colorScheme.inverseSurface)
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider()
                        }

                    }

                    // 🔷 Header com dados do depósito e estoquistas
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .clip(shape = RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                            .clickable {
                                showBottomSheet = true
                            }) {
                        val deposit = deposits.firstOrNull { it.depositId == selectedDepositId }
                        val depositName =
                            if (!deposit?.depositPhone.isNullOrBlank()) "${deposit?.depositName} - ${deposit?.depositPhone}"
                            else deposit?.depositName

                        Text(
                            text = depositName ?: "Selecione um depósito",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedDepositId == -1L) {
                                "Aguardando seleção..."
                            } else if (filteredStockists.isEmpty()) {
                                "Nenhum estoquista encontrado"
                            } else {
                                filteredStockists.joinToString("\n") { "${it.stockistName} ${it.stockistPhone ?: ""}" }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }

                    // 🔷 Lista de materiais filtrados
                    LazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        items(
                            filteredStock,
                            key = { it.materialStockId }
                        ) { material ->
                            ListItem(
                                colors = ListItemDefaults.colors(MaterialTheme.colorScheme.background),
                                headlineContent = {
                                    Text(
                                        text = "${material.materialName} - ${material.specs ?: ""}".trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.Inventory, // ou outro que combine
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                })

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp)
                    ) {
                        Icon(
                            contentDescription = null,
                            imageVector = Icons.Default.CheckCircle,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        )
                        Text(
                            "Requisição registrada com sucesso!",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Seu pedido foi salvo e está em processamento.",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                    }


                    Spacer(Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Código da solicitação:", style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.width(5.dp))
                        Tag(orderCode, MaterialTheme.colorScheme.primary)
                    }

                    Spacer(Modifier.height(30.dp))
                    TextButton(
                        onClick = {
                            showDetails = true
                        }) {
                        Text(
                            "Exibir detalhes da solicitação", textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Entre em contato com o almoxarifado ou estoquista para combinar a retirada ou entrega.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(10.dp))

                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp)
                            .padding(horizontal = 20.dp)
                    ) {
                        val deposit = deposits.firstOrNull { it.depositId == selectedDepositId }
                        val depositName =
                            if (!deposit?.depositPhone.isNullOrBlank()) "${deposit?.depositName} - ${deposit?.depositPhone}"
                            else deposit?.depositName

                        Text(
                            text = depositName ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = filteredStockists.joinToString("\n") { "${it.stockistName} ${it.stockistPhone ?: ""}" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // BottomSheet de seleção de depósito
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column {

                        // Header com título e botão "Cancelar"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selecionar almoxarifado",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { showBottomSheet = false }) {
                                Text("Cancelar")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Lista de depósitos
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxHeight(0.6f)
                                .padding(horizontal = 4.dp)
                        ) {
                            items(deposits) { deposit ->
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }, headlineContent = {
                                        Text(
                                            text = deposit.depositName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }, modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDepositId = deposit.depositId
                                            showBottomSheet = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 4.dp))
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }

            // Botao de seleção de depósito/salvar
            if (orderCode.isBlank()) {
                Button(
                    onClick = {
                        if (selectedDepositId == -1L) {
                            showBottomSheet = true
                        } else {
                            confirmModal = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(20.dp)
                        .height(48.dp) // altura mais achatada
                        .defaultMinSize(minWidth = 200.dp) // aumenta largura mínima
                        .clip(RoundedCornerShape(50)) // borda bem arredondada
                        .shadow(8.dp, RoundedCornerShape(50)), // sombra leve para efeito flutuante
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp, pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = if (selectedDepositId == -1L) "Selecionar Almoxarifado" else "Enviar Solicitação",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

        }
    }


}

@Composable
@Preview
fun PrevStockContent() {
//    SelectDeposit(
//        message = "",
//        loading = false,
//        orderCode = "",
//        navController = rememberNavController(),
//        stockData = listOf(
//            MaterialStock(
//                materialId = 1,
//                materialStockId = 11,
//                materialName = "LUMINÁRIA LED",
//                specs = "120W",
//                stockQuantity = 12.0,
//                stockAvailable = 0.0,
//                requestUnit = "UN",
//                type = "LED"
//            ),
//            MaterialStock(
//                materialId = 2,
//                materialStockId = 22,
//                materialName = "LÂMPADA DE SÓDIO TUBULAR",
//                specs = "400W",
//                stockQuantity = 15.0,
//                stockAvailable = 10.0,
//                requestUnit = "UN",
//                type = "LÂMPADA"
//            ),
//            MaterialStock(
//                materialId = 3,
//                materialStockId = 33,
//                materialName = "LÂMPADA DE MERCÚRIO",
//                specs = "250W",
//                stockQuantity = 62.0,
//                stockAvailable = 48.0,
//                requestUnit = "UN",
//                type = "LÂMPADA"
//            ),
//        ),
//        selectedMaterials = listOf(1, 2, 3),
//        deposits = listOf(
//            Deposit(
//                depositId = 1,
//                depositName = "GALPÃO ITAPECERICA",
//                depositAddress = "Rua Antonio Claret Araújo",
//                depositPhone = "31996546000"
//            )
//        ),
//        stockists = listOf(
//            Stockist(
//                stockistId = 1,
//                stockistName = "Gabriela",
//                stockistPhone = "31999998080",
//                depositId = 1
//            )
//        ),
//        finish = { },
//        back = {})

    CheckStockContent(
        navController = rememberNavController(),
        lastRoute = null,
        hasInternet = true,
        message = "",
        loading = false,
        stockData = listOf(
            MaterialStock(
                materialStockId = 1,
                materialId = 2,
                materialName = "LUMINÁRIA LED",
                specs = "120W",
                stockQuantity = "12",
                stockAvailable = "0",
                requestUnit = "UN",
                type = "",
            ),
            MaterialStock(
                materialStockId = 2,
                materialId = 2,
                materialName = "LÂMPADA DE SÓDIO TUBULAR",
                specs = "400W",
                stockQuantity = "15",
                stockAvailable = "10",
                requestUnit = "UN",
                type = "",
            ),
            MaterialStock(
                materialStockId = 3,
                materialId = 2,
                materialName = "LÂMPADA DE MERCÚRIO",
                specs = "250W",
                stockQuantity = "15",
                stockAvailable = "48",
                requestUnit = "UN",
                type = "",
            ),
        ),
        resync = {},
        next = {},
        alertMessage = mapOf(),
        alertModal = false,
        closeAlertModal = {

        },
        selectedMaterialsCopy = emptyList(),
        selectedModeCopy = false
    )
}