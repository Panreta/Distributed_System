package com.cs6650.scsservice;

import com.cs6650.scsservice.service.RabbitMQService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ScsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScsServiceApplication.class, args);
    }

    @Bean
    public ApplicationRunner initRmq(RabbitMQService rabbitMQService) {
        return args -> {
            // Forces RabbitMQService (and its RMQConfig dependency) to initialize eagerly
        };
    }

}
