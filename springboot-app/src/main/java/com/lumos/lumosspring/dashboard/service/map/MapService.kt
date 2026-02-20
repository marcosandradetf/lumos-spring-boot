package com.lumos.lumosspring.dashboard.service.map

import com.lumos.lumosspring.dashboard.repository.map.MapRepository
import com.lumos.lumosspring.util.Utils
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class MapService(
    private val mapRepository: MapRepository
) {
    fun getExecutions(): ResponseEntity<Any> {
        return ResponseEntity.ok().body(mapRepository.getExecutions(Utils.getCurrentTenantId()))
    }
}