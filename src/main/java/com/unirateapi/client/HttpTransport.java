package com.unirateapi.client;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Pluggable HTTP transport used by {@link UniRateClient}.
 *
 * <p>The default implementation wraps {@link java.net.http.HttpClient}. Tests
 * can supply a stub that returns canned responses instead of hitting the
 * network — see {@code UniRateClientTest} for the pattern.</p>
 */
@FunctionalInterface
public interface HttpTransport {

    /**
     * Execute the request and return the response body as a string.
     *
     * @param request the HTTP request to send
     * @return the HTTP response with a string body
     * @throws IOException on network / transport errors
     * @throws InterruptedException if the calling thread is interrupted
     */
    HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException;
}
