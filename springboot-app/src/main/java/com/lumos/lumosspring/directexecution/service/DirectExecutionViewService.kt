package com.lumos.lumosspring.directexecution.service

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.directexecution.dto.DirectExecutionDTOResponse
import com.lumos.lumosspring.directexecution.repository.DirectExecutionViewRepository
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

        val executions = viewRepository.getDirectExecutions(userUUID)

        return ResponseEntity.ok().body(executions)
    }

    fun getGroupedInstallations(): List<Map<String, JsonNode>> {
        return viewRepository.getGroupedInstallations()
    }

}