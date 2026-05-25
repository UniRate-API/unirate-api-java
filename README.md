# UniRate Java Client

Official Java client for the [UniRate API](https://unirateapi.com) — free, real-time and historical currency exchange rates plus VAT rates.

- Real-time exchange rates between 170+ currencies (fiat + crypto)
- Historical rates back to 1999
- Time-series ranges up to 5 years
- Currency conversion (current and historical)
- VAT rates for countries worldwide
- Free tier, no credit card required
- Pure Java — `java.net.http.HttpClient` + Jackson, nothing else
- Typed exception hierarchy rooted at `UniRateException`

## Requirements

- Java 11+
- Maven (or Gradle)

## Installation

### Maven

```xml
<dependency>
    <groupId>com.unirateapi</groupId>
    <artifactId>unirate-api</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation "com.unirateapi:unirate-api:0.1.0"
```

## Quick start

```java
import com.unirateapi.client.UniRateClient;

UniRateClient client = new UniRateClient("your-api-key");

// Current rate
double rate = client.getRate("USD", "EUR");
System.out.println("USD -> EUR: " + rate);

// Convert
double euros = client.convert(100, "USD", "EUR");
System.out.println("100 USD = " + euros + " EUR");

// All supported currencies
java.util.List<String> currencies = client.getSupportedCurrencies();
System.out.println(currencies.size() + " currencies supported");
```

Get a free API key at [https://unirateapi.com](https://unirateapi.com).

## API

### Current rates

```java
// Single pair
double rate = client.getRate("USD", "EUR");

// All rates for a base
Map<String, Double> rates = client.getAllRates("USD");

// Convert an amount
double result = client.convert(100, "USD", "EUR");

// Supported currency list
List<String> codes = client.getSupportedCurrencies();
```

### Historical data

```java
// Rate on a specific date
double rate = client.getHistoricalRate("2024-01-01", "USD", "EUR");

// All rates on a date
Map<String, Double> rates = client.getHistoricalRates("2024-01-01", "USD");

// Convert using historical rate
double amount = client.convertHistorical(100, "USD", "EUR", "2024-01-01");

// Time series
Map<String, Map<String, Double>> series = client.getTimeSeries(
        "2024-01-01", "2024-01-07",
        "USD", List.of("EUR", "GBP"),
        1);

// Available historical coverage per currency
HistoricalLimitsResponse limits = client.getHistoricalLimits();
```

### VAT rates

```java
// All countries
VatRatesResponse vatRates = client.getVatRates();

// Single country (ISO-3166 alpha-2 code)
VatCountryResponse germany = client.getVatRate("DE");
System.out.println("Germany VAT: " + germany.getVatData().getVatRate() + "%");
```

## Error handling

All methods throw unchecked exceptions extending `UniRateException`:

```java
try {
    double rate = client.getRate("USD", "ZZZ");
} catch (AuthenticationException e) {
    // invalid API key
} catch (InvalidCurrencyException e) {
    // unknown currency code
} catch (RateLimitException e) {
    // back off and retry
} catch (InvalidDateException e) {
    // bad date format / parameters
} catch (ApiException e) {
    // other HTTP error (e.g. 403 Pro-gated, 503)
    System.err.println("status " + e.getStatusCode() + ": " + e.getBody());
} catch (UniRateException e) {
    // transport / decoding failure
}
```

| HTTP status | Exception type |
|---|---|
| 400 | `InvalidDateException` |
| 401 | `AuthenticationException` |
| 403 | `ApiException` (Pro-gated endpoint on free tier) |
| 404 | `InvalidCurrencyException` |
| 429 | `RateLimitException` |
| 503 | `ApiException` |
| other | `ApiException` |
| network / decode | `UniRateException` |

## Advanced — custom HTTP transport / dependency injection

The client accepts anything implementing `HttpTransport` (one method, takes an `HttpRequest`, returns an `HttpResponse<String>`), which makes mocking trivial in tests:

```java
HttpTransport stub = request -> new StubResponse(200, "{\"rate\": 0.9}", request);
UniRateClient client = new UniRateClient(
        "test-key",
        "https://api.unirateapi.com",
        Duration.ofSeconds(5),
        stub);
```

See `src/test/java/com/unirateapi/client/UniRateClientTest.java` for the full pattern.

## Running the example

```bash
UNIRATE_API_KEY=your-key mvn -q compile exec:java \
    -Dexec.mainClass=com.unirateapi.examples.Example
```

## Running tests

```bash
# Mock tests (~18, no network)
mvn -q test

# Mock + live free-tier tests
UNIRATE_API_KEY=your-key mvn -q test
```

## Rate limits

- **Currency endpoints:** standard rate limits apply
- **Historical endpoints:** 50 requests/hour on the free tier
- **VAT endpoints:** 1800 requests/hour on the free tier

## Related clients

- [unirate-api-python](https://github.com/UniRate-API/unirate-api-python) (PyPI: `unirate-api`)
- [unirate-api-nodejs](https://github.com/UniRate-API/unirate-api-nodejs) (npm: `unirate-api`)
- [unirate-api-swift](https://github.com/UniRate-API/unirate-api-swift) (Swift Package Manager)

<!-- unirate-ecosystem-footer:start -->
## Other UniRate clients

UniRate ships official client libraries and framework integrations across the
ecosystem. The repos below are all maintained under the
[UniRate-API](https://github.com/UniRate-API) org.

- **Languages:** [Python](https://github.com/UniRate-API/unirate-api-python) · [Node.js / TypeScript](https://github.com/UniRate-API/unirate-api-nodejs) · [Go](https://github.com/UniRate-API/unirate-api-go) · [Rust](https://github.com/UniRate-API/unirate-api-rust) · [Java](https://github.com/UniRate-API/unirate-api-java) · [Ruby](https://github.com/UniRate-API/unirate-api-ruby) · [PHP](https://github.com/UniRate-API/unirate-api-php) · [.NET](https://github.com/UniRate-API/unirate-api-dotnet) · [Swift](https://github.com/UniRate-API/unirate-api-swift)
- **Web frameworks:** [Django / Wagtail](https://github.com/UniRate-API/wagtail-unirate) · [FastAPI](https://github.com/UniRate-API/fastapi-unirate) · [Flask](https://github.com/UniRate-API/flask-unirate) · [React](https://github.com/UniRate-API/react-unirate) · [tRPC](https://github.com/UniRate-API/trpc-unirate)
- **Static-site generators:** [Astro](https://github.com/UniRate-API/astro-unirate) · [Eleventy](https://github.com/UniRate-API/eleventy-unirate) · [Hugo](https://github.com/UniRate-API/hugo-unirate)
- **Data / orchestration:** [Airflow](https://github.com/UniRate-API/airflow-provider-unirate) · [dbt](https://github.com/UniRate-API/dbt-unirate) · [LangChain](https://github.com/UniRate-API/langchain-unirate)
- **Workflow / no-code:** [n8n](https://github.com/UniRate-API/n8n-nodes-unirate) · [Google Sheets](https://github.com/UniRate-API/unirate-sheets) · [MCP server](https://github.com/UniRate-API/unirate-mcp)
- **Editors / tools:** [VS Code](https://github.com/UniRate-API/vscode-unirate) · [Obsidian](https://github.com/UniRate-API/obsidian-currency)
- **Specialty bridges:** [NodaMoney (.NET)](https://github.com/UniRate-API/UniRateApi.NodaMoney)

Get a free API key at [unirateapi.com](https://unirateapi.com).
<!-- unirate-ecosystem-footer:end -->

## License

MIT — see [LICENSE](LICENSE).
