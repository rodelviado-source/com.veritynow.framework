package com.veritynow.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.veritynow.core")
@EnableJpaRepositories(basePackages = "com.veritynow.core")
@EntityScan(basePackages = "com.veritynow.core")
public class StarterApplication {
	public static void main(String[] args) {
		SpringApplication.run(StarterApplication.class, args);
	}
}