package com.taskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // used to purge expired entries from the revoked-token table
public class TaskFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskFlowApplication.class, args);
    }
}
