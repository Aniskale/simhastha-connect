package com.simhastha.dao.implementation;

import com.simhastha.dao.BusinessDao;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.view.AppDataStore;
import com.simhastha.view.BusinessAuthPage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class FirestoreBusinessDao implements BusinessDao {

    private final FirestoreGateway gateway;

    public FirestoreBusinessDao(FirestoreGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public List<AppDataStore.BusinessRecord> findAll(String idToken) throws IOException, InterruptedException {
        return gateway.loadBusinesses(idToken);
    }

    @Override
    public Optional<FirestoreGateway.BusinessProfile> findByOwner(String ownerId, String idToken)
            throws IOException, InterruptedException {
        return Optional.ofNullable(gateway.loadBusinessForOwner(ownerId, idToken));
    }

    @Override
    public List<FirestoreGateway.BusinessInventoryItem> findInventory(String businessId, String ownerId, String idToken)
            throws IOException, InterruptedException {
        return gateway.loadBusinessItems(businessId, ownerId, idToken);
    }

    @Override
    public void saveRegistration(String uid, BusinessAuthPage.BusinessAccount account, String idToken)
            throws IOException, InterruptedException {
        gateway.saveBusinessProfile(uid, account, idToken);
    }

    @Override
    public void saveInventoryItem(FirestoreGateway.BusinessInventoryItem item, String idToken)
            throws IOException, InterruptedException {
        gateway.saveBusinessItem(item, idToken);
    }

    @Override
    public void updateStatus(String businessId, String status, boolean approved, String idToken)
            throws IOException, InterruptedException {
        gateway.updateDocumentStatus("businesses", businessId, status, approved, idToken);
    }
}
