package com.simhastha.gateway.firebase;

/**
 * Canonical top-level Firestore collection ids.
 *
 * <p>Authentication profiles for every role, including administrators, live
 * in {@value #USERS}. Role-specific records belong in their own collections;
 * they must not be used as a second account-profile source.</p>
 */
public final class FirestoreCollections {

    public static final String USERS = "users";
    public static final String BUSINESSES = "businesses";
    public static final String TRANSPORT_OPERATORS = "transportOperators";
    public static final String APPROVAL_REQUESTS = "approvalRequests";
    public static final String BOOKINGS = "bookings";
    public static final String TICKETS = "tickets";
    public static final String PUJA_PROVIDERS = "pujaProviders";
    public static final String PUJA_SERVICES = "pujaServices";
    public static final String PUJA_BOOKINGS = "pujaBookings";
    public static final String FRAUD_REPORTS = "fraudReports";
    public static final String LOST_FOUND_REPORTS = "lostFoundReports";
    public static final String TRANSPORT_ROUTES = "transportRoutes";

    private FirestoreCollections() {
    }
}
