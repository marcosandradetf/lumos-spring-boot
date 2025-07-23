package com.lumos.lumosspring.team.repository

import com.lumos.lumosspring.team.entities.TeamComplementaryMember
import com.lumos.lumosspring.team.entities.TeamComplementaryMemberId
import org.springframework.data.repository.CrudRepository

interface TeamComplementaryMemberRepository : CrudRepository<TeamComplementaryMember, TeamComplementaryMemberId>
