package com.lumos.ui.executions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.lumos.data.repository.ReservationStatus
import com.lumos.domain.model.Execution
import com.lumos.domain.model.Reserve
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.Alert
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.ui.viewmodel.ExecutionViewModel
import com.lumos.utils.Utils.buildAddress
import com.lumos.utils.Utils.formatDouble
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.math.BigDecimal

@Composable
fun MaterialScreen(
    streetId: Long = 0,
    contractId: Long = 0,
    executionViewModel: ExecutionViewModel,
    context: Context,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToExecutions: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    pSelected: Int,
    navController: NavHostController,
    notificationsBadge: String,
    directExecution: Boolean = false,
) {
    var reserves by remember { mutableStateOf<List<Reserve>>(emptyList()) }
    var hasPosted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var execution by remember { mutableStateOf<Execution?>(null) }
    var alertModal by remember { mutableStateOf(false) }
    val isLoading by executionViewModel.isLoadingReserves.collectAsState()

    LaunchedEffect(Unit) {
        if(!directExecution) {
            execution = executionViewModel.getExecution(streetId)
            reserves = executionViewModel.getReservesOnce(
                streetId,
                listOf(ReservationStatus.COLLECTED)
            )
        } else {
            execution = executionViewModel.getExecution(contractId)
            reserves = executionViewModel.getReservesOnce(
                contractId,
                listOf(ReservationStatus.COLLECTED)
            )
        }
    }

    execution?.let {
        MaterialsContent(
            execution = it,
            reserves = reserves,
            onNavigateToHome = onNavigateToHome,
            onNavigateToMenu = onNavigateToMenu,
            onNavigateToExecutions = onNavigateToExecutions,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToNotifications = onNavigateToNotifications,
            pSelected = pSelected,
            context = context,
            navController = navController,
            notificationsBadge = notificationsBadge,
            takePhoto = { uri ->
                it.photoUri = uri.toString()
                executionViewModel.setPhotoUri(
                    photoUri = uri.toString(),
                    streetId = it.streetId
                )
            },
            onFinishMaterial = { reserveId, quantityExecuted ->
                if (reserves.size == 1 && execution?.photoUri == null) {
                    alertModal = true
                } else {
                    executionViewModel.finishAndCheckPostExecution(
                        reserveId = reserveId,
                        quantityExecuted = quantityExecuted,
                        streetId = streetId,
                        context = context,
                        hasPosted = hasPosted,
                        onPostExecuted = { hasPosted = true },
                        onReservesUpdated = { reserves = it },
                        onError = {
                            errorMessage = it
                        }
                    )
                }
            },
            alertModal = alertModal,
            closeAlertModal = {
                alertModal = false
            },
            loadingReserves = isLoading,
            hasPosted = hasPosted,
            errorMessage = errorMessage
        )
    } ?: Loading()

}

