package com.itech.learnspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LearnSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearnSpaceApplication.class, args);
    }
}
