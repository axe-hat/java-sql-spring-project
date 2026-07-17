package com.hiring.sqlchallenge.service;

import com.hiring.sqlchallenge.exception.ChallengeException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Picks the SQL to submit from the registration number: an odd final two digits
 * gets question one, an even value gets question two. The queries themselves
 * live under {@code resources/queries} so they can be edited without touching
 * Java.
 */
@Component
public class SqlSolutionProvider {

    static final String ODD_QUERY = "queries/odd-question.sql";
    static final String EVEN_QUERY = "queries/even-question.sql";

    /** Returns the final query for the given registration number. */
    public String forRegNo(String regNo) {
        int lastTwo = lastTwoDigits(regNo);
        String resource = (lastTwo % 2 == 1) ? ODD_QUERY : EVEN_QUERY;
        return load(resource);
    }

    /** Trailing two digits of the registration number, e.g. {@code TST0042 -> 42}. */
    int lastTwoDigits(String regNo) {
        if (regNo == null) {
            throw new ChallengeException("Registration number is not set");
        }
        StringBuilder digits = new StringBuilder();
        for (int i = regNo.length() - 1; i >= 0 && digits.length() < 2; i--) {
            char c = regNo.charAt(i);
            if (Character.isDigit(c)) {
                digits.insert(0, c);
            }
        }
        if (digits.isEmpty()) {
            throw new ChallengeException("Registration number has no digits: " + regNo);
        }
        return Integer.parseInt(digits.toString());
    }

    private String load(String resource) {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new ChallengeException("Could not read query resource: " + resource, e);
        }
    }
}
