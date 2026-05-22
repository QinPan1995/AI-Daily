package com.aidaily;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiDailyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDailyApplication.class, args);
    }
}
