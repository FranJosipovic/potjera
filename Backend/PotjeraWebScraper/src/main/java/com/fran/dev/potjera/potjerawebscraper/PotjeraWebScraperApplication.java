package com.fran.dev.potjera.potjerawebscraper;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;

@SpringBootApplication(scanBasePackages = "com.fran.dev.potjera")
@EntityScan(basePackages = "com.fran.dev.potjera")
@EnableJpaRepositories(basePackages = "com.fran.dev.potjera")
public class PotjeraWebScraperApplication {
    public static void main(String[] args) {
        var ctx = SpringApplication.run(PotjeraWebScraperApplication.class, args);
        var flyway = ctx.getBean(Flyway.class);
        System.out.println("Flyway locations: " + Arrays.toString(flyway.getConfiguration().getLocations()));
    }
}
