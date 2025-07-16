package com.lumos.ui.maintenance

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.TaskAlt
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.Maintenance
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.Loading
import com.lumos.ui.components.Tag
import com.lumos.utils.Utils
import java.time.Instant
import java.util.UUID

@Composable
fun MaintenanceHomeContent(
    maintenance: Maintenance,
    streets: List<MaintenanceStreet>,
    navController: NavHostController,
    loading: Boolean,
    newStreet: () -> Unit,
    newMaintenance: () -> Unit,
    finishMaintenance: (Maintenance) -> Unit,
    contractor: String?,
    back: () -> Unit,
    maintenanceSize: Int,
    finish: Boolean,
) {
    var confirmModal by remember { mutableStateOf(false) }
    val alertMessage = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem", "body" to "Você está na rua da execução neste momento?"
        )
    }

    val navigateBack = if (maintenanceSize > 1) {
        back
    } else {
        null
    }

    var showFinishForm by remember { mutableStateOf(false) }

    var pendingPointsError by remember { mutableStateOf<String?>(null) }
    var typeError by remember { mutableStateOf<String?>(null) }

    var maintenanceSend by remember {
        mutableStateOf(
            Maintenance(
                maintenanceId = maintenance.maintenanceId,
                contractId = maintenance.contractId,
                pendingPoints = true,
                quantityPendingPoints = maintenance.quantityPendingPoints,
                dateOfVisit = maintenance.dateOfVisit,
                type = maintenance.type,
                status = "FINISHED"
            )
        )
    }

    AppLayout(
        title = "Gerenciar manutenção",
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
            navController.navigate(Routes.DIRECT_EXECUTION_SCREEN)
        }
    ) { _, _ ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (confirmModal) {
                Confirm(body = "Deseja finalizar essa manutenção?", confirm = {
                    confirmModal = false
                    finishMaintenance(maintenanceSend)
                }, cancel = {
                    confirmModal = false
                })
            }

            if (loading) {
                Loading()
            }else if (finish) {
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
                            back()
                        }
                    ) {
                        Text("Voltar")
                    }
                }
            } else if (showFinishForm) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                maintenanceSend = maintenanceSend.copy(pendingPoints = true)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (maintenanceSend.pendingPoints)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent,
                                contentColor = if (maintenanceSend.pendingPoints)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (maintenanceSend.pendingPoints)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text("Sim")
                        }

                        OutlinedButton(
                            onClick = {
                                maintenanceSend = maintenanceSend.copy(
                                    pendingPoints = false,
                                    quantityPendingPoints = null
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!maintenanceSend.pendingPoints)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent,
                                contentColor = if (!maintenanceSend.pendingPoints)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (!maintenanceSend.pendingPoints)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text("Não")
                        }
                    }

                    if (maintenanceSend.pendingPoints) {
                        OutlinedTextField(
                            value = maintenanceSend.quantityPendingPoints?.toString() ?: "",
                            onValueChange = {
                                val intValue = it.toIntOrNull()
                                maintenanceSend =
                                    maintenanceSend.copy(quantityPendingPoints = intValue)
                                pendingPointsError = null
                            },
                            isError = pendingPointsError != null,
                            singleLine = true,
                            label = { Text("Número de pontos pendentes") },
                            supportingText = {
                                if (pendingPointsError != null) {
                                    Text(
                                        text = pendingPointsError ?: "",
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
                                typeError = null
                                maintenanceSend = maintenanceSend.copy(
                                    type = "Rural"
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (maintenanceSend.type == "Rural")
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent,
                                contentColor = if (maintenanceSend.type == "Rural")
                                    MaterialTheme.colorScheme.primary
                                else if (typeError != null)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (maintenanceSend.type == "Rural")
                                    MaterialTheme.colorScheme.primary
                                else if (typeError != null)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text("Rural")
                        }

                        OutlinedButton(
                            onClick = {
                                typeError = null
                                maintenanceSend = maintenanceSend.copy(
                                    type = "Urbana"
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (maintenanceSend.type == "Urbana")
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent,
                                contentColor = if (maintenanceSend.type == "Urbana")
                                    MaterialTheme.colorScheme.primary
                                else if (typeError != null)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (maintenanceSend.type == "Urbana")
                                    MaterialTheme.colorScheme.primary
                                else if (typeError != null)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text("Urbana")
                        }
                    }

                    if (typeError != null) {
                        Text(
                            typeError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Button(
                        onClick = {
                            var hasError = false

                            if (maintenanceSend.pendingPoints && maintenanceSend.quantityPendingPoints == null) {
                                pendingPointsError = "Qual a quantidade pendente?"
                                hasError = true
                            }

                            if (maintenanceSend.type.isBlank()) {
                                typeError = "Selecione a região da manutenção"
                                hasError = true
                            }

                            if (!hasError) {
                                confirmModal = true
                            }
                        },
                        modifier = Modifier.padding(30.dp)
                    ) {
                        Text(
                            "Enviar",
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
                                            ),
                                        contentAlignment = Alignment.Center
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,

                                            ) {
                                            Row {
                                                Text(
                                                    text = Utils.abbreviate(contractor.toString()),
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
                                            Text("Iniciada há ${Utils.timeSinceCreation(Instant.parse(maintenance.dateOfVisit))}")

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
                        streets,
                        key = { it.maintenanceStreetId }
                    ) { street ->
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
                    modifier = Modifier
                        .align(Alignment.BottomCenter) // <-- Aqui dentro de um Box
                ) {

                    if(streets.isNotEmpty()) {
                        Button(
                            onClick = {
                                showFinishForm = true
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text(
                                "Enviar manutenção",
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            newStreet()
                        },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text(
                            "Adicionar nova rua",
                            fontSize = 12.sp
                        )
                    }

                    if (maintenanceSize == 1) {
                        TextButton(
                            onClick = {
                                newMaintenance()
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Icon(
                                contentDescription = null,
                                imageVector = Icons.Default.Add,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Criar nova execução",
                                fontSize = 12.sp
                            )
                        }
                    }


                }
            }

        }


    }
}

@Composable
@Preview
fun PrevMaintenanceHomeContent() {
    MaintenanceHomeContent(
        maintenance = Maintenance(
            maintenanceId = UUID.randomUUID().toString(),
            contractId = 1,
            pendingPoints = false,
            quantityPendingPoints = null,
            dateOfVisit = "2025-07-14T15:42:30Z",
            type = "",
            status = ""
        ),
        streets = listOf(
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            ),
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            ),
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            ),
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            ),
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            ),
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            ),
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            ),
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            ),
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            ),
            MaintenanceStreet(
                maintenanceStreetId = UUID.randomUUID().toString(),
                maintenanceId = UUID.randomUUID().toString(),
                address = "Rua Wanderlan, 12 - Itamarati",
                latitude = null,
                longitude = null,
                comment = null,
                lastPower = null,
                lastSupply = null,
                currentSupply = null,
                reason = null
            )
        ),
        navController = rememberNavController(),
        loading = false,
        newStreet = {

        },
        newMaintenance = {

        },
        finishMaintenance = {

        },
        contractor = "PREFEITURA DE BELO HORIZONTE",
        back = {

        },
        maintenanceSize = 1,
        finish = false
    )
}
