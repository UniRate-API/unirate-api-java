package com.unirateapi.examples;

import com.unirateapi.client.UniRateClient;

import java.util.List;
import java.util.Map;

/**
 * Runnable example for the UniRate Java client.
 *
 * <p>Reads the API key from the {@code UNIRATE_API_KEY} environment variable.
 * Run with Maven:</p>
 *
 * <pre>
 *     UNIRATE_API_KEY=your-key mvn -q compile exec:java \
 *         -Dexec.mainClass=com.unirateapi.examples.Example
 * </pre>
 */
public final class Example {

    private Example() {
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("UNIRATE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Set UNIRATE_API_KEY in the environment.");
            System.exit(1);
        }

        UniRateClient client = new UniRateClient(apiKey);

        // 1. Current single pair rate
        double rate = client.getRate("USD", "EUR");
        System.out.printf("1 USD = %.4f EUR%n", rate);

        // 2. Convert an amount
        double euros = client.convert(100.0, "USD", "EUR");
        System.out.printf("100 USD = %.2f EUR%n", euros);

        // 3. Supported currency list
        List<String> codes = client.getSupportedCurrencies();
        System.out.printf("Supported currencies: %d%n", codes.size());
        System.out.println("First 10: " + codes.subList(0, Math.min(10, codes.size())));

        // 4. All rates for USD
        Map<String, Double> all = client.getAllRates("USD");
        System.out.printf("Fetched %d USD rates. GBP = %s%n", all.size(), all.get("GBP"));
    }
}
