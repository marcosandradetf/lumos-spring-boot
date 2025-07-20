package com.lumos.ui.maintenance

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.lumos.domain.model.Maintenance
import com.lumos.domain.model.MaintenanceJoin
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.ui.components.SignatureScreenLandscape
import com.lumos.ui.components.Tag
import com.lumos.utils.Utils
import com.lumos.utils.Utils.hasFullName
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID

class MaintenanceHomeViewModel : ViewModel() {
    var showSignScreen by mutableStateOf(false)
    var showFinishForm by mutableStateOf(false)
    var maintenanceSend by mutableStateOf<Maintenance?>(null)
    var hasResponsible by mutableStateOf<Boolean?>(null)
    var pendingPointsError by mutableStateOf<String?>(null)
    var typeError by mutableStateOf<String?>(null)
    var responsibleError by mutableStateOf<String?>(null)

    fun initializeMaintenance(maintenance: MaintenanceJoin) {
        if (maintenanceSend == null) {
            maintenanceSend = Maintenance(
                maintenanceId = maintenance.maintenanceId,
                contractId = maintenance.contractId,
                pendingPoints = true,
                quantityPendingPoints = maintenance.quantityPendingPoints,
                dateOfVisit = maintenance.dateOfVisit,
                type = maintenance.type,
                status = "FINISHED"
            )
        }
    }

    fun clear(){
        showSignScreen = false
        showFinishForm = false
        maintenanceSend = null
        hasResponsible = null
        pendingPointsError = null
        typeError = null
        responsibleError = null
    }
}


