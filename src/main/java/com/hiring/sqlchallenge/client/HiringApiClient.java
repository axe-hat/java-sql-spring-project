package com.hiring.sqlchallenge.client;

import com.hiring.sqlchallenge.config.ChallengeProperties;
import com.hiring.sqlchallenge.exception.ChallengeException;
import com.hiring.sqlchallenge.model.SolutionRequest;
import com.hiring.sqlchallenge.model.WebhookRequest;
import com.hiring.sqlchallenge.model.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

/** Thin, typed wrapper over the two HTTP calls the challenge needs. */
@Component
public class HiringApiClient {

    private static final Logger log = LoggerFactory.getLogger(HiringApiClient.class);

    private final RestClient restClient;
    private final ChallengeProperties props;

    public HiringApiClient(RestClient.Builder builder, ChallengeProperties props) {
        this.props = props;
        this.restClient = builder.baseUrl(props.getApi().getBaseUrl()).build();
    }

    /** Register and receive the submit URL plus access token. */
    public WebhookResponse generateWebhook(WebhookRequest request) {
        log.info("Registering with the hiring API");
        WebhookResponse response = restClient.post()
                .uri(props.getApi().getGeneratePath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(WebhookResponse.class);

        if (response == null || response.webhook() == null || response.accessToken() == null) {
            throw new ChallengeException("Registration returned an incomplete response");
        }
        return response;
    }

    /**
     * Submit the final query to the returned webhook. The submit is the flaky
     * step, so it retries with backoff; {@link #recoverSubmit} turns a final
     * failure into a clear exception.
     */
    @Retryable(
            retryFor = RestClientException.class,
            maxAttemptsExpression = "${challenge.retry.max-attempts:4}",
            backoff = @Backoff(delayExpression = "${challenge.retry.backoff-ms:1000}"))
    public String submitSolution(String webhookUrl, String accessToken, SolutionRequest solution) {
        log.info("Submitting solution to {}", webhookUrl);
        return restClient.post()
                .uri(URI.create(webhookUrl))
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(solution)
                .retrieve()
                .body(String.class);
    }

    @Recover
    public String recoverSubmit(RestClientException ex, String webhookUrl,
                                String accessToken, SolutionRequest solution) {
        throw new ChallengeException("Failed to submit solution after retries", ex);
    }
}
