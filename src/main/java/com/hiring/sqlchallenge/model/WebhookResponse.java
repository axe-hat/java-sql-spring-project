package com.hiring.sqlchallenge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** What registration returns: where to submit and the token to submit with. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookResponse(String webhook, String accessToken) {
}
