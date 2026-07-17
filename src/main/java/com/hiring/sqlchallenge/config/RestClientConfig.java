package com.hiring.sqlchallenge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Exposes a plain {@link RestClient.Builder} so the API client (and tests) can
 * build from a shared, customisable starting point without depending on the web
 * auto-configuration.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
