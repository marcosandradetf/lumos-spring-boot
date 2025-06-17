package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.DirectExecution
import com.lumos.lumosspring.execution.entities.DirectExecutionItem
import com.lumos.lumosspring.execution.entities.DirectExecutionStreet
import org.springframework.data.jpa.repository.JpaRepository


interface DirectExecutionRepository : JpaRepository<DirectExecution, Long> {

}

interface DirectExecutionRepositoryItem : JpaRepository<DirectExecutionItem, Long> {
}

interface DirectExecutionRepositoryStreet : JpaRepository<DirectExecutionStreet, Long> {
}
