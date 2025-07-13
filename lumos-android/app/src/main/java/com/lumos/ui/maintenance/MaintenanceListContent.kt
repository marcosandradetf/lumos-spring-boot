package com.lumos.ui.maintenance

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.lumos.domain.model.Maintenance

@Composable
fun MaintenanceListContent(
    maintenances: List<Maintenance>,
    navController: NavHostController,
    loading: Boolean,
    selectMaintenance: (String) -> Unit,
    newMaintenance: () -> Unit,
    lastRoute: String?,
) {

}