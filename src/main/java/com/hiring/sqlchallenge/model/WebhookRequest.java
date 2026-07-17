package com.hiring.sqlchallenge.model;

/** Registration payload sent to the hiring API. */
public record WebhookRequest(String name, String regNo, String email) {
}
