package com.simhastha.service;

import com.simhastha.view.AppDataStore;

public interface BookingNotificationService {
    void bookingConfirmed(AppDataStore.BookingRecord booking, AppDataStore.TicketRecord ticket);
}
