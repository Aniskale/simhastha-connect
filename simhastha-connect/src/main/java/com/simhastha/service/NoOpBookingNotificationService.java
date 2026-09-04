package com.simhastha.service;

import com.simhastha.view.AppDataStore;

public final class NoOpBookingNotificationService implements BookingNotificationService {

    @Override
    public void bookingConfirmed(AppDataStore.BookingRecord booking, AppDataStore.TicketRecord ticket) {
        AppDataStore.saveOperationalItem("announcement", new AppDataStore.ServiceItem(
                "booking-notification-" + booking.bookingId,
                "announcement",
                "Booking confirmed",
                booking.title + " confirmed for " + booking.customerName + ". Ticket " + ticket.ticketId + " issued.",
                "Booking"));
    }
}
