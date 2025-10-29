package com.example.finsight_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinsightBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinsightBackendApplication.class, args);
    }

}
