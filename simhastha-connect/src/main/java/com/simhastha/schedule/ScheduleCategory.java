package com.simhastha.schedule;

public enum ScheduleCategory {
    SNAN("Snan"), AARTI("Aarti"), AKHADA("Akhada"), SAMAJ("Samaj"),
    CULTURAL("Cultural"), GOVERNMENT("Government"), IMPORTANT("Important");

    private final String label;
    ScheduleCategory(String label) { this.label = label; }
    public String label() { return label; }
}
