package com.hiring.sqlchallenge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the context wires up (with autorun disabled, so nothing goes out to
 * the network).
 */
@SpringBootTest
@ActiveProfiles("test")
class SqlChallengeApplicationTests {

    @Test
    void contextLoads() {
    }
}
