package com.simhastha.view;

public interface BookingNotificationService {
    void bookingConfirmed(AppDataStore.BookingRecord booking, AppDataStore.TicketRecord ticket);
}
