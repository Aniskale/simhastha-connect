package com.simhastha.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleEvent(String id, String title, ScheduleCategory category, String location, LocalDate date,
        LocalTime startTime, LocalTime endTime, String description, String organizer, boolean important,
        String note, boolean cancelled, Double latitude, Double longitude, String locationId) {
    public ScheduleEvent(String id, String title, ScheduleCategory category, String location, LocalDate date,
            LocalTime startTime, LocalTime endTime, String description, String organizer, boolean important,
            String note, boolean cancelled) {
        this(id, title, category, location, date, startTime, endTime, description, organizer, important, note,
                cancelled, null, null, "");
    }
}
