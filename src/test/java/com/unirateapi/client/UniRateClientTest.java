package com.unirateapi.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.*;

/** Mock tests for {@link UniRateClient} — stub out the HTTP transport, never touch the network. */
class UniRateClientTest {

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Stub transport that records the request and returns a scripted response. */
    static final class StubTransport implements HttpTransport {
        private final Function<HttpRequest, HttpResponse<String>> handler;
        private HttpRequest lastRequest;

        StubTransport(Function<HttpRequest, HttpResponse<String>> handler) {
            this.handler = handler;
        }

        @Override
        public HttpResponse<String> send(HttpRequest request) {
            this.lastRequest = request;
            return handler.apply(request);
        }

        HttpRequest last() {
            return lastRequest;
        }
    }

    /** Minimal {@link HttpResponse} used by the stub. */
    static final class StubResponse implements HttpResponse<String> {
        private final int status;
        private final String body;
        private final HttpRequest request;

        StubResponse(int status, String body, HttpRequest request) {
            this.status = status;
            this.body = body;
            this.request = request;
        }

        @Override public int statusCode() { return status; }
        @Override public HttpRequest request() { return request; }
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
        @Override public String body() { return body; }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public java.net.http.HttpClient.Version version() { return java.net.http.HttpClient.Version.HTTP_1_1; }
    }

    private UniRateClient client(StubTransport transport) {
        return new UniRateClient("test-key", "https://api.unirateapi.com", Duration.ofSeconds(5), transport);
    }

    private static Map<String, String> queryParams(HttpRequest request) {
        String query = request.uri().getRawQuery();
        if (query == null || query.isEmpty()) {
            return Map.of();
        }
        return Stream.of(query.split("&"))
                .map(p -> p.split("=", 2))
                .collect(Collectors.toMap(
                        a -> java.net.URLDecoder.decode(a[0], java.nio.charset.StandardCharsets.UTF_8),
                        a -> a.length == 2 ? java.net.URLDecoder.decode(a[1], java.nio.charset.StandardCharsets.UTF_8) : "",
                        (a, b) -> b));
    }

    private static HttpResponse<String> ok(HttpRequest req, String body) {
        return new StubResponse(200, body, req);
    }

    private static HttpResponse<String> status(HttpRequest req, int code, String body) {
        return new StubResponse(code, body, req);
    }

    // ------------------------------------------------------------------
    // Success paths — one per method (9 total)
    // ------------------------------------------------------------------

    @Test
    void getRateReturnsSingleRate() {
        StubTransport stub = new StubTransport(req -> ok(req, "{\"rate\": 0.9321}"));
        double rate = client(stub).getRate("usd", "eur");
        assertEquals(0.9321, rate, 0.0001);

        assertTrue(stub.last().uri().getPath().endsWith("/api/rates"));
        Map<String, String> q = queryParams(stub.last());
        assertEquals("USD", q.get("from"));
        assertEquals("EUR", q.get("to"));
        assertEquals("test-key", q.get("api_key"));
        assertEquals("application/json", stub.last().headers().firstValue("Accept").orElse(""));
        assertTrue(stub.last().headers().firstValue("User-Agent").orElse("").startsWith("unirate-java/"));
    }

    @Test
    void getAllRatesReturnsMap() {
        StubTransport stub = new StubTransport(req ->
                ok(req, "{\"rates\": {\"EUR\": 0.9, \"GBP\": 0.8}}"));
        Map<String, Double> rates = client(stub).getAllRates("USD");
        assertEquals(0.9, rates.get("EUR"));
        assertEquals(0.8, rates.get("GBP"));
    }

    @Test
    void convertReturnsResult() {
        StubTransport stub = new StubTransport(req -> ok(req, "{\"result\": 93.21}"));
        double result = client(stub).convert(100, "USD", "EUR");
        assertEquals(93.21, result, 0.01);

        Map<String, String> q = queryParams(stub.last());
        assertEquals("100", q.get("amount"));
        assertEquals("USD", q.get("from"));
        assertEquals("EUR", q.get("to"));
    }

