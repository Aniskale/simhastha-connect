package com.simhastha.dao.implementation;

import com.simhastha.dao.BookingDao;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.List;

public final class FirestoreBookingDao implements BookingDao {

    private final FirestoreGateway gateway;

    public FirestoreBookingDao(FirestoreGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public List<AppDataStore.BookingRecord> findAll(String idToken) throws IOException, InterruptedException {
        return gateway.loadBookings(idToken);
    }

    @Override
    public List<AppDataStore.BookingRecord> findByField(String fieldName, String value, String idToken)
            throws IOException, InterruptedException {
        return gateway.loadBookingsForField(fieldName, value, idToken);
    }

    @Override
    public void save(AppDataStore.BookingRecord booking, String businessOwnerId, String idToken)
            throws IOException, InterruptedException {
        gateway.saveBooking(booking, businessOwnerId, idToken);
    }

    @Override
    public void updateStatus(String bookingId, String bookingStatus, String paymentStatus, String idToken)
            throws IOException, InterruptedException {
        gateway.updateBookingStatus(bookingId, bookingStatus, paymentStatus, idToken);
    }
}
