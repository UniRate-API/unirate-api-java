package com.unirateapi.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Response from {@code GET /api/vat/rates?country=XX}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class VatCountryResponse {

    @JsonProperty("country")
    private String country;

    @JsonProperty("vat_data")
    private VatRate vatData;

    public VatCountryResponse() {
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public VatRate getVatData() {
        return vatData;
    }

    public void setVatData(VatRate vatData) {
        this.vatData = vatData;
    }
}
