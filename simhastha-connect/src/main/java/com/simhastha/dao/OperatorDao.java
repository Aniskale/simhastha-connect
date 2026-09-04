package com.simhastha.dao;

import com.simhastha.view.AppDataStore;
import com.simhastha.view.OperatorAuthPage;

import java.io.IOException;
import java.util.List;

public interface OperatorDao {
    List<AppDataStore.TransportOperatorRecord> findAll(String idToken) throws IOException, InterruptedException;

    void saveRegistration(String uid, OperatorAuthPage.OperatorAccount account, String idToken)
            throws IOException, InterruptedException;

    void updateStatus(String operatorId, String status, boolean approved, String idToken)
            throws IOException, InterruptedException;
}