    @Test
    void getSupportedCurrenciesReturnsList() {
        StubTransport stub = new StubTransport(req ->
                ok(req, "{\"currencies\": [\"USD\", \"EUR\", \"GBP\", \"BTC\"]}"));
        List<String> list = client(stub).getSupportedCurrencies();
        assertEquals(Arrays.asList("USD", "EUR", "GBP", "BTC"), list);
    }

    @Test
    void getHistoricalRateSendsDate() {
        StubTransport stub = new StubTransport(req -> ok(req, "{\"rate\": 0.8412}"));
        double rate = client(stub).getHistoricalRate("2024-01-01", "USD", "EUR");
        assertEquals(0.8412, rate, 0.0001);

        Map<String, String> q = queryParams(stub.last());
        assertEquals("2024-01-01", q.get("date"));
        assertEquals("USD", q.get("from"));
        assertEquals("EUR", q.get("to"));
        assertEquals("1", q.get("amount"));
    }

    @Test
    void getHistoricalRatesReturnsMap() {
        StubTransport stub = new StubTransport(req ->
                ok(req, "{\"rates\": {\"EUR\": 0.84, \"GBP\": 0.78}}"));
        Map<String, Double> rates = client(stub).getHistoricalRates("2024-01-01", "USD");
        assertEquals(0.84, rates.get("EUR"));
        assertEquals(0.78, rates.get("GBP"));
    }

    @Test
    void convertHistoricalReturnsResult() {
        StubTransport stub = new StubTransport(req -> ok(req, "{\"result\": 84.12}"));
        double result = client(stub).convertHistorical(100, "USD", "EUR", "2024-01-01");
        assertEquals(84.12, result, 0.01);

        Map<String, String> q = queryParams(stub.last());
        assertEquals("2024-01-01", q.get("date"));
        assertEquals("100", q.get("amount"));
    }

    @Test
    void getTimeSeriesReturnsNestedMap() {
        StubTransport stub = new StubTransport(req -> ok(req,
                "{\"data\": {\"2024-01-01\": {\"EUR\": 0.90}, \"2024-01-02\": {\"EUR\": 0.91}}}"));
        Map<String, Map<String, Double>> series = client(stub).getTimeSeries(
                "2024-01-01", "2024-01-02", "USD", List.of("EUR"), 1);
        assertEquals(0.90, series.get("2024-01-01").get("EUR"));
        assertEquals(0.91, series.get("2024-01-02").get("EUR"));

        Map<String, String> q = queryParams(stub.last());
        assertEquals("EUR", q.get("currencies"));
        assertEquals("2024-01-01", q.get("start_date"));
        assertEquals("2024-01-02", q.get("end_date"));
        assertEquals("USD", q.get("base"));
    }

    @Test
    void getHistoricalLimitsReturnsTypedResponse() {
        StubTransport stub = new StubTransport(req -> ok(req,
                "{\"total_currencies\": 2, \"currencies\": {"
                + "\"USD\": {\"earliest_date\": \"1999-01-01\", \"latest_date\": \"2026-04-20\"},"
                + "\"EUR\": {\"earliest_date\": \"1999-01-01\", \"latest_date\": \"2026-04-20\"}"
                + "}}"));
        HistoricalLimitsResponse limits = client(stub).getHistoricalLimits();
        assertEquals(2, limits.getTotalCurrencies());
        assertEquals("1999-01-01", limits.getCurrencies().get("USD").getEarliestDate());
        assertEquals("2026-04-20", limits.getCurrencies().get("USD").getLatestDate());
    }

    @Test
    void getVatRateReturnsCountryResponse() {
        StubTransport stub = new StubTransport(req -> ok(req,
                "{\"country\": \"DE\", \"vat_data\": {"
                + "\"country_code\": \"DE\", \"country_name\": \"Germany\", \"vat_rate\": 19.0"
                + "}}"));
        VatCountryResponse resp = client(stub).getVatRate("de");
        assertEquals("DE", resp.getCountry());
        assertEquals("DE", resp.getVatData().getCountryCode());
        assertEquals("Germany", resp.getVatData().getCountryName());
        assertEquals(19.0, resp.getVatData().getVatRate(), 0.001);

        Map<String, String> q = queryParams(stub.last());
        assertEquals("DE", q.get("country"));
    }

