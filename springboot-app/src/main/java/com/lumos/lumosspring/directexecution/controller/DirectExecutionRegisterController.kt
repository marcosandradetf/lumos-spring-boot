package com.lumos.lumosspring.directexecution.controller

import com.lumos.lumosspring.directexecution.dto.InstallationRequest
import com.lumos.lumosspring.directexecution.dto.InstallationStreetRequest
import com.lumos.lumosspring.directexecution.service.DirectExecutionRegisterService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

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
        @RequestPart("execution") execution: InstallationStreetRequest?
    ): ResponseEntity<Any> = registerService.saveStreetInstallation(photo, execution)

    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        value = ["/mobile/execution/save-street-installation"]
    )
    fun saveStreetInstallationV2(
        @RequestPart("photo") photo: MultipartFile,
        @RequestPart("execution") execution: InstallationStreetRequest?
    ): ResponseEntity<Any> = registerService.saveStreetInstallationV2(photo, execution)

    @PostMapping("/mobile/execution/finish-direct-execution/{directExecutionId}")
    fun finishDirectExecution(
        @PathVariable directExecutionId: Long,
        @RequestParam(required = false) operationalUsers: List<UUID>?
    ): ResponseEntity<Any> =
        registerService.finishDirectExecution(directExecutionId, operationalUsers)

    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        value =["/mobile/v2/direct-execution/finish"])
    fun finishDirectExecution(
        @RequestPart("signature", required = false) signature: MultipartFile?,
        @RequestPart("installation") installation: InstallationRequest?
    ): ResponseEntity<Any> =
        registerService.finishDirectExecutionV2(signature, installation)

}