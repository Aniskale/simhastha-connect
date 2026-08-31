package com.simhastha.dao;

import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.model.BusinessLocation;
import com.simhastha.model.BusinessProfileUpdate;
import com.simhastha.view.AppDataStore;
import com.simhastha.view.BusinessAuthPage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface BusinessDao {
    List<AppDataStore.BusinessRecord> findAll(String idToken) throws IOException, InterruptedException;

    List<AppDataStore.BusinessRecord> findPublic(String idToken) throws IOException, InterruptedException;

    Optional<FirestoreGateway.BusinessProfile> findByOwner(String ownerId, String idToken)
            throws IOException, InterruptedException;

    List<FirestoreGateway.BusinessInventoryItem> findInventory(String businessId, String ownerId, String idToken)
            throws IOException, InterruptedException;

    List<FirestoreGateway.BusinessInventoryItem> findPublicInventory(String businessId, String idToken)
            throws IOException, InterruptedException;

    void saveRegistration(String uid, BusinessAuthPage.BusinessAccount account, String idToken)
            throws IOException, InterruptedException;

    void saveInventoryItem(FirestoreGateway.BusinessInventoryItem item, String idToken)
            throws IOException, InterruptedException;

    void updateProfile(BusinessProfileUpdate update, String idToken) throws IOException, InterruptedException;

    void updateStatus(String businessId, String status, boolean approved, String idToken)
            throws IOException, InterruptedException;

    void updateLocation(String businessId, BusinessLocation location, String idToken)
            throws IOException, InterruptedException;
}
