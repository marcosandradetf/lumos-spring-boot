package com.lumos.ui.premeasurementinstallation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.lumos.navigation.Routes
import com.lumos.utils.Utils.hasFullName
import com.lumos.viewmodel.PreMeasurementInstallationViewModel

@Composable
fun FinishFormScreen(
    viewModel: PreMeasurementInstallationViewModel,
    navController: NavHostController
) {
    val showFinishForm = viewModel.showFinishForm
    val currentInstallationId = viewModel.installationID
    val currentStreets = viewModel.currentInstallationStreets
    val triedToSubmit = viewModel.triedToSubmit

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (!showFinishForm) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.TaskAlt,
                    contentDescription = "Tarefa concluída",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (currentInstallationId == null) "Missão Cumprida!"
                    else if (currentStreets.isEmpty()) "Quase lá!"
                    else "Bom trabalho!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text =
                        if (currentInstallationId == null) "Instalação concluída com sucesso.\nOs dados serão enviados para o sistema."
                        else if (currentStreets.isEmpty()) "Todas as ruas foram concluídas com sucesso.\nToque no botão abaixo para preencher os dados restantes."
                        else "Esse ponto foi concluído com sucesso.\nOs dados serão enviados para o sistema.\n\nToque no botão abaixo para selecionar e iniciar o próximo ponto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // HEADER PRINCIPAL
                Text(
                    text = "Última Etapa",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )


                // =========================================================
                //                  SEÇÃO — RESPONSÁVEL
                // =========================================================
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {

                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Coletar Assinatura do Responsável?",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {

                            // Botão SIM
                            FilterChip(
                                selected = viewModel.hasResponsible == true,
                                onClick = {
                                    viewModel.responsibleError = null
                                    viewModel.hasResponsible = true
                                    viewModel.message = "Solicite o Nome do Responsável"
                                },
                                label = { Text("Sim") }
                            )

                            // Botão NÃO
                            FilterChip(
                                selected = viewModel.hasResponsible == false,
                                onClick = {
                                    if (viewModel.signPhotoUri == null) {
                                        viewModel.responsibleError = null
                                        viewModel.hasResponsible = false
                                    }
                                },
                                enabled = viewModel.signPhotoUri == null,
                                label = { Text("Não") }
                            )
                        }

                        if (viewModel.hasResponsible == null && viewModel.responsibleError != null) {
                            Text(
                                viewModel.responsibleError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // RESPONSÁVEL
                        if (viewModel.hasResponsible == true) {

                            // Nome antes da assinatura
                            if (viewModel.signPhotoUri == null) {

                                OutlinedTextField(
                                    value = viewModel.responsible ?: "",
                                    onValueChange = {
                                        viewModel.responsible = it
                                        viewModel.responsibleError = null
                                    },
                                    label = { Text("Nome do Responsável") },
                                    isError = viewModel.responsibleError != null,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                if (viewModel.responsibleError != null) {
                                    Text(
                                        viewModel.responsibleError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            // Assinatura
                            if (viewModel.signPhotoUri != null) {

                                Box(
                                    modifier = Modifier
                                        .height(120.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            viewModel.signPhotoUri
                                        ),
                                        contentDescription = "Assinatura",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = "Ao finalizar o envio, presume-se que o responsável pela manutenção está ciente de que os dados fornecidos, incluindo a imagem da assinatura, poderão ser utilizados como comprovação da execução do serviço.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (viewModel.signPhotoUri == null && hasFullName(viewModel.responsible ?: "")) {
                                Spacer(Modifier.height(25.dp))
                                Button(
                                    onClick = {
                                        viewModel.showSignScreen = true
                                    },
                                    modifier = Modifier
                                        .padding(bottom = 10.dp)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(32.dp)
                                ) {
                                    Text(
                                        "Coletar Assinatura"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================
        //                      BOTÃO FINAL
        // =========================================================
        Button(
            onClick = {
                if (currentInstallationId == null) {
                    navController.navigate(Routes.HOME){
                        popUpTo(Routes.PRE_MEASUREMENT_INSTALLATION_FLOW) { inclusive = true }
                    }
                } else if (currentStreets.isEmpty()) {
                    if (!showFinishForm) {
                        viewModel.showFinishForm = true
                    } else if (viewModel.hasResponsible == null) {
                        viewModel.responsibleError = "Informe se possui responsável"
                    } else if (viewModel.hasResponsible == true && !hasFullName(viewModel.responsible ?: "")) {
                        viewModel.responsibleError =
                            "Nome e sobrenome do responsável"
                    } else if (viewModel.hasResponsible == true && viewModel.signPhotoUri == null) {
                        viewModel.showSignScreen = true
                    } else {
                        viewModel.openConfirmation = true
                    }
                } else {
                    viewModel.loading = true
                    navController.getBackStackEntry(Routes.PRE_MEASUREMENT_INSTALLATION_FLOW)
                        .savedStateHandle["route_event"] = Routes.PRE_MEASUREMENT_INSTALLATION_STREETS

                    navController.popBackStack()
                }
            },
            modifier = Modifier
                .padding(bottom = 10.dp)
                .fillMaxWidth()
                .height(50.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(32.dp)
        ) {
            Text(
                if (currentInstallationId == null) "Sair"
                else if (viewModel.hasResponsible == true && viewModel.signPhotoUri == null && showFinishForm) "Continuar"
                else if (showFinishForm && currentStreets.isEmpty()) "Salvar e Finalizar"
                else if (currentStreets.isEmpty()) "Preencher Dados da Instalação"
                else "Selecionar Próximo Ponto"
            )
        }
    }
}