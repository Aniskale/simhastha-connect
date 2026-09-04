package com.simhastha.packages;

/** Indian Railways class vocabulary for future provider-backed searches. */
public enum TrainTravelClass {
    SL("SL — Sleeper"), THREE_A("3A — AC 3 Tier"), TWO_A("2A — AC 2 Tier"), ONE_A("1A — First AC"),
    CC("CC — AC Chair Car"), TWO_S("2S — Second Sitting"), THREE_E("3E — AC 3 Economy");
    private final String label;
    TrainTravelClass(String label) { this.label = label; }
    public String label() { return label; }
    @Override public String toString() { return label; }
}
