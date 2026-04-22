package com.unirateapi.client;

/** Thrown when the API returns {@code 400 Bad Request} — malformed parameters. */
public class InvalidDateException extends UniRateException {

    private static final long serialVersionUID = 1L;

    public InvalidDateException(String message) {
        super(message);
    }
}
