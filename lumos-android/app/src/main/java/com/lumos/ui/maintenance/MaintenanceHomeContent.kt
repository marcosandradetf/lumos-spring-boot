package com.lumos.ui.maintenance

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.lumos.domain.model.Maintenance

@Composable
fun MaintenanceHomeContent(
    maintenances: List<Maintenance>,
    maintenanceId: String,
    navController: NavHostController,
    loading: Boolean,
    newStreet: () -> Unit,
    newMaintenance: () -> Unit,
    lastRoute: String?,
) {

}