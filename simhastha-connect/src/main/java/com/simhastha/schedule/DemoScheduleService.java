package com.simhastha.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/** Local Part 2 data source. It can be replaced by a remote repository in Part 3. */
public final class DemoScheduleService {
    public List<ScheduleEvent> loadDemoSchedule(LocalDate date) {
        return List.of(
                event("morning-aarti", "Morning Aarti", ScheduleCategory.AARTI, "Ramkund, Panchavati", date, 5, 30, 6, 30, "Morning prayer ceremony at Ramkund.", "Ramkund Seva Committee", true, "Arrive 15 minutes early."),
                event("holy-snan", "Holy Snan", ScheduleCategory.SNAN, "Godavari Ghat", date, 7, 0, 8, 30, "Guided sacred bathing window.", "Ghat Management Team", true, "Follow volunteer instructions."),
                event("akhada-procession", "Akhada Procession", ScheduleCategory.AKHADA, "Panchavati Main Road", date, 9, 30, 11, 0, "Traditional akhada procession.", "Akhada Coordination Cell", false, "Use marked viewing areas."),
                event("pravachan", "Pravachan", ScheduleCategory.SAMAJ, "Kumbh Pravachan Mandap", date, 13, 0, 14, 30, "Spiritual discourse for pilgrims.", "Pravachan Mandap", false, "Seating is first come, first served."),
                event("bhandara", "Bhandara", ScheduleCategory.SAMAJ, "Vishram Ghat", date, 15, 0, 16, 0, "Community meal service.", "Seva Mandal", false, "Please use the designated queue."),
                event("maha-aarti", "Godavari Maha Aarti", ScheduleCategory.AARTI, "Ramkund, Panchavati", date, 18, 30, 19, 30, "Evening maha aarti by the Godavari.", "Ramkund Seva Committee", true, "High crowd movement is expected."),
                event("bhajan-kirtan", "Bhajan / Kirtan", ScheduleCategory.CULTURAL, "Sant Dnyaneshwar Maidan", date, 20, 0, 21, 0, "Devotional music programme.", "Cultural Committee", false, "Family seating available."),
                event("cultural-program", "Cultural Program", ScheduleCategory.CULTURAL, "Cultural Ground", date, 21, 30, 23, 0, "Traditional cultural performances.", "Nashik Cultural Cell", false, "Entry closes at 10:30 PM."))
                .stream().sorted(Comparator.comparing(ScheduleEvent::startTime)).toList();
    }

    public List<ScheduleAlert> loadDemoAlerts(LocalDate date) {
        return List.of(new ScheduleAlert("ramkund-gate-2", "Ramkund Gate 2 closed until 10:00 AM.",
                "Use Gate 3 for entry. Volunteers are available to guide pilgrims along the alternate approach.", "Ramkund, Panchavati", date,
                LocalTime.of(8, 0), LocalTime.of(10, 0), "WARNING", true));
    }

    public ScheduleStatus statusFor(ScheduleEvent event, LocalDateTime now) {
        if (event.cancelled()) return ScheduleStatus.CANCELLED;
        if (event.date().isBefore(now.toLocalDate())) return ScheduleStatus.COMPLETED;
        if (event.date().isAfter(now.toLocalDate())) return ScheduleStatus.UPCOMING;
        if (now.toLocalTime().isBefore(event.startTime())) return ScheduleStatus.UPCOMING;
        if (!now.toLocalTime().isAfter(event.endTime())) return ScheduleStatus.LIVE_NOW;
        return ScheduleStatus.COMPLETED;
    }

    private ScheduleEvent event(String id, String title, ScheduleCategory category, String location, LocalDate date,
            int startHour, int startMinute, int endHour, int endMinute, String description, String organizer,
            boolean important, String note) {
        return new ScheduleEvent(id + "-" + date, title, category, location, date, LocalTime.of(startHour, startMinute),
                LocalTime.of(endHour, endMinute), description, organizer, important, note, false);
    }
}
