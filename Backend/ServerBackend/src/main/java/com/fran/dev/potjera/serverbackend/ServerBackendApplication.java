package com.fran.dev.potjera.serverbackend;

import com.fran.dev.potjera.potjeradb.repositories.RoomRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fran.dev.potjera")
@EntityScan(basePackages = "com.fran.dev.potjera")
@EnableJpaRepositories(basePackages = "com.fran.dev.potjera")
public class ServerBackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(ServerBackendApplication.class, args);
    }

    @Bean
    ApplicationRunner cleanupOnStartup(RoomRepository roomRepository) {
        return args -> roomRepository.deleteAll();
    }

}
