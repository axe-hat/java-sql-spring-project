package com.hiring.sqlchallenge.exception;

/** Raised when the challenge flow cannot complete (registration or submit failed). */
public class ChallengeException extends RuntimeException {

    public ChallengeException(String message) {
        super(message);
    }

    public ChallengeException(String message, Throwable cause) {
        super(message, cause);
    }
}
