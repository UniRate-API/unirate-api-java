package com.unirateapi.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration tests that hit {@code api.unirateapi.com}.
 *
 * <p>Automatically skipped unless {@code UNIRATE_API_KEY} is set in the
 * environment. Run with:</p>
 *
 * <pre>
 *     UNIRATE_API_KEY=your-key mvn test
 * </pre>
 *
 * <p>These tests only exercise free-tier endpoints. Pro-gated endpoints
 * (historical rates / timeseries / limits) are covered by the mock suite,
 * since they 403 on a free-tier key.</p>
 */
@EnabledIfEnvironmentVariable(named = "UNIRATE_API_KEY", matches = ".+")
class UniRateClientLiveTest {

    private UniRateClient client;

    @BeforeEach
    void setUp() {
        String key = System.getenv("UNIRATE_API_KEY");
        assertNotNull(key, "UNIRATE_API_KEY must be set");
        client = new UniRateClient(key);
    }

    @Test
    void liveGetRate() {
        double rate = client.getRate("USD", "EUR");
        assertTrue(rate > 0 && rate < 10, "implausible USD/EUR rate: " + rate);
    }

    @Test
    void liveGetAllRates() {
        Map<String, Double> rates = client.getAllRates("USD");
        assertNotNull(rates.get("EUR"));
        assertTrue(rates.size() > 100, "expected >100 currencies, got " + rates.size());
    }

    @Test
    void liveConvert() {
        double result = client.convert(100, "USD", "EUR");
        assertTrue(result > 0 && result < 1000, "implausible convert result: " + result);
    }

    @Test
    void liveGetSupportedCurrencies() {
        List<String> currencies = client.getSupportedCurrencies();
        assertTrue(currencies.contains("USD"));
        assertTrue(currencies.contains("EUR"));
        assertTrue(currencies.size() > 100);
    }

    @Test
    void liveGetVatRate() {
        VatCountryResponse resp = client.getVatRate("DE");
        assertEquals("DE", resp.getVatData().getCountryCode());
        assertEquals("Germany", resp.getVatData().getCountryName());
        assertEquals(19.0, resp.getVatData().getVatRate(), 0.01);
    }

    @Test
    void liveGetVatRates() {
        VatRatesResponse resp = client.getVatRates();
        assertTrue(resp.getTotalCountries() > 20);
        assertNotNull(resp.getVatRates().get("DE"));
        assertEquals("Germany", resp.getVatRates().get("DE").getCountryName());
    }
}
