package com.lumos.ui.stock

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.MaterialStock
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.NoInternet
import com.lumos.ui.components.NothingData
import com.lumos.ui.components.Tag
import com.lumos.ui.viewmodel.MaintenanceViewModel
import com.lumos.utils.ConnectivityUtils
import com.lumos.utils.Utils
import com.lumos.worker.SyncTypes

@Composable
fun CheckStockScreen(
    context: Context,
    navController: NavHostController,
    lastRoute: String? = null,
    maintenanceViewModel: MaintenanceViewModel
) {
    val stock by maintenanceViewModel.stock.collectAsState()
    val loading by maintenanceViewModel.loading.collectAsState()
    val message by maintenanceViewModel.message.collectAsState()

    var hasInternet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (ConnectivityUtils.hasRealInternetConnection()) {
            hasInternet = true

            maintenanceViewModel.getFlowExistsTypeInQueue(
                listOf(
                    SyncTypes.SYNC_STOCK,
                    SyncTypes.POST_MAINTENANCE
                )
            )
            maintenanceViewModel.callSyncStock()
            maintenanceViewModel.loadStockFlow()
        }
    }

    CheckStockContent(
        navController = navController,
        lastRoute = lastRoute,
        hasInternet = hasInternet,
        message = message,
        loading = loading,
        stockData = stock
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckStockContent(
    navController: NavHostController,
    lastRoute: String?,
    hasInternet: Boolean,
    message: String,
    loading: Boolean,
    stockData: List<MaterialStock>
) {

    var lastMessage by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(false) }

    var selectedMaterials by remember { mutableStateOf<List<Long>>(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }

    val filteredStock = stockData.filter {
        it.materialName.contains(searchQuery, ignoreCase = true) ||
                it.specs?.contains(searchQuery, ignoreCase = true) ?: false
    }

    val navigateBack: (() -> Unit)? =
        if (lastRoute == Routes.MAINTENANCE) {
            { navController.navigate(Routes.MAINTENANCE) }
        } else {
            null
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
        }
    ) { modifier, showSnackBar ->

        if (message.isNotBlank() && message != lastMessage) {
            lastMessage = message
            showSnackBar(message, null)
        }

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = {

            },
            modifier = modifier
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
                        modifier = Modifier
                            .padding(top = 50.dp, bottom = 10.dp),
                    )
                    Button(
                        onClick = {
                            navController
                                .navigate("${Routes.SYNC}/${SyncTypes.POST_MAINTENANCE}?lastRoute=${Routes.STOCK}")
                        },
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
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
                LazyColumn {
                    items(
                        items = filteredStock,
                        key = { it.materialIdStock }, // Opcional, se os itens forem únicos
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
                                            material.stockAvailable == 0.0 -> {
                                                Tag(
                                                    "Sem estoque disponível",
                                                    Color.Red,
                                                    Icons.Default.Close
                                                )
                                            }

                                            material.stockAvailable <= 10.0 -> {
                                                Tag(
                                                    "Disponível: ${Utils.formatDouble(material.stockAvailable)} ${material.requestUnit}",
                                                    Color(0xFFFF9800),
                                                    Icons.Default.Warning
                                                )
                                            }

                                            else -> {
                                                Tag(
                                                    "Disponível: ${Utils.formatDouble(material.stockAvailable)} ${material.requestUnit}",
                                                    MaterialTheme.colorScheme.primary,
                                                    Icons.Default.Check
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    // Quantidade total, mais discreto
                                    Text(
                                        text = "Total em estoque: ${Utils.formatDouble(material.stockQuantity)} ${material.requestUnit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            supportingContent = {
                                material.specs?.let {
                                    Tag(
                                        text = it,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            trailingContent = {
                                if (selectedMode)
                                    IconToggleButton(
                                        checked = false,
                                        onCheckedChange = { isChecked ->
                                            selectedMaterials = if (isChecked)
                                                selectedMaterials - material.materialIdStock
                                            else
                                                selectedMaterials + material.materialIdStock
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
                                                    if (!selectedMaterials.contains(material.materialIdStock)) 2.dp else 0.dp,
                                                    MaterialTheme.colorScheme.onBackground.copy(
                                                        alpha = 0.6f
                                                    )
                                                ), shape = CircleShape
                                            )
                                            .size(30.dp)
                                    ) {
                                        if (selectedMaterials.contains(material.materialIdStock))
                                            Icon(
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
                    modifier = Modifier
                        .align(Alignment.BottomStart) // <-- Aqui dentro de um Box
                        .padding(horizontal = 10.dp),
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


                if (!selectedMode)
                    FloatingActionButton(
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
                else
                    FloatingActionButton(
                        onClick = {

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
                                "Enviar Pedido",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
            }
        }
    }
}

@Composable
@Preview
fun PrevStockContent() {
    CheckStockContent(
        navController = rememberNavController(),
        lastRoute = null,
        hasInternet = true,
        message = "",
        loading = false,
        stockData = listOf(
            MaterialStock(
                materialIdStock = 1,
                materialName = "LUMINÁRIA LED",
                specs = "120W",
                stockQuantity = 12.0,
                stockAvailable = 0.0,
                requestUnit = "UN"
            ),
            MaterialStock(
                materialIdStock = 2,
                materialName = "LÂMPADA DE SÓDIO TUBULAR",
                specs = "400W",
                stockQuantity = 15.0,
                stockAvailable = 10.0,
                requestUnit = "UN"
            ),
            MaterialStock(
                materialIdStock = 3,
                materialName = "LÂMPADA DE MERCÚRIO",
                specs = "250W",
                stockQuantity = 62.0,
                stockAvailable = 48.0,
                requestUnit = "UN"
            ),
        )
    )
}