package com.loanshark.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoanSharkApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanSharkApiApplication.class, args);
    }
}
