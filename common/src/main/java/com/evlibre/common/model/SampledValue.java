package com.evlibre.common.model;

public record SampledValue(
        String value,
        String context,
        String format,
        String measurand,
        String phase,
        String location,
        String unit
) {}
