package com.lumos.lumosspring.stock.materialstock.service


import com.lumos.lumosspring.stock.deposit.repository.DepositRepository
import com.lumos.lumosspring.stock.materialsku.dto.MaterialResponse
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockViewRepository
import com.lumos.lumosspring.stock.materialstock.repository.StockQueryRepository
import com.lumos.lumosspring.team.repository.TeamQueryRepository
import com.lumos.lumosspring.util.Utils
import com.lumos.lumosspring.util.Utils.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class MaterialStockViewService(
    private val materialStockViewRepository: MaterialStockViewRepository,
    private val teamQueryRepository: TeamQueryRepository,
    private val depositRepository: DepositRepository,
    private val stockQueryRepository: StockQueryRepository
) {
    data class PagedResponse<T>(
        val data: List<T>,
        val page: Int,
        val size: Int,
        val totalRecords: Int,
    )

    fun getAllMaterialsWithPagination(
        page: Int,
        size: Int,
        depositId: Long
    ): ResponseEntity<PagedResponse<MaterialResponse>> {
        val offset = page * size
        val content = materialStockViewRepository.getAllMaterialsWithPagination(size, offset, depositId)
        val totalRecords =
            if (content.isEmpty()) 0 else materialStockViewRepository.countMaterialStockByDepositId(depositId)

        return ResponseEntity.ok(
            PagedResponse(
                data = content,
                page = page,
                size = size,
                totalRecords = totalRecords,
            )
        )
    }

    fun getMaterialsBySearchWithPagination(
        name: String,
        page: Int,
        size: Int,
        depositId: Long
    ): ResponseEntity<PagedResponse<MaterialResponse>> {
        val offset = page * size

        val content = materialStockViewRepository.getMaterialsBySearchWithPagination(
            size,
            offset,
            depositId,
            "%${name.lowercase(Locale.getDefault())}%",
        )
        val totalRecords =
            if (content.isEmpty()) 0 else materialStockViewRepository.countMaterialStockByDepositIdAndMaterialName(
                depositId,
                name,
                "%${name.lowercase(Locale.getDefault())}%"
            )

        return ResponseEntity.ok(
            PagedResponse(
                data = content,
                page = page,
                size = size,
                totalRecords = totalRecords,
            )
        )
    }


    fun findByBarcodeAndDepositId(barcode: String, depositId: Long): ResponseEntity<*> {
        val material = materialStockViewRepository.findByBarcodeAndDepositId(barcode, depositId)
            .orElseThrow { BusinessException("Material não encontrado") }

        return ResponseEntity.ok(material)
    }

    fun findMaterialsByContractReference(contractReferenceItemId: Long, teamId: Long): ResponseEntity<Any> {
//        val materials = if (type != "NULL" && linking != "NULL") {
//            materialStockViewRepository.findAllByLinkingAndType(
//                linking.lowercase(),
//                type.lowercase(),
//                teamId
//            )
//        } else {
//            materialStockViewRepository.findAllByType(type.lowercase(), teamId)
//        }

        val materials = materialStockViewRepository.findMaterialsByContractReference(contractReferenceItemId, teamId)

        return ResponseEntity.ok(materials);
    }

    fun getMaterialsForMaintenance(
        userId: UUID,
        currentTeamId: Long? = null
    ): ResponseEntity<Any> {
        val teamId =
            currentTeamId ?: (teamQueryRepository.getTeamIdByUserId(userId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Equipe enviada não encontrada, informe ao Administrador do sistema"))

        val depositId =
            depositRepository.getDepositIdByTeamId(teamId) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Depósito enviado não encontrado, informe ao Administrador do sistema")

        return ResponseEntity.ok(stockQueryRepository.getMaterialsForMaintenance(depositId))
    }


    fun getTruckStock(): ResponseEntity<Any> {
        val userId = Utils.getCurrentUserId()
        val teamId = teamQueryRepository.getTeamIdByUserId(userId) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Equipe enviada não encontrada, informe ao Administrador do sistema")

        val depositId =
            depositRepository.getDepositIdByTeamId(teamId) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Depósito enviado não encontrado, informe ao Administrador do sistema")

        return ResponseEntity.ok(stockQueryRepository.getTruckStock(depositId))
    }

}