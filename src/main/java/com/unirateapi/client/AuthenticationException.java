package com.unirateapi.client;

/** Thrown when the API returns {@code 401 Unauthorized}. */
public class AuthenticationException extends UniRateException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
