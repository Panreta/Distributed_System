package com.cs6650.scsservice.config;

import org.springframework.beans.factory.annotation.Value;
// Spring Constructor
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

// config class
@Configuration
public class RestTemplateConfig {

    @Bean
    // Generate RestTemplate
    public RestTemplate restTemplate(
            // In application.properties change config
            RestTemplateBuilder builder,
            @Value("${cca.connect-timeout-ms:200}") long connectMs,
            @Value("${cca.read-timeout-ms:800}") long readMs
    ) {
        return builder
                // connect CCA Timeout, Fail Fast
                .connectTimeout(Duration.ofMillis(connectMs))
                // CCA return Timeout
                .readTimeout(Duration.ofMillis(readMs))
                .build();
    }
}


