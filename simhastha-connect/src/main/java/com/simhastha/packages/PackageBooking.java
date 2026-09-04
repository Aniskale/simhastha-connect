package com.simhastha.packages;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Immutable persisted confirmation snapshot for a Kumbh package booking. */
public record PackageBooking(
        String bookingId, String userId, String packageId, String packageName, String route, String duration,
        PrimaryContact primaryContact, List<BookingTraveller> travellers, Map<String, String> selections,
        Map<String, Integer> componentPrices, int baseAmount, int finalAmount, String currency,
        String paymentMode, String paymentStatus, String bookingStatus, String demoReference, String createdAt) {

    public static PackageBooking confirmedTest(String userId, String packageName, String route, String duration,
            PackageBookingDraft draft) {
        PackageCustomizationSnapshot snapshot = draft.customizationSnapshot();
        List<BookingTraveller> safeTravellers = draft.travellers().stream()
                .map(traveller -> new BookingTraveller(traveller.fullName(), traveller.age(), traveller.gender(),
                        traveller.idType(), maskId(traveller.idType(), traveller.idNumber())))
                .toList();
        String id = "SC-KP-" + Year.now().getValue() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        int baseAmount = snapshot.componentPrices().getOrDefault("Base Package", draft.totalAmount());
        return new PackageBooking(id, userId, draft.packageId(), packageName, route, duration, draft.primaryContact(),
                safeTravellers, Map.copyOf(snapshot.selections()), Map.copyOf(snapshot.componentPrices()), baseAmount,
                draft.totalAmount(), snapshot.currency(), "TEST", "PAYMENT_NOT_VERIFIED", "CONFIRMED_TEST", id,
                Instant.now().toString());
    }

    private static String maskId(String idType, String id) {
        String value = id == null ? "" : id.trim();
        if (value.length() <= 4) return "XXXX";
        String suffix = value.substring(value.length() - 4);
        return "Aadhaar".equalsIgnoreCase(idType) ? "XXXX-XXXX-" + suffix : "XXXX-" + suffix;
    }

    /** This intentionally stores only an ID mask, never the raw traveller ID. */
    public record BookingTraveller(String fullName, int age, String gender, String idType, String maskedId) { }
}
