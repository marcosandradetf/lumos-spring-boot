package com.lumos.ui.maintenance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    back: () -> Unit,
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
        navigateBack = back,
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

        if (confirmModal) {
            Confirm(body = "Deseja finalizar essa manutenção?", confirm = {
                confirmModal = false
                finishMaintenance()
            }, cancel = {
                confirmModal = false
            })
        }

    }



    }