package com.loanshark.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.loanshark.api")
@EnableJpaRepositories("com.loanshark.api.repository")
@EntityScan("com.loanshark.api.entity")
public class LoanSharkApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanSharkApiApplication.class, args);
    }
}
