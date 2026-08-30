package com.simhastha.schedule;

import java.time.LocalDate;
import java.util.List;

import com.simhastha.view.AppSession;
import com.simhastha.view.FirebaseConfig;
import com.simhastha.view.FirestoreGateway;

final class FirestoreScheduleDataSource implements ScheduleDataSource {
    private final FirestoreGateway gateway = new FirestoreGateway(FirebaseConfig.load());
    private String token() { AppSession.User user = AppSession.currentUser(); return user == null ? "" : user.idToken(); }
    private String adminUid() { AppSession.User user = AppSession.currentUser(); if (user == null || !user.isAdmin()) throw new SecurityException("An active admin session is required."); return user.uid(); }
    public List<ScheduleEvent> eventsForDate(LocalDate date) throws Exception { return gateway.loadScheduleEventsByDate(date, token()); }
    public List<ScheduleAlert> alertsForDate(LocalDate date) throws Exception { return gateway.loadScheduleAlertsByDate(date, token()); }
    public ScheduleEvent saveEvent(ScheduleEvent event) throws Exception { gateway.saveScheduleEvent(event, adminUid(), token()); return event; }
    public void deleteEvent(String id) throws Exception { gateway.deleteScheduleEvent(id, token()); }
    public ScheduleAlert saveAlert(ScheduleAlert alert) throws Exception { gateway.saveScheduleAlert(alert, adminUid(), token()); return alert; }
    public void deleteAlert(String id) throws Exception { gateway.deleteScheduleAlert(id, token()); }
}
