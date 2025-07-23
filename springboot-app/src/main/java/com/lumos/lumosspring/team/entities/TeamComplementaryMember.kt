package com.lumos.lumosspring.team.entities

import org.springframework.data.annotation.Id
import java.util.UUID

data class TeamComplementaryMemberId(
    val teamId: Long,
    val userId: UUID
)


data class TeamComplementaryMember(
    @Id
    val id: TeamComplementaryMemberId
)
