package com.lumos.ui.measurement

import android.content.Context
import android.widget.ScrollView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.content.MediaType.Companion.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.R
import com.lumos.data.repository.ContractRepository
import com.lumos.domain.model.Contract
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import com.lumos.ui.home.HomeScreen
import com.lumos.ui.viewmodel.ContractViewModel
import com.lumos.ui.viewmodel.StockViewModel
import com.lumos.utils.ConnectivityUtils

@Composable
fun ContractsScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    contractViewModel: ContractViewModel,
    connection: ConnectivityUtils,
    navController: NavHostController,

    ) {
    val contracts by contractViewModel.contracts

    ContractsScreenContent(
        contracts = contracts,
        onNavigateToHome = onNavigateToHome,
        onNavigateToMenu = onNavigateToMenu,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToNotifications = onNavigateToNotifications,
        context = context,
        navController = navController
    )
}

@Composable
fun ContractsScreenContent(
    contracts: List<Contract>,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    navController: NavHostController,
) {
    AppLayout(
        title = "Contratos",
        pSelected = BottomBar.MENU.value,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        navigateBack = onNavigateToMenu,
        context = context
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp) // Espaço entre os cards
        ) {
            items(contracts) { contract -> // Iteração na lista
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Primeira linha (Nome + Ícone Expand)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = contract.contractor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(onClick = { /* Ação do ícone */ }) {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Expandir",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Informação extra
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Horário",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp) // Ajuste do tamanho do ícone
                            )
                            Text(
                                modifier = Modifier.padding(start = 5.dp),
                                text = "Criado por Gabriela há 10 minutos",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }


                        // Linha inferior (Contrato + Ações)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 25.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally, // Centraliza os itens horizontalmente
                                verticalArrangement = Arrangement.Center // Mantém o alinhamento vertical
                            ) {
                                Text(
                                    text = "Contrato",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 2.dp) // Pequeno espaço entre o texto e o ícone
                                )

                                Icon(
                                    imageVector = Icons.Default.Downloading,
                                    contentDescription = "Baixar Contrato",
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(24.dp) // Ajuste do tamanho do ícone
                                )

                            }


                            TextButton(onClick = { /* Ação */ }) {
                                Text(
                                    text = "Iniciar Pré-Medição",
                                    color = Color(0xFFFF2F55),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}


@Preview(showBackground = true)
@Composable
fun PrevContract() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val values =
        listOf(
            Contract(
                contractId = 1,
                contractor = "Prefeitura Municipal de Belo Horizonte",
                contractFile = "arquivo.pdf",
                status = ""
            ),
            Contract(
                contractId = 1,
                contractor = "Prefeitura Municipal de Ibirité",
                contractFile = "arquivo.pdf",
                status = ""
            )
        )


    ContractsScreenContent(
        contracts = values,
        onNavigateToHome = { },
        onNavigateToMenu = { },
        onNavigateToProfile = { },
        onNavigateToNotifications = { },
        context = fakeContext,
        navController = rememberNavController()
    )
}

