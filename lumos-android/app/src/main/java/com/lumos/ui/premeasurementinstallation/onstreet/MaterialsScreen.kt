package com.lumos.ui.premeasurementinstallation.onstreet

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.lumos.domain.model.ItemView
import com.lumos.domain.model.PreMeasurementInstallationStreet
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.Alert
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.ConfirmNavigation
import com.lumos.ui.components.LoadImageComponent
import com.lumos.ui.components.SignatureScreenLandscape
import com.lumos.utils.Utils
import com.lumos.utils.Utils.hasFullName
import com.lumos.viewmodel.PreMeasurementInstallationViewModel
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Composable
fun MaterialScreen(
    viewModel: PreMeasurementInstallationViewModel,
    context: Context,
    navController: NavHostController,
) {
    if (viewModel.showSignScreen) {
        SignatureScreenLandscape(
            description = Utils.abbreviate(viewModel.contractor ?: "PREFEITURA DE BELO HORIZONTE"),
            onSave = { bitmap, signDate ->
                val file = File(context.filesDir, "signature_${System.currentTimeMillis()}.png")
                file.createNewFile()
                val uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    viewModel.signPhotoUri = uri.toString()
                    viewModel.signDate = signDate.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    viewModel.showSignScreen = false
                }
            },
            onCancel = {
                viewModel.showSignScreen = false
            })
    } else {
        MaterialsContent(
            viewModel = viewModel,
            navController = navController,
            context = context,
        )
    }
}