@Composable
fun MaterialsContent(
    execution: Execution,
    reserves: List<Reserve>,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToExecutions: () -> Unit,
    onNavigateToProfile: () -> Unit,
    pSelected: Int,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    takePhoto: (uri: Uri) -> Unit,
    onFinishMaterial: (Long, Double) -> Unit,
    alertModal: Boolean,
    closeAlertModal: () -> Unit,
    loadingReserves: Boolean,
    hasPosted: Boolean,
    errorMessage: String?
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val fileUri: MutableState<Uri?> = remember {
        mutableStateOf(
            execution.photoUri?.toUri()
        )
    }

    val imageSaved = remember { mutableStateOf(execution.photoUri != null) }
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

    AppLayout(
        title = buildAddress(
            streetName = execution.streetName,
            number = execution.streetNumber
        ),
        pSelected = pSelected,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        navigateBack = onNavigateToExecutions,
        context = context,
        notificationsBadge = notificationsBadge,
    ) { _, snackBar ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            LaunchedEffect(errorMessage) {
                if (errorMessage != null)
                    snackBar(errorMessage, null)
            }

            if (!hasPosted) {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 90.dp),// deixa espaço pros botões
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp) // Espaço entre os cards
                ) {
                    item {
                        TextField(
                            value = "",
                            onValueChange = {  },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            placeholder = {
                                Text(
                                    text = "Digite um endereço...",
                                    color = Color.Gray
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Localização",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
//                            colors = TextFieldDefaults.textFieldColors(
//                                containerColor = Color.White,
//                                focusedIndicatorColor = Color.Transparent,
//                                unfocusedIndicatorColor = Color.Transparent,
//                                disabledIndicatorColor = Color.Transparent
//                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    // Aqui você pode fazer algo com o searchQuery
                                }
                            )
                        )
                    }
                    items(
                        items = reserves,
                        key = { it.reserveId }
                    ) {
                        MaterialItem(material = it, finish = { quantityExecuted ->
                            onFinishMaterial(it.reserveId, quantityExecuted)
                        }, loadingReserves = loadingReserves)
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

                if (alertModal) {
                    Alert(
                        title = "Você esqueceu da foto",
                        body = "Antes de finalizar tire uma foto.",
                        confirm = {
                            closeAlertModal()
                        })
                }


                FloatingActionButton(
                    onClick = {
                        val latitude = execution.latitude
                        val longitude = execution.longitude

                        val gmmIntentUri: Uri =
                            if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0)
                                "google.navigation:q=$latitude,$longitude".toUri()
                            else {
                                val fullAddress = buildAddress(
                                    execution.streetName,
                                    execution.streetNumber,
                                    execution.streetHood,
                                    execution.city,
                                    execution.state
                                )

                                val encodedAddress = Uri.encode(fullAddress)

                                "google.navigation:q=$encodedAddress".toUri()
                            }

                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }

                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        }

                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // <-- Também aqui
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(10.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                contentDescription = null,
                                imageVector = Icons.Rounded.Navigation,
                                modifier = Modifier.size(30.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                "Navegar",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp
                            )
                        }

                    }
                }

            } else {
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
                            text = "Todas as reservas foram concluídas com sucesso.\nOs dados serão enviados para o sistema.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            onNavigateToExecutions()
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
    material: Reserve,
    finish: (Double) -> Unit,
    loadingReserves: Boolean
) {
    var confirmModal by remember { mutableStateOf(false) }

    var quantityExecuted by remember(material.reserveId) {
        mutableStateOf(BigDecimal(material.materialQuantity.toString()))
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
                modifier = Modifier.fillMaxHeight()
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
                        Text(
                            text = material.materialName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Quantidade medida: ${formatDouble(material.materialQuantity)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            enabled = !loadingReserves,
                            shape = RoundedCornerShape(10.dp),
                            onClick = { confirmModal = true },
                        ) {
                            Text("Concluir")
                        }
                    }


                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Qtde.\nExecutada",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(10.dp))

                        IconButton(
                            onClick = {
                                val hasDecimalPart = material.materialQuantity % 1 != 0.0
                                val increment =
                                    if (hasDecimalPart) BigDecimal("0.1") else BigDecimal("1")
                                quantityExecuted = quantityExecuted.add(increment)
                            },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                                .size(30.dp)
                                .padding(5.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Aumentar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }


                        Spacer(modifier = Modifier.height(6.dp)) // Espaçamento entre os ícones

                        Text(
                            text = formatDouble(quantityExecuted.toDouble()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        IconButton(
                            onClick = {
                                if (quantityExecuted > BigDecimal.ZERO) {
                                    val hasDecimalPart = material.materialQuantity % 1 != 0.0
                                    val decrement =
                                        if (hasDecimalPart) BigDecimal("0.1") else BigDecimal("1")

                                    quantityExecuted =
                                        (quantityExecuted - decrement).coerceAtLeast(BigDecimal.ZERO) // para não ficar negativo
                                }

                            },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                                .size(30.dp)
                                .padding(5.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Diminuir",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (confirmModal)
            Confirm(
                body = "Deseja confirmar a execução de ${formatDouble(quantityExecuted.toDouble())} ${material.requestUnit}?",
                confirm = {
                    confirmModal = false
                    finish(quantityExecuted.toDouble())
                },
                cancel = {
                    confirmModal = false
                }
            )
    }
}


@Preview
@Composable
fun PrevMScreen() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val values =
        Execution(
            streetId = 1,
            streetName = "Rua Dona Tina",
            streetNumber = "251",
            executionStatus = "PENDING",
            priority = true,
            type = "INSTALLATION",
            itemsQuantity = 7,
            creationDate = "",
            latitude = 0.0,
            longitude = 0.0,
            photoUri = "",
            contractId = 1,
            contractor = ""
        )

    val reserves = listOf(
        Reserve(
            reserveId = 10,

            materialName = "LED 120W",
            materialQuantity = 12.0,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 1,
            depositName = "GALPÃO BH",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "Elton Melo",
            phoneNumber = "31999998090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 2,

            materialName = "BRAÇO DE 3,5",
            materialQuantity = 16.0,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 1,
            depositName = "GALPÃO BH",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "Elton Melo",
            phoneNumber = "31999998090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 3,

            materialName = "BRAÇO DE 3,5",
            materialQuantity = 16.0,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 1,
            depositName = "GALPÃO BH",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "Elton Melo",
            phoneNumber = "31999998090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 4,

            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 5,

            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 6,

            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 7,

            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090",
            requestUnit = "UN",
            contractId = -1
        ),
        Reserve(
            reserveId = 8,

            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090",
            requestUnit = "UN",
            contractId = -1
        )
    )


    MaterialsContent(
        execution = values,
        reserves = reserves,
        onNavigateToHome = { },
        onNavigateToMenu = { },
        onNavigateToProfile = { },
        onNavigateToExecutions = { },
        onNavigateToNotifications = { },
        context = fakeContext,
        navController = rememberNavController(),
        notificationsBadge = "12",
        pSelected = BottomBar.HOME.value,
        takePhoto = { },
        onFinishMaterial = { _, _ -> },
        alertModal = false,
        closeAlertModal = { },
        loadingReserves = true,
        hasPosted = false,
        errorMessage = null
    )
}