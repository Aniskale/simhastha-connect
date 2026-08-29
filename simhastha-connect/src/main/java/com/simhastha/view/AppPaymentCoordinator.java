package com.simhastha.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.simhastha.payment.MoneyUtil;
import com.simhastha.payment.PaymentCatalog;
import com.simhastha.payment.PaymentCatalog.CatalogItem;
import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.payment.PaymentResult;
import com.simhastha.payment.PaymentStatus;
import com.simhastha.payment.PaymentType;
import com.simhastha.payment.ui.PaymentDialog;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;

public final class AppPaymentCoordinator {

    private final TicketService ticketService = new TicketService();
    private final BookingNotificationService notificationService = new NoOpBookingNotificationService();

    public void startPaidBooking(Window owner, String catalogItemId, int quantity, int nights) {
        AppSession.User user = AppSession.currentUser();
        if (user == null) {
            showInfo("Login required", "Please login before creating a booking.");
            return;
        }

        CatalogItem item = PaymentCatalog.find(catalogItemId).orElse(null);
        if (item == null) {
            showInfo("Unavailable", "This service is not available for online booking.");
            return;
        }
        if (!item.paymentRequired() || item.paymentType() == PaymentType.PAY_AT_LOCATION) {
            createPayAtLocationBooking(user, item, quantity, nights);
            return;
        }

        try {
            BigDecimal amount = item.amount().multiply(BigDecimal.valueOf(Math.max(1, quantity)))
                    .multiply(BigDecimal.valueOf(item.multipliesByNights() ? Math.max(1, nights) : 1));
            String bookingId = "BKG-" + item.moduleType().name() + "-" + UUID.randomUUID().toString().substring(0, 8);
            long amountPaise = MoneyUtil.toSmallestUnit(amount, item.currency());
            AppDataStore.BookingRecord booking = AppDataStore.addBooking(new AppDataStore.BookingRecord(bookingId,
                    user.uid(), item.moduleType().name(), item.id(), item.businessId(), item.title(),
                    displayName(user), LocalDate.now().plusDays(1).toString(), item.location(), quantity, nights,
                    amountPaise, item.currency(), "PENDING_PAYMENT", "PENDING", "", ""));

            PaymentRequest request = PaymentRequest.builder()
                    .userId(user.uid())
                    .bookingId(bookingId)
                    .moduleType(item.moduleType())
                    .itemId(item.id())
                    .businessId(item.businessId())
                    .title(item.title())
                    .description(item.description())
                    .amount(amount)
                    .currency(item.currency())
                    .customerName(displayName(user))
                    .customerEmail(user.email())
                    .metadata(PaymentCatalog.META_CATALOG_ITEM_ID, item.id())
                    .metadata(PaymentCatalog.META_QUANTITY, String.valueOf(Math.max(1, quantity)))
                    .metadata(PaymentCatalog.META_NIGHTS, String.valueOf(Math.max(1, nights)))
                    .metadata(PaymentCatalog.META_BOOKING_DATE, booking.dateText)
                    .metadata(PaymentCatalog.META_LOCATION, item.location())
                    .build();

            AppDataStore.updateBookingStatus(bookingId, "PAYMENT_PROCESSING", "PENDING");
            new PaymentDialog(PaymentServiceFactory.get()).show(owner, request)
                    .whenComplete((result, throwable) -> Platform.runLater(() -> handlePaymentResult(booking, result)));
        } catch (PaymentException exception) {
            showInfo("Payment unavailable", exception.getMessage());
        }
    }

    private void handlePaymentResult(AppDataStore.BookingRecord booking, PaymentResult result) {
        if (result == null) {
            AppDataStore.updateBookingStatus(booking.bookingId, "PAYMENT_PROCESSING", "PENDING");
            showInfo("Payment processing", "Payment is being verified. Please do not pay again.");
            return;
        }
        if (result.paymentStatus() == PaymentStatus.PAID || result.paymentStatus() == PaymentStatus.VERIFIED) {
            AppDataStore.updateBookingStatus(booking.bookingId, "CONFIRMED", "PAID");
            AppDataStore.BookingRecord confirmed = new AppDataStore.BookingRecord(booking.bookingId, booking.userId,
                    booking.moduleType, booking.catalogItemId, booking.businessId, booking.title, booking.customerName,
                    booking.dateText, booking.location, booking.quantity, booking.nights, booking.amountPaise,
                    booking.currency, "CONFIRMED", "PAID", result.internalPaymentId(), result.razorpayPaymentId());
            AppDataStore.bookings().remove(booking);
            AppDataStore.addBooking(confirmed);
            AppDataStore.TicketRecord ticket = ticketService.issueTicket(confirmed);
            notificationService.bookingConfirmed(confirmed, ticket);
            TicketViewDialog.show(ticket);
        } else if (result.paymentStatus() == PaymentStatus.CANCELLED) {
            AppDataStore.updateBookingStatus(booking.bookingId, "CANCELLED", "CANCELLED");
            showInfo("Payment cancelled", "Your booking hold was cancelled. No ticket was issued.");
        } else if (result.paymentStatus() == PaymentStatus.FAILED) {
            AppDataStore.updateBookingStatus(booking.bookingId, "PAYMENT_FAILED", "FAILED");
            showInfo("Payment failed", result.message());
        } else {
            AppDataStore.updateBookingStatus(booking.bookingId, "PAYMENT_PROCESSING", "PENDING");
            showInfo("Payment processing", "Payment is being verified. Please do not pay again.");
        }
    }

    private void createPayAtLocationBooking(AppSession.User user, CatalogItem item, int quantity, int nights) {
        String bookingId = "BKG-" + item.moduleType().name() + "-" + UUID.randomUUID().toString().substring(0, 8);
        AppDataStore.addBooking(new AppDataStore.BookingRecord(bookingId, user.uid(), item.moduleType().name(),
                item.id(), item.businessId(), item.title(), displayName(user), LocalDate.now().plusDays(1).toString(),
                item.location(), quantity, nights, 0, item.currency(), "PAY_AT_LOCATION", "NOT_REQUIRED", "", ""));
        showInfo("Booking request saved", "This service is pay-at-location. No online payment was created.");
    }

    private String displayName(AppSession.User user) {
        if (user.displayName() != null && !user.displayName().isBlank()) {
            return user.displayName();
        }
        return user.email() == null || user.email().isBlank() ? "Simhastha pilgrim" : user.email();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
