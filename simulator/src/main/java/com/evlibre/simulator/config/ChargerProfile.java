package com.evlibre.simulator.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ChargerProfile {

    public String vendor = "GenericVendor";
    public String model = "GenericModel";
    public String protocol = "ocpp1.6";

    @JsonProperty("evse_count")
    public int evseCount = 1;

    @JsonProperty("id_tags")
    public List<String> idTags = List.of("TAG001", "TAG002");

    public String vendor() { return vendor; }
    public String model() { return model; }
    public String protocol() { return protocol; }
    public int evseCount() { return evseCount; }
    public List<String> idTags() { return idTags; }
}
