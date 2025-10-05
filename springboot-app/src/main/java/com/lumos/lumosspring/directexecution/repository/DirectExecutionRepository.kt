package com.lumos.lumosspring.directexecution.repository

import com.lumos.lumosspring.directexecution.model.*
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

@Repository
interface DirectExecutionExecutorRepository : CrudRepository<DirectExecutionExecutor, Long> {
}