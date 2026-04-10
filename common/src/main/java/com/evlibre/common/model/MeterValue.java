package com.evlibre.common.model;

import java.time.Instant;
import java.util.List;

public record MeterValue(Instant timestamp, List<SampledValue> sampledValue) {}
