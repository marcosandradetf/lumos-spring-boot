package com.lumos.lumosspring.installation.controller.direct_execution

import com.lumos.lumosspring.installation.dto.direct_execution.InstallationRequest
import com.lumos.lumosspring.installation.dto.direct_execution.InstallationStreetRequest
import com.lumos.lumosspring.installation.service.direct_execution.DirectExecutionRegisterService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

@RestController
@RequestMapping("/api")
class DirectExecutionRegisterController(
    private val registerService: DirectExecutionRegisterService,
) {

    data class InstallationCreateRequest(
        val directExecutionId: Long,
        var contractId: Long?,
        var description: String,
        var instructions: String? = null,
        val executionStatus: String,
        val creationDate: Instant,
        val executorsIds: List<String>? = null,
    )
    @PostMapping("/mobile/v1/direct-execution/create-installation")
    fun createInstallation(
        @RequestBody execution: InstallationCreateRequest,
        @RequestParam teamId: Long
    ): ResponseEntity<Any> {
        return registerService.createInstallation(execution, teamId)
    }


    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        value = ["/mobile/execution/save-street-installation"]
    )
    fun saveStreetInstallationV2(
        @RequestPart("photo") photo: MultipartFile,
        @RequestPart("execution") execution: InstallationStreetRequest?
    ): ResponseEntity<Any> = registerService.saveStreetInstallationV2(photo, execution)


    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        value =["/mobile/v2/direct-execution/finish"])
    fun finishDirectExecution(
        @RequestPart("signature", required = false) signature: MultipartFile?,
        @RequestPart("installation") installation: InstallationRequest?
    ): ResponseEntity<Any> =
        registerService.finishDirectExecutionV2(signature, installation)


    data class ReqValidation(
        val directExecutionId: Long,
        val contractId: Long,
        val items: List<ReqValidationItems>
    )

    data class ReqValidationItems(
        val directExecutionStreetItemId: Long,
        val contractItemId: Long,
    )

    @PostMapping("/direct-execution/pre-validate-execution")
    fun preValidateExecution(@RequestBody request: ReqValidation): ResponseEntity<Any> {
        return registerService.preValidateExecution(request)
    }

    @DeleteMapping("/direct-execution/delete-item/{streetItemId}")
    fun deleteItem(
        @PathVariable streetItemId: Long,
    ): ResponseEntity<Any>  {
        return registerService.deleteItem(streetItemId)
    }

    @PutMapping("/direct-execution/cancel-validation")
    fun cancelValidation(
        @RequestParam executionId: Long,
        @RequestParam streetItemsIds: List<Long>,
    ): ResponseEntity<Any> {
        registerService.cancelValidation(executionId, streetItemsIds)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/direct-execution/validate-execution/{executionId}")
    fun validateExecution (
        @PathVariable executionId: Long,
    ): ResponseEntity<Any> {
        registerService.validateExecution(executionId)
        return ResponseEntity.noContent().build()
    }

}