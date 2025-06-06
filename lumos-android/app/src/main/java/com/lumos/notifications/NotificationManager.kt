package com.lumos.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.lumos.midleware.SecureStorage

class NotificationManager(private val context: Context, private val secureStorage: SecureStorage) {

    fun subscribeToSavedTopics() {
        Log.e("n", "No notification manager")
        val roles = secureStorage.getRoles()
        val teams = secureStorage.getTeams()

        (roles + teams + "mobile_update").forEach { topic ->
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

        (roles + teams + "mobile_update").forEach { topic ->
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
