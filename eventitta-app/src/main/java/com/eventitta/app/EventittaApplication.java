package com.eventitta.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.eventitta")
@EntityScan(basePackages = "com.eventitta.domain")
@EnableJpaRepositories(basePackages = {"com.eventitta.domain", "com.eventitta.infra"})
public class EventittaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventittaApplication.class, args);
    }

}
