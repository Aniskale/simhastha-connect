package com.simhastha.schedule;

import java.time.LocalDate;
import java.util.List;

final class LocalScheduleDataSource implements ScheduleDataSource {
    public List<ScheduleEvent> eventsForDate(LocalDate date) { return LocalScheduleStore.eventsForDate(date); }
    public List<ScheduleAlert> alertsForDate(LocalDate date) { return LocalScheduleStore.alertsForDate(date); }
    public ScheduleEvent saveEvent(ScheduleEvent event) { return LocalScheduleStore.saveEvent(event); }
    public void deleteEvent(String id) { LocalScheduleStore.deleteEvent(id); }
    public ScheduleAlert saveAlert(ScheduleAlert alert) { return LocalScheduleStore.saveAlert(alert); }
    public void deleteAlert(String id) { LocalScheduleStore.deleteAlert(id); }
}
