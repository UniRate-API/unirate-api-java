package com.unirateapi.client;

/**
 * Generic API error. Carries the HTTP status code and response body for
 * status codes that don't map to a more specific exception (notably 403 for
 * Pro-gated endpoints on a free-tier key, 503 service unavailable, etc.).
 */
public class ApiException extends UniRateException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String body;

    public ApiException(int statusCode, String body) {
        super(buildMessage(statusCode, body));
        this.statusCode = statusCode;
        this.body = body == null ? "" : body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    private static String buildMessage(int statusCode, String body) {
        if (statusCode == 403) {
            return "API error (status 403): Endpoint requires a Pro subscription";
        }
        if (statusCode == 503) {
            return "API error (status 503): Service unavailable";
        }
        String b = body == null ? "" : body;
        return "API error (status " + statusCode + "): " + b;
    }
}
