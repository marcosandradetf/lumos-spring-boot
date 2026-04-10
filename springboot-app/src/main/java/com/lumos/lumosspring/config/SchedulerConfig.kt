package com.lumos.lumosspring.config

import com.lumos.lumosspring.notifications.repository.EmailQueueRepository
import com.lumos.lumosspring.notifications.service.EmailService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
class SchedulerConfig(
    private val emailQueueRepository: EmailQueueRepository,
    private val emailService: EmailService,
) {

    @Bean
    @Primary
    fun taskScheduler(): TaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 5
        scheduler.threadNamePrefix = "execution-confirm-"
        scheduler.initialize()
        return scheduler
    }


    // Roda todos os dias às 01:00 AM
    @Scheduled(cron = "0 0 1 * * *")
    fun retryPendingEmails() {
        val pending = emailQueueRepository.findAllByStatus("MANY_REQUESTS")

        for (email in pending) {
            try {
                emailService.sendEmail(email.to, email.subject, email.message)
                emailQueueRepository.deleteById(email.id)
            } catch (e: Exception) {
                // Se falhar de novo, mantém para o próximo dia ou loga o erro
                System.err.println("Falha na tentativa de reenvio para: " + email.to)
            }
        }
    }
}