package com.lumos.lumosspring;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
@EnableJdbcRepositories(basePackages = "com.lumos.lumosspring")
public class LumosSpringApplication {
    @Value("${resend.api.key}")
    private String apiKey;

    public static void main(String[] args) {
        SpringApplication.run(LumosSpringApplication.class, args);
    }

    @Bean
    public Resend resend() {
        return new Resend(apiKey);
    }
}
