package com.lumos.lumosspring.util

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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

    @GetMapping("/generic/get-object")
    fun getObject(@RequestBody request: GetObjectRequest): ResponseEntity<Any> {
        val result = util.getObject(request)
        return ResponseEntity.ok(result)
    }


}