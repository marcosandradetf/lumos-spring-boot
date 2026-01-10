package com.lumos.lumosspring.stock.materialstock.service


import com.lumos.lumosspring.stock.materialsku.dto.MaterialResponse
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockViewRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class MaterialStockViewService(
    private val materialStockViewRepository: MaterialStockViewRepository
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

}