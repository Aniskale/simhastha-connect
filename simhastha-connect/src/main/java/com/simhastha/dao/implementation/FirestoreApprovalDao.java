package com.simhastha.dao.implementation;

import com.simhastha.dao.ApprovalDao;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.List;

public final class FirestoreApprovalDao implements ApprovalDao {

    private final FirestoreGateway gateway;

    public FirestoreApprovalDao(FirestoreGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public List<AppDataStore.ApprovalRequest> findAll(String idToken) throws IOException, InterruptedException {
        return gateway.loadApprovals(idToken);
    }

    @Override
    public List<AppDataStore.ApprovalRequest> findPendingBusinessApprovals(String idToken)
            throws IOException, InterruptedException {
        return gateway.loadPendingBusinessApprovals(idToken);
    }

    @Override
    public void save(AppDataStore.ApprovalRequest request, String idToken) throws IOException, InterruptedException {
        gateway.saveApproval(request, idToken);
    }

    @Override
    public void save(AppDataStore.ApprovalRequest request) {
        gateway.saveApproval(request);
    }

    @Override
    public void delete(AppDataStore.ApprovalRequest request, String idToken) {
        gateway.deleteApproval(request, idToken);
    }

    @Override
    public void updateStatus(String requestId, String status, String idToken) throws IOException, InterruptedException {
        gateway.updateApprovalRequestStatus(requestId, status, idToken);
    }
}
