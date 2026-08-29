package com.simhastha.view;

public final class NoOpBookingNotificationService implements BookingNotificationService {

    @Override
    public void bookingConfirmed(AppDataStore.BookingRecord booking, AppDataStore.TicketRecord ticket) {
        // Future SMS, WhatsApp, and email integrations can subscribe here.
    }
}
