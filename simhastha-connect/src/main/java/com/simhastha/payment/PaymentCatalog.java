package com.simhastha.payment;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PaymentCatalog {

    // Temporary developer fixture. Remove after module payment integration is verified.
    public static final String DEV_RAZORPAY_TEST_ITEM_ID = "dev-razorpay-test";
    public static final String META_CATALOG_ITEM_ID = "catalogItemId";
    public static final String META_QUANTITY = "quantity";
    public static final String META_NIGHTS = "nights";
    public static final String META_BOOKING_DATE = "bookingDate";
    public static final String META_LOCATION = "location";

    private static final Map<String, CatalogItem> ITEMS = createItems();

    private PaymentCatalog() {
    }

    public static Optional<CatalogItem> find(String id) {
        return Optional.ofNullable(ITEMS.get(id));
    }

    public static Map<String, CatalogItem> items() {
        return Map.copyOf(ITEMS);
    }

    public static BigDecimal expectedAmount(PaymentRequest request) throws PaymentException {
        String catalogItemId = request.metadata().get(META_CATALOG_ITEM_ID);
        CatalogItem item = find(catalogItemId)
                .orElseThrow(() -> new PaymentException("Payment item is not approved for online payment."));
        if (item.moduleType() != request.moduleType()) {
            throw new PaymentException("Payment item does not match the selected module.");
        }
        if (!item.paymentRequired()) {
            throw new PaymentException("This booking does not require online payment.");
        }
        int quantity = intMetadata(request, META_QUANTITY, 1);
        int nights = item.multipliesByNights() ? intMetadata(request, META_NIGHTS, 1) : 1;
        return item.amount().multiply(BigDecimal.valueOf(quantity)).multiply(BigDecimal.valueOf(nights));
    }

    private static int intMetadata(PaymentRequest request, String key, int fallback) throws PaymentException {
        String value = request.metadata().get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new PaymentException("Invalid booking quantity.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new PaymentException("Invalid booking quantity.", exception);
        }
    }

    private static Map<String, CatalogItem> createItems() {
        Map<String, CatalogItem> items = new LinkedHashMap<>();
        // TEMPORARY RAZORPAY API TEST
        // Remove after module payment integration is verified.
        add(items, new CatalogItem(DEV_RAZORPAY_TEST_ITEM_ID, PaymentModuleType.BUSINESS,
                "Razorpay Test Payment", "Developer-only Test Mode payment request", new BigDecimal("10.00"),
                "INR", "dev-test", "Developer Test", true, PaymentType.FULL, false));
        add(items, new CatalogItem("package-divine-nashik", PaymentModuleType.PACKAGE, "Divine Nashik Package",
                "Ramkund, Trimbakeshwar and guided darshan support", new BigDecimal("1500.00"), "INR", "",
                "Ramkund / Trimbakeshwar", true, PaymentType.FULL, false));
        add(items, new CatalogItem("package-family-seva", PaymentModuleType.PACKAGE, "Family Seva Package",
                "Family assistance, route help and puja desk coordination", new BigDecimal("2400.00"), "INR", "",
                "Panchavati", true, PaymentType.FULL, false));
        add(items, new CatalogItem("stay-dharamshala-bed", PaymentModuleType.STAY, "Dharamshala Bed Reservation",
                "Budget pilgrim bed near Panchavati", new BigDecimal("499.00"), "INR", "",
                "Panchavati", true, PaymentType.FULL, true));
        add(items, new CatalogItem("stay-family-room", PaymentModuleType.STAY, "Family Hotel Room",
                "Family room around Nashik Road / CBS", new BigDecimal("1899.00"), "INR", "",
                "Nashik Road", true, PaymentType.FULL, true));
        add(items, new CatalogItem("stay-festival-tent", PaymentModuleType.STAY, "Festival Camp Tent",
                "Temporary tent stay with basic pilgrim support", new BigDecimal("999.00"), "INR", "",
                "Festival Camp", true, PaymentType.FULL, true));
        add(items, new CatalogItem("puja-rudrabhishek", PaymentModuleType.PUJA, "Ramkund Rudrabhishek",
                "Verified pandit slot and receipt support", new BigDecimal("751.00"), "INR", "puja-provider-ramkund",
                "Ramkund", true, PaymentType.FULL, false));
        add(items, new CatalogItem("puja-trimbakeshwar-darshan", PaymentModuleType.PUJA,
                "Trimbakeshwar Darshan Support", "Verified puja counter assistance", new BigDecimal("1100.00"), "INR",
                "puja-provider-trimbakeshwar", "Trimbakeshwar", true, PaymentType.FULL, false));
        add(items, new CatalogItem("business-paid-parking", PaymentModuleType.BUSINESS, "Paid Parking Reservation",
                "Approved private parking near Ramkund approach", new BigDecimal("150.00"), "INR",
                "business-parking-ramkund", "Ramkund approach road", true, PaymentType.FULL, false));
        add(items, new CatalogItem("business-tent-advance", PaymentModuleType.BUSINESS, "Tent Booking Advance",
                "Advance for approved paid tent booking", new BigDecimal("500.00"), "INR",
                "business-tent-zone", "Festival Camp", true, PaymentType.ADVANCE, false));
        add(items, new CatalogItem("business-guide-pay-location", PaymentModuleType.BUSINESS, "Local Guide Inquiry",
                "Pay at location after service confirmation", BigDecimal.ZERO, "INR",
                "business-guide-panchavati", "Panchavati", false, PaymentType.PAY_AT_LOCATION, false));
        addBusinessPresentationItems(items);
        add(items, new CatalogItem("transport-private-cab", PaymentModuleType.TRANSPORT, "Private Cab Reservation",
                "Optional private paid cab booking", new BigDecimal("650.00"), "INR",
                "transport-private-cab", "Nashik city", true, PaymentType.FULL, false));
        return items;
    }

    private static void addBusinessPresentationItems(Map<String, CatalogItem> items) {
        // For lockers, the shared multiplier represents booked hours; for stays it represents nights.
        // The trusted catalogue controls this flag, so JavaFX cannot choose an arbitrary payment total.
        add(items, new CatalogItem("locker-small", PaymentModuleType.BUSINESS, "Small Locker", "Secure locker", new BigDecimal("80"), "INR", "presentation-locker", "Ramkund, Panchavati", true, PaymentType.FULL, true));
        add(items, new CatalogItem("locker-medium", PaymentModuleType.BUSINESS, "Medium Locker", "Secure locker", new BigDecimal("140"), "INR", "presentation-locker", "Ramkund, Panchavati", true, PaymentType.FULL, true));
        add(items, new CatalogItem("locker-large", PaymentModuleType.BUSINESS, "Large Locker", "Secure locker", new BigDecimal("220"), "INR", "presentation-locker", "Ramkund, Panchavati", true, PaymentType.FULL, true));
        add(items, new CatalogItem("tent-standard", PaymentModuleType.BUSINESS, "Standard Tent", "Tent stay", new BigDecimal("1200"), "INR", "presentation-tent", "Tapovan Riverside", true, PaymentType.FULL, true));
        add(items, new CatalogItem("tent-family", PaymentModuleType.BUSINESS, "Family Tent", "Tent stay", new BigDecimal("2200"), "INR", "presentation-tent", "Tapovan Riverside", true, PaymentType.FULL, true));
        add(items, new CatalogItem("tent-premium", PaymentModuleType.BUSINESS, "Premium Tent", "Tent stay", new BigDecimal("3200"), "INR", "presentation-tent", "Tapovan Riverside", true, PaymentType.FULL, true));
        add(items, new CatalogItem("dharamshala-shared", PaymentModuleType.BUSINESS, "Shared Bed", "Dharamshala stay", new BigDecimal("450"), "INR", "presentation-dharamshala", "Panchavati", true, PaymentType.FULL, true));
        add(items, new CatalogItem("dharamshala-standard", PaymentModuleType.BUSINESS, "Standard Room", "Dharamshala stay", new BigDecimal("1100"), "INR", "presentation-dharamshala", "Panchavati", true, PaymentType.FULL, true));
        add(items, new CatalogItem("dharamshala-family", PaymentModuleType.BUSINESS, "Family Room", "Dharamshala stay", new BigDecimal("1800"), "INR", "presentation-dharamshala", "Panchavati", true, PaymentType.FULL, true));
        add(items, new CatalogItem("hotel-standard", PaymentModuleType.BUSINESS, "Standard Room", "Hotel stay", new BigDecimal("2500"), "INR", "presentation-hotel", "Godavari Ghat, Nashik", true, PaymentType.FULL, true));
        add(items, new CatalogItem("hotel-deluxe", PaymentModuleType.BUSINESS, "Deluxe Room", "Hotel stay", new BigDecimal("3600"), "INR", "presentation-hotel", "Godavari Ghat, Nashik", true, PaymentType.FULL, true));
        add(items, new CatalogItem("hotel-family", PaymentModuleType.BUSINESS, "Family Room", "Hotel stay", new BigDecimal("4800"), "INR", "presentation-hotel", "Godavari Ghat, Nashik", true, PaymentType.FULL, true));
    }

    private static void add(Map<String, CatalogItem> items, CatalogItem item) {
        items.put(item.id(), item);
    }

    public record CatalogItem(String id, PaymentModuleType moduleType, String title, String description,
            BigDecimal amount, String currency, String businessId, String location, boolean paymentRequired,
            PaymentType paymentType, boolean multipliesByNights) {
    }
}
