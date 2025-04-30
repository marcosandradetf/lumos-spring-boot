package com.lumos.lumosspring.config

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.FileInputStream


@Configuration
open class FirebaseConfig {

    @Bean
    open fun firebaseInit(): FirebaseApp {
        // Verifica se o Firebase já foi inicializado
        return if (FirebaseApp.getApps().isEmpty()) {
            // mudar para parametro no properties
            val serviceAccount = ClassPathResource("lumos-firebase-sdk.json").inputStream


            val options = FirebaseOptions.builder()
                .setCredentials(ServiceAccountCredentials.fromStream(serviceAccount))
                .build()

            FirebaseApp.initializeApp(options).also {
                println("✅ Firebase inicializado com sucesso!")
            }
        } else {
            println("⚠️ Firebase já foi inicializado anteriormente. Retornando instância existente.")
            FirebaseApp.getInstance()
        }
    }
}