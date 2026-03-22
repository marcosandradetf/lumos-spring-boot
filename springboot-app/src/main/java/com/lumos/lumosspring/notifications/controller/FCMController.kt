package com.lumos.lumosspring.notifications.controller

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.lumos.lumosspring.util.Utils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fcm")
class FCMController(
    private val firebaseApp: FirebaseApp
) {

    @PostMapping("/subscribe")
    fun subscribe(@RequestBody request: Map<String, String>, @RequestParam roles: List<String>) {
        val token = request["token"]

        FirebaseMessaging.getInstance().subscribeToTopic(listOf(token), Utils.getCurrentUserId().toString())

        roles.forEach { role ->
            val topic = "${role}_${Utils.getCurrentTenantId()}"
            FirebaseMessaging.getInstance().subscribeToTopic(listOf(token), topic)
            println("✅ Token inscrito no tópico: $topic")
        }

        if(roles.contains("SUPPORT")) {
            FirebaseMessaging.getInstance().subscribeToTopic(listOf(token), "SUPPORT")
            println("✅ Token inscrito no tópico: SUPPORT")
        }

    }

    @PostMapping("/unsubscribe")
    fun unsubscribe(
        @RequestBody request: Map<String, String>,
        @RequestParam roles: List<String>
    ) {
        val token = request["token"]

        FirebaseMessaging.getInstance().unsubscribeFromTopic(listOf(token), Utils.getCurrentUserId().toString())

        roles.forEach { role ->
            val topic = "${role}_${Utils.getCurrentTenantId()}"
            FirebaseMessaging.getInstance().unsubscribeFromTopic(listOf(token), topic)
            println("🚫 Removido do tópico: $topic")
        }
    }
}