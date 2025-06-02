package com.lumos.ui.executions

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.Execution
import com.lumos.domain.model.Reserve
import com.lumos.navigation.BottomBar
import com.lumos.ui.components.AppLayout
import com.lumos.ui.viewmodel.ExecutionViewModel
import com.lumos.utils.ConnectivityUtils
import com.lumos.utils.Utils.formatDouble
import java.io.File

@Composable
fun MaterialScreen(
    executionViewModel: ExecutionViewModel,
    connection: ConnectivityUtils,
    context: Context,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    navController: NavHostController,
    notificationsBadge: String,
    pSelected: Int,
    onNavigateToExecution: (Long) -> Unit,
) {


    MaterialsContent(
        execution = TODO(),
        reserves = TODO(),
        onNavigateToHome = TODO(),
        onNavigateToMenu = TODO(),
        onNavigateToProfile = TODO(),
        onNavigateToNotifications = TODO(),
        context = TODO(),
        navController = TODO(),
        notificationsBadge = TODO(),
        pSelected = TODO(),
        select = TODO(),
        alert = TODO(),
        onDismiss = TODO(),
        onConfirmed = TODO(),
        takePhoto = {
            it.toString()
        },
    )

}

@Composable
fun MaterialsContent(
    execution: Execution,
    reserves: List<Reserve>,
    onNavigateToHome: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    context: Context,
    navController: NavHostController,
    notificationsBadge: String,
    pSelected: Int,
    select: (Long) -> Unit,
    alert: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: (Long) -> Unit,
    takePhoto: (uri: Uri) -> Unit,
) {
    val fileUri: MutableState<Uri?> = remember { mutableStateOf(null) }
    val imageSaved = remember { mutableStateOf(false) }
    val createFile: () -> Uri = {
        val file = File(context.filesDir, "photo_${System.currentTimeMillis()}.jpg")
        file.createNewFile() // Garante que o arquivo seja criado
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        Log.d("ImageDebug", "URI criada: $uri") // 📌 Adiciona log aqui

        uri
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                Log.d(
                    "ImageDebug",
                    "Foto tirada com sucesso! URI: ${fileUri.value}"
                ) // 🔍 Verifica se foi salvo
                fileUri.value?.let { uri ->
                    takePhoto(uri)
                    imageSaved.value = true
                }
            } else {
                Log.e("ImageDebug", "Erro ao tirar foto.")
            }
        }

    AppLayout(
        title = execution.streetName,
        pSelected = pSelected,
        sliderNavigateToMenu = onNavigateToMenu,
        sliderNavigateToHome = onNavigateToHome,
        sliderNavigateToNotifications = onNavigateToNotifications,
        sliderNavigateToProfile = onNavigateToProfile,
        navController = navController,
        navigateBack = onNavigateToMenu,
        context = context,

        notificationsBadge = notificationsBadge
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp),// deixa espaço pros botões
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp) // Espaço entre os cards
            ) {
                items(reserves) {
                    MaterialItem(material = it, finish = { /* update */ })
                }
            }

            FloatingActionButton(
                onClick = {
                    val newUri = createFile() // Gera um novo Uri
                    fileUri.value = newUri // Atualiza o estado
                    launcher.launch(newUri) // Usa a variável temporária, garantindo que o valor correto seja usado
                },
                modifier = Modifier
                    .align(Alignment.BottomStart) // <-- Aqui dentro de um Box
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            contentDescription = null,
                            imageVector = Icons.Rounded.PhotoCamera,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Tirar Foto",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp
                        )
                    }

                }
            }


            FloatingActionButton(
                onClick = { /* ... */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // <-- Também aqui
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            contentDescription = null,
                            imageVector = Icons.Rounded.Navigation,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "Navegar",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp
                        )
                    }

                }
            }
        }

    }
}

@Composable
fun MaterialItem(material: Reserve, finish: () -> Unit) {
    Card(
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(3.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSecondary,
            contentColor = MaterialTheme.colorScheme.onPrimary
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
                        imageVector = Icons.Default.Build,
                        contentDescription = "Local",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 📦 Nome e quantidade do material
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = material.materialName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Quantidade medida: ${formatDouble(material.materialQuantity)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            shape = RoundedCornerShape(10.dp),
                            onClick = {}
                        ) {
                            Text("Concluir")
                        }
                    }


                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Qtde.\nExecutada",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(10.dp))

                        IconButton(
                            onClick = {
//                                quantity += 1
//                                onQuantityChange(material.materialId, quantity)
                            },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                                .size(30.dp)
                                .padding(5.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Aumentar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }


                        Spacer(modifier = Modifier.height(6.dp)) // Espaçamento entre os ícones

                        Text(
                            text = formatDouble(material.materialQuantity),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        IconButton(
                            onClick = {
//                                if (quantity > 0) {
//                                    quantity -= 1
//                                    onQuantityChange(
//                                        material.materialId,
//                                        quantity
//                                    )
//                                }
                            },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                                .size(30.dp)
                                .padding(5.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Diminuir",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

        }
    }
}


@Preview
@Composable
fun PrevMScreen() {
    // Criando um contexto fake para a preview
    val fakeContext = LocalContext.current
    val values =
        Execution(
            streetId = 1,
            streetName = "Rua Dona Tina, 251",
            teamId = 12,
            teamName = "Equipe Norte",
            executionStatus = "PENDING",
            priority = true,
            type = "INSTALLATION",
            itemsQuantity = 7,
            creationDate = "",
            latitude = 0.0,
            longitude = 0.0,
            photoUri = "",
        )

    val reserves = listOf(
        Reserve(
            reserveId = 1,
            materialId = 1,
            materialName = "LED 120W",
            materialQuantity = 12.0,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 1,
            depositName = "GALPÃO BH",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "Elton Melo",
            phoneNumber = "31999998090"
        ),
        Reserve(
            reserveId = 1,
            materialId = 1,
            materialName = "BRAÇO DE 3,5",
            materialQuantity = 16.0,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 1,
            depositName = "GALPÃO BH",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "Elton Melo",
            phoneNumber = "31999998090"
        ),
        Reserve(
            reserveId = 1,
            materialId = 1,
            materialName = "BRAÇO DE 3,5",
            materialQuantity = 16.0,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 1,
            depositName = "GALPÃO BH",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "Elton Melo",
            phoneNumber = "31999998090"
        ),
        Reserve(
            reserveId = 1,
            materialId = 1,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090"
        ),
        Reserve(
            reserveId = 1,
            materialId = 1,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090"
        ),
        Reserve(
            reserveId = 1,
            materialId = 1,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090"
        ),
        Reserve(
            reserveId = 1,
            materialId = 1,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090"
        ),
        Reserve(
            reserveId = 1,
            materialId = 1,
            materialName = "CABO 1.5MM",
            materialQuantity = 30.4,
            reserveStatus = "APPROVED",
            streetId = 1,
            depositId = 2,
            depositName = "GALPÃO ITAPECIRICA",
            depositAddress = "Av. Raja Gabaglia, 1200 - Belo Horizonte, MG",
            stockistName = "João Gomes",
            phoneNumber = "31999999090"
        )
    )


    MaterialsContent(
        execution = values,
        reserves = reserves,
        onNavigateToHome = { },
        onNavigateToMenu = { },
        onNavigateToProfile = { },
        onNavigateToNotifications = { },
        context = fakeContext,
        navController = rememberNavController(),
        notificationsBadge = "12",
        pSelected = BottomBar.HOME.value,
        select = {},
        alert = false,
        onDismiss = {},
        onConfirmed = {},
        takePhoto = {}
    )
}