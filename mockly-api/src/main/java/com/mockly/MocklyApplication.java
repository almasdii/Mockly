package com.mockly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.mockly.data.repository")
public class MocklyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MocklyApplication.class, args);
    }
}
