package com.simhastha.schedule;

import java.time.LocalDate;
import java.util.List;

interface ScheduleDataSource {
    List<ScheduleEvent> eventsForDate(LocalDate date) throws Exception;
    List<ScheduleAlert> alertsForDate(LocalDate date) throws Exception;
    ScheduleEvent saveEvent(ScheduleEvent event) throws Exception;
    void deleteEvent(String id) throws Exception;
    ScheduleAlert saveAlert(ScheduleAlert alert) throws Exception;
    void deleteAlert(String id) throws Exception;
}
