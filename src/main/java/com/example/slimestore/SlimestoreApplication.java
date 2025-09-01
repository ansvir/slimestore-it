package com.example.slimestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class SlimestoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlimestoreApplication.class, args);
    }

}
