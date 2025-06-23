package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.DirectExecution
import com.lumos.lumosspring.execution.entities.DirectExecutionItem
import com.lumos.lumosspring.execution.entities.DirectExecutionStreet
import org.springframework.data.repository.CrudRepository


interface DirectExecutionRepository : CrudRepository<DirectExecution, Long> {
    fun findAllByDirectExecutionStatus(status: String): List<DirectExecution>

    fun findByContractId(contractId: Long): DirectExecution?
}

interface DirectExecutionRepositoryItem : CrudRepository<DirectExecutionItem, Long> {
}


interface DirectExecutionRepositoryStreet : CrudRepository<DirectExecutionStreet, Long> {
}
