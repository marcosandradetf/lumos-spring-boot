package com.lumos.lumosspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableJdbcRepositories
@EnableAsync
public class LumosSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(LumosSpringApplication.class, args);
    }
}
