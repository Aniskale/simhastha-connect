package com.simhastha.dao;

import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.List;

public interface OperationalDataDao {
    List<AppDataStore.ServiceItem> loadItems(String idToken) throws IOException, InterruptedException;

    List<AppDataStore.ServiceItem> loadPublicModuleItems(String idToken) throws IOException, InterruptedException;

    List<AppDataStore.ServiceItem> loadAdminOperationalItems(String idToken);

    List<AppDataStore.RouteRecord> loadTransportRoutes(String idToken) throws IOException, InterruptedException;

    List<AppDataStore.LostFoundCaseRecord> loadLostFoundCases(String idToken) throws IOException, InterruptedException;

    AppDataStore.AdminOverview loadAdminOverview(String idToken) throws IOException, InterruptedException;

    void saveOperationalItem(String module, AppDataStore.ServiceItem item, String idToken)
            throws IOException, InterruptedException;

    void saveItem(String module, AppDataStore.ServiceItem item, String idToken)
            throws IOException, InterruptedException;

    void saveItem(String module, AppDataStore.ServiceItem item);

    void deleteItem(AppDataStore.ServiceItem item, String idToken);

    void saveRoute(AppDataStore.RouteRecord route, String idToken) throws IOException, InterruptedException;

    void updateRouteFlags(String routeId, boolean published, boolean active, String idToken)
            throws IOException, InterruptedException;

    void updateOperationalItemFlags(String module, String documentId, boolean published, boolean active, String idToken)
            throws IOException, InterruptedException;

    void updateLostFoundStatus(String caseId, String status, String note, String idToken)
            throws IOException, InterruptedException;
}