    @Test
    void getVatRatesReturnsAllCountries() {
        StubTransport stub = new StubTransport(req -> ok(req,
                "{\"date\": \"2026-01-22\", \"total_countries\": 2, \"vat_rates\": {"
                + "\"DE\": {\"country_code\": \"DE\", \"country_name\": \"Germany\", \"vat_rate\": 19.0},"
                + "\"FR\": {\"country_code\": \"FR\", \"country_name\": \"France\", \"vat_rate\": 20.0}"
                + "}}"));
        VatRatesResponse resp = client(stub).getVatRates();
        assertEquals(2, resp.getTotalCountries());
        assertEquals("2026-01-22", resp.getDate());
        assertEquals(19.0, resp.getVatRates().get("DE").getVatRate(), 0.001);
        assertEquals("France", resp.getVatRates().get("FR").getCountryName());
    }

    // ------------------------------------------------------------------
    // Error paths — all five HTTP status → exception mappings
    // ------------------------------------------------------------------

    @Test
    void status400MapsToInvalidDateException() {
        StubTransport stub = new StubTransport(req -> status(req, 400, "{\"error\": \"bad date\"}"));
        assertThrows(InvalidDateException.class, () -> client(stub).getHistoricalRate("not-a-date", "USD", "EUR"));
    }

    @Test
    void status401MapsToAuthenticationException() {
        StubTransport stub = new StubTransport(req -> status(req, 401, ""));
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> client(stub).getRate("USD", "EUR"));
        assertTrue(ex.getMessage().contains("API key"));
    }

    @Test
    void status403MapsToApiException() {
        // Free-tier key → Pro endpoint
        StubTransport stub = new StubTransport(req -> status(req, 403,
                "{\"error\": \"Historical data access requires a Pro subscription\"}"));
        ApiException ex = assertThrows(ApiException.class,
                () -> client(stub).getHistoricalRate("2024-01-01", "USD", "EUR"));
        assertEquals(403, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Pro"));
    }

    @Test
    void status404MapsToInvalidCurrencyException() {
        StubTransport stub = new StubTransport(req -> status(req, 404, ""));
        assertThrows(InvalidCurrencyException.class, () -> client(stub).getRate("USD", "ZZZ"));
    }

    @Test
    void status429MapsToRateLimitException() {
        StubTransport stub = new StubTransport(req -> status(req, 429, ""));
        assertThrows(RateLimitException.class, () -> client(stub).getRate("USD", "EUR"));
    }

    @Test
    void status503MapsToApiException() {
        StubTransport stub = new StubTransport(req -> status(req, 503, ""));
        ApiException ex = assertThrows(ApiException.class, () -> client(stub).getRate("USD", "EUR"));
        assertEquals(503, ex.getStatusCode());
    }

    // ------------------------------------------------------------------
    // Request plumbing
    // ------------------------------------------------------------------

    @Test
    void apiKeyAlwaysSentAsQueryParam() {
        AtomicReference<URI> captured = new AtomicReference<>();
        StubTransport stub = new StubTransport(req -> {
            captured.set(req.uri());
            return ok(req, "{\"currencies\": []}");
        });
        client(stub).getSupportedCurrencies();
        Map<String, String> q = queryParams(stub.last());
        assertEquals("test-key", q.get("api_key"));
    }

    @Test
    void currencyCodesUppercasedBeforeSending() {
        StubTransport stub = new StubTransport(req -> ok(req, "{\"rate\": 1.0}"));
        client(stub).getRate("usd", "gbp");
        Map<String, String> q = queryParams(stub.last());
        assertEquals("USD", q.get("from"));
        assertEquals("GBP", q.get("to"));
    }
}
