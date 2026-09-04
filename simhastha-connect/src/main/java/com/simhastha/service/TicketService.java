package com.simhastha.service;

import com.simhastha.view.AppDataStore;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class TicketService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public AppDataStore.TicketRecord issueTicket(AppDataStore.BookingRecord booking) {
        String ticketId = "TKT-" + booking.moduleType + "-" + booking.bookingId.substring(Math.max(0, booking.bookingId.length() - 8));
        String verificationRef = ticketId + ":" + shortToken();
        return AppDataStore.addTicket(new AppDataStore.TicketRecord(ticketId, booking.bookingId,
                booking.internalPaymentId, booking.userId, booking.moduleType, booking.businessId, booking.title,
                booking.customerName, booking.dateText, booking.location, booking.amountPaise, "ACTIVE",
                verificationRef));
    }

    private String shortToken() {
        byte[] bytes = new byte[6];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes).toUpperCase();
    }
}
