package com.simhastha.dao;

import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.List;

public interface BookingDao {
    List<AppDataStore.BookingRecord> findAll(String idToken) throws IOException, InterruptedException;

    List<AppDataStore.BookingRecord> findByField(String fieldName, String value, String idToken)
            throws IOException, InterruptedException;

    void save(AppDataStore.BookingRecord booking, String businessOwnerId, String idToken)
            throws IOException, InterruptedException;

    void updateStatus(String bookingId, String bookingStatus, String paymentStatus, String idToken)
            throws IOException, InterruptedException;
}
