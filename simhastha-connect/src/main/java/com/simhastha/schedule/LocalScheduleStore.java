package com.simhastha.schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** One in-app store shared by the user and admin schedule views. */
final class LocalScheduleStore {
    private static final List<ScheduleEvent> EVENTS = new ArrayList<>();
    private static final List<ScheduleAlert> ALERTS = new ArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static boolean seeded;

    private LocalScheduleStore() { }

    static synchronized List<ScheduleEvent> eventsForDate(LocalDate date) { seed(); return EVENTS.stream().filter(event -> event.date().equals(date)).sorted(Comparator.comparing(ScheduleEvent::startTime)).toList(); }
    static synchronized List<ScheduleAlert> alertsForDate(LocalDate date) { seed(); return ALERTS.stream().filter(alert -> alert.date().equals(date)).toList(); }
    static synchronized ScheduleEvent saveEvent(ScheduleEvent event) { seed(); EVENTS.removeIf(current -> current.id().equals(event.id())); EVENTS.add(event); notifyChanged(); return event; }
    static synchronized void deleteEvent(String id) { seed(); EVENTS.removeIf(event -> event.id().equals(id)); notifyChanged(); }
    static synchronized ScheduleAlert saveAlert(ScheduleAlert alert) { seed(); ALERTS.removeIf(current -> current.id().equals(alert.id())); ALERTS.add(alert); notifyChanged(); return alert; }
    static synchronized void deleteAlert(String id) { seed(); ALERTS.removeIf(alert -> alert.id().equals(id)); notifyChanged(); }
    static void addListener(Runnable listener) { LISTENERS.addIfAbsent(listener); }
    static void removeListener(Runnable listener) { LISTENERS.remove(listener); }

    private static void seed() {
        if (seeded) return;
        seeded = true;
        DemoScheduleService demo = new DemoScheduleService();
        LocalDate today = LocalDate.now();
        EVENTS.addAll(demo.loadDemoSchedule(today));
        ALERTS.addAll(demo.loadDemoAlerts(today));
    }
    private static void notifyChanged() { LISTENERS.forEach(listener -> { try { listener.run(); } catch (RuntimeException ignored) { } }); }
}
