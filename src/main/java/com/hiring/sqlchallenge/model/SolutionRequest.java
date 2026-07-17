package com.hiring.sqlchallenge.model;

/** The single-field body the webhook expects. */
public record SolutionRequest(String finalQuery) {
}
