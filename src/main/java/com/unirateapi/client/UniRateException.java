package com.unirateapi.client;

/**
 * Base exception for all UniRate API client errors.
 *
 * <p>All typed exceptions thrown by {@link UniRateClient} extend this class,
 * so a single {@code catch (UniRateException e)} block covers every failure
 * mode.</p>
 */
public class UniRateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UniRateException(String message) {
        super(message);
    }

    public UniRateException(String message, Throwable cause) {
        super(message, cause);
    }
}
