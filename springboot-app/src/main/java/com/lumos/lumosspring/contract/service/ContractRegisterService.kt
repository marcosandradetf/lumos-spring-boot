package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.contract.dto.ContractItemBalance
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDTO
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.notifications.service.FCMService
import com.lumos.lumosspring.util.ContractStatus
import com.lumos.lumosspring.util.NotificationType
import com.lumos.lumosspring.util.Utils
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
class ContractRegisterService(
    private val contractRepository: ContractRepository,
    private val fCMService: FCMService,
) {
    fun validateContract(
        contractId: Long,
        approved: Boolean,
        reason: String?,
        ibgeCode: String
    ): ResponseEntity<Any> {
        val contract = contractRepository.findById(contractId).orElseThrow {
            Utils.BusinessException("Contrato $contractId não encontrado")
        }

        if(approved) {
            val otherContracts = contractRepository.findAllByIbgeCode(ibgeCode)
                .map {
                    it.status = ContractStatus.EXPIRED
                    it
                }
            contractRepository.saveAll(otherContracts)

            val tenants = otherContracts.map {
                Triple(it.tenantId, it.contractor, it.contractNumber)
            }
            tenants.forEach { tenant ->
                fCMService.sendNotificationForTopic(
                    title = "Contrato Expirado",
                    body = "O Contrato ${tenant.third} - ${tenant.second} expirou no sistema e não está mais disponível para operação",
                    notificationCode = "ANALISTA_${tenant.first}",
                    type = NotificationType.CONTRACT,
                    platform = FCMService.TargetPlatform.WEB,
                    uri = "/contratos/expirados",
                    isPopUp = true,
                    subtitle = "Foi detectado que esse contrato agora está ativo sob outra empresa.",
                    tenant = tenant.first.toString()
                )
            }

            fCMService.sendNotificationForTopic(
                title = "Boa Notícia",
                body = "O Contrato ${contract.contractNumber} - ${contract.contractor} está aprovado no sistema e está disponível para operação",
                notificationCode = "ANALISTA_${contract.tenantId}",
                type = NotificationType.CONTRACT,
                platform = FCMService.TargetPlatform.WEB,
                uri = "/contratos/listar?for=view",
                subtitle = "Nosso time finalizou a análise e aprovou esse contrato.",
                tenant = contract.tenantId.toString()
            )

        } else {
            contractRepository.save(
                contract.apply {
                    rejectedReason = reason
                }
            )
            fCMService.sendNotificationForTopic(
                title = "Contrato Analisado",
                body = "O Contrato ${contract.contractNumber} - ${contract.contractor} ainda não foi aprovado no sistema.",
                notificationCode = "ANALISTA_${contract.tenantId}",
                type = NotificationType.CONTRACT,
                platform = FCMService.TargetPlatform.WEB,
                uri = "/contratos/rejeitados/${contract.contractId}",
                subtitle = "Após uma análise pelo nosso time, ainda não foi possível aprovar esse contrato.",
                tenant = contract.tenantId.toString()
            )
        }

        return ResponseEntity.noContent().build()
    }
}
