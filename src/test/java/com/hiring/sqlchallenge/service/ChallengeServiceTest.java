package com.hiring.sqlchallenge.service;

import com.hiring.sqlchallenge.client.HiringApiClient;
import com.hiring.sqlchallenge.config.ChallengeProperties;
import com.hiring.sqlchallenge.model.SolutionRequest;
import com.hiring.sqlchallenge.model.WebhookRequest;
import com.hiring.sqlchallenge.model.WebhookResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeServiceTest {

    private HiringApiClient client;
    private SqlSolutionProvider solutionProvider;
    private ChallengeService service;

    @BeforeEach
    void setUp() {
        client = mock(HiringApiClient.class);
        solutionProvider = mock(SqlSolutionProvider.class);

        ChallengeProperties props = new ChallengeProperties();
        props.getCandidate().setName("Test User");
        props.getCandidate().setRegNo("TST0011");
        props.getCandidate().setEmail("test@example.com");

        service = new ChallengeService(client, solutionProvider, props);
    }

    @Test
    void registersSelectsQueryAndSubmits() {
        when(client.generateWebhook(any()))
                .thenReturn(new WebhookResponse("http://hook/submit", "token-123"));
        when(solutionProvider.forRegNo("TST0011")).thenReturn("SELECT 1");
        when(client.submitSolution(eq("http://hook/submit"), eq("token-123"), any()))
                .thenReturn("{\"success\":true}");

        String result = service.execute();

        assertThat(result).isEqualTo("{\"success\":true}");
        verify(client).generateWebhook(new WebhookRequest("Test User", "TST0011", "test@example.com"));
        verify(client).submitSolution("http://hook/submit", "token-123", new SolutionRequest("SELECT 1"));
    }
}
