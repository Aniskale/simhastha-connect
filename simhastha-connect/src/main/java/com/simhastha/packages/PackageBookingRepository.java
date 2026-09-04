package com.simhastha.packages;

import com.simhastha.view.FirebaseConfig;
import com.simhastha.view.FirestoreGateway;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/** Firestore-backed repository for Kumbh package confirmations only. */
public final class PackageBookingRepository {
    private static final Logger LOGGER = Logger.getLogger(PackageBookingRepository.class.getName());
    private final FirestoreGateway firestore = new FirestoreGateway(FirebaseConfig.load());

    public CompletableFuture<PackageBooking> save(PackageBooking booking, String idToken) {
        boolean tokenPresent = idToken != null && !idToken.isBlank();
        LOGGER.info(() -> "Kumbh package booking persistence requested: bookingId=" + booking.bookingId()
                + ", ownerUid=" + booking.userId() + ", packageId=" + booking.packageId()
                + ", path=package_bookings/" + booking.bookingId() + ", operation=CREATE, authTokenPresent="
                + tokenPresent);
        if (!firestore.isEnabled()) {
            LOGGER.warning(() -> "Kumbh package booking Firestore CREATE skipped: bookingId=" + booking.bookingId()
                    + ", ownerUid=" + booking.userId() + ", packageId=" + booking.packageId()
                    + ", path=package_bookings/" + booking.bookingId()
                    + ", operation=CREATE, firestoreConfigured=false");
            return CompletableFuture.failedFuture(new IllegalStateException("Firestore is not configured."));
        }
        if (idToken == null || idToken.isBlank()) {
            LOGGER.warning(() -> "Kumbh package booking Firestore CREATE skipped: bookingId=" + booking.bookingId()
                    + ", ownerUid=" + booking.userId() + ", packageId=" + booking.packageId()
                    + ", path=package_bookings/" + booking.bookingId() + ", operation=CREATE, authTokenPresent=false");
            return CompletableFuture.failedFuture(new IllegalStateException("Firebase authentication token is unavailable."));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                firestore.savePackageBooking(booking, idToken);
                return booking;
            } catch (Exception exception) {
                throw new IllegalStateException("Package booking persistence failed.", exception);
            }
        });
    }

    public CompletableFuture<List<PackageBooking>> findByUser(String userId, String idToken) {
        if (!firestore.isEnabled() || userId == null || userId.isBlank()) return CompletableFuture.completedFuture(List.of());
        return CompletableFuture.supplyAsync(() -> {
            try {
                return firestore.loadPackageBookingsForUser(userId, idToken);
            } catch (Exception exception) {
                throw new IllegalStateException("Package booking loading failed.", exception);
            }
        });
    }
}
