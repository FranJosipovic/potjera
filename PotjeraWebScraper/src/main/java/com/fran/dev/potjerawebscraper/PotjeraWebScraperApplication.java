package com.fran.dev.potjerawebscraper;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;

@SpringBootApplication(scanBasePackages = "com.fran.dev")
@EntityScan(basePackages = "com.fran.dev")
@EnableJpaRepositories(basePackages = "com.fran.dev")
public class PotjeraWebScraperApplication {
    public static void main(String[] args) {
        var ctx = SpringApplication.run(PotjeraWebScraperApplication.class, args);
        var flyway = ctx.getBean(Flyway.class);
        System.out.println("Flyway locations: " + Arrays.toString(flyway.getConfiguration().getLocations()));
    }
}
