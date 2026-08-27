package com.simhastha.dao.implementation;

import com.simhastha.dao.OperationalDataDao;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.List;

public final class FirestoreOperationalDataDao implements OperationalDataDao {

    private final FirestoreGateway gateway;

    public FirestoreOperationalDataDao(FirestoreGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public List<AppDataStore.ServiceItem> loadItems(String idToken) throws IOException, InterruptedException {
        return gateway.loadItems(idToken);
    }

    @Override
    public List<AppDataStore.ServiceItem> loadPublicModuleItems(String idToken) throws IOException, InterruptedException {
        return gateway.loadPublicModuleItems(idToken);
    }

    @Override
    public List<AppDataStore.ServiceItem> loadAdminOperationalItems(String idToken) {
        return gateway.loadAdminOperationalItems(idToken);
    }

    @Override
    public List<AppDataStore.RouteRecord> loadTransportRoutes(String idToken) throws IOException, InterruptedException {
        return gateway.loadTransportRoutes(idToken);
    }

    @Override
    public List<AppDataStore.LostFoundCaseRecord> loadLostFoundCases(String idToken)
            throws IOException, InterruptedException {
        return gateway.loadLostFoundCases(idToken);
    }

    @Override
    public AppDataStore.AdminOverview loadAdminOverview(String idToken) throws IOException, InterruptedException {
        return gateway.loadAdminOverview(idToken);
    }

    @Override
    public void saveOperationalItem(String module, AppDataStore.ServiceItem item, String idToken)
            throws IOException, InterruptedException {
        gateway.saveOperationalItem(module, item, idToken);
    }

    @Override
    public void saveItem(String module, AppDataStore.ServiceItem item, String idToken)
            throws IOException, InterruptedException {
        gateway.saveItem(module, item, idToken);
    }

    @Override
    public void saveItem(String module, AppDataStore.ServiceItem item) {
        gateway.saveItem(module, item);
    }

    @Override
    public void deleteItem(AppDataStore.ServiceItem item, String idToken) {
        gateway.deleteItem(item, idToken);
    }

    @Override
    public void saveRoute(AppDataStore.RouteRecord route, String idToken) throws IOException, InterruptedException {
        gateway.saveTransportRoute(route, idToken);
    }

    @Override
    public void updateRouteFlags(String routeId, boolean published, boolean active, String idToken)
            throws IOException, InterruptedException {
        gateway.updateRouteFlags(routeId, published, active, idToken);
    }

    @Override
    public void updateOperationalItemFlags(String module, String documentId, boolean published, boolean active,
            String idToken) throws IOException, InterruptedException {
        gateway.updateOperationalItemFlags(module, documentId, published, active, idToken);
    }

    @Override
    public void updateLostFoundStatus(String caseId, String status, String note, String idToken)
            throws IOException, InterruptedException {
        gateway.updateLostFoundStatus(caseId, status, note, idToken);
    }
}
