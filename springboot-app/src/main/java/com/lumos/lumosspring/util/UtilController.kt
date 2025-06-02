package com.lumos.lumosspring.util

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UtilController(
    private val util: Util
) {

    data class GetObjectRequest(
        val fields: List<String>,
        val table: String,
        val where: String,
        val equal: List<Any>
    )

    data class UpdateEntity(
        val table: String,
        val field: String,
        val set: Any,
        val where: String,
        val equal: Any
    )

    @PostMapping("/util/generic/get-object")
    fun getObject(@RequestBody request: GetObjectRequest): ResponseEntity<Any> {
        val result = util.getObject(request)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/mobile/util/generic/set-entity")
    fun setEntity(@RequestBody request: UpdateEntity): ResponseEntity<Any> {
        try {
            util.updateEntity(request)
            return ResponseEntity.ok().build()
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(e.message)
        }
    }


}