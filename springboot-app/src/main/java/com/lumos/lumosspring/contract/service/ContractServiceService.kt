package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.contract.controller.dto.ContractDTO
import com.lumos.lumosspring.contract.controller.dto.ContractItemsDTO
import com.lumos.lumosspring.contract.controller.dto.ContractServicesDTO
import com.lumos.lumosspring.contract.entities.Contract
import com.lumos.lumosspring.contract.entities.ContractItem
import com.lumos.lumosspring.contract.entities.ContractService
import com.lumos.lumosspring.contract.repository.ContractItemRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.contract.repository.ContractServiceRepository
import com.lumos.lumosspring.stock.entities.Material
import com.lumos.lumosspring.stock.repository.MaterialRepository
import com.lumos.lumosspring.stock.repository.MaterialServiceRepository
import com.lumos.lumosspring.util.DefaultResponse
import com.lumos.lumosspring.util.Util
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ContractServiceService(
    private val materialRepository: MaterialRepository,
    private val contractRepository: ContractRepository,
    private val contractItemRepository: ContractItemRepository,
    private val contractServiceRepository: ContractServiceRepository,
    private val materialServiceRepository: MaterialServiceRepository,
    private val util: Util
) {

    fun getMaterials(): ResponseEntity<Any> {
        val materials = mutableListOf<Material>()
        val contractItemsResponse = mutableListOf<ContractItemsDTO>()

        // Pega todos os materiais, excluindo "PARAFUSO" e "CONECTOR"
        materials.addAll(materialRepository.findAllMaterialsExcludingScrewStrapAndConnector())

        // Pega um material de cada tipo "PARAFUSO" e "CONECTOR"
        materials.addAll(materialRepository.findOneScrewStrapAndConnector())
        var bLed = false
        var bArm = false
        for (material in materials.sortedWith(compareBy({ it.materialName }, { extractNumber(it.materialLength) }, {extractNumber(it.materialPower)}))) {
            contractItemsResponse.add(
                ContractItemsDTO(
                    material.idMaterial,
                    material.materialType.typeName.uppercase(),
                    material.materialLength?.uppercase(),
                    material.materialPower?.uppercase(),
                    0.0,
                    "0,00",
                    if (material.materialType.typeName.uppercase() == "LED" && !bLed) {
                        bLed = true
                        material.materialServices?.map {
                            ContractServicesDTO(
                                it.serviceId,
                                it.serviceName,
                                0.0,
                                "0,00"
                            )
                        }
                    } else if (material.materialType.typeName.uppercase() == "BRAÇO" && !bArm) {
                        bArm = true
                        material.materialServices?.map {
                            ContractServicesDTO(
                                it.serviceId,
                                it.serviceName,
                                0.0,
                                "0,00"
                            )
                        }
                    } else null
                )
            )
        }

        return ResponseEntity.ok().body(contractItemsResponse)
    }

    private fun extractNumber(value: String?): Double {
        return value?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
    }


    fun saveContract(contractDTO: ContractDTO): ResponseEntity<Any> {
        val contract = Contract().apply {
            contractNumber = contractDTO.number
            socialReason = contractDTO.socialReason
            cnpj = contractDTO.cnpj
            address = contractDTO.address
            phone = contractDTO.phone
            creationDate = util.dateTime
        }
        contractRepository.save(contract)

        for (item in contractDTO.items) {
            val material = materialRepository.findById(item.id)
                .orElseThrow { IllegalArgumentException("Material com ID ${item.id} não encontrado") }

            val contractItem = ContractItem().apply {
                this.contract = contract
                this.material = material
                this.contractedQuantity = item.quantity!!
                this.setPrices(util.convertToBigDecimal(item.price))
            }
            contractItemRepository.save(contractItem)

            // Verifica se a lista de serviços não é nula e não está vazia
            if (!item.services.isNullOrEmpty()) {
                for (service in item.services) {
                    val materialService = materialServiceRepository.findById(service.id)
                        .orElseThrow { IllegalArgumentException("Serviço com ID ${service.id} não encontrado") }

                    val contractService = ContractService().apply {
                        this.materialService = materialService
                        this.contractedQuantity = service.quantity!!
                    }
                    contractServiceRepository.save(contractService) // Salva para gerar o ID

                    contractService.setPrices(util.convertToBigDecimal(service.price), contractService.contractServiceId)
                    contractService.contractsItems.add(contractItem)
                    contractServiceRepository.save(contractService)
                }
            }
        }

        return ResponseEntity.ok(DefaultResponse("Contrato salvo com sucesso!"))
    }

}
