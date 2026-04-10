package com.lumos.lumosspring.notifications.service

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.WebpushConfig
import com.google.firebase.messaging.WebpushFcmOptions
import com.google.firebase.messaging.WebpushNotification
import com.lumos.lumosspring.util.Utils
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class FCMService(
    private val firebaseApp: FirebaseApp
) {
    enum class TargetPlatform { ANDROID, WEB, BOTH }

    fun sendNotificationForTopic(
        title: String,
        body: String,
        action: String? = null,
        notificationCode: String,
        time: Instant = Instant.now(),
        type: String,
        platform: TargetPlatform = TargetPlatform.ANDROID,
        isPopUp: Boolean = false,
        uri: String? = null,
        relatedId: String? = null,
        subtitle: String? = null,
        tenant: String
    ) {
        // Criar a mensagem para o tópico
        val messageBuilder = Message.builder()
            .setTopic(notificationCode)  // Nome do tópico
            .putData("title", title)  // 🔹 Agora a notificação será tratada no onMessageReceived
            .putData("body", body)
            .putData("time", time.toString())
            .putData("type", type)
            .putData("isWebPopup", isPopUp.toString())
            .putData("tenant", tenant)

        relatedId?.let {
            messageBuilder.putData("relatedId", it)
        }

        action?.let {
            messageBuilder.putData("action", it)
        }

        uri?.let {
            messageBuilder.putData("uri", it)
        }

        subtitle?.let {
            messageBuilder.putData("subtitle", it)
        }

        // --- CONTROLE PARA ANDROID ---
//        1. No seu NavHost (Kotlin/Compose)
//
//        Você precisa dizer para a sua rota de materiais que ela aceita um "link externo".
//        Kotlin
//
//        composable(
//            route = "${Routes.DIRECT_EXECUTION_SCREEN_MATERIALS}/{id}",
//            deepLinks = listOf(
//                navDeepLink {
//                    // O padrão de URL que o app vai reconhecer
//                    uriPattern = "https://lumos.com/materials/{id}"
//                }
//            ),
//            arguments = listOf(navArgument("id") { type = NavType.LongType })
//        ) {
//            // ... seu código de tela
//        }
//
//        2. No seu AndroidManifest.xml (Configuração Única)
//
//        Você só precisa avisar ao Android que a sua MainActivity sabe lidar com esses links de "lumos.com". Adicione isso dentro da sua <activity> principal:
//        XML
//
//        <intent-filter android:autoVerify="true">
//        <action android:name="android.intent.action.VIEW" />
//        <category android:name="android.intent.category.DEFAULT" />
//        <category android:name="android.intent.category.BROWSABLE" />
//        <data android:scheme="https" android:host="lumos.com" />
//        </intent-filter>
        if (platform == TargetPlatform.ANDROID || platform == TargetPlatform.BOTH) {
            messageBuilder.setAndroidConfig(
                AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(
                    AndroidNotification.builder()
                    .setSound("default")
//                    .setClickAction("https://lumos.com/materials/${1}")
                    .build())
                .build())
        }

        if (platform == TargetPlatform.WEB || platform == TargetPlatform.BOTH) {
            val webpushBuilder = WebpushConfig.builder()
                .setNotification(
                    WebpushNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setIcon("/assets/icons/icon-alert.png")
                        .build()
                )

            // Só adiciona o link se ele existir, evitando erro de nulo
            uri?.let {
                webpushBuilder.setFcmOptions(
                    WebpushFcmOptions.builder()
                        .setLink(it)
                        .build()
                )
            }

            messageBuilder.setWebpushConfig(webpushBuilder.build())
        }

        val message = messageBuilder.build()

        // Enviar a notificação
        try {
            val response = FirebaseMessaging.getInstance().send(message)
            println("✅ Notificação enviada para o tópico $notificationCode com sucesso: $response")
        } catch (e: Exception) {
            println("❌ Erro ao enviar notificação: ${e.message}")
        }
    }

}

object Routes {
    const val AUTH_FLOW = "auth-flow"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val HOME = "home"
    const val NO_ACCESS = "no-access"
    const val MORE = "more"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val CONTRACT_SCREEN = "contract-screen"
    const val PRE_MEASUREMENT_FLOW = "pre-measurement-flow"
    const val PRE_MEASUREMENTS = "pre-measurements"
    const val PRE_MEASUREMENT_PROGRESS = "pre-measurement-progress"
    const val PRE_MEASUREMENT_STREET = "pre-measurement-street"
    const val INSTALLATION_HOLDER = "installation-holder-screen"
    const val MAINTENANCE = "maintenance"
    const val STOCK = "stock"
    const val ORDER = "order"

    // -> pre-measurement-installations
    const val PRE_MEASUREMENT_INSTALLATION_FLOW = "pre-measurement-installation-flow"
    const val PRE_MEASUREMENT_INSTALLATION_STREETS = "pre-measurement-installation-streets"
    const val PRE_MEASUREMENT_INSTALLATION_MATERIALS = "pre-measurement-installation-materials"

    // -> direct-installations
    const val DIRECT_EXECUTION_FLOW = "direct-execution-flow"
    const val DIRECT_EXECUTION_HOME_SCREEN = "direct-execution-home-screen"
    const val DIRECT_EXECUTION_SCREEN_MATERIALS = "direct-execution-screen-materials"
    const val UPDATE = "update"
    const val SYNC_FLOW = "sync-flow"
    const val SYNC = "sync"
    const val TEAM_SCREEN = "team-screen"
}

object NotificationType {
    const val CONTRACT = "CONTRACT"
    const val UPDATE = "UPDATE"
    const val EVENT = "EVENT"
    const val WARNING = "WARNING"
    const val CASH = "CASH"
    const val ALERT = "ALERT"
    const val EXECUTION = "EXECUTION"
    const val CHANGE_TEAM = "CHANGE_TEAM"
}