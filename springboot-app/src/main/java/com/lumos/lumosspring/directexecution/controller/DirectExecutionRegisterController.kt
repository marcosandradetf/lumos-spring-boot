package com.lumos.lumosspring.directexecution.controller

import com.lumos.lumosspring.directexecution.dto.InstallationRequest
import com.lumos.lumosspring.directexecution.service.DirectExecutionRegisterService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api")
class DirectExecutionRegisterController(
    private val registerService: DirectExecutionRegisterService,
) {
    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        value = ["/mobile/execution/upload-direct-execution"]
    )
    fun uploadDirectExecution(
        @RequestPart("photo") photo: MultipartFile,
        @RequestPart("execution") execution: InstallationRequest?
    ): ResponseEntity<Any> = registerService.saveStreetInstallation(photo, execution)

    @PostMapping("/mobile/execution/finish-direct-execution/{directExecutionId}")
    fun finishDirectExecution(
        @PathVariable directExecutionId: Long,
        @RequestParam(required = false) operationalUsers: List<UUID>?
    ): ResponseEntity<Any> =
        registerService.finishDirectExecution(directExecutionId, operationalUsers)

}