package com.evlibre.simulator.config;

import com.evlibre.simulator.charger.Behavior;

public class ChargerConfig {

    public String id;
    public String tenant = "demo-tenant";
    public String profile;
    public Behavior behavior = Behavior.IDLE;

    public String id() { return id; }
    public String tenant() { return tenant; }
    public String profile() { return profile; }
    public Behavior behavior() { return behavior; }
}
