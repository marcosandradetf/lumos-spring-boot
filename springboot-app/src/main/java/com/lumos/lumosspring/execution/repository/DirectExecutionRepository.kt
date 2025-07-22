package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.DirectExecution
import com.lumos.lumosspring.execution.entities.DirectExecutionItem
import com.lumos.lumosspring.execution.entities.DirectExecutionStreet
import com.lumos.lumosspring.execution.entities.DirectExecutionStreetItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Indexed

@Indexed
interface DirectExecutionRepository : CrudRepository<DirectExecution, Long> {
    fun findAllByDirectExecutionStatus(status: String): List<DirectExecution>

    fun findByContractId(contractId: Long): DirectExecution?
}
@Indexed
interface DirectExecutionRepositoryItem : CrudRepository<DirectExecutionItem, Long> {
}

@Indexed
interface DirectExecutionRepositoryStreet : CrudRepository<DirectExecutionStreet, Long> {
}

@Indexed
interface DirectExecutionRepositoryStreetItem : CrudRepository<DirectExecutionStreetItem, Long> {
}
