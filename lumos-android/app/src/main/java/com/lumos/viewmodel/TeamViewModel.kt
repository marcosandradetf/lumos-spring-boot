package com.lumos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.domain.model.OperationalUser
import com.lumos.domain.model.Team
import com.lumos.repository.TeamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeamViewModel(
    private val repository: TeamRepository? = null,
    initialTeams: List<Team> = emptyList(),
    initialOperationalUsers: List<OperationalUser> = emptyList()
) : ViewModel() {

    private val _teams = MutableStateFlow(initialTeams)
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _operationalUsers = MutableStateFlow(initialOperationalUsers)
    val operationalUser: StateFlow<List<OperationalUser>> = _operationalUsers.asStateFlow()

    var loading by mutableStateOf(false)
    var finished by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    init {
        startFlow()
    }

    fun callGetOperationalAndTeams() {
        viewModelScope.launch(Dispatchers.IO) {
            loading = true
            try {
                withContext(Dispatchers.IO) {
                    repository?.callGetOperationalAndTeams()
                }
            } catch (e: Exception) {
                message = e.message ?: "ViewModel - Erro ao tentar sincronizar estoque"
            } finally {
                loading = false
            }
        }
    }

    fun queueUpdateTeams(teamId: Long, operationalUsers: Set<String>) {
        viewModelScope.launch {
            loading = true
            try {
                withContext(Dispatchers.IO) {
                    repository?.setTeamAndQueue(teamId, operationalUsers)
                }
                finished = true
            } catch (e: Exception) {
                message = e.message ?: "Erro ao tentar salvar"
            } finally {
                loading = false
            }
        }
    }

    private fun startFlow() {
        // teams
        repository?.getTeamsFlow()
            ?.onEach { _teams.value = it }
            ?.launchIn(viewModelScope)

        // users
        repository?.getUsersFlow()
            ?.onEach { _operationalUsers.value = it }
            ?.launchIn(viewModelScope)
    }


}