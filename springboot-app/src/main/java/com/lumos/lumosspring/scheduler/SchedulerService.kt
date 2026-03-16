package com.lumos.lumosspring.scheduler

import com.lumos.lumosspring.installation.service.direct_execution.DirectExecutionRegisterService
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

@Service
class SchedulerService(
    private val taskScheduler: TaskScheduler,
    @param:Lazy private val executionService: DirectExecutionRegisterService
) {

    private val tasks = ConcurrentHashMap<Long, ScheduledFuture<*>>()

    fun scheduleAutoConfirm(executionId: Long, tenantId: UUID, whenExecute: Instant) {
        cancelAutoConfirm(executionId)

        val future = taskScheduler.schedule(
            {
                try {
                    println("schedule ativado")
                    AsyncTenantContext.setTenant(tenantId)
                    executionService.confirmPreparedExecution(executionId)
                } finally {
                    tasks.remove(executionId)
                    AsyncTenantContext.clear()
                    println("tenant limpo")
                }
            },
            whenExecute
        )

        if (future != null) {
            tasks[executionId] = future
        }
    }

    fun cancelAutoConfirm(executionId: Long) {
        tasks.remove(executionId)?.cancel(false)
    }
}

object AsyncTenantContext {
    private val storage = ThreadLocal<UUID>()

    fun setTenant(id: UUID) = storage.set(id)
    fun getTenant(): UUID? = storage.get()
    fun clear() = storage.remove()
}