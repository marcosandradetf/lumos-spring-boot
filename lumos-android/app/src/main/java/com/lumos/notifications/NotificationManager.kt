package com.lumos.notifications

import com.google.firebase.messaging.FirebaseMessaging

class NotificationManager() {

    fun subscribeInTopics(topics: Set<String>) {
        (topics + "mobile_update").forEach { topic ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        println("✅ Inscrito em: $topic")
                    } else {
                        println("❌ Falha na inscrição em: $topic")
                    }
                }
        }
    }

    fun unsubscribeFromAllTopics() {
        // Invalida completamente
        FirebaseMessaging.getInstance().deleteToken()

        // Recria novo token automaticamente
        FirebaseMessaging.getInstance().token

        println("🎯 Todas as inscrições foram apagadas (reset do token FCM).")
    }


    fun unsubscribeInTopics(topics: Set<String>) {
        (topics).forEach { topic ->
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        println("✅ Cancelada inscrição em: $topic")
                    } else {
                        println("❌ Falha ao cancelar inscrição em: $topic")
                    }
                }
        }
    }
}
