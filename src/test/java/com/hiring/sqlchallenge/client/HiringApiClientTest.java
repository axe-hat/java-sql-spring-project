package com.hiring.sqlchallenge.client;

import com.hiring.sqlchallenge.config.ChallengeProperties;
import com.hiring.sqlchallenge.model.SolutionRequest;
import com.hiring.sqlchallenge.model.WebhookRequest;
import com.hiring.sqlchallenge.model.WebhookResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class HiringApiClientTest {

    private MockRestServiceServer server;
    private HiringApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        MockServerRestClientCustomizer customizer = new MockServerRestClientCustomizer();
        customizer.customize(builder);

        ChallengeProperties props = new ChallengeProperties();
        props.getApi().setBaseUrl("http://api.test");
        props.getApi().setGeneratePath("/generateWebhook/JAVA");

        client = new HiringApiClient(builder, props);
        server = customizer.getServer();
    }

    @Test
    void generateWebhookMapsResponse() {
        server.expect(requestTo("http://api.test/generateWebhook/JAVA"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.regNo").value("TST0011"))
                .andRespond(withSuccess(
                        "{\"webhook\":\"http://hook/submit\",\"accessToken\":\"tok\"}",
                        MediaType.APPLICATION_JSON));

        WebhookResponse response = client.generateWebhook(
                new WebhookRequest("Test User", "TST0011", "test@example.com"));

        assertThat(response.webhook()).isEqualTo("http://hook/submit");
        assertThat(response.accessToken()).isEqualTo("tok");
        server.verify();
    }

    @Test
    void submitSolutionSendsTokenAndQuery() {
        server.expect(requestTo("http://hook/submit"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "tok"))
                .andExpect(jsonPath("$.finalQuery").value("SELECT 1"))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        String result = client.submitSolution(
                "http://hook/submit", "tok", new SolutionRequest("SELECT 1"));

        assertThat(result).contains("success");
        server.verify();
    }
}
