package com.lumos.ui.premeasurementinstallation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import com.lumos.domain.model.ItemView
import com.lumos.domain.model.PreMeasurementInstallationStreet
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.repository.ExecutionStatus
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Loading
import com.lumos.utils.Utils
import com.lumos.viewmodel.PreMeasurementInstallationViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

@Composable
fun PreMeasurementInstallationStreetsScreen(
    viewModel: PreMeasurementInstallationViewModel,
    navController: NavHostController
) {

    Content(
        viewModel = viewModel,
        navController = navController,
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Content(
    viewModel: PreMeasurementInstallationViewModel,
    navController: NavHostController,
) {
    val currentStreets = viewModel.currentInstallationStreets
    val currentItems = viewModel.currentInstallationItems
    val errorMessage = viewModel.message
    val loading = viewModel.loading

    LaunchedEffect(currentItems) {
        if (currentItems.isNotEmpty()) navController.navigate(Routes.PRE_MEASUREMENT_INSTALLATION_MATERIALS)
    }


    AppLayout(
        title = Utils.abbreviate(viewModel.contractor ?: "PREFEITURA DE BELO HORIZONTE"),
        selectedIcon = BottomBar.EXECUTIONS.value,
        navigateBack = {
            navController.popBackStack()
        },
        navigateToMaintenance = {
            navController.navigate(Routes.MAINTENANCE)
        },
        navigateToStock = {
            navController.navigate(Routes.STOCK)
        },
        navigateToMore = {
            navController.navigate(Routes.MORE)
        },
        navigateToHome = {
            navController.navigate(Routes.HOME)
        },
        navigateToExecutions = {
            navController.navigate(Routes.INSTALLATION_HOLDER)
        }
    ) { _, showSnackBar ->

        if (loading) {
            Loading()
        } else if (currentStreets.isEmpty()) {
            FinishFormScreen(
                viewModel = viewModel,
                navController = navController
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (errorMessage != null) 60.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp) // Espaço entre os cards
            ) {

                items(currentStreets) { installation -> // Iteração na lista
                    val status = when (installation.status) {
                        ExecutionStatus.PENDING -> "PENDENTE"
                        ExecutionStatus.IN_PROGRESS -> "EM PROGRESSO"
                        ExecutionStatus.FINISHED -> "FINALIZADO"
                        else -> "STATUS DESCONHECIDO"
                    }

                    Card(
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(3.dp)
                            .clickable {
                                viewModel.setStreetAndItems(installation.preMeasurementStreetId)
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
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(10.dp)
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
                                        imageVector = Icons.Default.Power,
                                        contentDescription = "Local",
                                        tint = Color.White,
                                        modifier = Modifier.size(
                                            18.dp
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
                                                text = installation.address,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }

                                    // Informação extra
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Status",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(5.dp)
                                        ) {
                                            Text(
                                                text = status,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontSize = 12.sp
                                            )
                                        }

                                    }


                                    // Informação extra
                                    if (installation.priority)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Solicitado Prioridade",
                                                modifier = Modifier.padding(horizontal = 5.dp)
                                            )
                                            Column(
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector =
                                                        Icons.Default.Warning,
                                                    contentDescription = "Prioridade",
                                                    tint = Color(0xFFFC4705),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                }
                            }
                        }
                    }
                }
            }
        }

    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview()
@Composable
fun PrevStreetsScreen() {
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
            stockQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1002L,
            contractItemId = 2002L,
            materialName = "POSTE DE CONCRETO",
            materialQuantity = "3",
            requestUnit = "UN",
            specs = "9M",
            stockQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1003L,
            contractItemId = 2003L,
            materialName = "LUMINÁRIA LED",
            materialQuantity = "5",
            requestUnit = "UN",
            specs = "150W",
            stockQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1004L,
            contractItemId = 2004L,
            materialName = "PARAFUSO E ARRUELA",
            materialQuantity = "100",
            requestUnit = "UN",
            specs = "M8",
            stockQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1005L,
            contractItemId = 2005L,
            materialName = "DISJUNTOR",
            materialQuantity = "2",
            requestUnit = "UN",
            specs = "40A",
            stockQuantity = "0"
        ),
        ItemView(
            preMeasurementStreetId = UUID.randomUUID().toString(),
            materialStockId = 1006L,
            contractItemId = 2006L,
            materialName = "TERMINAL DE CABO",
            materialQuantity = "200",
            requestUnit = "UN",
            specs = "10MM",
            stockQuantity = "0"
        )
    )

    Content(
        viewModel = PreMeasurementInstallationViewModel(
            repository = null,
            contractRepository = null,
            mockStreets = mockInstallationStreets,
            mockItems = mockInstallationItems
        ),
        navController = rememberNavController()
    )
}

