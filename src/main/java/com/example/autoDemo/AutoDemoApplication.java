package com.example.autoDemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoDemoApplication.class, args);
    }
}

