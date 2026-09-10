package com.simhastha.view;

import com.simhastha.dao.ApprovalDao;
import com.simhastha.dao.BookingDao;
import com.simhastha.dao.BusinessDao;
import com.simhastha.dao.OperationalDataDao;
import com.simhastha.dao.OperatorDao;
import com.simhastha.dao.UserDao;
import com.simhastha.dao.implementation.FirestoreApprovalDao;
import com.simhastha.dao.implementation.FirestoreBookingDao;
import com.simhastha.dao.implementation.FirestoreBusinessDao;
import com.simhastha.dao.implementation.FirestoreOperationalDataDao;
import com.simhastha.dao.implementation.FirestoreOperatorDao;
import com.simhastha.dao.implementation.FirestoreUserDao;
import com.simhastha.gateway.firebase.FirebaseConfig;
import com.simhastha.model.BusinessMedia;
import com.simhastha.model.CloudImage;
import com.simhastha.model.PublicBusinessItem;
import com.simhastha.util.AppSession;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AppDataStore {

    private static final List<ServiceItem> transport = new ArrayList<>();
    private static final List<ServiceItem> packages = new ArrayList<>();
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
    private static final List<UserRecord> users = new ArrayList<>();
    private static final List<BusinessRecord> businesses = new ArrayList<>();
    private static final Map<String, List<BusinessMedia>> businessMedia = new LinkedHashMap<>();
    private static final Map<String, String> businessItemPhotos = new LinkedHashMap<>();
    private static final Map<String, List<PublicBusinessItem>> businessItems = new LinkedHashMap<>();
    private static final List<TransportOperatorRecord> transportOperators = new ArrayList<>();
    private static final List<PujaServiceRecord> pujaServices = new ArrayList<>();
    private static final List<PujaProviderRecord> pujaProviders = new ArrayList<>();
    private static final List<PujaBookingRecord> pujaBookings = new ArrayList<>();
    private static final List<FraudReportRecord> fraudReports = new ArrayList<>();
    private static final List<LostFoundCaseRecord> lostFoundCases = new ArrayList<>();
    private static final List<RouteRecord> transportRoutes = new ArrayList<>();
    private static final List<FaqRecord> faqs = new ArrayList<>();
    private static final Set<String> remoteModules = new HashSet<>();
    // Puja-specific data remains on the legacy adapter until it is migrated.
    // Accounts, Admin user lists, business approvals and generic bookings use
    // the canonical gateway below, so every role resolves its profile from users/{uid}.
    private static final FirestoreGateway firestore = new FirestoreGateway(FirebaseConfig.load());
    private static final com.simhastha.gateway.firebase.FirestoreGateway canonicalFirestore =
            new com.simhastha.gateway.firebase.FirestoreGateway(FirebaseConfig.load());
    private static final UserDao userDao = new FirestoreUserDao(canonicalFirestore);
    private static final BusinessDao businessDao = new FirestoreBusinessDao(canonicalFirestore);
    private static final OperatorDao operatorDao = new FirestoreOperatorDao(canonicalFirestore);
    private static final ApprovalDao approvalDao = new FirestoreApprovalDao(canonicalFirestore);
    private static final BookingDao bookingDao = new FirestoreBookingDao(canonicalFirestore);
    private static final OperationalDataDao operationalDataDao = new FirestoreOperationalDataDao(canonicalFirestore);
    private static AdminOverview adminOverview = AdminOverview.empty();

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
        seedFaqs();
        // Firebase data must not be fetched while the unauthenticated login UI is starting.
        // Role-scoped refreshes begin only after a profile-backed session has been established.
    }

    private AppDataStore() {
    }

    public static List<ServiceItem> items(String module) {
        return switch (module) {
            case "transport" -> transport;
            case "packages" -> packages;
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

    public static List<UserRecord> users() {
        return users;
    }

    public static List<BusinessRecord> businesses() {
        return businesses;
    }

    public static void rememberBusinessMedia(String businessId, List<BusinessMedia> media) {
        if (businessId == null || businessId.isBlank()) return;
        businessMedia.put(businessId, media == null ? List.of() : List.copyOf(media));
    }

    public static List<BusinessMedia> businessMediaFor(String businessId) {
        if (businessId == null || businessId.isBlank()) return List.of();
        return businessMedia.getOrDefault(businessId, List.of());
    }

    public static void rememberBusinessItemPhoto(String itemId, String photoUrl) {
        if (itemId == null || itemId.isBlank()) return;
        if (photoUrl == null || photoUrl.isBlank()) businessItemPhotos.remove(itemId);
        else businessItemPhotos.put(itemId, photoUrl);
    }

    public static String businessItemPhotoFor(String itemId) {
        if (itemId == null || itemId.isBlank()) return "";
        return businessItemPhotos.getOrDefault(itemId, "");
    }

    public static void rememberBusinessItem(PublicBusinessItem item) {
        if (item == null || item.businessId() == null || item.businessId().isBlank()
                || item.itemId() == null || item.itemId().isBlank()) return;
        List<PublicBusinessItem> current = new ArrayList<>(businessItems.getOrDefault(item.businessId(), List.of()));
        current.removeIf(existing -> item.itemId().equals(existing.itemId()));
        current.add(item);
        businessItems.put(item.businessId(), List.copyOf(current));
    }

    public static List<PublicBusinessItem> businessItemsFor(String businessId) {
        if (businessId == null || businessId.isBlank()) return List.of();
        return businessItems.getOrDefault(businessId, List.of());
    }

    public static void removeBusinessItem(String businessId, String itemId) {
        if (businessId == null || businessId.isBlank() || itemId == null || itemId.isBlank()) return;
        List<PublicBusinessItem> current = new ArrayList<>(businessItems.getOrDefault(businessId, List.of()));
        current.removeIf(item -> itemId.equals(item.itemId()));
        businessItems.put(businessId, List.copyOf(current));
    }

    public static void rememberPublicBusiness(com.simhastha.model.PublicBusinessListing listing) {
        if (listing == null || listing.businessId() == null || listing.businessId().isBlank()) return;
        businesses.removeIf(existing -> listing.businessId().equals(existing.businessId));
        String logoUrl = listing.media().stream().filter(media -> "logo".equalsIgnoreCase(media.type()))
                .map(BusinessMedia::url).findFirst().orElse("");
        String logoPublicId = listing.media().stream().filter(media -> "logo".equalsIgnoreCase(media.type()))
                .map(BusinessMedia::publicId).findFirst().orElse("");
        String coverPhotoUrl = listing.coverPhotoUrl();
        String coverPhotoPublicId = listing.media().stream().filter(BusinessMedia::cover)
                .map(BusinessMedia::publicId).findFirst().orElse("");
        List<CloudImage> galleryImages = listing.media().stream()
                .filter(media -> "gallery".equalsIgnoreCase(media.type()))
                .map(media -> new CloudImage(media.url(), media.publicId()))
                .toList();
        businesses.add(new BusinessRecord(listing.businessId(), listing.ownerId(), listing.name(), "", listing.category(),
                listing.description(), listing.location(), listing.address(), listing.area(), listing.city(),
                listing.latitude(), listing.longitude(), listing.locationUpdatedAt(), listing.mobile(), listing.email(),
                listing.operatingHours(), listing.priceRange(), "active", true, logoUrl, logoPublicId, coverPhotoUrl,
                coverPhotoPublicId, galleryImages, "", ""));
        rememberBusinessMedia(listing.businessId(), listing.media());
        for (PublicBusinessItem item : listing.items()) rememberBusinessItem(item);
    }

    public static List<FaqRecord> faqs() {
        return faqs;
    }

    private static void seedFaqs() {
        mergeFaqs(List.of(
                new FaqRecord("faq-general-help", "General", "How do I get help during Simhastha?",
                        "Use the Emergency section for urgent support and the Help Center for common service questions.", true, "1"),
                new FaqRecord("faq-booking-payment", "Bookings", "Where can I see my bookings?",
                        "User bookings are available from the relevant module after sign-in.", true, "2"),
                new FaqRecord("faq-business-listing", "Business", "How does a business listing become public?",
                        "An admin must approve the business and its active inventory before it appears in the marketplace.", true, "3")));
    }

    private static void mergeFaqs(List<FaqRecord> records) {
        if (records == null) return;
        for (FaqRecord record : records) {
            if (record == null) continue;
            faqs.removeIf(existing -> existing.id.equals(record.id));
            faqs.add(record);
        }
        faqs.sort(java.util.Comparator
                .comparingInt((FaqRecord faq) -> parseOrder(faq.sortOrder))
                .thenComparing(faq -> faq.question));
    }

    private static int parseOrder(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException ignored) {
            return 99;
        }
    }

    public static void saveFaq(FaqRecord faq) {
        if (faq == null) {
            return;
        }
        boolean updated = false;
        for (int i = 0; i < faqs.size(); i++) {
            if (faqs.get(i).id.equals(faq.id)) {
                faqs.set(i, faq);
                updated = true;
                break;
            }
        }
        if (!updated) {
            faqs.add(faq);
        }
        if (canonicalFirestore.isEnabled()) {
            try {
                canonicalFirestore.saveFaq(faq, currentToken());
            } catch (Exception ignored) {
                // Local FAQ changes stay available even if the remote sync is temporarily unavailable.
            }
        }
    }

    public static void deleteFaq(String id) {
        faqs.removeIf(faq -> faq.id.equals(id));
        if (canonicalFirestore.isEnabled()) {
            try {
                canonicalFirestore.deleteFaq(id, currentToken());
            } catch (Exception ignored) {
                // Keep the admin UI responsive if the remote delete fails.
            }
        }
    }

    public static BusinessRecord businessForOwner(String ownerId) {
        return businesses.stream()
                .filter(business -> business.ownerId.equals(ownerId) || business.businessId.equals(ownerId))
                .findFirst()
                .orElse(null);
    }

    public static List<TransportOperatorRecord> transportOperators() {
        return transportOperators;
    }

    public static List<PujaProviderRecord> pujaProviders() {
        return pujaProviders;
    }

    public static List<PujaServiceRecord> pujaServices() {
        return pujaServices;
    }

    public static List<PujaServiceRecord> publicPujaServices() {
        return pujaServices.stream()
                .filter(PujaServiceRecord::isPubliclyVisible)
                .toList();
    }

    public static List<PujaBookingRecord> pujaBookings() {
        return pujaBookings;
    }

    public static List<FraudReportRecord> fraudReports() {
        return fraudReports;
    }

    public static List<PujaBookingRecord> pujaBookingsForUser(String userId) {
        return pujaBookings.stream()
                .filter(booking -> booking.userId.equals(userId))
                .toList();
    }

    public static List<PujaProviderRecord> approvedPujaProviders() {
        return pujaProviders.stream()
                .filter(provider -> provider.approved
                        && ("approved".equalsIgnoreCase(provider.status)
                                || "active".equalsIgnoreCase(provider.status)))
                .toList();
    }

    public static List<LostFoundCaseRecord> lostFoundCases() {
        return lostFoundCases;
    }

    public static List<RouteRecord> transportRoutes() {
        return transportRoutes;
    }

    public static boolean hasRemoteItems(String module) {
        return remoteModules.contains(module);
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
        saveBookingIfPossible(booking);
        return booking;
    }

    public static void updateBookingStatus(String bookingId, String bookingStatus, String paymentStatus) {
        for (BookingRecord booking : bookings) {
            if (booking.bookingId.equals(bookingId)) {
                booking.bookingStatus = bookingStatus;
                booking.paymentStatus = paymentStatus;
                booking.updatedAt = String.valueOf(System.currentTimeMillis());
                try {
                    bookingDao.updateStatus(bookingId, bookingStatus, paymentStatus, currentToken());
                } catch (Exception ignored) {
                    // The existing payment backend remains authoritative when client writes are not allowed.
                }
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
        saveOperationalItem(module, item);
    }

    public static void saveOperationalItem(String module, ServiceItem item) {
        items(module).removeIf(existing -> existing.id.equals(item.id));
        items(module).add(item);
        try {
            operationalDataDao.saveOperationalItem(module, item, currentToken());
        } catch (Exception ignored) {
            operationalDataDao.saveItem(module, item);
        }
    }

    public static void removeItem(String module, ServiceItem item) {
        items(module).remove(item);
        operationalDataDao.deleteItem(item, currentToken());
    }

    public static void requestApproval(String type, String title, String detail, String targetModule) {
        ApprovalRequest request = new ApprovalRequest(type, title, detail, targetModule);
        pendingApprovals.add(request);
        try {
            approvalDao.save(request, currentToken());
        } catch (Exception ignored) {
            approvalDao.save(request);
        }
    }

    public static void approve(ApprovalRequest request) throws ApprovalUpdateException {
        requireOwnerId(request);
        try {
            userDao.updateStatus(request.ownerId, "approved", currentToken());
            if ("business".equals(request.targetModule)) {
                businessDao.updateStatus(request.ownerId, "approved", true, currentToken());
            } else if ("transport".equals(request.targetModule)) {
                operatorDao.updateStatus(request.ownerId, "approved", true, currentToken());
            }
            approvalDao.updateStatus(request.id, "approved", currentToken());
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Approval could not be saved to Firestore. The request is still pending.", exception);
        }

        addItem(request.targetModule, request.title, request.detail);
        pendingApprovals.remove(request);
    }

    public static void reject(ApprovalRequest request) throws ApprovalUpdateException {
        requireOwnerId(request);
        try {
            userDao.updateStatus(request.ownerId, "rejected", currentToken());
            if ("business".equals(request.targetModule)) {
                businessDao.updateStatus(request.ownerId, "rejected", false, currentToken());
            } else if ("transport".equals(request.targetModule)) {
                operatorDao.updateStatus(request.ownerId, "rejected", false, currentToken());
            }
            approvalDao.updateStatus(request.id, "rejected", currentToken());
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Rejection could not be saved to Firestore. The request is still pending.", exception);
        }

        pendingApprovals.remove(request);
    }

    public static void suspendUser(String uid) throws ApprovalUpdateException {
        try {
            userDao.updateStatus(uid, "suspended", currentToken());
            users.replaceAll(user -> user.uid.equals(uid) ? user.withStatus("suspended") : user);
        } catch (Exception exception) {
            throw new ApprovalUpdateException("User status could not be updated in Firestore.", exception);
        }
    }

    public static void reactivateUser(String uid) throws ApprovalUpdateException {
        try {
            userDao.updateStatus(uid, "active", currentToken());
            users.replaceAll(user -> user.uid.equals(uid) ? user.withStatus("active") : user);
        } catch (Exception exception) {
            throw new ApprovalUpdateException("User status could not be updated in Firestore.", exception);
        }
    }

    public static void updateLocalUserProfile(String uid, String name, String email, String mobile) {
        if (uid == null || uid.isBlank()) {
            return;
        }
        users.replaceAll(user -> user.uid.equals(uid)
                ? new UserRecord(uid, name, email, mobile, user.role, user.status, user.createdAt,
                        String.valueOf(System.currentTimeMillis()))
                : user);
    }

    public static void updateBusinessStatus(String businessId, String status, boolean approved) throws ApprovalUpdateException {
        try {
            businessDao.updateStatus(businessId, status, approved, currentToken());
            businesses.replaceAll(business -> business.businessId.equals(businessId)
                    ? business.withStatus(status, approved)
                    : business);
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Business status could not be updated in Firestore.", exception);
        }
    }

    public static void updateTransportOperatorStatus(String operatorId, String status, boolean approved)
            throws ApprovalUpdateException {
        try {
            operatorDao.updateStatus(operatorId, status, approved, currentToken());
            transportOperators.replaceAll(operator -> operator.operatorId.equals(operatorId)
                    ? operator.withStatus(status)
                    : operator);
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Transport operator status could not be updated in Firestore.", exception);
        }
    }

    public static void registerPujaProvider(PujaProviderRecord provider) throws ApprovalUpdateException {
        if (provider == null) {
            throw new ApprovalUpdateException("Provider details are missing.");
        }
        if (provider.containsUnauthorizedVipDarshan()) {
            throw new ApprovalUpdateException("Providers cannot create or publish unauthorized VIP Darshan services. Official Special Darshan must be admin-created or admin-approved.");
        }
        pujaProviders.removeIf(existing -> existing.providerId.equals(provider.providerId));
        pujaProviders.add(provider.withStatus("pending", false));
        try {
            firestore.savePujaProvider(provider.withStatus("pending", false), currentToken());
        } catch (Exception exception) {
            if (firestore.isEnabled()) {
                throw new ApprovalUpdateException("Provider registration could not be saved to Firestore.", exception);
            }
        }
    }

    public static void updatePujaProviderStatus(String providerId, String status, boolean approved)
            throws ApprovalUpdateException {
        if (!firestore.isEnabled()) {
            pujaProviders.replaceAll(provider -> provider.providerId.equals(providerId)
                    ? provider.withStatus(status, approved)
                    : provider);
            return;
        }
        try {
            firestore.updateDocumentStatus("pujaProviders", providerId, status, approved, currentToken());
            pujaProviders.replaceAll(provider -> provider.providerId.equals(providerId)
                    ? provider.withStatus(status, approved)
                    : provider);
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Puja provider status could not be updated in Firestore.", exception);
        }
    }

    public static void savePujaService(PujaServiceRecord service) throws ApprovalUpdateException {
        if (service == null || service.name.isBlank()) {
            throw new ApprovalUpdateException("Puja service details are missing.");
        }
        if (service.name.toLowerCase(Locale.ROOT).contains("vip darshan")) {
            throw new ApprovalUpdateException("Unauthorized VIP Darshan cannot be published.");
        }
        if (firestore.isEnabled()) {
            try {
                firestore.savePujaService(service, currentToken());
            } catch (Exception exception) {
                throw new ApprovalUpdateException("Puja service could not be saved to Firestore: "
                        + safeMessage(exception), exception);
            }
        }
        addUniqueItem(service.toServiceItem());
        pujaServices.removeIf(existing -> existing.serviceId.equals(service.serviceId));
        pujaServices.add(service);
    }

    public static void deletePujaService(String serviceId) throws ApprovalUpdateException {
        if (clean(serviceId).isBlank()) {
            throw new ApprovalUpdateException("Select a Puja service before deleting.");
        }
        if (firestore.isEnabled()) {
            try {
                firestore.deletePujaService(serviceId, currentToken());
            } catch (Exception exception) {
                throw new ApprovalUpdateException("Puja service could not be deleted from Firestore: "
                        + safeMessage(exception), exception);
            }
        }
        pujaServices.removeIf(existing -> existing.serviceId.equals(serviceId));
        items("puja").removeIf(existing -> existing.id.equals(serviceId));
    }

    public static void updatePujaServiceFlags(String serviceId, boolean published, boolean enabled,
            boolean adminApproved, String verificationStatus) throws ApprovalUpdateException {
        PujaServiceRecord current = pujaServices.stream()
                .filter(service -> service.serviceId.equals(serviceId))
                .findFirst()
                .orElse(null);
        if (current == null) {
            throw new ApprovalUpdateException("Puja service was not found.");
        }
        savePujaService(current.withControl(published, enabled, adminApproved, verificationStatus));
    }

    public static void savePujaBooking(PujaBookingRecord booking) throws ApprovalUpdateException {
        pujaBookings.removeIf(existing -> existing.bookingId.equals(booking.bookingId));
        pujaBookings.add(booking);
        if (firestore.isEnabled()) {
            try {
                firestore.savePujaBooking(booking, currentToken());
            } catch (Exception exception) {
                System.err.println("Puja booking saved locally; Firestore sync failed: "
                        + safeMessage(exception));
            }
        }
    }

    public static void updatePujaBookingStatus(String bookingId, String bookingStatus) throws ApprovalUpdateException {
        PujaBookingRecord current = pujaBookings.stream()
                .filter(booking -> booking.bookingId.equals(bookingId))
                .findFirst()
                .orElse(null);
        if (current == null) {
            throw new ApprovalUpdateException("Puja booking was not found.");
        }
        PujaBookingRecord updated = current.withBookingStatus(bookingStatus);
        pujaBookings.replaceAll(booking -> booking.bookingId.equals(bookingId) ? updated : booking);
        try {
            firestore.updatePujaBookingStatus(bookingId, bookingStatus, currentToken());
        } catch (Exception exception) {
            if (firestore.isEnabled()) {
                throw new ApprovalUpdateException("Puja booking status could not be updated in Firestore.", exception);
            }
        }
    }

    public static void updatePujaBookingPayment(String bookingId, String bookingStatus, String paymentStatus,
            String internalPaymentId, String razorpayOrderId, String razorpayPaymentId, String paymentMethod,
            String paymentCreatedAt, String paymentCompletedAt, String paymentFailureReason)
            throws ApprovalUpdateException {
        PujaBookingRecord current = pujaBookings.stream()
                .filter(booking -> booking.bookingId.equals(bookingId))
                .findFirst()
                .orElse(null);
        if (current == null) {
            throw new ApprovalUpdateException("Puja booking was not found.");
        }
        PujaBookingRecord updated = current.withPayment(bookingStatus, paymentStatus, internalPaymentId,
                razorpayOrderId, razorpayPaymentId, paymentMethod, paymentCreatedAt, paymentCompletedAt,
                paymentFailureReason);
        pujaBookings.replaceAll(booking -> booking.bookingId.equals(bookingId) ? updated : booking);
        if (firestore.isEnabled()) {
            try {
                firestore.updatePujaBookingPayment(bookingId, updated.bookingStatus, updated.paymentStatus,
                        updated.internalPaymentId, updated.razorpayOrderId, updated.razorpayPaymentId,
                        updated.paymentMethod, updated.paymentCreatedAt, updated.paymentCompletedAt,
                        updated.paymentFailureReason, currentToken());
            } catch (Exception exception) {
                System.err.println("Puja booking payment updated locally; Firestore sync failed: "
                        + safeMessage(exception));
            }
        }
    }

    public static PujaBookingRecord updatePujaBookingQrTicket(String bookingId, String qrTicketId,
            String qrVerificationToken) throws ApprovalUpdateException {
        PujaBookingRecord current = pujaBookings.stream()
                .filter(booking -> booking.bookingId.equals(bookingId))
                .findFirst()
                .orElse(null);
        if (current == null) {
            throw new ApprovalUpdateException("Puja booking was not found.");
        }
        PujaBookingRecord updated = current.withQrTicket(qrTicketId, qrVerificationToken);
        pujaBookings.replaceAll(booking -> booking.bookingId.equals(bookingId) ? updated : booking);
        if (firestore.isEnabled()) {
            try {
                firestore.updatePujaBookingQrTicket(bookingId, updated.qrTicketId, updated.qrVerificationToken,
                        currentToken());
            } catch (Exception exception) {
                System.err.println("Puja ticket saved locally; Firestore sync failed: "
                        + safeMessage(exception));
            }
        }
        return updated;
    }

    public static void submitFraudReport(FraudReportRecord report) throws ApprovalUpdateException {
        fraudReports.removeIf(existing -> existing.reportId.equals(report.reportId));
        fraudReports.add(report);
        try {
            firestore.saveFraudReport(report, currentToken());
        } catch (Exception exception) {
            if (firestore.isEnabled()) {
                throw new ApprovalUpdateException("Fraud report could not be saved to Firestore.", exception);
            }
        }
    }

    public static void saveRoute(RouteRecord route) throws ApprovalUpdateException {
        try {
            operationalDataDao.saveRoute(route, currentToken());
            transportRoutes.removeIf(existing -> existing.routeId.equals(route.routeId));
            transportRoutes.add(route);
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Route could not be saved to Firestore.", exception);
        }
    }

    public static void updateRouteFlags(String routeId, boolean published, boolean active) throws ApprovalUpdateException {
        try {
            operationalDataDao.updateRouteFlags(routeId, published, active, currentToken());
            transportRoutes.replaceAll(route -> route.routeId.equals(routeId)
                    ? route.withFlags(published, active)
                    : route);
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Route status could not be updated in Firestore.", exception);
        }
    }

    public static void updateOperationalItemFlags(String module, ServiceItem item, boolean published, boolean active)
            throws ApprovalUpdateException {
        try {
            operationalDataDao.updateOperationalItemFlags(module, item.id, published, active, currentToken());
            if (!active || !published) {
                items(module).removeIf(existing -> existing.id.equals(item.id));
            }
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Operational item status could not be updated in Firestore.", exception);
        }
    }

    public static void updateLostFoundStatus(String caseId, String status) throws ApprovalUpdateException {
        try {
            operationalDataDao.updateLostFoundStatus(caseId, status, "Updated from Admin Control Center", currentToken());
            lostFoundCases.replaceAll(item -> item.caseId.equals(caseId) ? item.withStatus(status) : item);
        } catch (Exception exception) {
            throw new ApprovalUpdateException("Lost & Found case could not be updated in Firestore.", exception);
        }
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

    public static void refreshPujaFirebaseData(String idToken) {
        loadPujaFirebaseDataIfAvailable(idToken);
    }

    public static AdminOverview refreshAdminOverview(String idToken) {
        if (!firestore.isEnabled()) {
            adminOverview = AdminOverview.empty("Firebase is not configured.");
            return adminOverview;
        }
        try {
            adminOverview = operationalDataDao.loadAdminOverview(idToken);
        } catch (Exception exception) {
            adminOverview = AdminOverview.empty("Firebase is unavailable or permission was denied.");
        }
        return adminOverview;
    }

    public static AdminOverview adminOverview() {
        return adminOverview;
    }

    public static String displayName(String module) {
        return switch (module) {
            case "transport" -> "Transport";
            case "packages" -> "Kumbh Packages";
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
        public final String imageUrl;
        public final String imagePublicId;
        public final List<CloudImage> galleryImages;

        public ServiceItem(String title, String detail, String category) {
            this(slug(category + "-" + title), moduleKey(category), title, detail, category);
        }

        public ServiceItem(String module, String title, String detail, String category) {
            this(randomId(module), module, title, detail, category);
        }

        public ServiceItem(String id, String module, String title, String detail, String category) {
            this(id, module, title, detail, category, "", "", List.of());
        }

        public ServiceItem(String id, String module, String title, String detail, String category,
                String imageUrl, String imagePublicId, List<CloudImage> galleryImages) {
            this.id = id;
            this.module = module;
            this.title = title;
            this.detail = detail;
            this.category = category;
            this.imageUrl = clean(imageUrl);
            this.imagePublicId = clean(imagePublicId);
            this.galleryImages = galleryImages == null ? List.of() : List.copyOf(galleryImages);
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

    public static final class UserRecord {
        public final String uid;
        public final String name;
        public final String email;
        public final String mobile;
        public final String role;
        public final String status;
        public final String createdAt;
        public final String updatedAt;

        public UserRecord(String uid, String name, String email, String mobile, String role, String status,
                String createdAt, String updatedAt) {
            this.uid = clean(uid);
            this.name = clean(name);
            this.email = clean(email);
            this.mobile = clean(mobile);
            this.role = clean(role);
            this.status = clean(status);
            this.createdAt = clean(createdAt);
            this.updatedAt = clean(updatedAt);
        }

        public UserRecord withStatus(String status) {
            return new UserRecord(uid, name, email, mobile, role, status, createdAt,
                    String.valueOf(System.currentTimeMillis()));
        }
    }

    public static final class FaqRecord {
        public final String id;
        public final String category;
        public final String question;
        public final String answer;
        public final boolean active;
        public final String sortOrder;

        public FaqRecord(String id, String category, String question, String answer, boolean active, String sortOrder) {
            this.id = clean(id).isBlank() ? "faq-" + UUID.randomUUID() : clean(id);
            this.category = clean(category).isBlank() ? "General" : clean(category);
            this.question = clean(question);
            this.answer = clean(answer);
            this.active = active;
            this.sortOrder = clean(sortOrder).isBlank() ? "99" : clean(sortOrder);
        }

        public FaqRecord withActive(boolean active) {
            return new FaqRecord(id, category, question, answer, active, sortOrder);
        }
    }

    public static final class BusinessRecord {
        public final String businessId;
        public final String ownerId;
        public final String businessName;
        public final String ownerName;
        public final String category;
        public final String description;
        public final String location;
        public final String address;
        public final String area;
        public final String city;
        public final String latitude;
        public final String longitude;
        public final String locationUpdatedAt;
        public final String mobile;
        public final String email;
        public final String operatingHours;
        public final String priceRange;
        public final String status;
        public final boolean approved;
        public final String logoUrl;
        public final String logoPublicId;
        public final String coverPhotoUrl;
        public final String coverPhotoPublicId;
        public final List<CloudImage> galleryImages;
        public final String createdAt;
        public final String updatedAt;

        public BusinessRecord(String businessId, String ownerId, String businessName, String ownerName, String category,
                String description, String location, String mobile, String email, String operatingHours,
                String priceRange, String status, boolean approved, String createdAt, String updatedAt) {
            this(businessId, ownerId, businessName, ownerName, category, description, location, "", "", "", "", "",
                    "", mobile, email, operatingHours, priceRange, status, approved, "", "", "", "", List.of(),
                    createdAt, updatedAt);
        }

        public BusinessRecord(String businessId, String ownerId, String businessName, String ownerName, String category,
                String description, String location, String address, String area, String city, String latitude,
                String longitude, String locationUpdatedAt, String mobile, String email, String operatingHours,
                String priceRange, String status, boolean approved, String createdAt, String updatedAt) {
            this(businessId, ownerId, businessName, ownerName, category, description, location, address, area, city,
                    latitude, longitude, locationUpdatedAt, mobile, email, operatingHours, priceRange, status, approved,
                    "", "", "", "", List.of(), createdAt, updatedAt);
        }

        public BusinessRecord(String businessId, String ownerId, String businessName, String ownerName, String category,
                String description, String location, String mobile, String email, String operatingHours,
                String priceRange, String status, boolean approved, String logoUrl, String logoPublicId,
                String coverPhotoUrl, String coverPhotoPublicId, List<CloudImage> galleryImages,
                String createdAt, String updatedAt) {
            this(businessId, ownerId, businessName, ownerName, category, description, location, "", "", "", "", "",
                    "", mobile, email, operatingHours, priceRange, status, approved, logoUrl, logoPublicId,
                    coverPhotoUrl, coverPhotoPublicId, galleryImages, createdAt, updatedAt);
        }

        public BusinessRecord(String businessId, String ownerId, String businessName, String ownerName, String category,
                String description, String location, String address, String area, String city, String latitude,
                String longitude, String locationUpdatedAt, String mobile, String email, String operatingHours,
                String priceRange, String status, boolean approved, String logoUrl, String logoPublicId,
                String coverPhotoUrl, String coverPhotoPublicId, List<CloudImage> galleryImages, String createdAt,
                String updatedAt) {
            this.businessId = clean(businessId);
            this.ownerId = clean(ownerId);
            this.businessName = clean(businessName);
            this.ownerName = clean(ownerName);
            this.category = clean(category);
            this.description = clean(description);
            this.location = clean(location);
            this.address = clean(address);
            this.area = clean(area);
            this.city = clean(city);
            this.latitude = clean(latitude);
            this.longitude = clean(longitude);
            this.locationUpdatedAt = clean(locationUpdatedAt);
            this.mobile = clean(mobile);
            this.email = clean(email);
            this.operatingHours = clean(operatingHours);
            this.priceRange = clean(priceRange);
            this.status = clean(status).isBlank() ? "pending" : clean(status);
            this.approved = approved;
            this.logoUrl = clean(logoUrl);
            this.logoPublicId = clean(logoPublicId);
            this.coverPhotoUrl = clean(coverPhotoUrl);
            this.coverPhotoPublicId = clean(coverPhotoPublicId);
            this.galleryImages = galleryImages == null ? List.of() : List.copyOf(galleryImages);
            this.createdAt = clean(createdAt);
            this.updatedAt = clean(updatedAt);
        }

        public BusinessRecord withStatus(String status, boolean approved) {
            return new BusinessRecord(businessId, ownerId, businessName, ownerName, category, description, location,
                    address, area, city, latitude, longitude, locationUpdatedAt, mobile, email, operatingHours,
                    priceRange, status, approved, logoUrl, logoPublicId, coverPhotoUrl, coverPhotoPublicId,
                    galleryImages, createdAt,
                    String.valueOf(System.currentTimeMillis()));
        }
    }

    public static final class TransportOperatorRecord {
        public final String operatorId;
        public final String ownerId;
        public final String organizationName;
        public final String contactPerson;
        public final String mobile;
        public final String email;
        public final String serviceType;
        public final String status;
        public final String vehicleCount;
        public final String routesSubmitted;
        public final String activeRoutes;
        public final String createdAt;
        public final String updatedAt;

        public TransportOperatorRecord(String operatorId, String ownerId, String organizationName,
                String contactPerson, String mobile, String email, String serviceType, String status,
                String vehicleCount, String routesSubmitted, String activeRoutes, String createdAt, String updatedAt) {
            this.operatorId = clean(operatorId);
            this.ownerId = clean(ownerId);
            this.organizationName = clean(organizationName);
            this.contactPerson = clean(contactPerson);
            this.mobile = clean(mobile);
            this.email = clean(email);
            this.serviceType = clean(serviceType);
            this.status = clean(status).isBlank() ? "active" : clean(status);
            this.vehicleCount = clean(vehicleCount);
            this.routesSubmitted = clean(routesSubmitted);
            this.activeRoutes = clean(activeRoutes);
            this.createdAt = clean(createdAt);
            this.updatedAt = clean(updatedAt);
        }

        public TransportOperatorRecord withStatus(String status) {
            return new TransportOperatorRecord(operatorId, ownerId, organizationName, contactPerson, mobile, email,
                    serviceType, status, vehicleCount, routesSubmitted, activeRoutes, createdAt,
                    String.valueOf(System.currentTimeMillis()));
        }
    }

    public static final class PujaProviderRecord {
        public final String providerId;
        public final String fullName;
        public final String profilePhoto;
        public final String phone;
        public final String email;
        public final String address;
        public final String experience;
        public final String specialization;
        public final String languages;
        public final String templeOrganization;
        public final String identityDocument;
        public final String supportingCertificates;
        public final String servicesOffered;
        public final String serviceLocations;
        public final String status;
        public final boolean approved;
        public final String createdAt;
        public final String updatedAt;

        public PujaProviderRecord(String providerId, String fullName, String profilePhoto, String phone, String email,
                String address, String experience, String specialization, String languages, String templeOrganization,
                String identityDocument, String supportingCertificates, String servicesOffered, String serviceLocations,
                String status, boolean approved, String createdAt, String updatedAt) {
            this.providerId = clean(providerId).isBlank() ? randomId("puja-provider") : clean(providerId);
            this.fullName = clean(fullName);
            this.profilePhoto = clean(profilePhoto);
            this.phone = clean(phone);
            this.email = clean(email);
            this.address = clean(address);
            this.experience = clean(experience);
            this.specialization = clean(specialization);
            this.languages = clean(languages);
            this.templeOrganization = clean(templeOrganization);
            this.identityDocument = clean(identityDocument);
            this.supportingCertificates = clean(supportingCertificates);
            this.servicesOffered = clean(servicesOffered);
            this.serviceLocations = clean(serviceLocations);
            this.status = clean(status).isBlank() ? "pending" : clean(status).toLowerCase(Locale.ROOT);
            this.approved = approved;
            this.createdAt = clean(createdAt).isBlank() ? String.valueOf(System.currentTimeMillis()) : clean(createdAt);
            this.updatedAt = clean(updatedAt).isBlank() ? this.createdAt : clean(updatedAt);
        }

        public PujaProviderRecord withStatus(String status, boolean approved) {
            return new PujaProviderRecord(providerId, fullName, profilePhoto, phone, email, address, experience,
                    specialization, languages, templeOrganization, identityDocument, supportingCertificates,
                    servicesOffered, serviceLocations, status, approved, createdAt,
                    String.valueOf(System.currentTimeMillis()));
        }

        public boolean containsUnauthorizedVipDarshan() {
            String services = servicesOffered.toLowerCase(Locale.ROOT);
            return services.contains("vip darshan");
        }
    }

    public static final class PujaServiceRecord {
        public final String serviceId;
        public final String name;
        public final String description;
        public final String pujaType;
        public final String templeOrGhat;
        public final String price;
        public final String duration;
        public final String providerId;
        public final String providerName;
        public final String verificationStatus;
        public final String availableSlots;
        public final String mode;
        public final String languages;
        public final String imageUrl;
        public final String imagePublicId;
        public final String bookingStatus;
        public final String status;
        public final boolean published;
        public final boolean enabled;
        public final boolean adminApproved;
        public final String createdAt;
        public final String updatedAt;
        public final String createdBy;
        public final String updatedBy;
        public final String verifiedAt;
        public final String verifiedBy;

        public PujaServiceRecord(String serviceId, String name, String description, String pujaType,
                String templeOrGhat, String price, String duration, String providerId, String providerName,
                String verificationStatus, String availableSlots, String mode, String languages, String imageUrl,
                String bookingStatus, String status, boolean published, boolean enabled, boolean adminApproved,
                String createdAt, String updatedAt) {
            this(serviceId, name, description, pujaType, templeOrGhat, price, duration, providerId, providerName,
                    verificationStatus, availableSlots, mode, languages, imageUrl, "", bookingStatus, status, published,
                    enabled, adminApproved, createdAt, updatedAt, "", "", "", "");
        }

        public PujaServiceRecord(String serviceId, String name, String description, String pujaType,
                String templeOrGhat, String price, String duration, String providerId, String providerName,
                String verificationStatus, String availableSlots, String mode, String languages, String imageUrl,
                String bookingStatus, String status, boolean published, boolean enabled, boolean adminApproved,
                String createdAt, String updatedAt, String createdBy, String updatedBy, String verifiedAt,
                String verifiedBy) {
            this(serviceId, name, description, pujaType, templeOrGhat, price, duration, providerId, providerName,
                    verificationStatus, availableSlots, mode, languages, imageUrl, "", bookingStatus, status,
                    published, enabled, adminApproved, createdAt, updatedAt, createdBy, updatedBy, verifiedAt,
                    verifiedBy);
        }

        public PujaServiceRecord(String serviceId, String name, String description, String pujaType,
                String templeOrGhat, String price, String duration, String providerId, String providerName,
                String verificationStatus, String availableSlots, String mode, String languages, String imageUrl,
                String imagePublicId, String bookingStatus, String status, boolean published, boolean enabled,
                boolean adminApproved, String createdAt, String updatedAt, String createdBy, String updatedBy,
                String verifiedAt, String verifiedBy) {
            this.serviceId = clean(serviceId).isBlank() ? randomId("puja-service") : clean(serviceId);
            this.name = clean(name);
            this.description = clean(description);
            this.pujaType = clean(pujaType).isBlank() ? "Custom" : clean(pujaType);
            this.templeOrGhat = clean(templeOrGhat).isBlank() ? "Approved Location" : clean(templeOrGhat);
            this.price = clean(price).isBlank() ? "0" : clean(price);
            this.duration = clean(duration).isBlank() ? "Varies" : clean(duration);
            this.providerId = clean(providerId);
            this.providerName = clean(providerName);
            this.verificationStatus = clean(verificationStatus).isBlank() ? "PENDING" : clean(verificationStatus).toUpperCase(Locale.ROOT);
            this.availableSlots = clean(availableSlots).isBlank() ? "Varies" : clean(availableSlots);
            this.mode = clean(mode).isBlank() ? "Offline" : clean(mode);
            this.languages = clean(languages).isBlank() ? "Marathi, Hindi, Sanskrit" : clean(languages);
            this.imageUrl = clean(imageUrl).isBlank() ? "/images/trimbakeshwar.jpg" : clean(imageUrl);
            this.bookingStatus = clean(bookingStatus).isBlank() ? "OPEN" : clean(bookingStatus).toUpperCase(Locale.ROOT);
            this.status = clean(status).isBlank() ? "draft" : clean(status).toLowerCase(Locale.ROOT);
            this.published = published;
            this.enabled = enabled;
            this.adminApproved = adminApproved;
            this.createdAt = clean(createdAt).isBlank() ? String.valueOf(System.currentTimeMillis()) : clean(createdAt);
            this.updatedAt = clean(updatedAt).isBlank() ? this.createdAt : clean(updatedAt);
            this.createdBy = clean(createdBy);
            this.updatedBy = clean(updatedBy);
            this.verifiedAt = clean(verifiedAt);
            this.verifiedBy = clean(verifiedBy);
            this.imagePublicId = clean(imagePublicId);
        }

        public PujaServiceRecord withImage(String imageUrl, String imagePublicId) {
            return new PujaServiceRecord(serviceId, name, description, pujaType, templeOrGhat, price, duration,
                    providerId, providerName, verificationStatus, availableSlots, mode, languages,
                    clean(imageUrl).isBlank() ? this.imageUrl : imageUrl,
                    clean(imagePublicId).isBlank() ? this.imagePublicId : imagePublicId,
                    bookingStatus, status, published, enabled, adminApproved, createdAt, updatedAt, createdBy,
                    updatedBy, verifiedAt, verifiedBy);
        }

        public PujaServiceRecord withControl(boolean published, boolean enabled, boolean adminApproved,
                String verificationStatus) {
            String nextVerificationStatus = clean(verificationStatus).isBlank()
                    ? this.verificationStatus
                    : clean(verificationStatus).toUpperCase(Locale.ROOT);
            String now = String.valueOf(System.currentTimeMillis());
            String actor = AppSession.currentUser() == null ? "" : AppSession.currentUser().uid();
            boolean verified = "VERIFIED".equalsIgnoreCase(nextVerificationStatus);
            return new PujaServiceRecord(serviceId, name, description, pujaType, templeOrGhat, price, duration,
                    providerId, providerName, nextVerificationStatus, availableSlots, mode, languages, imageUrl,
                    imagePublicId, bookingStatus, enabled ? "active" : "disabled", published, enabled, adminApproved,
                    createdAt, now, createdBy, actor, verified && verifiedAt.isBlank() ? now : verifiedAt,
                    verified && verifiedBy.isBlank() ? actor : verifiedBy);
        }

        public boolean isPubliclyVisible() {
            return enabled
                    && published
                    && adminApproved
                    && "active".equalsIgnoreCase(status)
                    && "verified".equalsIgnoreCase(verificationStatus);
        }

        public ServiceItem toServiceItem() {
            String detail = "Temple / Ghat: " + templeOrGhat
                    + " | Puja Type: " + pujaType
                    + " | Price: " + price
                    + " | Duration: " + duration
                    + " | Available Slots: " + availableSlots
                    + " | Pandit / Provider: " + providerName
                    + " | Languages: " + languages
                    + " | Mode: " + mode
                    + " | Verification Status: " + verificationStatus
                    + " | Booking Status: " + bookingStatus;
            return new ServiceItem(serviceId, "puja", name, detail, "Puja");
        }
    }

    public static final class PujaBookingRecord {
        public final String bookingId;
        public final String userId;
        public final String userName;
        public final String userPhone;
        public final String userEmail;
        public final String serviceId;
        public final String serviceName;
        public final String serviceType;
        public final String providerId;
        public final String providerName;
        public final String templeOrGhat;
        public final String date;
        public final String time;
        public final String location;
        public final int devoteesCount;
        public final String language;
        public final String mode;
        public final long amount;
        public final String bookingStatus;
        public final String paymentStatus;
        public final String createdAt;
        public final String updatedAt;
        public final String locationId;
        public final String locationName;
        public final boolean samagriSelected;
        public final long samagriAmount;
        public final boolean prasadSelected;
        public final long prasadAmount;
        public final long basePrice;
        public final long serviceFee;
        public final long totalAmount;
        public final String specialRequirements;
        public final String internalPaymentId;
        public final String razorpayOrderId;
        public final String razorpayPaymentId;
        public final String paymentMethod;
        public final String paymentCreatedAt;
        public final String paymentCompletedAt;
        public final String paymentFailureReason;
        public final String qrTicketId;
        public final String qrVerificationToken;

        public PujaBookingRecord(String bookingId, String userId, String userName, String serviceId,
                String serviceName, String providerId, String providerName, String date, String time, String location,
                int devoteesCount, String language, String mode, long amount, String bookingStatus,
                String paymentStatus, String createdAt, String updatedAt) {
            this(bookingId, userId, userName, serviceId, serviceName, providerId, providerName, date, time, location,
                    devoteesCount, language, mode, amount, bookingStatus, paymentStatus, createdAt, updatedAt,
                    "", location, false, 0, false, 0, amount, 0, amount, "",
                    "", "", "", "", "", "", "");
        }

        public PujaBookingRecord(String bookingId, String userId, String userName, String serviceId,
                String serviceName, String providerId, String providerName, String date, String time, String location,
                int devoteesCount, String language, String mode, long amount, String bookingStatus,
                String paymentStatus, String createdAt, String updatedAt, String locationId, String locationName,
                boolean samagriSelected, long samagriAmount, boolean prasadSelected, long prasadAmount,
                long basePrice, long serviceFee, long totalAmount, String specialRequirements) {
            this(bookingId, userId, userName, serviceId, serviceName, providerId, providerName, date, time, location,
                    devoteesCount, language, mode, amount, bookingStatus, paymentStatus, createdAt, updatedAt,
                    locationId, locationName, samagriSelected, samagriAmount, prasadSelected, prasadAmount,
                    basePrice, serviceFee, totalAmount, specialRequirements, "", "", "", "", "", "", "");
        }

        public PujaBookingRecord(String bookingId, String userId, String userName, String serviceId,
                String serviceName, String providerId, String providerName, String date, String time, String location,
                int devoteesCount, String language, String mode, long amount, String bookingStatus,
                String paymentStatus, String createdAt, String updatedAt, String locationId, String locationName,
                boolean samagriSelected, long samagriAmount, boolean prasadSelected, long prasadAmount,
                long basePrice, long serviceFee, long totalAmount, String specialRequirements,
                String internalPaymentId, String razorpayOrderId, String razorpayPaymentId, String paymentMethod,
                String paymentCreatedAt, String paymentCompletedAt, String paymentFailureReason) {
            this(bookingId, userId, userName, "", "", serviceId, serviceName, "", providerId, providerName, "",
                    date, time, location, devoteesCount, language, mode, amount, bookingStatus, paymentStatus,
                    createdAt, updatedAt, locationId, locationName, samagriSelected, samagriAmount, prasadSelected,
                    prasadAmount, basePrice, serviceFee, totalAmount, specialRequirements, internalPaymentId,
                    razorpayOrderId, razorpayPaymentId, paymentMethod, paymentCreatedAt, paymentCompletedAt,
                    paymentFailureReason, "", "");
        }

        public PujaBookingRecord(String bookingId, String userId, String userName, String userPhone, String userEmail,
                String serviceId, String serviceName, String serviceType, String providerId, String providerName,
                String templeOrGhat, String date, String time, String location, int devoteesCount, String language,
                String mode, long amount, String bookingStatus, String paymentStatus, String createdAt,
                String updatedAt, String locationId, String locationName, boolean samagriSelected, long samagriAmount,
                boolean prasadSelected, long prasadAmount, long basePrice, long serviceFee, long totalAmount,
                String specialRequirements, String internalPaymentId, String razorpayOrderId,
                String razorpayPaymentId, String paymentMethod, String paymentCreatedAt, String paymentCompletedAt,
                String paymentFailureReason, String qrTicketId, String qrVerificationToken) {
            this.bookingId = clean(bookingId).isBlank() ? randomId("puja-booking") : clean(bookingId);
            this.userId = clean(userId);
            this.userName = clean(userName);
            this.userPhone = clean(userPhone);
            this.userEmail = clean(userEmail);
            this.serviceId = clean(serviceId);
            this.serviceName = clean(serviceName);
            this.serviceType = clean(serviceType).isBlank() ? "Puja" : clean(serviceType);
            this.providerId = clean(providerId);
            this.providerName = clean(providerName);
            this.templeOrGhat = clean(templeOrGhat);
            this.date = clean(date);
            this.time = clean(time);
            this.location = clean(location);
            this.devoteesCount = Math.max(1, devoteesCount);
            this.language = clean(language).isBlank() ? "Marathi/Hindi" : clean(language);
            this.mode = clean(mode).isBlank() ? "Offline" : clean(mode);
            this.amount = amount;
            this.bookingStatus = clean(bookingStatus).isBlank() ? "PENDING" : clean(bookingStatus).toUpperCase(Locale.ROOT);
            this.paymentStatus = clean(paymentStatus).isBlank() ? "UNPAID" : clean(paymentStatus).toUpperCase(Locale.ROOT);
            this.createdAt = clean(createdAt).isBlank() ? String.valueOf(System.currentTimeMillis()) : clean(createdAt);
            this.updatedAt = clean(updatedAt).isBlank() ? this.createdAt : clean(updatedAt);
            this.locationId = clean(locationId);
            this.locationName = clean(locationName).isBlank() ? this.location : clean(locationName);
            this.samagriSelected = samagriSelected;
            this.samagriAmount = Math.max(0, samagriAmount);
            this.prasadSelected = prasadSelected;
            this.prasadAmount = Math.max(0, prasadAmount);
            this.basePrice = Math.max(0, basePrice);
            this.serviceFee = Math.max(0, serviceFee);
            this.totalAmount = Math.max(0, totalAmount);
            this.specialRequirements = clean(specialRequirements);
            this.internalPaymentId = clean(internalPaymentId);
            this.razorpayOrderId = clean(razorpayOrderId);
            this.razorpayPaymentId = clean(razorpayPaymentId);
            this.paymentMethod = clean(paymentMethod);
            this.paymentCreatedAt = clean(paymentCreatedAt);
            this.paymentCompletedAt = clean(paymentCompletedAt);
            this.paymentFailureReason = clean(paymentFailureReason);
            this.qrTicketId = clean(qrTicketId);
            this.qrVerificationToken = clean(qrVerificationToken);
        }

        public PujaBookingRecord withBookingStatus(String status) {
            return new PujaBookingRecord(bookingId, userId, userName, serviceId, serviceName, providerId, providerName,
                    date, time, location, devoteesCount, language, mode, amount, status, paymentStatus,
                    createdAt, String.valueOf(System.currentTimeMillis()), locationId, locationName,
                    samagriSelected, samagriAmount, prasadSelected, prasadAmount, basePrice, serviceFee,
                    totalAmount, specialRequirements, internalPaymentId, razorpayOrderId, razorpayPaymentId,
                    paymentMethod, paymentCreatedAt, paymentCompletedAt, paymentFailureReason)
                    .withContactAndTicket(userPhone, userEmail, serviceType, templeOrGhat, qrTicketId, qrVerificationToken);
        }

        public PujaBookingRecord withPayment(String bookingStatus, String paymentStatus, String internalPaymentId,
                String razorpayOrderId, String razorpayPaymentId, String paymentMethod, String paymentCreatedAt,
                String paymentCompletedAt, String paymentFailureReason) {
            return new PujaBookingRecord(bookingId, userId, userName, userPhone, userEmail, serviceId, serviceName,
                    serviceType, providerId, providerName, templeOrGhat, date, time, location, devoteesCount,
                    language, mode, amount, bookingStatus, paymentStatus, createdAt,
                    String.valueOf(System.currentTimeMillis()), locationId, locationName, samagriSelected,
                    samagriAmount, prasadSelected, prasadAmount, basePrice, serviceFee, totalAmount,
                    specialRequirements,
                    clean(internalPaymentId).isBlank() ? this.internalPaymentId : internalPaymentId,
                    clean(razorpayOrderId).isBlank() ? this.razorpayOrderId : razorpayOrderId,
                    clean(razorpayPaymentId).isBlank() ? this.razorpayPaymentId : razorpayPaymentId,
                    clean(paymentMethod).isBlank() ? this.paymentMethod : paymentMethod,
                    clean(paymentCreatedAt).isBlank() ? this.paymentCreatedAt : paymentCreatedAt,
                    clean(paymentCompletedAt).isBlank() ? this.paymentCompletedAt : paymentCompletedAt,
                    clean(paymentFailureReason), qrTicketId, qrVerificationToken);
        }

        public PujaBookingRecord withQrTicket(String qrTicketId, String qrVerificationToken) {
            return new PujaBookingRecord(bookingId, userId, userName, userPhone, userEmail, serviceId, serviceName,
                    serviceType, providerId, providerName, templeOrGhat, date, time, location, devoteesCount,
                    language, mode, amount, bookingStatus, paymentStatus, createdAt,
                    String.valueOf(System.currentTimeMillis()), locationId, locationName, samagriSelected,
                    samagriAmount, prasadSelected, prasadAmount, basePrice, serviceFee, totalAmount,
                    specialRequirements, internalPaymentId, razorpayOrderId, razorpayPaymentId, paymentMethod,
                    paymentCreatedAt, paymentCompletedAt, paymentFailureReason, qrTicketId, qrVerificationToken);
        }

        private PujaBookingRecord withContactAndTicket(String userPhone, String userEmail, String serviceType,
                String templeOrGhat, String qrTicketId, String qrVerificationToken) {
            return new PujaBookingRecord(bookingId, userId, userName, userPhone, userEmail, serviceId, serviceName,
                    serviceType, providerId, providerName, templeOrGhat, date, time, location, devoteesCount,
                    language, mode, amount, bookingStatus, paymentStatus, createdAt, updatedAt, locationId,
                    locationName, samagriSelected, samagriAmount, prasadSelected, prasadAmount, basePrice,
                    serviceFee, totalAmount, specialRequirements, internalPaymentId, razorpayOrderId,
                    razorpayPaymentId, paymentMethod, paymentCreatedAt, paymentCompletedAt, paymentFailureReason,
                    qrTicketId, qrVerificationToken);
        }
    }

    public static final class FraudReportRecord {
        public final String reportId;
        public final String userId;
        public final String userName;
        public final String bookingId;
        public final String serviceId;
        public final String serviceName;
        public final String providerId;
        public final String providerName;
        public final String issue;
        public final String status;
        public final String createdAt;
        public final String updatedAt;

        public FraudReportRecord(String reportId, String userId, String userName, String bookingId,
                String serviceId, String serviceName, String providerId, String providerName, String issue,
                String status, String createdAt, String updatedAt) {
            this.reportId = clean(reportId).isBlank() ? randomId("fraud-report") : clean(reportId);
            this.userId = clean(userId);
            this.userName = clean(userName);
            this.bookingId = clean(bookingId);
            this.serviceId = clean(serviceId);
            this.serviceName = clean(serviceName);
            this.providerId = clean(providerId);
            this.providerName = clean(providerName);
            this.issue = clean(issue);
            this.status = clean(status).isBlank() ? "OPEN" : clean(status).toUpperCase(Locale.ROOT);
            this.createdAt = clean(createdAt).isBlank() ? String.valueOf(System.currentTimeMillis()) : clean(createdAt);
            this.updatedAt = clean(updatedAt).isBlank() ? this.createdAt : clean(updatedAt);
        }
    }

    public static final class LostFoundCaseRecord {
        public final String caseId;
        public final String type;
        public final String name;
        public final String age;
        public final String gender;
        public final String clothing;
        public final String identificationMarks;
        public final String lastSeenLocation;
        public final String lastSeenDateTime;
        public final String reporterName;
        public final String relation;
        public final String contact;
        public final String status;
        public final String priority;
        public final String createdAt;
        public final String updatedAt;

        public LostFoundCaseRecord(String caseId, String type, String name, String age, String gender,
                String clothing, String identificationMarks, String lastSeenLocation, String lastSeenDateTime,
                String reporterName, String relation, String contact, String status, String priority,
                String createdAt, String updatedAt) {
            this.caseId = clean(caseId);
            this.type = clean(type);
            this.name = clean(name);
            this.age = clean(age);
            this.gender = clean(gender);
            this.clothing = clean(clothing);
            this.identificationMarks = clean(identificationMarks);
            this.lastSeenLocation = clean(lastSeenLocation);
            this.lastSeenDateTime = clean(lastSeenDateTime);
            this.reporterName = clean(reporterName);
            this.relation = clean(relation);
            this.contact = clean(contact);
            this.status = clean(status).isBlank() ? "open" : clean(status);
            this.priority = clean(priority).isBlank() ? "normal" : clean(priority);
            this.createdAt = clean(createdAt);
            this.updatedAt = clean(updatedAt);
        }

        public LostFoundCaseRecord withStatus(String status) {
            return new LostFoundCaseRecord(caseId, type, name, age, gender, clothing, identificationMarks,
                    lastSeenLocation, lastSeenDateTime, reporterName, relation, contact, status, priority,
                    createdAt, String.valueOf(System.currentTimeMillis()));
        }
    }

    public static final class RouteRecord {
        public final String routeId;
        public final String routeName;
        public final String from;
        public final String to;
        public final String via;
        public final String mode;
        public final String startTime;
        public final String endTime;
        public final String fare;
        public final String duration;
        public final String operatorId;
        public final boolean official;
        public final String mapUrl;
        public final String liveSourceUrl;
        public final boolean published;
        public final boolean active;
        public final String createdAt;
        public final String updatedAt;

        public RouteRecord(String routeId, String routeName, String from, String to, String via, String mode,
                String startTime, String endTime, String fare, String duration, String operatorId, boolean official,
                String mapUrl, String liveSourceUrl, boolean published, boolean active, String createdAt,
                String updatedAt) {
            this.routeId = clean(routeId).isBlank() ? randomId("route") : clean(routeId);
            this.routeName = clean(routeName);
            this.from = clean(from);
            this.to = clean(to);
            this.via = clean(via);
            this.mode = clean(mode);
            this.startTime = clean(startTime);
            this.endTime = clean(endTime);
            this.fare = clean(fare);
            this.duration = clean(duration);
            this.operatorId = clean(operatorId);
            this.official = official;
            this.mapUrl = clean(mapUrl);
            this.liveSourceUrl = clean(liveSourceUrl);
            this.published = published;
            this.active = active;
            this.createdAt = clean(createdAt);
            this.updatedAt = clean(updatedAt);
        }

        public RouteRecord withFlags(boolean published, boolean active) {
            return new RouteRecord(routeId, routeName, from, to, via, mode, startTime, endTime, fare, duration,
                    operatorId, official, mapUrl, liveSourceUrl, published, active, createdAt,
                    String.valueOf(System.currentTimeMillis()));
        }
    }

    public record AdminOverview(
            int totalUsers,
            int approvedBusinesses,
            int pendingBusinesses,
            int transportOperators,
            int activeRoutes,
            int todaysBookings,
            int activeEvents,
            int lostFoundOpenCases,
            int activeAnnouncements,
            boolean firebaseConnected,
            String message) {

        public static AdminOverview empty() {
            return empty("");
        }

        public static AdminOverview empty(String message) {
            return new AdminOverview(0, 0, 0, 0, 0, 0, 0, 0, 0, false, message);
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

    private static void loadPujaFirebaseDataIfAvailable(String idToken) {
        if (!firestore.isEnabled()) {
            return;
        }
        AppSession.User current = AppSession.currentUser();
        try {
            List<PujaProviderRecord> loadedProviders = firestore.loadPujaProviders(idToken);
            pujaProviders.clear();
            pujaProviders.addAll(loadedProviders);
        } catch (Exception ignored) {
            // Provider verification can be role-protected.
        }
        try {
            List<PujaServiceRecord> loadedServices;
            if (current != null && "admin".equals(current.role())) {
                loadedServices = firestore.loadPujaServices(idToken);
            } else {
                try {
                    loadedServices = firestore.loadPublicPujaServices(idToken);
                } catch (Exception publicQueryException) {
                    loadedServices = firestore.loadPujaServices(idToken).stream()
                            .filter(PujaServiceRecord::isPubliclyVisible)
                            .toList();
                }
            }
            pujaServices.clear();
            pujaServices.addAll(loadedServices);
            for (PujaServiceRecord service : pujaServices) {
                addUniqueItem(service.toServiceItem());
            }
        } catch (Exception ignored) {
            // Keep the last known Puja services if Firestore is temporarily unavailable.
        }
        try {
            List<PujaBookingRecord> loadedBookings;
            if (current != null && "admin".equals(current.role())) {
                loadedBookings = firestore.loadPujaBookings(idToken);
            } else if (current != null) {
                loadedBookings = firestore.loadPujaBookingsForUser(current.uid(), idToken);
            } else {
                loadedBookings = List.of();
            }
            mergePujaBookingsFromFirestore(loadedBookings);
        } catch (Exception ignored) {
            // Booking access is role-protected.
        }
        try {
            if (current != null && "admin".equals(current.role())) {
                List<FraudReportRecord> loadedReports = firestore.loadFraudReports(idToken);
                fraudReports.clear();
                fraudReports.addAll(loadedReports);
            }
        } catch (Exception ignored) {
            // Fraud reports are admin-only.
        }
    }

    private static void loadFirebaseDataIfAvailable(String idToken) {
        if (!firestore.isEnabled()) {
            return;
        }
        try {
            try {
                List<ServiceItem> remoteItems = firestore.loadItems(idToken);
                if (!remoteItems.isEmpty()) {
                    clearModuleItems();
                    for (ServiceItem item : remoteItems) {
                        items(item.module).add(item);
                        remoteModules.add(item.module);
                    }
                }
            } catch (Exception ignored) {
                // Generic item access can be unavailable for some roles; do not block module-specific data.
            }
            try {
                for (ServiceItem item : firestore.loadPublicModuleItems(idToken)) {
                    addUniqueItem(item);
                    remoteModules.add(item.module);
                }
            } catch (Exception ignored) {
                // Public operational items are optional for pages backed by dedicated collections.
            }
            try {
                for (ServiceItem item : firestore.loadAdminOperationalItems(idToken)) {
                    addUniqueItem(item);
                    remoteModules.add(item.module);
                }
            } catch (Exception ignored) {
                // Admin operational item access must not prevent Puja Firestore sync.
            }

            try {
                users.clear();
                users.addAll(userDao.findAll(idToken));
            } catch (Exception ignored) {
                // Keep the last known user snapshot if user listing is temporarily unavailable.
            }
            try {
                List<FaqRecord> remoteFaqs = canonicalFirestore.loadFaqs(idToken);
                if (!remoteFaqs.isEmpty()) {
                    mergeFaqs(remoteFaqs);
                }
            } catch (Exception ignored) {
                // FAQ defaults remain available if remote FAQ loading is temporarily unavailable.
            }
            try {
                List<BusinessRecord> latestBusinesses = businessDao.findAll(idToken);
                businesses.clear();
                businesses.addAll(latestBusinesses);
            } catch (Exception ignored) {
                // Business registry is optional for general dashboard startup.
            }
            for (BusinessRecord businessRecord : businesses) {
                if (businessRecord.approved
                        && ("approved".equalsIgnoreCase(businessRecord.status)
                                || "active".equalsIgnoreCase(businessRecord.status))) {
                    addUniqueItem(new ServiceItem(businessRecord.businessId, "business",
                            businessRecord.businessName,
                            businessRecord.category + " | " + businessRecord.location + " | "
                                    + businessRecord.description,
                            "Business",
                            businessRecord.coverPhotoUrl.isBlank() ? businessRecord.logoUrl : businessRecord.coverPhotoUrl,
                            businessRecord.coverPhotoPublicId.isBlank() ? businessRecord.logoPublicId : businessRecord.coverPhotoPublicId,
                            businessRecord.galleryImages));
                }
            }
            try {
                transportOperators.clear();
                transportOperators.addAll(operatorDao.findAll(idToken));
            } catch (Exception ignored) {
                // Operators are loaded when permitted; startup should continue without them.
            }
            try {
                List<PujaProviderRecord> loadedProviders = firestore.loadPujaProviders(idToken);
                pujaProviders.clear();
                pujaProviders.addAll(loadedProviders);
            } catch (Exception ignored) {
                // Puja provider verification data is optional until Firestore rules are ready.
            }
            try {
                List<PujaServiceRecord> loadedServices;
                AppSession.User current = AppSession.currentUser();
                if (current != null && "admin".equals(current.role())) {
                    loadedServices = firestore.loadPujaServices(idToken);
                } else {
                    try {
                        loadedServices = firestore.loadPublicPujaServices(idToken);
                    } catch (Exception publicQueryException) {
                        loadedServices = firestore.loadPujaServices(idToken).stream()
                                .filter(PujaServiceRecord::isPubliclyVisible)
                                .toList();
                    }
                }
                pujaServices.clear();
                pujaServices.addAll(loadedServices);
                for (PujaServiceRecord service : pujaServices) {
                    addUniqueItem(service.toServiceItem());
                }
            } catch (Exception ignored) {
                // Puja service records are loaded when permitted.
            }
            try {
                AppSession.User current = AppSession.currentUser();
                List<PujaBookingRecord> loadedBookings;
                if (current != null && "admin".equals(current.role())) {
                    loadedBookings = firestore.loadPujaBookings(idToken);
                } else if (current != null) {
                    loadedBookings = firestore.loadPujaBookingsForUser(current.uid(), idToken);
                } else {
                    loadedBookings = List.of();
                }
                mergePujaBookingsFromFirestore(loadedBookings);
            } catch (Exception ignored) {
                // Puja bookings are role-protected and optional during startup.
            }
            try {
                List<FraudReportRecord> loadedReports = firestore.loadFraudReports(idToken);
                fraudReports.clear();
                fraudReports.addAll(loadedReports);
            } catch (Exception ignored) {
                // Fraud reports are admin-protected and optional during startup.
            }
            try {
                bookings.clear();
                AppSession.User current = AppSession.currentUser();
                if (current != null && "admin".equals(current.role())) {
                    bookings.addAll(bookingDao.findAll(idToken));
                } else if (current != null && "user".equals(current.role())) {
                    bookings.addAll(bookingDao.findByField("userId", current.uid(), idToken));
                } else if (current != null && "business".equals(current.role())) {
                    bookings.addAll(bookingDao.findByField("businessOwnerId", current.uid(), idToken));
                } else if (current != null && "transport_operator".equals(current.role())) {
                    bookings.addAll(bookingDao.findByField("transportOwnerId", current.uid(), idToken));
                }
            } catch (Exception ignored) {
                // Booking collection access can vary by role.
            }
            try {
                lostFoundCases.clear();
                lostFoundCases.addAll(operationalDataDao.loadLostFoundCases(idToken));
            } catch (Exception ignored) {
                // Lost/found is sensitive and can be unavailable for non-admin sessions.
            }
            try {
                transportRoutes.clear();
                transportRoutes.addAll(operationalDataDao.loadTransportRoutes(idToken));
            } catch (Exception ignored) {
                // Route management continues with any cached routes.
            }

            pendingApprovals.clear();
            addUniqueApprovals(approvalDao.findPendingBusinessApprovals(idToken), true);
            addUniqueApprovals(approvalDao.findAll(idToken), false);
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
        packages.clear();
        puja.clear();
        ghats.clear();
        emergency.clear();
        stay.clear();
        lostFound.clear();
        schedule.clear();
        business.clear();
        announcements.clear();
        about.clear();
        remoteModules.clear();
    }

    private static void addUniqueItem(ServiceItem item) {
        List<ServiceItem> target = items(item.module);
        boolean exists = target.stream().anyMatch(existing -> existing.id.equals(item.id)
                || existing.title.equalsIgnoreCase(item.title));
        if (!exists) {
            target.add(item);
        }
    }

    private static void saveBookingIfPossible(BookingRecord booking) {
        try {
            bookingDao.save(booking, businessOwnerIdFor(booking.businessId), currentToken());
        } catch (Exception ignored) {
            // Local cache remains usable if rules/backend own this write path.
        }
    }

    private static String businessOwnerIdFor(String businessId) {
        if (businessId == null || businessId.isBlank()) {
            return "";
        }
        return businesses.stream()
                .filter(businessRecord -> businessId.equals(businessRecord.businessId))
                .map(businessRecord -> businessRecord.ownerId)
                .filter(ownerId -> ownerId != null && !ownerId.isBlank())
                .findFirst()
                .orElse("");
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
            case "Kumbh Packages" -> "packages";
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

    private static String safeMessage(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static void mergePujaBookingsFromFirestore(List<PujaBookingRecord> loadedBookings) {
        List<PujaBookingRecord> localOnlyBookings = pujaBookings.stream()
                .filter(existing -> loadedBookings.stream()
                        .noneMatch(remote -> remote.bookingId.equals(existing.bookingId)))
                .toList();
        pujaBookings.clear();
        pujaBookings.addAll(loadedBookings);
        pujaBookings.addAll(localOnlyBookings);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
