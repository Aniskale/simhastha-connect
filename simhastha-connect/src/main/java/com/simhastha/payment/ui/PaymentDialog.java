package com.simhastha.payment.ui;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.simhastha.payment.PaymentResult;
import com.simhastha.payment.PaymentService;
import com.simhastha.payment.PaymentStatus;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.view.ThemeManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class PaymentDialog {

    private final PaymentService paymentService;

    public PaymentDialog(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public CompletableFuture<PaymentResult> show(Window owner, PaymentRequest request) {
        CompletableFuture<PaymentResult> finalResult = new CompletableFuture<>();
        Stage dialog = new Stage();
        dialog.setTitle("Payment");
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        Label title = label(request.title(), "payment-title");
        Label booking = label("Booking: " + request.bookingId(), "payment-detail");
        Label amount = label("Total payable: " + formatAmount(request), "payment-amount");
        Label customer = label(request.customerName() + customerContact(request), "payment-detail");
        Label status = label("Payment will be verified securely before booking confirmation.", "payment-status-text");

        Button pay = new Button("Pay Securely");
        pay.getStyleClass().add("primary-button");
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("back-button");

        pay.setOnAction(event -> {
            pay.setDisable(true);
            cancel.setDisable(true);
            status.setText("Creating secure payment order...");
            paymentService.startPayment(dialog, request).whenComplete((result, throwable) -> Platform.runLater(() -> {
                PaymentResult resolved = result == null
                        ? PaymentResult.failed("Payment could not be completed.", "PAYMENT_ERROR")
                        : result;
                status.setText(resolved.message());
                finalResult.complete(resolved);
                if (resolved.paymentStatus() == PaymentStatus.PAID
                        || resolved.paymentStatus() == PaymentStatus.VERIFIED
                        || resolved.paymentStatus() == PaymentStatus.PENDING
                        || resolved.paymentStatus() == PaymentStatus.CANCELLED) {
                    dialog.close();
                } else {
                    cancel.setDisable(false);
                    pay.setDisable(false);
                }
            }));
        });

        cancel.setOnAction(event -> {
            PaymentResult cancelled = PaymentResult.cancelled("Payment was cancelled before checkout.");
            finalResult.complete(cancelled);
            dialog.close();
        });

        HBox actions = new HBox(10, pay, cancel);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox content = new VBox(10, title, booking, amount, customer, status, actions);
        content.getStyleClass().add("payment-dialog");
        content.setPadding(new Insets(20));
        content.setPrefWidth(430);

        dialog.setOnCloseRequest(event -> {
            if (!finalResult.isDone()) {
                finalResult.complete(PaymentResult.cancelled("Payment dialog was closed."));
            }
        });
        Scene scene = new Scene(content);
        ThemeManager.addTheme(scene, this);
        ThemeManager.addListener(() -> ThemeManager.applyTo(content));
        dialog.setScene(scene);
        dialog.show();
        return finalResult;
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private String formatAmount(PaymentRequest request) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return format.format(request.amount());
    }

    private String customerContact(PaymentRequest request) {
        if (!request.customerPhone().isBlank()) {
            return " | " + request.customerPhone();
        }
        if (!request.customerEmail().isBlank()) {
            return " | " + request.customerEmail();
        }
        return "";
    }
}
