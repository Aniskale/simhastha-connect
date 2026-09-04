package com.simhastha.dao.implementation;

import com.simhastha.dao.OperatorDao;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.view.AppDataStore;
import com.simhastha.view.OperatorAuthPage;

import java.io.IOException;
import java.util.List;

public final class FirestoreOperatorDao implements OperatorDao {

    private final FirestoreGateway gateway;

    public FirestoreOperatorDao(FirestoreGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public List<AppDataStore.TransportOperatorRecord> findAll(String idToken) throws IOException, InterruptedException {
        return gateway.loadTransportOperators(idToken);
    }

    @Override
    public void saveRegistration(String uid, OperatorAuthPage.OperatorAccount account, String idToken)
            throws IOException, InterruptedException {
        gateway.saveTransportOperatorProfile(uid, account, idToken);
    }

    @Override
    public void updateStatus(String operatorId, String status, boolean approved, String idToken)
            throws IOException, InterruptedException {
        gateway.updateDocumentStatus("transportOperators", operatorId, status, approved, idToken);
    }
}
