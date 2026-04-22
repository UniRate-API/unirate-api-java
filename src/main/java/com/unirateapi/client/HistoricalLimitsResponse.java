package com.unirateapi.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/** Response from {@code GET /api/historical/limits}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class HistoricalLimitsResponse {

    @JsonProperty("total_currencies")
    private int totalCurrencies;

    @JsonProperty("currencies")
    private Map<String, HistoricalLimit> currencies = Collections.emptyMap();

    public HistoricalLimitsResponse() {
    }

    public int getTotalCurrencies() {
        return totalCurrencies;
    }

    public void setTotalCurrencies(int totalCurrencies) {
        this.totalCurrencies = totalCurrencies;
    }

    public Map<String, HistoricalLimit> getCurrencies() {
        return currencies;
    }

    public void setCurrencies(Map<String, HistoricalLimit> currencies) {
        this.currencies = currencies == null ? Collections.emptyMap() : currencies;
    }
}
