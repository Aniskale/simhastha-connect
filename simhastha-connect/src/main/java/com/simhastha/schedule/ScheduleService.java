package com.simhastha.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import com.simhastha.gateway.firebase.FirebaseConfig;

/** Chooses Firestore until it fails, then uses one shared local development store for this app session. */
public final class ScheduleService {
    private static final Logger LOGGER = Logger.getLogger(ScheduleService.class.getName());
    private static final Object MODE_LOCK = new Object();
    private static final ScheduleDataSource LOCAL = new LocalScheduleDataSource();
    private static ScheduleDataSource source;
    private static boolean localMode;
    private static boolean fallbackLogged;

    private ScheduleDataSource source() {
        synchronized (MODE_LOCK) {
            if (source != null) return source;
            if (!FirebaseConfig.load().isEnabled()) return activateLocal(null);
            source = new FirestoreScheduleDataSource();
            return source;
        }
    }
    public List<ScheduleEvent> eventsForDate(LocalDate date) throws Exception { return read(current -> current.eventsForDate(date)); }
    public List<ScheduleAlert> alertsForDate(LocalDate date) throws Exception { return read(current -> current.alertsForDate(date)); }
    public ScheduleEvent saveEvent(ScheduleEvent event) throws Exception { validate(event); ScheduleEvent saved = event.id() == null || event.id().isBlank() ? new ScheduleEvent(UUID.randomUUID().toString(), event.title(), event.category(), event.location(), event.date(), event.startTime(), event.endTime(), event.description(), event.organizer(), event.important(), event.note(), event.cancelled()) : event; return write(current -> current.saveEvent(saved)); }
    public void deleteEvent(String id) throws Exception { write(current -> { current.deleteEvent(id); return null; }); }
    public ScheduleAlert saveAlert(ScheduleAlert alert) throws Exception { validateAlert(alert); ScheduleAlert saved = alert.id() == null || alert.id().isBlank() ? new ScheduleAlert(UUID.randomUUID().toString(), alert.title(), alert.message(), alert.location(), alert.date(), alert.startTime(), alert.endTime(), alert.severity(), alert.active()) : alert; return write(current -> current.saveAlert(saved)); }
    public void deleteAlert(String id) throws Exception { write(current -> { current.deleteAlert(id); return null; }); }
    public boolean isLocalMode() { synchronized (MODE_LOCK) { return localMode; } }
    public void addLocalChangeListener(Runnable listener) { LocalScheduleStore.addListener(listener); }
    public void removeLocalChangeListener(Runnable listener) { LocalScheduleStore.removeListener(listener); }
    public ScheduleStatus statusFor(ScheduleEvent event, LocalDateTime now) { if (event.cancelled()) return ScheduleStatus.CANCELLED; if (event.date().isBefore(now.toLocalDate())) return ScheduleStatus.COMPLETED; if (event.date().isAfter(now.toLocalDate()) || now.toLocalTime().isBefore(event.startTime())) return ScheduleStatus.UPCOMING; return now.toLocalTime().isAfter(event.endTime()) ? ScheduleStatus.COMPLETED : ScheduleStatus.LIVE_NOW; }
    private <T> T read(SourceOperation<T> operation) throws Exception { try { return operation.run(source()); } catch (Exception failure) { return operation.run(activateLocal(failure)); } }
    private <T> T write(SourceOperation<T> operation) throws Exception { try { return operation.run(source()); } catch (Exception failure) { return operation.run(activateLocal(failure)); } }
    private ScheduleDataSource activateLocal(Exception failure) { synchronized (MODE_LOCK) { source = LOCAL; localMode = true; if (!fallbackLogged) { fallbackLogged = true; LOGGER.fine("Schedule backend unavailable; using local development data." + (failure == null ? "" : " Cause: " + failure.getClass().getSimpleName())); } return source; } }
    private void validate(ScheduleEvent event) { if (event.title().isBlank() || event.category() == null || event.location().isBlank() || event.date() == null || event.startTime() == null || event.endTime() == null) throw new IllegalArgumentException("Title, category, date, time and location are required."); if (!event.endTime().isAfter(event.startTime())) throw new IllegalArgumentException("End time must be after start time."); }
    private void validateAlert(ScheduleAlert alert) { if (alert.title().isBlank() || alert.message().isBlank() || alert.date() == null) throw new IllegalArgumentException("Alert title, message and date are required."); if ((alert.startTime() == null) != (alert.endTime() == null) || (alert.startTime() != null && !alert.endTime().isAfter(alert.startTime()))) throw new IllegalArgumentException("Alert end time must be after start time."); }
    @FunctionalInterface private interface SourceOperation<T> { T run(ScheduleDataSource current) throws Exception; }
}
