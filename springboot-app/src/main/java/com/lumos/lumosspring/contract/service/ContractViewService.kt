package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.contract.dto.ContractItemBalance
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.premeasurement.repository.premeasurement.PreMeasurementRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class ContractViewService(
    private val contractRepository: ContractRepository,
) {
    fun getContractItemBalance(contractId: Long): ResponseEntity<List<ContractItemBalance>> {
        return ResponseEntity.ok(contractRepository.getContractItemBalance(contractId))
    }
}
