package com.unirateapi.client;

/** Thrown when the API returns {@code 429 Too Many Requests}. */
public class RateLimitException extends UniRateException {

    private static final long serialVersionUID = 1L;

    public RateLimitException(String message) {
        super(message);
    }
}
