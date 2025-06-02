package com.lumos.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.lumos.midleware.SecureStorage

class NotificationManager(private val context: Context, private val secureStorage: SecureStorage) {

    fun subscribeToSavedTopics() {
        val roles = secureStorage.getRoles()
        val teams = secureStorage.getTeams()

        (roles + teams).forEach { topic ->
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

    fun unsubscribeFromSavedTopics() {
        val roles = secureStorage.getRoles()
        val teams = secureStorage.getTeams()

        (roles + teams).forEach { topic ->
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
