package com.lumos.lumosspring.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Collections;

@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(Collections.singletonList("*"));
        corsConfiguration.addAllowedMethod("*"); // Permite todos os métodos (GET, POST, etc.)
        corsConfiguration.addAllowedHeader("*"); // Permite todos os cabeçalhos
        corsConfiguration.setAllowCredentials(true); // Permite o uso de credenciais (cookies HTTP-Only)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration); // Aplica CORS a todos os endpoints

        return new CorsFilter(source);
    }
}
