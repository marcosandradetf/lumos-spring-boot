package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.DirectExecution
import com.lumos.lumosspring.execution.entities.DirectExecutionItem
import com.lumos.lumosspring.execution.entities.DirectExecutionStreet
import com.lumos.lumosspring.execution.entities.DirectExecutionStreetItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DirectExecutionRepository : CrudRepository<DirectExecution, Long> {
    fun findAllByDirectExecutionStatus(status: String): List<DirectExecution>

    fun findByContractId(contractId: Long): DirectExecution?
}
@Repository
interface DirectExecutionRepositoryItem : CrudRepository<DirectExecutionItem, Long> {
}

@Repository
interface DirectExecutionRepositoryStreet : CrudRepository<DirectExecutionStreet, Long> {
}

@Repository
interface DirectExecutionRepositoryStreetItem : CrudRepository<DirectExecutionStreetItem, Long> {
}
