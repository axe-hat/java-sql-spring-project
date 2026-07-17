package com.hiring.sqlchallenge;

import com.hiring.sqlchallenge.config.ChallengeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Entry point. The actual work runs from {@link com.hiring.sqlchallenge.runner.ChallengeRunner}
 * once the context is up; this class only wires and launches Spring.
 */
@SpringBootApplication
@EnableRetry
@EnableConfigurationProperties(ChallengeProperties.class)
public class SqlChallengeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlChallengeApplication.class, args);
    }
}
