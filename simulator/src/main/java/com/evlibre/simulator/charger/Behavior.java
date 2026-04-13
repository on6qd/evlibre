package com.evlibre.simulator.charger;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Behavior {

    @JsonProperty("idle")
    IDLE,

    @JsonProperty("auto-charge")
    AUTO_CHARGE,

    @JsonProperty("error-prone")
    ERROR_PRONE
}
