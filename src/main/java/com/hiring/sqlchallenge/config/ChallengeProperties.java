package com.hiring.sqlchallenge.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Everything the run needs, bound from {@code challenge.*} and validated at
 * startup so a missing candidate detail fails fast instead of half way through
 * a network call.
 */
@Validated
@ConfigurationProperties(prefix = "challenge")
public class ChallengeProperties {

    /** When false the runner does nothing; used by tests to avoid live calls. */
    private boolean autorun = true;

    @Valid
    private Candidate candidate = new Candidate();

    @Valid
    private Api api = new Api();

    @Valid
    private Retry retry = new Retry();

    public boolean isAutorun() {
        return autorun;
    }

    public void setAutorun(boolean autorun) {
        this.autorun = autorun;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    /** Identity submitted to the hiring API. Supplied via environment, never committed. */
    public static class Candidate {

        @NotBlank
        private String name;

        @NotBlank
        private String regNo;

        @NotBlank
        @Email
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRegNo() {
            return regNo;
        }

        public void setRegNo(String regNo) {
            this.regNo = regNo;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /** Where to register. The submit URL is returned by the API at runtime. */
    public static class Api {

        @NotBlank
        private String baseUrl;

        @NotBlank
        private String generatePath;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getGeneratePath() {
            return generatePath;
        }

        public void setGeneratePath(String generatePath) {
            this.generatePath = generatePath;
        }
    }

    /** Backoff for the final submit, which is the flaky part of the flow. */
    public static class Retry {

        @Min(1)
        private int maxAttempts = 4;

        @Min(0)
        private long backoffMs = 1000;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getBackoffMs() {
            return backoffMs;
        }

        public void setBackoffMs(long backoffMs) {
            this.backoffMs = backoffMs;
        }
    }
}
