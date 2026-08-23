package com.simhastha.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AppDataStore {

    private static final List<ServiceItem> transport = new ArrayList<>();
    private static final List<ServiceItem> puja = new ArrayList<>();
    private static final List<ServiceItem> ghats = new ArrayList<>();
    private static final List<ServiceItem> emergency = new ArrayList<>();
    private static final List<ServiceItem> stay = new ArrayList<>();
    private static final List<ServiceItem> lostFound = new ArrayList<>();
    private static final List<ServiceItem> schedule = new ArrayList<>();
    private static final List<ServiceItem> business = new ArrayList<>();
    private static final List<ServiceItem> announcements = new ArrayList<>();
    private static final List<ServiceItem> about = new ArrayList<>();
    private static final List<ApprovalRequest> pendingApprovals = new ArrayList<>();
    private static final List<BookingRecord> bookings = new ArrayList<>();
    private static final List<TicketRecord> tickets = new ArrayList<>();
    private static final FirestoreGateway firestore = new FirestoreGateway(FirebaseConfig.load());

    static {
        transport.add(new ServiceItem("Nashik Road Railway Station to Ramkund Shuttle",
                "Every 20 minutes | 05:00 AM - 11:00 PM | Rs 30 expected fare", "Transport"));
        transport.add(new ServiceItem("CBS Nashik to Trimbakeshwar Bus",
                "Hourly service | 06:00 AM - 09:00 PM | Extra buses on snan days", "Transport"));
        transport.add(new ServiceItem("Tapovan Parking to Ghat E-Rickshaw",
                "Short route connector | Crowd-friendly drop zones", "Transport"));

        puja.add(new ServiceItem("Ramkund Rudrabhishek Help Desk",
                "Pandit booking, receipt guidance and puja slot assistance", "Puja"));
        puja.add(new ServiceItem("Trimbakeshwar Darshan Queue Support",
                "Temple direction, puja counter guidance and elderly assistance", "Puja"));
        puja.add(new ServiceItem("Pind Daan Information Counter",
                "Ritual requirements, timing windows and verified contact support", "Puja"));

        ghats.add(new ServiceItem("Ramkund Ghat", "Primary Nashik snan ghat | High crowd zone", "Ghat"));
        ghats.add(new ServiceItem("Kushavarta Kund, Trimbakeshwar",
                "Important Trimbakeshwar snan location | Shaiva Akhara focus", "Ghat"));
        ghats.add(new ServiceItem("Godavari Ghat Safety Line",
                "Use marked bathing points and follow police barricade instructions", "Ghat"));

        emergency.add(new ServiceItem("Unified Emergency Response", "112 | Police, fire and medical help", "Emergency"));
        emergency.add(new ServiceItem("Ambulance", "108 | Medical emergency support", "Emergency"));
        emergency.add(new ServiceItem("Fire", "101 | Fire and rescue support", "Emergency"));
        emergency.add(new ServiceItem("Police Control Room", "100 / 112 | Nashik city emergency control", "Emergency"));
        emergency.add(new ServiceItem("Women Helpline", "1091 | Women safety support", "Emergency"));
        emergency.add(new ServiceItem("Child Helpline", "1098 | Child safety support", "Emergency"));
        emergency.add(new ServiceItem("Cyber Crime", "1930 | Cyber fraud helpline", "Emergency"));
        emergency.add(new ServiceItem("District Disaster Management Nashik", "1077 | Disaster control support", "Emergency"));

        stay.add(new ServiceItem("Dharamshala Availability Desk",
                "Budget stay guidance near Panchavati and Trimbakeshwar", "Stay"));
        stay.add(new ServiceItem("Family Hotel Zone",
                "Nashik Road, CBS and Gangapur Road suggested clusters", "Stay"));
        stay.add(new ServiceItem("Festival Camp Stay",
                "Temporary tent/camp listings will appear after admin approval", "Stay"));

        lostFound.add(new ServiceItem("Lost Person Help Desk",
                "Report with name, age, clothing, last seen place and contact number", "Lost & Found"));
        lostFound.add(new ServiceItem("Found Item Counter",
                "Submit item type, location found and photo at verified counter", "Lost & Found"));
        lostFound.add(new ServiceItem("Public Announcement Link",
                "Admin can publish missing/found alerts from control dashboard", "Lost & Found"));

        schedule.add(new ServiceItem("04:00 - 05:00", "Ghat cleaning, safety barricade check, first medical patrol", "Schedule"));
        schedule.add(new ServiceItem("05:00 - 08:00", "Morning snan guidance, temple darshan route opens, shuttle frequency high", "Schedule"));
        schedule.add(new ServiceItem("08:00 - 11:00", "Breakfast/food counters, transport queue monitoring, crowd updates", "Schedule"));
        schedule.add(new ServiceItem("11:00 - 14:00", "Puja slots, information desk support, heat and water alerts", "Schedule"));
        schedule.add(new ServiceItem("14:00 - 17:00", "Stay check-in support, lost/found reconciliation, route diversions if needed", "Schedule"));
        schedule.add(new ServiceItem("17:00 - 20:00", "Evening aarti movement, return shuttle load management", "Schedule"));
        schedule.add(new ServiceItem("20:00 - 24:00", "Night transport, emergency patrol, crowd dispersal support", "Schedule"));
        schedule.add(new ServiceItem("00:00 - 04:00", "Night control room, sanitation reset and next-day route preparation", "Schedule"));

        business.add(new ServiceItem("Verified Food & Prasadam Zone",
                "Approved food counters, water points and nearby snack services", "Business"));
        business.add(new ServiceItem("Approved Local Services",
                "Guides, lockers, charging, medical stores and pilgrim support shops", "Business"));

        announcements.add(new ServiceItem("Official Data Notice",
                "Live transport, emergency and crowd updates can be controlled from Admin Dashboard.", "Announcement"));
        announcements.add(new ServiceItem("Simhastha 2027 Planning",
                "NTKMA is the nodal authority for Nashik-Trimbakeshwar Kumbh Mela planning.", "Announcement"));

        about.add(new ServiceItem("Connected Pilgrim Platform",
                "One desktop app for pilgrims, admin, transport operators and business partners.", "About"));
        about.add(new ServiceItem("Admin-controlled Data",
                "Every user-facing module can be updated from Admin Dashboard.", "About"));
        about.add(new ServiceItem("Approval-first Marketplace",
                "Business and transport entries become public only after admin approval.", "About"));
        loadFirebaseDataIfAvailable();
    }

    private AppDataStore() {
    }

    public static List<ServiceItem> items(String module) {
        return switch (module) {
            case "transport" -> transport;
            case "puja" -> puja;
            case "ghat" -> ghats;
            case "emergency" -> emergency;
            case "stay" -> stay;
            case "lost" -> lostFound;
            case "schedule" -> schedule;
            case "business" -> business;
            case "announcement" -> announcements;
            case "about" -> about;
            default -> announcements;
        };
    }

    public static List<ApprovalRequest> pendingApprovals() {
        return pendingApprovals;
    }

    public static List<BookingRecord> bookings() {
        return bookings;
    }

    public static List<BookingRecord> bookingsForUser(String userId) {
        return bookings.stream()
                .filter(booking -> booking.userId.equals(userId))
                .toList();
    }

    public static List<BookingRecord> bookingsForBusiness(String businessId) {
        return bookings.stream()
                .filter(booking -> booking.businessId.equals(businessId))
                .toList();
    }

    public static BookingRecord addBooking(BookingRecord booking) {
        bookings.add(booking);
        return booking;
    }

    public static void updateBookingStatus(String bookingId, String bookingStatus, String paymentStatus) {
        for (BookingRecord booking : bookings) {
            if (booking.bookingId.equals(bookingId)) {
                booking.bookingStatus = bookingStatus;
                booking.paymentStatus = paymentStatus;
                booking.updatedAt = String.valueOf(System.currentTimeMillis());
                return;
            }
        }
    }

    public static TicketRecord addTicket(TicketRecord ticket) {
        tickets.removeIf(existing -> existing.bookingId.equals(ticket.bookingId));
        tickets.add(ticket);
        return ticket;
    }

    public static List<TicketRecord> ticketsForUser(String userId) {
        return tickets.stream()
                .filter(ticket -> ticket.userId.equals(userId))
                .toList();
    }

    public static TicketRecord ticketForBooking(String bookingId) {
        return tickets.stream()
                .filter(ticket -> ticket.bookingId.equals(bookingId))
                .findFirst()
                .orElse(null);
    }

    public static void addItem(String module, String title, String detail) {
        ServiceItem item = new ServiceItem(module, title, detail, displayName(module));
        items(module).add(item);
        try {
            firestore.saveItem(module, item, currentToken());
        } catch (Exception ignored) {
            firestore.saveItem(module, item);
        }
    }

    public static void removeItem(String module, ServiceItem item) {
        items(module).remove(item);
        firestore.deleteItem(item, currentToken());
    }

    public static void requestApproval(String type, String title, String detail, String targetModule) {
        ApprovalRequest request = new ApprovalRequest(type, title, detail, targetModule);
        pendingApprovals.add(request);
        try {
            firestore.saveApproval(request, currentToken());
        } catch (Exception ignored) {
            firestore.saveApproval(request);
        }
    }

    public static void approve(ApprovalRequest request) throws ApprovalUpdateException {
        requireOwnerId(request);
        try {
            firestore.updateUserStatus(request.ownerId, "approved", currentToken());
            if ("business".equals(request.targetModule)) {
                firestore.updateDocumentStatus("businesses", request.ownerId, "approved", true, currentToken());
            } else if ("transport".equals(request.targetModule)) {
                firestore.updateDocumentStatus("transportOperators", request.ownerId, "approved", true, currentToken());
            }
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Approval could not be saved to Firestore. The request is still pending.", exception);
        }

        addItem(request.targetModule, request.title, request.detail);
        pendingApprovals.remove(request);
        firestore.deleteApproval(request, currentToken());
    }

    public static void reject(ApprovalRequest request) throws ApprovalUpdateException {
        requireOwnerId(request);
        try {
            firestore.updateUserStatus(request.ownerId, "rejected", currentToken());
            if ("business".equals(request.targetModule)) {
                firestore.updateDocumentStatus("businesses", request.ownerId, "rejected", false, currentToken());
            } else if ("transport".equals(request.targetModule)) {
                firestore.updateDocumentStatus("transportOperators", request.ownerId, "rejected", false, currentToken());
            }
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Rejection could not be saved to Firestore. The request is still pending.", exception);
        }

        pendingApprovals.remove(request);
        firestore.deleteApproval(request, currentToken());
    }

    private static void requireOwnerId(ApprovalRequest request) throws ApprovalUpdateException {
        if (request.ownerId == null || request.ownerId.isBlank()) {
            throw new ApprovalUpdateException("Approval request is missing the Firebase UID. The request is still pending.");
        }
    }

    public static boolean isFirebaseEnabled() {
        return firestore.isEnabled();
    }

    public static void refreshFirebaseData(String idToken) {
        loadFirebaseDataIfAvailable(idToken);
    }

    public static String displayName(String module) {
        return switch (module) {
            case "transport" -> "Transport";
            case "puja" -> "Puja Services";
            case "ghat" -> "Ghats & Snan";
            case "emergency" -> "Emergency";
            case "stay" -> "Stay";
            case "lost" -> "Lost & Found";
            case "schedule" -> "All Day Schedule";
            case "business" -> "Business";
            case "announcement" -> "Announcement";
            case "about" -> "About Us";
            default -> "Dashboard";
        };
    }

    public static final class ServiceItem {
        public final String id;
        public final String module;
        public final String title;
        public final String detail;
        public final String category;

        public ServiceItem(String title, String detail, String category) {
            this(slug(category + "-" + title), moduleKey(category), title, detail, category);
        }

        public ServiceItem(String module, String title, String detail, String category) {
            this(randomId(module), module, title, detail, category);
        }

        public ServiceItem(String id, String module, String title, String detail, String category) {
            this.id = id;
            this.module = module;
            this.title = title;
            this.detail = detail;
            this.category = category;
        }
    }

    public static final class ApprovalRequest {
        public final String id;
        public final String type;
        public final String title;
        public final String detail;
        public final String targetModule;
        public final String ownerId;

        public ApprovalRequest(String type, String title, String detail, String targetModule) {
            this(randomId("approval"), type, title, detail, targetModule, "");
        }

        

        public ApprovalRequest(String type, String title, String detail, String targetModule, String ownerId) {
            this(randomId("approval"), type, title, detail, targetModule, ownerId);
        }

        public ApprovalRequest(String id, String type, String title, String detail, String targetModule, String ownerId) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.detail = detail;
            this.targetModule = targetModule;
            this.ownerId = ownerId == null ? "" : ownerId.trim();
        }
    }

    public static final class ApprovalUpdateException extends Exception {
        public ApprovalUpdateException(String message) {
            super(message);
        }

        public ApprovalUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class BookingRecord {
        public final String bookingId;
        public final String userId;
        public final String moduleType;
        public final String catalogItemId;
        public final String businessId;
        public final String title;
        public final String customerName;
        public final String dateText;
        public final String location;
        public final int quantity;
        public final int nights;
        public final long amountPaise;
        public final String currency;
        public String bookingStatus;
        public String paymentStatus;
        public final String internalPaymentId;
        public final String razorpayPaymentId;
        public final String createdAt;
        public String updatedAt;

        public BookingRecord(String bookingId, String userId, String moduleType, String catalogItemId,
                String businessId, String title, String customerName, String dateText, String location, int quantity,
                int nights, long amountPaise, String currency, String bookingStatus, String paymentStatus,
                String internalPaymentId, String razorpayPaymentId) {
            this.bookingId = bookingId;
            this.userId = userId;
            this.moduleType = moduleType;
            this.catalogItemId = catalogItemId;
            this.businessId = businessId == null ? "" : businessId;
            this.title = title;
            this.customerName = customerName;
            this.dateText = dateText;
            this.location = location;
            this.quantity = quantity;
            this.nights = nights;
            this.amountPaise = amountPaise;
            this.currency = currency;
            this.bookingStatus = bookingStatus;
            this.paymentStatus = paymentStatus;
            this.internalPaymentId = internalPaymentId == null ? "" : internalPaymentId;
            this.razorpayPaymentId = razorpayPaymentId == null ? "" : razorpayPaymentId;
            this.createdAt = String.valueOf(System.currentTimeMillis());
            this.updatedAt = this.createdAt;
        }
    }

    public static final class TicketRecord {
        public final String ticketId;
        public final String bookingId;
        public final String paymentId;
        public final String userId;
        public final String moduleType;
        public final String businessId;
        public final String title;
        public final String customerName;
        public final String dateText;
        public final String location;
        public final long amountPaise;
        public final String status;
        public final String qrVerificationReference;
        public final String issuedAt;

        public TicketRecord(String ticketId, String bookingId, String paymentId, String userId, String moduleType,
                String businessId, String title, String customerName, String dateText, String location,
                long amountPaise, String status, String qrVerificationReference) {
            this.ticketId = ticketId;
            this.bookingId = bookingId;
            this.paymentId = paymentId == null ? "" : paymentId;
            this.userId = userId;
            this.moduleType = moduleType;
            this.businessId = businessId == null ? "" : businessId;
            this.title = title;
            this.customerName = customerName;
            this.dateText = dateText;
            this.location = location;
            this.amountPaise = amountPaise;
            this.status = status;
            this.qrVerificationReference = qrVerificationReference;
            this.issuedAt = String.valueOf(System.currentTimeMillis());
        }
    }

    private static void loadFirebaseDataIfAvailable() {
        loadFirebaseDataIfAvailable("");
    }

    private static void loadFirebaseDataIfAvailable(String idToken) {
        if (!firestore.isEnabled()) {
            return;
        }
        try {
            List<ServiceItem> remoteItems = firestore.loadItems(idToken);
            if (!remoteItems.isEmpty()) {
                clearModuleItems();
                for (ServiceItem item : remoteItems) {
                    items(item.module).add(item);
                }
            }

            pendingApprovals.clear();
            addUniqueApprovals(firestore.loadPendingBusinessApprovals(idToken), true);
            addUniqueApprovals(firestore.loadApprovals(idToken), false);
        } catch (Exception ignored) {
            // Keep local seed data if Firebase is offline or rules are not ready yet.
        }
    }

    private static void addUniqueApprovals(List<ApprovalRequest> approvals, boolean includeBusinessApprovals) {
        for (ApprovalRequest approval : approvals) {
            if (!includeBusinessApprovals && "business".equals(approval.targetModule)) {
                continue;
            }
            if (approval.ownerId.isBlank()) {
                continue;
            }
            boolean exists = pendingApprovals.stream()
                    .anyMatch(existing -> approval.ownerId.equals(existing.ownerId)
                            && approval.targetModule.equals(existing.targetModule));
            if (!exists) {
                pendingApprovals.add(approval);
            }
        }
    }

    private static void clearModuleItems() {
        transport.clear();
        puja.clear();
        ghats.clear();
        emergency.clear();
        stay.clear();
        lostFound.clear();
        schedule.clear();
        business.clear();
        announcements.clear();
        about.clear();
    }

    private static String randomId(String prefix) {
        return slug(prefix) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String slug(String value) {
        String normalized = value == null ? "item" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isEmpty() ? "item" : normalized;
    }

    private static String moduleKey(String category) {
        return switch (category) {
            case "Transport" -> "transport";
            case "Puja" -> "puja";
            case "Ghat" -> "ghat";
            case "Emergency" -> "emergency";
            case "Stay" -> "stay";
            case "Lost & Found" -> "lost";
            case "Schedule" -> "schedule";
            case "Business" -> "business";
            case "Announcement" -> "announcement";
            case "About" -> "about";
            default -> "announcement";
        };
    }

    private static String currentToken() {
        AppSession.User user = AppSession.currentUser();
        return user == null ? "" : user.idToken();
    }
}
