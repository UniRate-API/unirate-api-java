package com.unirateapi.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/** Response from {@code GET /api/vat/rates} (no country filter). */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class VatRatesResponse {

    @JsonProperty("date")
    private String date;

    @JsonProperty("total_countries")
    private int totalCountries;

    @JsonProperty("vat_rates")
    private Map<String, VatRate> vatRates = Collections.emptyMap();

    public VatRatesResponse() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getTotalCountries() {
        return totalCountries;
    }

    public void setTotalCountries(int totalCountries) {
        this.totalCountries = totalCountries;
    }

    public Map<String, VatRate> getVatRates() {
        return vatRates;
    }

    public void setVatRates(Map<String, VatRate> vatRates) {
        this.vatRates = vatRates == null ? Collections.emptyMap() : vatRates;
    }
}
