package com.example.springtrial;

import org.springframework.boot.SpringApplication;

public class TestSpringTrialApplication {

    public static void main(String[] args) {
        SpringApplication.from(SpringTrialApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
