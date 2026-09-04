package com.simhastha.schedule;

import java.time.LocalTime;

public enum SchedulePeriod {
    MORNING("MORNING", "05:00 AM – 12:00 PM"), AFTERNOON("AFTERNOON", "12:00 PM – 05:00 PM"),
    EVENING("EVENING", "05:00 PM – 09:00 PM"), NIGHT("NIGHT", "09:00 PM – 11:30 PM");
    private final String title;
    private final String duration;
    SchedulePeriod(String title, String duration) { this.title = title; this.duration = duration; }
    public String title() { return title; }
    public String duration() { return duration; }
    public static SchedulePeriod from(LocalTime time) {
        if (time.isBefore(LocalTime.NOON)) return MORNING;
        if (time.isBefore(LocalTime.of(17, 0))) return AFTERNOON;
        if (time.isBefore(LocalTime.of(21, 0))) return EVENING;
        return NIGHT;
    }
}
