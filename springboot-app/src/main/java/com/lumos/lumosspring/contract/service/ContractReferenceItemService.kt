package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import org.springframework.stereotype.Service

@Service
class ContractReferenceItemService(
    private val repository: ContractReferenceItemRepository,
) {

}
