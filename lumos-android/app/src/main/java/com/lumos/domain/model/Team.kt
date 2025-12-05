package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class Team(
    @PrimaryKey
    val teamId: Long,
    val depositName: String,
    val teamName: String,
    val plateVehicle: String,
    val notificationTopic: String? = null
)

@Entity
data class OperationalUser(
    @PrimaryKey
    val userId: String,
    val completeName: String
)

data class OperationalAndTeamsResponse(
    val users: List<OperationalUser>,
    val teams: List<Team>
)

data class SendTeamEdit(val idTeam: Long, val userIds: List<String>)