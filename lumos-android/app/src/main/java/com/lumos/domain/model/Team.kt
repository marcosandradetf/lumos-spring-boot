package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Team(
    @PrimaryKey
    val teamId: Long,
    val depositName: String,
    val teamName: String,
    val plateVehicle: String
)

@Entity
data class OperationalUsers(
    @PrimaryKey
    val userId: String,
    val completeName: String
)

data class OperationalAndTeamsResponse(
    val users: List<OperationalUsers>,
    val teams: List<Team>
)

data class SendTeamEdit(val idTeam: Long, val userIds: List<String>)