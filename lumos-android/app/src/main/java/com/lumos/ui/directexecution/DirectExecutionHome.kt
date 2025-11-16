package com.lumos.ui.directexecution

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.ui.components.SignatureScreenLandscape
import com.lumos.utils.Utils
import com.lumos.utils.Utils.hasFullName
import com.lumos.viewmodel.DirectExecutionViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

@Composable
fun DirectExecutionHomeScreen(
    viewModel: DirectExecutionViewModel,
    navController: NavHostController,
) {
    val contractor = viewModel.contractor
    val loading = viewModel.isLoading
    val streets = viewModel.streets
    val creationDate = viewModel.creationDate
    val triedToSubmit = viewModel.triedToSubmit
    val instructions = viewModel.instructions


    var confirmModal by remember { mutableStateOf(false) }
    val alertMessage = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem", "body" to "Você está na rua da execução neste momento?"
        )
    }

    val context = LocalContext.current

    if (viewModel.showSignScreen) {
        SignatureScreenLandscape(
            description = Utils.abbreviate(contractor ?: ""),
            onSave = { bitmap, signDate ->
                val file = File(context.filesDir, "signature_${System.currentTimeMillis()}.png")
                file.createNewFile()
                val uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    viewModel.signPath = uri.toString()
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
        AppLayout(
            title = Utils.abbreviate(contractor ?: ""),
            selectedIcon = BottomBar.EXECUTIONS.value,
            navigateBack = {
                navController.popBackStack()
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
            }) { _, showSnackBar ->

            if(viewModel.errorMessage != null) {
                showSnackBar(viewModel.errorMessage!!, null, null)
                viewModel.errorMessage = null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {

                // ========= CONFIRMAÇÃO =========
                if (confirmModal) {
                    Confirm(
                        body = "Deseja finalizar essa instalação?",
                        confirm = {
                            confirmModal = false
                            viewModel.submitInstallation()
                        },
                        cancel = { confirmModal = false }
                    )
                }

                // ========= LOADING =========
                if (loading) {
                    Loading()
                }

                // ────────────────────────────────────────────────
                //               FORMULÁRIO FINALIZAR
                // ────────────────────────────────────────────────
                else if (viewModel.showFinishForm) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
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
                                        value = viewModel.street?.currentSupply ?: "",
                                        onValueChange = {
                                            viewModel.triedToSubmit = false
                                            viewModel.street =
                                                viewModel.street?.copy(currentSupply = it)
                                        },
                                        label = { Text("Fornecedor atual") },
                                        isError = triedToSubmit && viewModel.street?.currentSupply.isNullOrBlank(),
                                        supportingText = {
                                            if (triedToSubmit && viewModel.street?.currentSupply.isNullOrBlank()) {
                                                Text("Informe o fornecedor atual")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = viewModel.street?.lastPower ?: "",
                                        onValueChange = {
                                            viewModel.triedToSubmit = false
                                            viewModel.street =
                                                viewModel.street?.copy(lastPower = it)
                                        },
                                        label = { Text("Potência anterior") },
                                        isError = triedToSubmit && viewModel.street?.lastPower.isNullOrBlank(),
                                        supportingText = {
                                            if (triedToSubmit && viewModel.street?.lastPower.isNullOrBlank()) {
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
                                                if (viewModel.signPath == null) {
                                                    viewModel.responsibleError = null
                                                    viewModel.hasResponsible = false
                                                }
                                            },
                                            enabled = viewModel.signPath == null,
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
                                        if (viewModel.signPath == null) {

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
                                        if (viewModel.signPath != null) {

                                            Box(
                                                modifier = Modifier
                                                    .height(120.dp)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                            ) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(viewModel.signPath),
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

                        // =========================================================
                        //                      BOTÃO FINAL
                        // =========================================================
                        Button(
                            onClick = {
                                var hasError = false

                                if (viewModel.street?.currentSupply.isNullOrBlank()) hasError = true
                                if (viewModel.street?.lastPower.isNullOrBlank()) hasError = true

                                if (viewModel.hasResponsible == null) {
                                    viewModel.responsibleError = "Informe se possui responsável"
                                } else if (viewModel.hasResponsible == true &&
                                    (viewModel.responsible.isNullOrBlank() || !hasFullName(
                                        viewModel.responsible!!
                                    ))
                                ) {
                                    viewModel.responsibleError =
                                        "Nome e sobrenome do responsável"
                                } else if (viewModel.hasResponsible == true && viewModel.signPath == null) {
                                    viewModel.showSignScreen = true
                                } else if (!hasError) {
                                    confirmModal = true
                                }
                            },
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .fillMaxWidth()
                                .height(45.dp)
                                .align(Alignment.BottomCenter),
                        ) {
                            Text(
                                if (viewModel.hasResponsible == true && viewModel.signPath == null)
                                    "Coletar Assinatura"
                                else
                                    "Salvar e Finalizar",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                }

                // ────────────────────────────────────────────────
                //                  LISTA + HEADER
                // ────────────────────────────────────────────────
                else {

                    // ---------- HEADER OUSADO ----------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .padding(horizontal = 22.dp, vertical = 20.dp)
                            .align(Alignment.TopCenter)
                    ) {

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            // LINHA 1: qtd de ruas finalizadas
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = if (streets.size > 1)
                                        "${streets.size} ruas finalizadas"
                                    else
                                        "${streets.size} rua finalizada",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    )
                                )
                            }

                            // LINHA 2: tempo da execução
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )

                                Text(
                                    text = "Iniciada há ${Utils.timeSinceCreation(creationDate ?: "")}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                )
                            }

                            // LINHA 3: mini separador visual Apple-style
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 4.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            // LINHA 4: Status geral da execução (estilo Apple Fitness summary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                if (instructions != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {

                                        Column {
                                            Text(
                                                "Instruções",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            )
                                            Text(
                                                instructions,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                } else {
                                    Column {
                                        Text(
                                            "Status",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        )
                                        Text(
                                            if (streets.isNotEmpty()) "Em andamento" else "Não iniciado",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }


                    // ---------- LISTA ----------
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 175.dp, bottom = 130.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        items(streets) { street ->

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                Text(
                                    text = street.address,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }


                    // ---------- BOTÕES INFERIORES ----------
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        if (streets.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.showFinishForm = true },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(45.dp),
                            ) {
                                Text("Salvar e Finalizar")
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.loadExecutionData()
                                navController.navigate(Routes.DIRECT_EXECUTION_SCREEN_MATERIALS)
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(45.dp),
                        ) {
                            Text("Adicionar Nova Rua")
                        }
                    }
                }
            }


        }
    }

}


@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun PrevDirectExecutionHome() {
    val mockStreets = listOf(
        DirectExecutionStreet(
            directStreetId = 1L,
            address = "Rua das Flores, 123",
            latitude = -23.550520,
            longitude = -46.633308,
            photoUri = "content://media/external/images/media/12345",
            deviceId = "DEVICE_ABC_123",
            directExecutionId = 10L,
            description = "Ponto de verificação de energia",
            lastPower = "220V",
            finishAt = "2025-01-15T14:30:00",
            currentSupply = "Em operação"
        ),
        DirectExecutionStreet(
            directStreetId = 2L,
            address = "Avenida Central, 987",
            latitude = -23.551000,
            longitude = -46.630000,
            photoUri = null,
            deviceId = "DEVICE_XYZ_789",
            directExecutionId = 11L,
            description = "Vistoria de rotina",
            lastPower = "110V",
            finishAt = null,
            currentSupply = "Interrompido"
        ),
        DirectExecutionStreet(
            directStreetId = 2L,
            address = "Avenida Central, 987",
            latitude = -23.551000,
            longitude = -46.630000,
            photoUri = null,
            deviceId = "DEVICE_XYZ_789",
            directExecutionId = 11L,
            description = "Vistoria de rotina",
            lastPower = "110V",
            finishAt = null,
            currentSupply = "Interrompido"
        ),
        DirectExecutionStreet(
            directStreetId = 2L,
            address = "Avenida Central, 987",
            latitude = -23.551000,
            longitude = -46.630000,
            photoUri = null,
            deviceId = "DEVICE_XYZ_789",
            directExecutionId = 11L,
            description = "Vistoria de rotina",
            lastPower = "110V",
            finishAt = null,
            currentSupply = "Interrompido"
        ),
        DirectExecutionStreet(
            directStreetId = 2L,
            address = "Avenida Central, 987",
            latitude = -23.551000,
            longitude = -46.630000,
            photoUri = null,
            deviceId = "DEVICE_XYZ_789",
            directExecutionId = 11L,
            description = "Vistoria de rotina",
            lastPower = "110V",
            finishAt = null,
            currentSupply = "Interrompido"
        ),
        DirectExecutionStreet(
            directStreetId = 2L,
            address = "Avenida Central, 987",
            latitude = -23.551000,
            longitude = -46.630000,
            photoUri = null,
            deviceId = "DEVICE_XYZ_789",
            directExecutionId = 11L,
            description = "Vistoria de rotina",
            lastPower = "110V",
            finishAt = null,
            currentSupply = "Interrompido"
        ),
        DirectExecutionStreet(
            directStreetId = 2L,
            address = "Avenida Central, 987",
            latitude = -23.551000,
            longitude = -46.630000,
            photoUri = null,
            deviceId = "DEVICE_XYZ_789",
            directExecutionId = 11L,
            description = "Vistoria de rotina",
            lastPower = "110V",
            finishAt = null,
            currentSupply = "Interrompido"
        )
    )


    DirectExecutionHomeScreen(
        DirectExecutionViewModel(
            null,
            null,
            mockContractor = "ETAPA 1 - Prefeitura de Iguatama",
            mockCreationDate = Instant.now().toString(),
            mockStreets = mockStreets,

            ),
        rememberNavController()
    )
}