package com.unirateapi.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Historical data coverage window for a single currency. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class HistoricalLimit {

    @JsonProperty("earliest_date")
    private String earliestDate;

    @JsonProperty("latest_date")
    private String latestDate;

    public HistoricalLimit() {
    }

    public String getEarliestDate() {
        return earliestDate;
    }

    public void setEarliestDate(String earliestDate) {
        this.earliestDate = earliestDate;
    }

    public String getLatestDate() {
        return latestDate;
    }

    public void setLatestDate(String latestDate) {
        this.latestDate = latestDate;
    }
}
