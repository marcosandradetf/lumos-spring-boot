package com.lumos.ui.maintenance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.utils.Utils
import java.util.UUID

@Composable
fun MaintenanceHomeContent(
    streets: List<MaintenanceStreet>,
    maintenanceId: UUID,
    navController: NavHostController,
    loading: Boolean,
    newStreet: () -> Unit,
    newMaintenance: () -> Unit,
    lastRoute: String?,
    finishMaintenance: () -> Unit,
    contractor: String?,
) {
    var confirmModal by remember { mutableStateOf(false) }
    val alertMessage = remember {
        mutableStateMapOf(
            "title" to "Título da mensagem", "body" to "Você está na rua da execução neste momento?"
        )
    }

    AppLayout(
        title = "MANUTENÇÃO - ${Utils.abbreviate(contractor.toString())}",
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
            if (confirmModal) {
                Confirm(body = "Deseja finalizar essa manutenção?", confirm = {
                    confirmModal = false
                    finishMaintenance()
                }, cancel = {
                    confirmModal = false
                })
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(
                    streets,
                    key = { it.maintenanceStreetId }
                ) { street ->


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