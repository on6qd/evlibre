package com.evlibre.simulator.config;

import com.evlibre.simulator.charger.Behavior;

public class FleetConfig {

    public String tenant = "demo-tenant";
    public String prefix = "SIM";
    public int count = 1;
    public String profile;
    public Behavior behavior = Behavior.IDLE;

    public String tenant() { return tenant; }
    public String prefix() { return prefix; }
    public int count() { return count; }
    public String profile() { return profile; }
    public Behavior behavior() { return behavior; }
}