@Composable
fun MaterialsContent(
    viewModel: PreMeasurementInstallationViewModel,
    navController: NavHostController,
    context: Context,
) {
    val currentInstallationId = viewModel.installationID
    val currentStreets = viewModel.currentInstallationStreets
    val currentStreet = viewModel.currentStreet
    val currentItems = viewModel.currentInstallationItems
    val hasPosted = viewModel.hasPosted
    val loading = viewModel.loading
    val alertModal = viewModel.alertModal
    val checkBalance = viewModel.checkBalance
    val route = viewModel.route
    val triedToSubmit = viewModel.triedToSubmit
    val showFinishForm = viewModel.showFinishForm

    val fileUri: MutableState<Uri?> = remember {
        mutableStateOf(
            viewModel.currentStreet?.installationPhotoUri?.toUri()
        )
    }

    val imageSaved = remember { mutableStateOf(currentStreet?.installationPhotoUri != null) }
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
                    viewModel.currentStreet =
                        viewModel.currentStreet?.copy(installationPhotoUri = uri.toString())
                    imageSaved.value = true
                }
            }
        }

    AppLayout(
        title = Utils.abbreviate(viewModel.contractor ?: "PREFEITURA DE BELO HORIZONTE"),
        selectedIcon = BottomBar.EXECUTIONS.value,
        navigateToMore = {
            if (!hasPosted) {
                viewModel.route = Routes.MORE
            } else {
                navController.navigate(Routes.MORE)
            }
        },
        navigateToHome = {
            if (!hasPosted) {
                viewModel.route = Routes.HOME
            } else {
                navController.navigate(Routes.HOME)
            }
        },
        navigateToStock = {
            if (!hasPosted) {
                viewModel.route = Routes.STOCK
            } else {
                navController.navigate(Routes.STOCK)
            }
        },
        navigateToMaintenance = {
            if (!hasPosted) {
                viewModel.route = Routes.MAINTENANCE
            } else {
                navController.navigate(Routes.MAINTENANCE)
            }
        },
        navigateBack = {
            viewModel.route = "back"
        },
    ) { _, snackBar ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (viewModel.message != null) {
                val item = viewModel.message == "Item concluído com sucesso"
                snackBar(viewModel.message!!, if (item) "Desfazer" else null) {
                    viewModel.message = null
                    if (item && viewModel.lastItem != null) {
                        viewModel.currentInstallationItems =
                            viewModel.currentInstallationItems + viewModel.lastItem!!
                        viewModel.lastItem = null
                        viewModel.message = "Operação desfeita com sucesso."
                    }
                }
                viewModel.message = null
            }

            if (route != null) {
                ConfirmNavigation(
                    route = route,
                    navController = navController
                ) {
                    viewModel.route = null
                }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            TextField(
                                value = currentStreet?.address ?: "",
                                enabled = false,
                                onValueChange = {},
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(CircleShape),
                                placeholder = {
                                    Text(
                                        text = "Qual o endereço atual?",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
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
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                enabled = currentItems.isNotEmpty(),
                                onClick = {
                                    val latitude = currentStreet?.latitude
                                    val longitude = currentStreet?.longitude

                                    val gmmIntentUri: Uri =
                                        if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0)
                                            "google.navigation:q=$latitude,$longitude".toUri()
                                        else {
                                            val encodedAddress = Uri.encode(currentStreet?.address)

                                            "google.navigation:q=$encodedAddress".toUri()
                                        }

                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }

                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Navigation,
                                        contentDescription = "Navegar",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Abrir GPS",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (currentItems.isNotEmpty()) {
                        items(
                            items = currentItems,
                            key = { it.materialStockId }
                        ) {
                            MaterialItem(
                                material = it,
                                checkBalance = checkBalance,
                                finish = { quantityExecuted, materialStockId, stockQuantity, currentBalance, contractItemId ->
                                    if (BigDecimal(quantityExecuted) > BigDecimal(stockQuantity)) {
                                        viewModel.message =
                                            "Não há estoque disponível para esse item."
                                        return@MaterialItem
                                    } else if (checkBalance && BigDecimal(quantityExecuted) > BigDecimal(
                                            currentBalance ?: "0"
                                        )
                                    ) {
                                        viewModel.message =
                                            "Não há saldo contratual disponível para esse item."
                                        return@MaterialItem
                                    } else if (quantityExecuted.trim() == "" || quantityExecuted.trim() == "0") {
                                        viewModel.message =
                                            "Para concluir, Insira a quantidade do item"
                                        return@MaterialItem
                                    }

                                    viewModel.setInstallationItemQuantity(
                                        quantityExecuted,
                                        materialStockId,
                                        contractItemId
                                    )
                                },
                                loading = loading
                            )
                        }
                    } else {
                        item {
                            UserAction(
                                finish = {
                                    viewModel.submitStreet()
                                },
                                restart = {
                                    viewModel.setStreetAndItems(viewModel.currentStreetId ?: "")
                                },
                                cancel = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }

                if (alertModal) {
                    Alert(
                        title = "Você esqueceu da foto",
                        body = "Antes de finalizar tire uma foto.",
                        confirm = {
                            viewModel.alertModal = false
                        }
                    )
                }

                if (currentItems.isNotEmpty()) {

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

                    LoadImageComponent(
                        imageUrl = currentStreet?.photoUrl,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        onRefreshUrl = {
                            viewModel.refreshUrlImage()
                        }
                    )
                }

            } else {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (!showFinishForm) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TaskAlt,
                                contentDescription = "Tarefa concluída",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = if (currentInstallationId == null) "Missão Cumprida!"
                                else if (currentStreets.isEmpty()) "Quase lá!"
                                else "Bom trabalho!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text =
                                    if (currentInstallationId == null) "Instalação concluída com sucesso.\nOs dados serão enviados para o sistema."
                                    else if (currentStreets.isEmpty()) "Todas as ruas foram concluídas com sucesso.\nToque no botão abaixo para preencher os dados restantes."
                                    else "Essa rua foi concluída com sucesso.\nOs dados serão enviados para o sistema.\n\nToque no botão abaixo para selecionar e iniciar a próxima rua.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // HEADER PRINCIPAL
                            Text(
                                text = "Dados da Instalação",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )


                            // =========================================================
                            //                  SEÇÃO 1 — DADOS PRINCIPAIS
                            // =========================================================
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {

                                Column(
                                    modifier = Modifier.padding(20.dp),
                                ) {
                                    OutlinedTextField(
                                        value = viewModel.currentStreet?.currentSupply ?: "",
                                        onValueChange = {
                                            viewModel.triedToSubmit = false
                                            viewModel.currentStreet =
                                                viewModel.currentStreet?.copy(currentSupply = it)
                                        },
                                        label = { Text("Fornecedor atual") },
                                        isError = triedToSubmit && viewModel.currentStreet?.currentSupply.isNullOrBlank(),
                                        supportingText = {
                                            if (triedToSubmit && viewModel.currentStreet?.currentSupply.isNullOrBlank()) {
                                                Text("Informe o fornecedor atual")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = viewModel.currentStreet?.lastPower ?: "",
                                        onValueChange = {
                                            viewModel.triedToSubmit = false
                                            viewModel.currentStreet =
                                                viewModel.currentStreet?.copy(lastPower = it)
                                        },
                                        label = { Text("Potência anterior") },
                                        isError = triedToSubmit && viewModel.currentStreet?.lastPower.isNullOrBlank(),
                                        supportingText = {
                                            if (triedToSubmit && viewModel.currentStreet?.lastPower.isNullOrBlank()) {
                                                Text("Informe a potência anterior")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }


                            // =========================================================
                            //                  SEÇÃO 2 — RESPONSÁVEL
                            // =========================================================
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {

                                Column(
                                    modifier = Modifier.padding(20.dp),
                                ) {

                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = "Coletar Assinatura do Responsável?",
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {

                                        // Botão SIM
                                        FilterChip(
                                            selected = viewModel.hasResponsible == true,
                                            onClick = {
                                                viewModel.responsibleError = null
                                                viewModel.hasResponsible = true
                                            },
                                            label = { Text("Sim") }
                                        )

                                        // Botão NÃO
                                        FilterChip(
                                            selected = viewModel.hasResponsible == false,
                                            onClick = {
                                                if (viewModel.signPhotoUri == null) {
                                                    viewModel.responsibleError = null
                                                    viewModel.hasResponsible = false
                                                }
                                            },
                                            enabled = viewModel.signPhotoUri == null,
                                            label = { Text("Não") }
                                        )
                                    }

                                    if (viewModel.hasResponsible == null && viewModel.responsibleError != null) {
                                        Text(
                                            viewModel.responsibleError ?: "",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    // RESPONSÁVEL
                                    if (viewModel.hasResponsible == true) {

                                        // Nome antes da assinatura
                                        if (viewModel.signPhotoUri == null) {

                                            Text(
                                                text = "Solicite o Nome do Responsável",
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            OutlinedTextField(
                                                value = viewModel.responsible ?: "",
                                                onValueChange = {
                                                    viewModel.responsible = it
                                                    viewModel.responsibleError = null
                                                },
                                                label = { Text("Nome do Responsável") },
                                                isError = viewModel.responsibleError != null,
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )

                                            if (viewModel.responsibleError != null) {
                                                Text(
                                                    viewModel.responsibleError ?: "",
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        // Assinatura
                                        if (viewModel.signPhotoUri != null) {

                                            Box(
                                                modifier = Modifier
                                                    .height(120.dp)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                            ) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(viewModel.signPhotoUri),
                                                    contentDescription = "Assinatura",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                            Spacer(Modifier.height(10.dp))

                                            Text(
                                                text = "Ao finalizar o envio, presume-se que o responsável pela manutenção está ciente de que os dados fornecidos, incluindo a imagem da assinatura, poderão ser utilizados como comprovação da execução do serviço.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================
                    //                      BOTÃO FINAL
                    // =========================================================
                    Button(
                        onClick = {
                            var hasError = false

                            if (currentInstallationId != null) {
                                navController.navigate(Routes.HOME)
                            } else if (currentStreets.isEmpty()) {
                                if (viewModel.currentStreet?.currentSupply.isNullOrBlank()) hasError =
                                    true
                                if (viewModel.currentStreet?.lastPower.isNullOrBlank()) hasError =
                                    true

                                if (viewModel.hasResponsible == null) {
                                    viewModel.responsibleError = "Informe se possui responsável"
                                } else if (viewModel.hasResponsible == true &&
                                    (viewModel.responsible.isNullOrBlank() || !hasFullName(
                                        viewModel.responsible ?: ""
                                    ))
                                ) {
                                    viewModel.responsibleError =
                                        "Nome e sobrenome do responsável"
                                } else if (viewModel.hasResponsible == true && viewModel.signPhotoUri == null) {
                                    viewModel.showSignScreen = true
                                } else if (!hasError) {
                                    viewModel.openConfirmation = true
                                }
                            } else {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .fillMaxWidth()
                            .height(50.dp)
                            .align(Alignment.BottomCenter),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text(
                            if (currentInstallationId == null) "Sair"
                            else if (viewModel.hasResponsible == true && viewModel.signPhotoUri == null)
                                "Coletar Assinatura"
                            else if (currentStreets.isEmpty()) "Preencher Dados da Instalação"
                            else "Selecionar Próxima Rua"
                        )
                    }
                }

            }
        }

        if (viewModel.openConfirmation) {
            Confirm(
                body = "Deseja confirmar o salvamento dessa instalação?",
                confirm = {
                    viewModel.submitInstallation()
                },
                cancel = {
                    viewModel.openConfirmation = false
                }
            )
        }
    }

}


@Composable
fun MaterialItem(
    material: ItemView,
    checkBalance: Boolean,
    finish: (String, Long, String, String?, Long) -> Unit,
    loading: Boolean
) {
    var confirmModal by remember { mutableStateOf(false) }
    var quantityExecuted by remember(material.materialStockId) {
        mutableStateOf(BigDecimal(material.materialQuantity))
    }

    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(6.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nome e quantidade medida
            Text(
                text = material.materialName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = """
                    Quantidade levantada na rua: ${material.materialQuantity}
                    Quantidade em estoque: ${material.stockQuantity}
                    ${if (checkBalance) "Saldo contratual: " + material.stockQuantity else ""}
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Instrução leve
            Text(
                text = "Ajuste a quantidade executada",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Controle animado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        if (quantityExecuted > BigDecimal.ZERO) {
                            val hasDecimalPart =
                                BigDecimal(material.materialQuantity) % BigDecimal.ONE != BigDecimal.ZERO
                            val decrement =
                                if (hasDecimalPart) BigDecimal("0.1") else BigDecimal.ONE
                            quantityExecuted =
                                (quantityExecuted - decrement).coerceAtLeast(BigDecimal.ZERO)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .size(42.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Diminuir",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Valor animado
                Text(
                    text = quantityExecuted.stripTrailingZeros().toPlainString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = {
                        val hasDecimalPart =
                            BigDecimal(material.materialQuantity) % BigDecimal.ONE != BigDecimal.ZERO
                        val increment = if (hasDecimalPart) BigDecimal("0.1") else BigDecimal.ONE
                        quantityExecuted = quantityExecuted.add(increment)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .size(42.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Aumentar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                enabled = !loading,
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    finish(
                        quantityExecuted.toString(),
                        material.materialStockId,
                        material.stockQuantity,
                        material.currentBalance,
                        material.contractItemId
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Concluir")
            }
        }
    }

    if (confirmModal) {
        Confirm(
            body = "Deseja confirmar a execução de $quantityExecuted ${material.requestUnit}?",
            confirm = {
                confirmModal = false
                finish(
                    quantityExecuted.toString(),
                    material.materialStockId,
                    material.stockQuantity,
                    material.currentBalance,
                    material.contractItemId
                )
            },
            cancel = {
                confirmModal = false
            }
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun PrevMScreen() {
    val mockInstallationStreets = listOf(
        PreMeasurementInstallationStreet(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            preMeasurementId = UUID.randomUUID().toString(),
            address = "RUA DAS ACÁCIAS, 123 - CENTRO",
            priority = true,
            latitude = -23.550520,
            longitude = -46.633308,
            lastPower = "220V",
            photoUrl = "https://example.com/fotos/rua_acacias.jpg",
            photoExpiration = Instant.now().plusSeconds(86400).epochSecond,
            objectUri = "content://photos/rua_acacias",
            status = "PENDING",
            installationPhotoUri = "content://photos/rua_acacias_instalacao"
        ),
        PreMeasurementInstallationStreet(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            preMeasurementId = UUID.randomUUID().toString(),
            address = "AVENIDA BRASIL, 450 - JARDIM DAS FLORES",
            priority = false,
            latitude = -23.555000,
            longitude = -46.640000,
            lastPower = "110V",
            photoUrl = "https://example.com/fotos/av_brasil.jpg",
            photoExpiration = Instant.now().plusSeconds(172800).epochSecond,
            objectUri = "content://photos/av_brasil",
            status = "PENDING",
            installationPhotoUri = "content://photos/av_brasil_instalacao"
        ),
        PreMeasurementInstallationStreet(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            preMeasurementId = UUID.randomUUID().toString(),
            address = "RUA DAS PALMEIRAS, 789 - PARQUE VERDE",
            priority = true,
            latitude = -23.560000,
            longitude = -46.650000,
            lastPower = "127V",
            photoUrl = "https://example.com/fotos/rua_palmeiras.jpg",
            photoExpiration = Instant.now().plusSeconds(259200).epochSecond,
            objectUri = "content://photos/rua_palmeiras",
            status = "PENDING",
            installationPhotoUri = "content://photos/rua_palmeiras_instalacao"
        )
    )


    val mockInstallationItems = listOf(
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1001L,
            contractItemId = 2001L,
            materialName = "CABO DE ENERGIA",
            materialQuantity = "50",
            requestUnit = "M",
            specs = "10MM",
            executedQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1002L,
            contractItemId = 2002L,
            materialName = "POSTE DE CONCRETO",
            materialQuantity = "3",
            requestUnit = "UN",
            specs = "9M",
            executedQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1003L,
            contractItemId = 2003L,
            materialName = "LUMINÁRIA LED",
            materialQuantity = "5",
            requestUnit = "UN",
            specs = "150W",
            executedQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1004L,
            contractItemId = 2004L,
            materialName = "PARAFUSO E ARRUELA",
            materialQuantity = "100",
            requestUnit = "UN",
            specs = "M8",
            executedQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1005L,
            contractItemId = 2005L,
            materialName = "DISJUNTOR",
            materialQuantity = "2",
            requestUnit = "UN",
            specs = "40A",
            executedQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1006L,
            contractItemId = 2006L,
            materialName = "TERMINAL DE CABO",
            materialQuantity = "200",
            requestUnit = "UN",
            specs = "10MM",
            executedQuantity = "0"
        )
    )

    MaterialsContent(
        viewModel = PreMeasurementInstallationViewModel(
            repository = null,
            contractRepository = null,
            mockStreets = emptyList(),
            mockItems = emptyList(),
            mockCurrentStreet = PreMeasurementInstallationStreet(
                preMeasurementStreetId = "",
                preMeasurementId = "",
                address = "Rua Dona Tina, 251 - Palmeiras, Belo Horizonte - MG",
                priority = false,
                latitude = 1.1,
                longitude = 1.2,
                lastPower = "100W",
                photoUrl = "https://cdn.pixabay.com/photo/2025/06/22/19/27/back-lit-9674838_1280.jpg",
                photoExpiration = 1L,
                objectUri = "",
                status = "",
                installationPhotoUri = null
            )
        ),
        navController = rememberNavController(),
        context = LocalContext.current
    )
}