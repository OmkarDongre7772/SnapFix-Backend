package com.snapfix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.snapfix")
public class SnapfixApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnapfixApplication.class, args);
    }

} 