@Composable
fun MaintenanceHomeContent(
    maintenance: MaintenanceJoin,
    streets: List<MaintenanceStreet>,
    navController: NavHostController,
    loading: Boolean,
    newStreet: () -> Unit,
    newMaintenance: () -> Unit,
    finishMaintenance: (Maintenance?) -> Unit,
    back: () -> Unit,
    maintenanceSize: Int,
    viewModel: MaintenanceHomeViewModel,
) {
    var confirmModal by remember { mutableStateOf(false) }
    val alertMessage = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem", "body" to "Você está na rua da execução neste momento?"
        )
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initializeMaintenance(maintenance)
    }

    if (viewModel.showSignScreen) {
        SignatureScreenLandscape(
            description = maintenance.contractor,
            onSave = { bitmap, signDate ->
                val file = File(context.filesDir, "signature_${System.currentTimeMillis()}.png")
                file.createNewFile()
                val uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    viewModel.maintenanceSend = viewModel.maintenanceSend?.copy(
                        signPath = uri.toString(), signDate = signDate.toString()
                    )
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
            title = "Gerenciar manutenção",
            selectedIcon = BottomBar.MAINTENANCE.value,
            navigateBack = {
                viewModel.clear()
                back()
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
            }) { _, _ ->

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (confirmModal) {
                    Confirm(body = "Deseja finalizar essa manutenção?", confirm = {
                        confirmModal = false
                        finishMaintenance(viewModel.maintenanceSend)
                    }, cancel = {
                        confirmModal = false
                    })
                }

                if (loading) {
                    Loading()
                } else if (viewModel.showFinishForm) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Dados da manutenção",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(10.dp)
                        )

                        Text(
                            text = "Ficaram pontos pendentes?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.maintenanceSend =
                                        viewModel.maintenanceSend?.copy(pendingPoints = true)
                                }, colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (viewModel.maintenanceSend?.pendingPoints == true) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    )
                                    else Color.Transparent,
                                    contentColor = if (viewModel.maintenanceSend?.pendingPoints == true) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                ), border = BorderStroke(
                                    1.dp,
                                    if (viewModel.maintenanceSend?.pendingPoints == true) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Sim")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.maintenanceSend = viewModel.maintenanceSend?.copy(
                                        pendingPoints = false, quantityPendingPoints = null
                                    )
                                }, colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (viewModel.maintenanceSend?.pendingPoints == false) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    )
                                    else Color.Transparent,
                                    contentColor = if (viewModel.maintenanceSend?.pendingPoints == false) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                ), border = BorderStroke(
                                    1.dp,
                                    if (viewModel.maintenanceSend?.pendingPoints == false) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Não")
                            }
                        }

                        if (viewModel.maintenanceSend?.pendingPoints == true) {
                            OutlinedTextField(
                                value = viewModel.maintenanceSend?.quantityPendingPoints?.toString()
                                    ?: "",
                                onValueChange = {
                                    val intValue = it.toIntOrNull()
                                    viewModel.maintenanceSend =
                                        viewModel.maintenanceSend?.copy(quantityPendingPoints = intValue)
                                    viewModel.pendingPointsError = null
                                },
                                isError = viewModel.pendingPointsError != null,
                                singleLine = true,
                                label = { Text("Número de pontos pendentes") },
                                supportingText = {
                                    if (viewModel.pendingPointsError != null) {
                                        Text(
                                            text = viewModel.pendingPointsError ?: "",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                ),
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                            )
                        }

                        Text(
                            text = "Região da manutenção",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.typeError = null
                                    viewModel.maintenanceSend = viewModel.maintenanceSend?.copy(
                                        type = "Rural"
                                    )
                                }, colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (viewModel.maintenanceSend?.type == "Rural") MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    )
                                    else Color.Transparent,
                                    contentColor = if (viewModel.maintenanceSend?.type == "Rural") MaterialTheme.colorScheme.primary
                                    else if (viewModel.typeError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                ), border = BorderStroke(
                                    1.dp,
                                    if (viewModel.maintenanceSend?.type == "Rural") MaterialTheme.colorScheme.primary
                                    else if (viewModel.typeError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Rural")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.typeError = null
                                    viewModel.maintenanceSend = viewModel.maintenanceSend?.copy(
                                        type = "Urbana"
                                    )
                                }, colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (viewModel.maintenanceSend?.type == "Urbana") MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    )
                                    else Color.Transparent,
                                    contentColor = if (viewModel.maintenanceSend?.type == "Urbana") MaterialTheme.colorScheme.primary
                                    else if (viewModel.typeError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                ), border = BorderStroke(
                                    1.dp,
                                    if (viewModel.maintenanceSend?.type == "Urbana") MaterialTheme.colorScheme.primary
                                    else if (viewModel.typeError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Urbana")
                            }
                        }

                        if (viewModel.typeError != null) {
                            Text(
                                viewModel.typeError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Text(
                            text = "Assinatura do responsável?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.responsibleError = null
                                    viewModel.hasResponsible = true
                                }, colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (viewModel.hasResponsible == true) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    )
                                    else Color.Transparent,
                                    contentColor = if (viewModel.hasResponsible == true) MaterialTheme.colorScheme.primary
                                    else if (viewModel.hasResponsible == null && viewModel.responsibleError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                ), border = BorderStroke(
                                    1.dp,
                                    if (viewModel.hasResponsible == true) MaterialTheme.colorScheme.primary
                                    else if (viewModel.hasResponsible == null && viewModel.responsibleError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Sim")
                            }

                            OutlinedButton(
                                enabled = viewModel.maintenanceSend?.signPath == null, onClick = {
                                    viewModel.responsibleError = null
                                    viewModel.hasResponsible = false
                                }, colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (viewModel.hasResponsible == false) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    )
                                    else Color.Transparent,
                                    contentColor = if (viewModel.hasResponsible == false) MaterialTheme.colorScheme.primary
                                    else if (viewModel.hasResponsible == null && viewModel.responsibleError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                ), border = BorderStroke(
                                    1.dp,
                                    if (viewModel.hasResponsible == false) MaterialTheme.colorScheme.primary
                                    else if (viewModel.hasResponsible == null && viewModel.responsibleError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Não")
                            }
                        }
                        if (viewModel.hasResponsible == null && viewModel.responsibleError != null) {
                            Text(
                                viewModel.responsibleError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        if (viewModel.maintenanceSend?.signPath != null) {
                            Box(
                                modifier = Modifier
                                    .height(50.dp)
                                    .width(250.dp)
                                    .background(
                                        Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    ) // ou Color.LightGray
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(viewModel.maintenanceSend?.signPath)
                                            .build()
                                    ),
                                    contentDescription = "Assinatura",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Text(
                                text = "Ao finalizar o envio, presume-se que o responsável pela manutenção está ciente de que os dados fornecidos, incluindo a imagem da assinatura, poderão ser utilizados como comprovação da execução do serviço.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else if (viewModel.hasResponsible == true) {
                            Text(
                                text = "Solicite o Nome do Responsável",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                isError = viewModel.responsibleError != null,
                                value = viewModel.maintenanceSend?.responsible ?: "",
                                onValueChange = {
                                    viewModel.maintenanceSend = viewModel.maintenanceSend?.copy(
                                        responsible = it
                                    )
                                    viewModel.responsibleError = null
                                },
                                label = {
                                    Text(
                                        "Nome do Responsável",
                                        style = MaterialTheme.typography.bodySmall.copy( // Texto menor
                                            fontSize = 14.sp
                                        )
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(0.7f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                textStyle = MaterialTheme.typography.bodySmall.copy( // Texto menor
                                    fontSize = 14.sp
                                ),
                                supportingText = {
                                    if (viewModel.responsibleError != null) {
                                        Text(
                                            text = viewModel.responsibleError ?: "",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 16.dp, top = 7.dp)
                                        )
                                    }
                                })
                        }

                        Button(
                            onClick = {
                                var hasError = false

                                if (viewModel.hasResponsible == true && viewModel.maintenanceSend?.signPath == null && viewModel.maintenanceSend?.responsible != null && hasFullName(
                                        viewModel.maintenanceSend?.responsible!!
                                    )
                                ) {
                                    viewModel.showSignScreen = true
                                }

                                if (viewModel.maintenanceSend?.pendingPoints == true && viewModel.maintenanceSend?.quantityPendingPoints == null) {
                                    viewModel.pendingPointsError = "Qual a quantidade pendente?"
                                    hasError = true
                                }

                                if (viewModel.maintenanceSend?.type?.isBlank() == true) {
                                    viewModel.typeError = "Selecione a região da manutenção"
                                    hasError = true
                                }

                                if (viewModel.hasResponsible == null) {
                                    viewModel.responsibleError = "Informe se possui responsável"
                                    hasError = true
                                }

                                if (
                                    viewModel.hasResponsible == true
                                    && (viewModel.maintenanceSend?.responsible == null ||
                                            (viewModel.maintenanceSend?.responsible != null && !hasFullName(viewModel.maintenanceSend?.responsible!!)
                                                    )
                                            )
                                ) {
                                    viewModel.responsibleError = "Nome e sobrenome do responsável"
                                    hasError = true
                                }

                                if (!hasError) {
                                    confirmModal = true
                                }

                            }, modifier = Modifier.padding(30.dp)
                        ) {
                            Text(
                                if (viewModel.hasResponsible == true && viewModel.maintenanceSend?.signPath == null) "Continuar" else "Enviar",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (maintenanceSize > 1) 100.dp else 140.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .fillMaxWidth(0.9f)
                                    .padding(5.dp)

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
                                                .fillMaxHeight(0.5f)
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
                                                ), contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Build,
                                                contentDescription = "Build",
                                                tint = Color.White,
                                                modifier = Modifier.size(
                                                    16.dp
                                                )
                                            )
                                        }
                                    }


                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,

                                                ) {
                                                Row {
                                                    Text(
                                                        text = Utils.abbreviate(maintenance.contractor),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                }
                                            }
                                            Tag(
                                                "${streets.size} ruas finalizadas",
                                                color = MaterialTheme.colorScheme.inverseSurface,
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    contentDescription = null,
                                                    imageVector = Icons.Default.AccessTime,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    "Iniciada há ${
                                                        Utils.timeSinceCreation(
                                                            Instant.parse(
                                                                maintenance.dateOfVisit
                                                            )
                                                        )
                                                    }"
                                                )

                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Ruas finalizadas",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                        items(
                            streets, key = { it.maintenanceStreetId }) { street ->
                            ListItem(
                                colors = ListItemDefaults.colors(MaterialTheme.colorScheme.background),
                                headlineContent = {
                                    Text(
                                        text = street.address.trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn, // ou outro que combine
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

                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter) // <-- Aqui dentro de um Box
                    ) {

                        if (streets.isNotEmpty()) {
                            Button(
                                onClick = {
                                    viewModel.showFinishForm = true
                                }, modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text(
                                    "Enviar manutenção", fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                newStreet()
                            }, modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text(
                                "Adicionar nova rua", fontSize = 12.sp
                            )
                        }

                    }
                }

            }


        }
    }

}
//
//@Composable
//@Preview
//fun PrevMaintenanceHomeContent() {
//    MaintenanceHomeContent(
//        maintenance = MaintenanceJoin(
//            maintenanceId = UUID.randomUUID().toString(),
//            contractId = 1,
//            pendingPoints = false,
//            quantityPendingPoints = null,
//            dateOfVisit = "2025-07-14T15:42:30Z",
//            type = "",
//            status = "",
//            contractor = ""
//        ),
//        streets = listOf(
//            MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            ), MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            ), MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            ), MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            ), MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            ), MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            ), MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            ), MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            ), MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            ), MaintenanceStreet(
//                maintenanceStreetId = UUID.randomUUID().toString(),
//                maintenanceId = UUID.randomUUID().toString(),
//                address = "Rua Wanderlan, 12 - Itamarati",
//                latitude = null,
//                longitude = null,
//                comment = null,
//                lastPower = null,
//                lastSupply = null,
//                currentSupply = null,
//                reason = null
//            )
//        ),
//        navController = rememberNavController(),
//        loading = false,
//        newStreet = {
//
//        },
//        newMaintenance = {
//
//        },
//        finishMaintenance = {
//
//        },
//        back = {
//
//        },
//        maintenanceSize = 1,
//        viewModelAux = MaintenanceHomeViewModel(),
//    )
//}
