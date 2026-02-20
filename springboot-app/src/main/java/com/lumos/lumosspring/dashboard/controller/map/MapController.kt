package com.lumos.lumosspring.dashboard.controller.map

import com.lumos.lumosspring.dashboard.service.map.MapService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard/map")
class MapController(
    private val mapService: MapService
) {
    @GetMapping("/get-executions")
    fun getExecutions() = mapService.getExecutions()
}