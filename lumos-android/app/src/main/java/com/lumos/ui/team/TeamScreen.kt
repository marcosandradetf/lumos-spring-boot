package com.lumos.ui.team

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FireTruck
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lumos.domain.model.OperationalUser
import com.lumos.domain.model.Team
import com.lumos.midleware.SecureStorage
import com.lumos.navigation.BottomBar
import com.lumos.navigation.Routes
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.Confirm
import com.lumos.ui.components.ConfirmNavigation
import com.lumos.ui.components.Loading
import com.lumos.ui.components.NoInternet
import com.lumos.ui.components.NothingData
import com.lumos.ui.components.Tag
import com.lumos.ui.components.UserAvatar
import com.lumos.utils.ConnectivityUtils
import com.lumos.viewmodel.TeamViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckTeamScreen(
    viewModel: TeamViewModel,
    navController: NavHostController,
    currentScreen: Int,
    secureStorage: SecureStorage
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var confirmModal by remember { mutableStateOf(false) }
    var hasInternet by remember { mutableStateOf(true) }

    val users by viewModel.operationalUser.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val currentUserId = secureStorage.getUserUuid()
    val currentUserName = secureStorage.getFullName()

    val scope = rememberCoroutineScope()

    var selectedTeamId by remember { mutableStateOf<Long?>(null) }
    var selectedTeamName by remember { mutableStateOf<String?>(null) }
    var notificationTopic by remember { mutableStateOf<String?>(null) }

    var operationalUsersIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var operationalNames by remember { mutableStateOf<Set<String>>(emptySet()) }

    var queryUsers by remember { mutableStateOf("") }
    var queryTeams by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }

    val filteredUsers = remember(users, queryUsers) {
        if (queryUsers.isBlank()) users else users.filter {
            val q = queryUsers.trim().lowercase()
            it.completeName.lowercase().contains(q)
        }
    }

    val filteredTeams = remember(teams, queryTeams) {
        if (queryTeams.isBlank()) teams else teams.filter {
            val q = queryTeams.trim().lowercase()
            it.plateVehicle.lowercase().contains(q)
        }
    }


    LaunchedEffect(Unit) {
        hasInternet = ConnectivityUtils.hasRealInternetConnection()
        if (hasInternet) viewModel.callGetOperationalAndTeams()

        if (currentUserId != null) {
            operationalUsersIds = operationalUsersIds + currentUserId
        }

        if (currentUserName != null) {
            operationalNames = operationalNames + currentUserName
        }
    }

    LaunchedEffect(teams) {
        if (teams.isNotEmpty() && currentUserName == null) {
            val currentUserName = users.find { it.userId == currentUserId }?.completeName
            currentUserName?.let {
                operationalNames = operationalNames + it
            }
        }
    }

    if (action.isNotBlank()) {
        ConfirmNavigation(
            confirm = {
                if (action == "back") {
                    navController.popBackStack()
                } else {
                    navController.navigate(action)
                }
            },
            onDismiss = {
                action = ""
            }
        )
    }

    if (confirmModal) {
        Confirm(
            title = "Deseja confirmar?",
            body = """
            Caminhão 
                • $selectedTeamName
            
            Colaboradores
                • ${
                operationalNames.joinToString(
                    """    
                • """
                )
            }
            """.trimIndent(),
            confirm = {
                confirmModal = false
                viewModel.queueUpdateTeams(selectedTeamId!!, operationalUsersIds, notificationTopic)
            }, cancel = {
                confirmModal = false
            },
            textAlign = TextAlign.Left
        )
    }

    AppLayout(
        title = "Confirmação de equipe",
        selectedIcon = currentScreen,
        navigateBack = {
            action = "back"
        },
        navigateToHome = {
            action = Routes.HOME
        },
        navigateToMore = {
            action = Routes.MORE
        },
        navigateToMaintenance = {
            action = Routes.MAINTENANCE
        },
        navigateToExecutions = {
            action = Routes.INSTALLATION_HOLDER
        },
        navigateToStock = {
            action = Routes.STOCK
        }
    ) { _, showSnackBar ->

        if (viewModel.message != null) {
            showSnackBar(viewModel.message!!, null) { viewModel.message = null }
            viewModel.message = null
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (viewModel.finished) {
                if (viewModel.loading) {
                    Loading("Carregando...")
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp)
                    ) {
                        Icon(
                            contentDescription = null,
                            imageVector = Icons.Default.CheckCircle,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        )
                        Text(
                            "Equipe registrada com sucesso!",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Seu registro foi salvo e está em processamento.",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                navController.popBackStack()
                                viewModel.finished = false
                            }
                        ) {
                            Text("Voltar a tela anterior")
                        }
                    }
                }
            } else {

                PullToRefreshBox(
                    isRefreshing = viewModel.loading,
                    onRefresh = {
                        scope.launch {
                            hasInternet = ConnectivityUtils.hasRealInternetConnection()
                            if (hasInternet) viewModel.callGetOperationalAndTeams()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!hasInternet) {
                        NoInternet()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (!hasInternet) 90.dp else 0.dp)
                            .padding(bottom = 80.dp)
                    ) {
                        // 🔷 Header com dados do depósito e estoquistas
                        if (teams.isNotEmpty())
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .clip(shape = RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(16.dp)
                                    .clickable {
                                        showBottomSheet = true
                                    }) {

                                Text(
                                    text = selectedTeamName ?: "Selecione a equipe",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                        if (users.isEmpty() || teams.isEmpty()) {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                NothingData("Nenhum dado encontrado, puxe para baixo para tentar atualizar")
                            }
                        } else if (selectedTeamId != null) {
                            // 🔷 Lista de usuários
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    OutlinedTextField(
                                        value = queryUsers,
                                        onValueChange = { queryUsers = it },
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        placeholder = { Text("Buscar pelo nome...") },
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }

                                items(
                                    items = filteredUsers,
                                    key = { it.userId }
                                ) { user ->
                                    val isSelected = user.userId in operationalUsersIds
                                    val bg by animateColorAsState(
                                        targetValue = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface,
                                        label = "bgAnim"
                                    )
                                    val border = if (isSelected)
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    else
                                        BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant
                                        )

                                    Surface(
                                        color = bg,
                                        shape = RoundedCornerShape(14.dp),
                                        tonalElevation = if (isSelected) 2.dp else 0.dp,
                                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                                        border = border,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(
                                                role = Role.Checkbox,
                                                onClick = {
                                                    if (user.userId == currentUserId) {
                                                        viewModel.message =
                                                            "Seu nome será mantido automaticamente."
                                                    } else if (isSelected) {
                                                        operationalUsersIds =
                                                            operationalUsersIds - user.userId
                                                        operationalNames =
                                                            operationalNames - user.completeName
                                                    } else {
                                                        operationalUsersIds =
                                                            operationalUsersIds + user.userId
                                                        operationalNames =
                                                            operationalNames + user.completeName
                                                    }
                                                }
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Avatar com iniciais
                                            UserAvatar(user.completeName)

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = user.completeName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            // Indicador de seleção
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            NothingData("Aguardando a seleção da equipe\npara listar os colaboradores")
                        }
                    }
                }

                // BottomSheet de seleção de depósito
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {

                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selecionar equipe/caminhão",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = {
                                    showBottomSheet = false
                                }) { Text("Cancelar") }
                            }

                            // Busca
                            OutlinedTextField(
                                value = queryTeams,
                                onValueChange = { queryTeams = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                placeholder = { Text("Buscar por placa...") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null
                                    )
                                }
                            )

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Lista
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxHeight(0.72f)
                                    .padding(horizontal = 8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = filteredTeams,
                                    key = { it.teamId }
                                ) { team ->
                                    TeamRowItem(
                                        team = team,
                                        selected = team.teamId == selectedTeamId,
                                        onClick = {
                                            notificationTopic = team.notificationTopic
                                            selectedTeamId = team.teamId
                                            selectedTeamName =
                                                "Caminhão placa - ${team.plateVehicle}"
                                            viewModel.message =
                                                "Caminhão da equipe selecionado com suceso"
                                            showBottomSheet = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Botao de seleção de equipe/salvar
                if (teams.isNotEmpty())
                    Button(
                        onClick = {
                            if (selectedTeamId == null) {
                                showBottomSheet = true
                            } else if (operationalUsersIds.isEmpty()) {
                                viewModel.message = "Selecione os colaboradores presentes"
                            } else {
                                confirmModal = true
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(20.dp)
                            .height(48.dp) // altura mais achatada
                            .defaultMinSize(minWidth = 200.dp) // aumenta largura mínima
                            .clip(RoundedCornerShape(50)) // borda bem arredondada
                            .shadow(
                                8.dp,
                                RoundedCornerShape(50)
                            ), // sombra leve para efeito flutuante
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp, pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = if (selectedTeamId == null) "Selecionar Equipe" else "Registrar Equipe",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
            }

        }
    }


}

@Composable
private fun TeamRowItem(
    team: Team,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        label = "teamRowBg"
    )
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ícone/“avatar”
            Icon(
                imageVector = Icons.Default.FireTruck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = team.teamName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Tag("PLACA - " + team.plateVehicle, color = MaterialTheme.colorScheme.primary)

                    RadioButton(
                        selected = selected,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Composable
@Preview(showBackground = true)
fun PrevTeam() {
    val fakeTeams = listOf(
        Team(
            teamId = 1,
            depositName = "CAMINHÃO EQUIPE CENTRO OESTE 03",
            teamName = "C.O 03 - RNU1B59",
            plateVehicle = "RNU1B59"
        ),
        Team(
            teamId = 2,
            depositName = "CAMINHÃO EQUIPE CENTRO OESTE 02",
            teamName = "C.O 02 - QXO9182",
            plateVehicle = "QXO9182"
        ),
        Team(
            teamId = 3,
            depositName = "CAMINHÃO EQUIPE CENTRO OESTE 04",
            teamName = "C.O 04 - RVD5C22",
            plateVehicle = "RVD5C22"
        ),
    )

    val fakeUsers = listOf(
        OperationalUser(
            userId = "597ee064-ac88-4c8e-9fd3-840391ed70c3",
            completeName = "Cleyton Garcia"
        ),
        OperationalUser(
            userId = UUID.randomUUID().toString(),
            completeName = "Felipe Henrique"
        ),
        OperationalUser(
            userId = UUID.randomUUID().toString(),
            completeName = "Elton Ferreira"
        ),
        OperationalUser(
            userId = UUID.randomUUID().toString(),
            completeName = "Eduardo Oliveira"
        ),
        OperationalUser(
            userId = UUID.randomUUID().toString(),
            completeName = "Cleyton Garcia"
        ),
    )

    val viewModel = TeamViewModel(
        initialTeams = fakeTeams,
        initialOperationalUsers = fakeUsers
    )

    CheckTeamScreen(
        viewModel = viewModel,
        navController = rememberNavController(),
        currentScreen = BottomBar.MAINTENANCE.value,
        secureStorage = SecureStorage(LocalContext.current)
    )
}

