package com.unirateapi.client;

/** Thrown when the API returns {@code 404 Not Found} — unknown currency or no data. */
public class InvalidCurrencyException extends UniRateException {

    private static final long serialVersionUID = 1L;

    public InvalidCurrencyException(String message) {
        super(message);
    }
}
