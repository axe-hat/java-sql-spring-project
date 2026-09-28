package com.hiring.sqlchallenge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Exposes the shared {@link RestClient.Builder} the API client builds from. It is
 * backed by the JDK HTTP client with the connect / read timeouts from
 * {@code challenge.http.*}, so a hung endpoint fails the run instead of stalling it.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(ChallengeProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(props.getHttp().getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.getHttp().getReadTimeout());
        return RestClient.builder().requestFactory(requestFactory);
    }
}
