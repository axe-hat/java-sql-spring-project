package com.hiring.sqlchallenge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** The app must refuse to start when candidate details are missing or malformed. */
class ChallengePropertiesValidationTest {

    @Configuration
    @EnableConfigurationProperties(ChallengeProperties.class)
    static class PropsConfig {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropsConfig.class)
            .withPropertyValues(
                    "challenge.candidate.name=Test User",
                    "challenge.candidate.reg-no=TST0011",
                    "challenge.candidate.email=test@example.com",
                    "challenge.api.base-url=http://localhost",
                    "challenge.api.generate-path=/generateWebhook/JAVA");

    @Test
    void validConfigurationStarts() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            ChallengeProperties props = ctx.getBean(ChallengeProperties.class);
            assertThat(props.getHttp().getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(props.getHttp().getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        });
    }

    @Test
    void missingEmailFailsStartup() {
        runner.withPropertyValues("challenge.candidate.email=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void invalidEmailFailsStartup() {
        runner.withPropertyValues("challenge.candidate.email=not-an-email")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void regNoWithoutDigitsFailsStartup() {
        runner.withPropertyValues("challenge.candidate.reg-no=ABCDEF")
                .run(ctx -> assertThat(ctx).getFailure()
                        .hasStackTraceContaining("must contain at least one digit"));
    }

    @Test
    void zeroRetryAttemptsFailsStartup() {
        runner.withPropertyValues("challenge.retry.max-attempts=0")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void timeoutsAreConfigurable() {
        runner.withPropertyValues("challenge.http.connect-timeout=3s",
                        "challenge.http.read-timeout=500ms")
                .run(ctx -> {
                    ChallengeProperties props = ctx.getBean(ChallengeProperties.class);
                    assertThat(props.getHttp().getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(props.getHttp().getReadTimeout()).isEqualTo(Duration.ofMillis(500));
                });
    }
}
