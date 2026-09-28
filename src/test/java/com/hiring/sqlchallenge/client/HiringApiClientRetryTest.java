package com.hiring.sqlchallenge.client;

import com.hiring.sqlchallenge.exception.ChallengeException;
import com.hiring.sqlchallenge.model.SolutionRequest;
import com.hiring.sqlchallenge.model.WebhookRequest;
import com.hiring.sqlchallenge.model.WebhookResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Retry behaviour through the real Spring proxy: transient failures (5xx, network)
 * are retried up to {@code challenge.retry.max-attempts} (3 in the test profile);
 * client errors (4xx) and incomplete responses fail on the first attempt.
 */
@SpringBootTest
@ActiveProfiles("test")
class HiringApiClientRetryTest {

    private static final String REGISTER = "http://localhost/generateWebhook/JAVA";
    private static final String HOOK = "http://hook.test/submit";
    private static final String OK_REGISTRATION =
            "{\"webhook\":\"" + HOOK + "\",\"accessToken\":\"tok\"}";

    @TestConfiguration
    static class MockServerConfig {
        @Bean
        MockServerRestClientCustomizer mockServerCustomizer() {
            return new MockServerRestClientCustomizer();
        }

        @Bean
        @Primary
        RestClient.Builder mockedRestClientBuilder(MockServerRestClientCustomizer customizer) {
            RestClient.Builder builder = RestClient.builder();
            customizer.customize(builder);
            return builder;
        }
    }

    @Autowired
    private HiringApiClient client;

    @Autowired
    private MockServerRestClientCustomizer customizer;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = customizer.getServer();
        server.reset();
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    private WebhookRequest candidate() {
        return new WebhookRequest("Test User", "TST0011", "test@example.com");
    }

    @Test
    void submitRetriesAServerErrorThenSucceeds() {
        server.expect(once(), requestTo(HOOK)).andExpect(method(POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(once(), requestTo(HOOK)).andExpect(method(POST))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        String result = client.submitSolution(HOOK, "tok", new SolutionRequest("SELECT 1"));

        assertThat(result).contains("success");
    }

    @Test
    void submitRetriesANetworkErrorThenSucceeds() {
        server.expect(once(), requestTo(HOOK))
                .andRespond(withException(new IOException("connection reset")));
        server.expect(once(), requestTo(HOOK))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertThat(client.submitSolution(HOOK, "tok", new SolutionRequest("SELECT 1")))
                .contains("success");
    }

    @Test
    void submitDoesNotRetryAClientError() {
        server.expect(once(), requestTo(HOOK)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.submitSolution(HOOK, "bad", new SolutionRequest("SELECT 1")))
                .isInstanceOf(ChallengeException.class)
                .hasMessageContaining("HTTP 401");
    }

    @Test
    void submitGivesUpAfterMaxAttempts() {
        server.expect(times(3), requestTo(HOOK)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.submitSolution(HOOK, "tok", new SolutionRequest("SELECT 1")))
                .isInstanceOf(ChallengeException.class)
                .hasMessageContaining("HTTP 502");
    }

    @Test
    void registrationRetriesAServerError() {
        server.expect(once(), requestTo(REGISTER)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(once(), requestTo(REGISTER))
                .andRespond(withSuccess(OK_REGISTRATION, MediaType.APPLICATION_JSON));

        WebhookResponse response = client.generateWebhook(candidate());

        assertThat(response.webhook()).isEqualTo(HOOK);
        assertThat(response.accessToken()).isEqualTo("tok");
    }

    @Test
    void registrationDoesNotRetryABadRequest() {
        server.expect(once(), requestTo(REGISTER)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.generateWebhook(candidate()))
                .isInstanceOf(ChallengeException.class)
                .hasMessageContaining("HTTP 400");
    }

    @Test
    void incompleteRegistrationFailsImmediately() {
        server.expect(once(), requestTo(REGISTER))
                .andRespond(withSuccess("{\"webhook\":\"" + HOOK + "\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateWebhook(candidate()))
                .isInstanceOf(ChallengeException.class)
                .hasMessageContaining("incomplete");
    }
}
