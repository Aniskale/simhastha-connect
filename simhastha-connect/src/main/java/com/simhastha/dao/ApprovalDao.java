package com.simhastha.dao;

import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.List;

public interface ApprovalDao {
    List<AppDataStore.ApprovalRequest> findAll(String idToken) throws IOException, InterruptedException;

    List<AppDataStore.ApprovalRequest> findPendingBusinessApprovals(String idToken)
            throws IOException, InterruptedException;

    void save(AppDataStore.ApprovalRequest request, String idToken) throws IOException, InterruptedException;

    void save(AppDataStore.ApprovalRequest request);

    void delete(AppDataStore.ApprovalRequest request, String idToken);

    void updateStatus(String requestId, String status, String idToken) throws IOException, InterruptedException;
}
