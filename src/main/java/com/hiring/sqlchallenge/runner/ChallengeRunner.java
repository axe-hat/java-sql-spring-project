package com.hiring.sqlchallenge.runner;

import com.hiring.sqlchallenge.exception.ChallengeException;
import com.hiring.sqlchallenge.service.ChallengeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fires the challenge once the context is ready. Gated on {@code challenge.autorun}
 * so tests can load the context without making live calls.
 */
@Component
@ConditionalOnProperty(name = "challenge.autorun", havingValue = "true", matchIfMissing = true)
public class ChallengeRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChallengeRunner.class);

    private final ChallengeService challengeService;

    public ChallengeRunner(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String response = challengeService.execute();
            log.info("Challenge complete. Response: {}", response);
        } catch (ChallengeException e) {
            log.error("Challenge failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
