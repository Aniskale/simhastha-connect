package com.simhastha.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleAlert(String id, String title, String message, String location, LocalDate date,
        LocalTime startTime, LocalTime endTime, String severity, boolean active) { }
