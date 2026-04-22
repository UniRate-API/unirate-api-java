package com.unirateapi.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Client for the <a href="https://unirateapi.com">UniRate API</a>.
 *
 * <p>Construct with an API key, then call any of the nine public methods.
 * Thread-safe — share a single instance across your application.</p>
 *
 * <pre>{@code
 * UniRateClient client = new UniRateClient("your-api-key");
 * double rate = client.getRate("USD", "EUR");
 * double eur = client.convert(100, "USD", "EUR");
 * List<String> codes = client.getSupportedCurrencies();
 * }</pre>
 */
public final class UniRateClient {

    /** Default base URL for the UniRate API. */
    public static final String DEFAULT_BASE_URL = "https://api.unirateapi.com";

    /** Client version — kept in sync with {@code pom.xml}. */
    public static final String VERSION = "0.1.0";

    private static final String USER_AGENT = "unirate-java/" + VERSION;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpTransport transport;
    private final ObjectMapper mapper;

    /** Construct with just an API key. Uses the default base URL and 30-second timeout. */
    public UniRateClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_TIMEOUT, null);
    }

    /** Construct with an API key and custom timeout. */
    public UniRateClient(String apiKey, Duration timeout) {
        this(apiKey, DEFAULT_BASE_URL, timeout, null);
    }

    /**
     * Full constructor.
     *
     * @param apiKey    API key obtained from <a href="https://unirateapi.com">unirateapi.com</a>
     * @param baseUrl   base URL (useful for mocking / testing)
     * @param timeout   request timeout
     * @param transport HTTP transport; pass {@code null} to use the default
     *                  {@link java.net.http.HttpClient}-backed implementation
     */
    public UniRateClient(String apiKey, String baseUrl, Duration timeout, HttpTransport transport) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.baseUrl = stripTrailingSlash(Objects.requireNonNull(baseUrl, "baseUrl"));
        this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        this.transport = transport == null ? new DefaultHttpTransport(this.timeout) : transport;
        this.mapper = new ObjectMapper();
    }

    // ---------------------------------------------------------------
    // Current rates and conversion
    // ---------------------------------------------------------------

    /**
     * Fetch the current exchange rate between two currencies.
     *
     * @param from source currency code (default {@code "USD"} if {@code null})
     * @param to   target currency code
     * @return exchange rate ({@code 1 from == rate to})
     */
    public double getRate(String from, String to) {
        Objects.requireNonNull(to, "to");
        Map<String, String> query = new LinkedHashMap<>();
        query.put("from", upper(from == null ? "USD" : from));
        query.put("to", upper(to));
        JsonNode body = requestJson("/api/rates", query);
        return parseDouble(body.get("rate"), "rate");
    }

    /** Convenience overload: {@code getRate(to)} with {@code from="USD"}. */
    public double getRate(String to) {
        return getRate("USD", to);
    }

    /**
     * Fetch all exchange rates for a base currency.
     *
     * @param from base currency code (default {@code "USD"} if {@code null})
     * @return map of currency code to exchange rate
     */
    public Map<String, Double> getAllRates(String from) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("from", upper(from == null ? "USD" : from));
        JsonNode body = requestJson("/api/rates", query);
        return parseDoubleMap(body.get("rates"));
    }

    /** Convenience overload: all rates with base {@code "USD"}. */
    public Map<String, Double> getAllRates() {
        return getAllRates("USD");
    }

    /**
     * Convert an amount between currencies at the current rate.
     *
     * @param amount amount in the source currency
     * @param from   source currency (default {@code "USD"} if {@code null})
     * @param to     target currency
     * @return the converted amount
     */
    public double convert(double amount, String from, String to) {
        Objects.requireNonNull(to, "to");
        Map<String, String> query = new LinkedHashMap<>();
        query.put("from", upper(from == null ? "USD" : from));
        query.put("to", upper(to));
        query.put("amount", formatAmount(amount));
        JsonNode body = requestJson("/api/convert", query);
        return parseDouble(body.get("result"), "result");
    }

    /** Convenience: convert from USD. */
    public double convert(double amount, String to) {
        return convert(amount, "USD", to);
    }

    /** Get the list of supported currency codes. */
    public List<String> getSupportedCurrencies() {
        JsonNode body = requestJson("/api/currencies", Collections.emptyMap());
        JsonNode arr = body.get("currencies");
        if (arr == null || !arr.isArray()) {
            throw new UniRateException("Malformed response: missing 'currencies' array");
        }
        List<String> out = new java.util.ArrayList<>(arr.size());
        for (JsonNode el : arr) {
            out.add(el.asText());
        }
        return out;
    }

    // ---------------------------------------------------------------
    // Historical data (Pro-gated on the free tier)
    // ---------------------------------------------------------------

    /**
     * Fetch a historical exchange rate for a specific date.
     *
     * @param date ISO date string {@code YYYY-MM-DD}
     * @param from source currency (default {@code "USD"} if {@code null})
     * @param to   target currency
     * @return the historical rate
     */
    public double getHistoricalRate(String date, String from, String to) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(to, "to");
        Map<String, String> query = new LinkedHashMap<>();
        query.put("date", date);
        query.put("amount", "1");
        query.put("from", upper(from == null ? "USD" : from));
        query.put("to", upper(to));
        JsonNode body = requestJson("/api/historical/rates", query);
        return parseDouble(body.get("rate"), "rate");
    }

    /**
     * Fetch all historical exchange rates for a base currency on a given date.
     *
     * @param date ISO date string {@code YYYY-MM-DD}
     * @param base base currency (default {@code "USD"} if {@code null})
     */
    public Map<String, Double> getHistoricalRates(String date, String base) {
        Objects.requireNonNull(date, "date");
        Map<String, String> query = new LinkedHashMap<>();
        query.put("date", date);
        query.put("amount", "1");
        query.put("from", upper(base == null ? "USD" : base));
        JsonNode body = requestJson("/api/historical/rates", query);
        return parseDoubleMap(body.get("rates"));
    }

    /** Convenience: historical rates with base {@code "USD"}. */
    public Map<String, Double> getHistoricalRates(String date) {
        return getHistoricalRates(date, "USD");
    }

    /**
     * Convert an amount using a historical exchange rate.
     *
     * @param amount amount in the source currency
     * @param from   source currency
     * @param to     target currency
     * @param date   ISO date string {@code YYYY-MM-DD}
     */
    public double convertHistorical(double amount, String from, String to, String date) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(date, "date");
        Map<String, String> query = new LinkedHashMap<>();
        query.put("date", date);
        query.put("amount", formatAmount(amount));
        query.put("from", upper(from));
        query.put("to", upper(to));
        JsonNode body = requestJson("/api/historical/rates", query);
        return parseDouble(body.get("result"), "result");
    }

    /**
     * Fetch a time series of exchange rates (up to 5 years).
     *
     * @param startDate  {@code YYYY-MM-DD}
     * @param endDate    {@code YYYY-MM-DD}
     * @param base       base currency (default {@code "USD"} if {@code null})
     * @param currencies target currencies; {@code null} or empty for all
     * @param amount     amount to convert; {@code 1} for raw rates
     * @return nested map: {@code {date -> {currency -> rate}}}
     */
    public Map<String, Map<String, Double>> getTimeSeries(
            String startDate,
            String endDate,
            String base,
            List<String> currencies,
            double amount) {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Map<String, String> query = new LinkedHashMap<>();
        query.put("start_date", startDate);
        query.put("end_date", endDate);
        query.put("amount", formatAmount(amount));
        query.put("base", upper(base == null ? "USD" : base));
        if (currencies != null && !currencies.isEmpty()) {
            String joined = currencies.stream()
                    .map(UniRateClient::upper)
                    .collect(Collectors.joining(","));
            query.put("currencies", joined);
        }
        JsonNode body = requestJson("/api/historical/timeseries", query);
        JsonNode data = body.get("data");
        if (data == null) {
            throw new UniRateException("Malformed response: missing 'data'");
        }
        try {
            return mapper.convertValue(data, new TypeReference<Map<String, Map<String, Double>>>() {});
        } catch (IllegalArgumentException e) {
            throw new UniRateException("Failed to decode time series: " + e.getMessage(), e);
        }
    }

    /** Convenience: time series with default base {@code "USD"}, all currencies, amount {@code 1}. */
    public Map<String, Map<String, Double>> getTimeSeries(String startDate, String endDate) {
        return getTimeSeries(startDate, endDate, "USD", null, 1);
    }

    /** Fetch available historical-data coverage per currency. */
    public HistoricalLimitsResponse getHistoricalLimits() {
        JsonNode body = requestJson("/api/historical/limits", Collections.emptyMap());
        try {
            return mapper.treeToValue(body, HistoricalLimitsResponse.class);
        } catch (IOException e) {
            throw new UniRateException("Failed to decode historical limits: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // VAT
    // ---------------------------------------------------------------

    /** Fetch VAT rates for all countries. */
    public VatRatesResponse getVatRates() {
        JsonNode body = requestJson("/api/vat/rates", Collections.emptyMap());
        try {
            return mapper.treeToValue(body, VatRatesResponse.class);
        } catch (IOException e) {
            throw new UniRateException("Failed to decode VAT rates: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch the VAT rate for a specific country.
     *
     * @param country ISO-3166 alpha-2 country code (e.g. {@code "DE"})
     */
    public VatCountryResponse getVatRate(String country) {
        Objects.requireNonNull(country, "country");
        Map<String, String> query = new LinkedHashMap<>();
        query.put("country", upper(country));
        JsonNode body = requestJson("/api/vat/rates", query);
        try {
            return mapper.treeToValue(body, VatCountryResponse.class);
        } catch (IOException e) {
            throw new UniRateException("Failed to decode VAT country response: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------

    private JsonNode requestJson(String path, Map<String, String> query) {
        String url = buildUrl(path, query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = transport.send(request);
        } catch (IOException e) {
            throw new UniRateException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UniRateException("Request interrupted", e);
        }

        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body();

        if (status >= 200 && status < 300) {
            try {
                return mapper.readTree(body);
            } catch (IOException e) {
                throw new UniRateException("Failed to parse response JSON: " + e.getMessage(), e);
            }
        }

        switch (status) {
            case 400:
                throw new InvalidDateException("Invalid request parameters");
            case 401:
                throw new AuthenticationException("Missing or invalid API key");
            case 404:
                throw new InvalidCurrencyException("Currency not found or no data available");
            case 429:
                throw new RateLimitException("Rate limit exceeded");
            default:
                throw new ApiException(status, body);
        }
    }

    private String buildUrl(String path, Map<String, String> query) {
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append(path);
        sb.append('?');
        boolean first = true;
        for (Map.Entry<String, String> e : query.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()));
        }
        if (!first) {
            sb.append('&');
        }
        sb.append("api_key=").append(urlEncode(apiKey));
        return sb.toString();
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ROOT);
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String formatAmount(double amount) {
        if (amount == Math.rint(amount) && !Double.isInfinite(amount)) {
            long asLong = (long) amount;
            if ((double) asLong == amount) {
                return Long.toString(asLong);
            }
        }
        return Double.toString(amount);
    }

    private static double parseDouble(JsonNode node, String field) {
        if (node == null) {
            throw new UniRateException("Malformed response: missing '" + field + "'");
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            try {
                return Double.parseDouble(node.asText());
            } catch (NumberFormatException e) {
                throw new UniRateException("Malformed response: '" + field + "' is not numeric: " + node.asText(), e);
            }
        }
        throw new UniRateException("Malformed response: '" + field + "' has unexpected type");
    }

    private Map<String, Double> parseDoubleMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new UniRateException("Malformed response: missing or invalid 'rates' map");
        }
        Map<String, Double> out = new LinkedHashMap<>();
        node.fields().forEachRemaining(e -> {
            JsonNode v = e.getValue();
            if (v.isNumber()) {
                out.put(e.getKey(), v.asDouble());
            } else if (v.isTextual()) {
                try {
                    out.put(e.getKey(), Double.parseDouble(v.asText()));
                } catch (NumberFormatException nfe) {
                    throw new UniRateException("Malformed rate for " + e.getKey() + ": " + v.asText(), nfe);
                }
            } else {
                throw new UniRateException("Unexpected rate type for " + e.getKey());
            }
        });
        return out;
    }
}
