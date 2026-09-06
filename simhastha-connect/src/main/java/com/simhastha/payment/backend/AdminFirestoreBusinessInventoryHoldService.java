package com.simhastha.payment.backend;

import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentModuleType;
import com.simhastha.payment.PaymentRecord;
import com.simhastha.payment.PaymentRequest;

/** Admin-SDK-only transactional inventory lifecycle for business bookings. */
public final class AdminFirestoreBusinessInventoryHoldService implements BusinessInventoryHoldService {
    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);
    private final Firestore db;

    public AdminFirestoreBusinessInventoryHoldService(Firestore db) { this.db = db; }

    @Override public void createHold(PaymentRequest request) throws PaymentException {
        if (request.moduleType() != PaymentModuleType.BUSINESS) return;
        run(transaction -> {
            DocumentReference hold = db.collection("bookingHolds").document(request.bookingId());
            DocumentSnapshot existing = transaction.get(hold).get();
            if (existing.exists() && !expired(existing)) return null;
            int quantity = positive(request.metadata().getOrDefault("quantity", "1"));
            DocumentReference item = db.collection("businessItems").document(request.itemId());
            DocumentSnapshot inventory = transaction.get(item).get();
            if (!inventory.exists() || !request.businessId().equals(text(inventory, "businessId"))) {
                throw new PaymentException("The selected business unit is no longer available.");
            }
            int expiredQuantity = existing.exists() && request.itemId().equals(text(existing, "itemId"))
                    ? units(existing, "quantity") : 0;
            int available = units(inventory, "availableUnits") + expiredQuantity;
            if (available < quantity) throw new PaymentException("No longer available. Please select another unit.");
            Map<String, Object> inventoryUpdate = new LinkedHashMap<>();
            inventoryUpdate.put("availableUnits", String.valueOf(available - quantity));
            inventoryUpdate.put("heldUnits", String.valueOf(Math.max(0, units(inventory, "heldUnits") - expiredQuantity) + quantity));
            inventoryUpdate.put("updatedAt", String.valueOf(System.currentTimeMillis()));
            transaction.update(item, inventoryUpdate);
            Date expires = new Date(System.currentTimeMillis() + HOLD_DURATION.toMillis());
            transaction.set(hold, Map.of("bookingId", request.bookingId(), "userId", request.userId(),
                    "businessId", request.businessId(), "itemId", request.itemId(), "quantity", quantity,
                    "expiresAt", expires, "createdAt", new Date()));
            Map<String, Object> booking = new LinkedHashMap<>();
            booking.put("bookingId", request.bookingId()); booking.put("userId", request.userId());
            booking.put("businessId", request.businessId()); booking.put("businessOwnerId", text(inventory, "ownerId"));
            booking.put("moduleType", request.moduleType().name()); booking.put("customerName", request.customerName());
            booking.put("catalogItemId", request.itemId()); booking.put("title", request.title());
            booking.put("bookingDate", request.metadata().getOrDefault("bookingDate", ""));
            booking.put("location", request.metadata().getOrDefault("location", ""));
            booking.put("quantity", quantity); booking.put("nights", positive(request.metadata().getOrDefault("nights", "1")));
            booking.put("amountPaise", request.amount().movePointRight(2).longValueExact()); booking.put("currency", request.currency());
            booking.put("bookingStatus", "PENDING_PAYMENT"); booking.put("paymentStatus", "PENDING");
            booking.put("holdExpiresAt", expires); booking.put("createdAt", new Date()); booking.put("updatedAt", new Date());
            transaction.set(db.collection("bookings").document(request.bookingId()), booking);
            return null;
        });
    }

    @Override public void confirmHold(PaymentRecord payment) throws PaymentException {
        if (payment.moduleType() != PaymentModuleType.BUSINESS) return;
        run(transaction -> {
            DocumentReference booking = db.collection("bookings").document(payment.bookingId());
            DocumentSnapshot bookingSnapshot = transaction.get(booking).get();
            if ("CONFIRMED".equals(text(bookingSnapshot, "bookingStatus"))) return null;
            DocumentReference hold = db.collection("bookingHolds").document(payment.bookingId());
            DocumentSnapshot held = transaction.get(hold).get();
            if (!held.exists() || expired(held)) throw new PaymentException("Booking hold expired before payment confirmation.");
            int quantity = units(held, "quantity");
            DocumentReference item = db.collection("businessItems").document(text(held, "itemId"));
            DocumentSnapshot inventory = transaction.get(item).get();
            transaction.update(item, Map.of("heldUnits", String.valueOf(Math.max(0, units(inventory, "heldUnits") - quantity)),
                    "bookedUnits", String.valueOf(units(inventory, "bookedUnits") + quantity), "updatedAt", String.valueOf(System.currentTimeMillis())));
            String ticketId = "TKT-" + payment.bookingId() + "-" + UUID.randomUUID().toString().substring(0, 8);
            String token = UUID.randomUUID().toString().replace("-", "");
            transaction.update(booking, Map.of("bookingStatus", "CONFIRMED", "paymentStatus", "VERIFIED",
                    "internalPaymentId", payment.internalPaymentId(), "razorpayPaymentId", payment.razorpayPaymentId(),
                    "razorpayOrderId", payment.razorpayOrderId(), "qrTicketId", ticketId, "qrVerificationToken", token,
                    "updatedAt", new Date()));
            transaction.set(db.collection("tickets").document(ticketId), Map.of("ticketId", ticketId, "bookingId", payment.bookingId(),
                    "userId", payment.userId(), "businessId", payment.businessId(), "qrVerificationToken", token,
                    "status", "CONFIRMED", "createdAt", new Date()));
            transaction.delete(hold); return null;
        });
    }

    @Override public void releaseHold(PaymentRecord payment, String reason) throws PaymentException { release(payment.bookingId(), reason); }
    @Override public void releaseHold(PaymentRequest request, String reason) throws PaymentException { release(request.bookingId(), reason); }
    @Override public void releaseExpiredHolds() throws PaymentException {
        try { for (QueryDocumentSnapshot hold : db.collection("bookingHolds").whereLessThan("expiresAt", Timestamp.now()).limit(100).get().get().getDocuments()) release(hold.getId(), "Inventory hold expired."); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new PaymentException("Hold cleanup interrupted.", e); }
        catch (ExecutionException e) { throw new PaymentException("Hold cleanup failed.", e); }
    }
    private void release(String bookingId, String reason) throws PaymentException { run(transaction -> {
        DocumentReference hold = db.collection("bookingHolds").document(bookingId); DocumentSnapshot held = transaction.get(hold).get(); if (!held.exists()) return null;
        int quantity = units(held, "quantity"); DocumentReference item = db.collection("businessItems").document(text(held, "itemId")); DocumentSnapshot inventory = transaction.get(item).get();
        transaction.update(item, Map.of("availableUnits", String.valueOf(units(inventory, "availableUnits") + quantity), "heldUnits", String.valueOf(Math.max(0, units(inventory, "heldUnits") - quantity)), "updatedAt", String.valueOf(System.currentTimeMillis())));
        transaction.update(db.collection("bookings").document(bookingId), Map.of("bookingStatus", reason.contains("expired") ? "EXPIRED" : "CANCELLED", "paymentStatus", reason.contains("failed") ? "FAILED" : "PENDING", "updatedAt", new Date())); transaction.delete(hold); return null; }); }
    private <T> T run(com.google.cloud.firestore.Transaction.Function<T> work) throws PaymentException { try { return db.runTransaction(work).get(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new PaymentException("Inventory transaction interrupted.", e); } catch (ExecutionException e) { Throwable cause=e.getCause(); if(cause instanceof PaymentException p) throw p; throw new PaymentException("Inventory transaction failed.", cause); } }
    private boolean expired(DocumentSnapshot doc) { Object value=doc.get("expiresAt"); return value instanceof Timestamp stamp && stamp.toDate().before(new Date()); }
    private int units(DocumentSnapshot doc, String key) { return positive(String.valueOf(doc.get(key) == null ? "0" : doc.get(key))); }
    private int positive(String value) { try { return Math.max(0, Integer.parseInt(value)); } catch (NumberFormatException e) { return 0; } }
    private String text(DocumentSnapshot doc, String key) { Object value=doc.get(key); return value == null ? "" : String.valueOf(value); }
}
