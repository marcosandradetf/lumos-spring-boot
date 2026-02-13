package com.lumos.lumosspring.installation.service.direct_execution

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.installation.dto.direct_execution.DirectExecutionDTOResponse
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionViewRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*


@Service
class DirectExecutionViewService(
    private val viewRepository: DirectExecutionViewRepository,
) {

    fun getDirectExecutions(strUUID: String?): ResponseEntity<List<DirectExecutionDTOResponse>> {
        val userUUID = try {
            UUID.fromString(strUUID)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Usuário não encontrado")
        }

        val executions = viewRepository.getDirectExecutions(operatorUUID = userUUID)

        return ResponseEntity.ok().body(executions)
    }

    fun getDirectExecutionsV2(teamId: Long, status: String): ResponseEntity<List<DirectExecutionDTOResponse>> {
        val executions = viewRepository.getDirectExecutions(teamId = teamId, status = status)

        return ResponseEntity.ok().body(executions)
    }

    fun getGroupedInstallations(): List<Map<String, JsonNode>> {
        return viewRepository.getGroupedInstallations()
    }

}