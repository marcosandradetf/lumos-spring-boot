package com.lumos.ui.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.work.impl.utils.forAll
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Maintenance
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NothingData
import com.lumos.utils.Utils
import java.time.Instant

@Composable
fun MaintenanceListContent(
    maintenances: List<Maintenance>,
    contracts: List<Contract>,
    navController: NavHostController,
    loading: Boolean,
    selectMaintenance: (String) -> Unit,
    newMaintenance: () -> Unit,
) {
    AppLayout(
        title = "Manutenções em andamento",
        selectedIcon = BottomBar.MAINTENANCE.value,
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
            if (loading) {
                Loading()
            }

            if(maintenances.isEmpty()) {
                NothingData("Nenhuma manutenção em andamento")
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(
                    maintenances,
                    key = { it.maintenanceId }
                ) { maintenance ->
                    Card(
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(3.dp)
                            .clickable {
                                selectMaintenance(maintenance.maintenanceId)
                            },
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
                                                text = Utils.abbreviate(
                                                    contracts
                                                        .find { it.contractId == maintenance.contractId }
                                                        ?.contractor.toString()
                                                ),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }

                                    Text("Iniciado há ${Utils.timeSinceCreation(Instant.parse(maintenance.dateOfVisit))}")
                                }
                            }
                        }
                    }

                }
            }

            FloatingActionButton(
                onClick = {
                    newMaintenance()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // <-- Aqui dentro de um Box
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        contentDescription = null,
                        imageVector = Icons.Default.Add,
                        modifier = Modifier.size(25.dp)
                    )
                    Text(
                        "Nova",
                        fontSize = 12.sp
                    )
                }
            }
        }



    }

}

@Preview
@Composable
fun PrevMaintenanceListContent() {
    MaintenanceListContent(
        maintenances = listOf(
            Maintenance(
                maintenanceId = "jfaoijfao",
                contractId = 1,
                pendingPoints = false,
                quantityPendingPoints = null,
                dateOfVisit = "2025-07-14T15:42:30Z",
                type = "",
                status = "IN_PROGRESS"
            )
        ),
        contracts = listOf(
            Contract(
                contractId = 1,
                contractor = "PREFEITURA DE BELO HORIZONTE",
                contractFile = "",
                createdBy = "",
                createdAt = "",
                status = "",
                startAt = "",
                deviceId = "",
                itemsIds = "",
                hasMaintenance = true
            )
        ),
        navController = rememberNavController(),
        loading = false,
        selectMaintenance = {  },
        newMaintenance = {  }
    )
}