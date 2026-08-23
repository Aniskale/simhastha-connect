package com.simhastha.payment.ui;

import java.util.concurrent.CompletableFuture;

import com.simhastha.payment.CheckoutHandler;
import com.simhastha.payment.PaymentOrder;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.payment.PaymentResult;
import com.simhastha.payment.PaymentStatus;
import com.simhastha.view.ThemeManager;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import netscape.javascript.JSObject;

public final class RazorpayWebViewCheckoutHandler implements CheckoutHandler {

    @Override
    public CompletableFuture<PaymentResult> openCheckout(Window owner, PaymentRequest request, PaymentOrder order) {
        CompletableFuture<PaymentResult> result = new CompletableFuture<>();
        Platform.runLater(() -> showCheckout(owner, request, order, result));
        return result;
    }

    private void showCheckout(Window owner, PaymentRequest request, PaymentOrder order,
            CompletableFuture<PaymentResult> result) {
        Stage dialog = new Stage();
        dialog.setTitle("Secure Payment");
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        WebView webView = new WebView();
        webView.setPrefSize(780, 640);
        Label loading = new Label("Opening secure payment checkout...");
        loading.getStyleClass().add("payment-status-text");

        BorderPane root = new BorderPane(webView);
        root.setTop(loading);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("payment-checkout-root");

        WebEngine engine = webView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("simhasthaPayment", new CheckoutBridge(dialog, order, result));
            } else if (newState == Worker.State.FAILED && !result.isDone()) {
                result.complete(PaymentResult.failed("Secure checkout could not be opened.", "CHECKOUT_LOAD_FAILED"));
                dialog.close();
            }
        });

        dialog.setOnCloseRequest(event -> {
            if (!result.isDone()) {
                result.complete(new PaymentResult(order.internalPaymentId(), order.razorpayOrderId(), "", "",
                        PaymentStatus.CANCELLED, "Payment checkout was cancelled.", "CHECKOUT_CANCELLED"));
            }
        });

        Scene scene = new Scene(root);
        ThemeManager.addTheme(scene, this);
        ThemeManager.addListener(() -> ThemeManager.applyTo(root));
        dialog.setScene(scene);
        engine.loadContent(checkoutHtml(request, order));
        dialog.show();
    }

    private String checkoutHtml(PaymentRequest request, PaymentOrder order) {
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta http-equiv="Content-Security-Policy" content="script-src 'self' 'unsafe-inline' https://checkout.razorpay.com; connect-src https://api.razorpay.com https://checkout.razorpay.com; img-src https: data:; style-src 'unsafe-inline';">
                  <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
                  <style>
                    body { margin: 0; font-family: Segoe UI, Arial, sans-serif; background: #fff8ed; color: #30190d; }
                    .shell { min-height: 100vh; display: grid; place-items: center; text-align: center; padding: 24px; }
                    .title { font-size: 22px; font-weight: 700; color: #7a1712; margin-bottom: 8px; }
                    .detail { font-size: 14px; color: #6a4525; margin-bottom: 18px; }
                    button { border: 0; border-radius: 8px; padding: 12px 18px; background: #b13b14; color: white; font-weight: 700; cursor: pointer; }
                    .muted { margin-top: 14px; font-size: 12px; color: #8a6a4a; }
                  </style>
                </head>
                <body>
                  <div class="shell">
                    <div>
                      <div class="title">Simhastha Connect Secure Payment</div>
                      <div class="detail">Complete the checkout window to continue. Final confirmation will happen after secure verification.</div>
                      <button onclick="openCheckout()">Pay Securely</button>
                      <div class="muted">Do not close this window while payment is processing.</div>
                    </div>
                  </div>
                  <script>
                    function openCheckout() {
                      if (!window.Razorpay) {
                        window.simhasthaPayment.failed('Razorpay checkout is unavailable.', 'RAZORPAY_UNAVAILABLE');
                        return;
                      }
                      var options = {
                        key: '%s',
                        amount: %d,
                        currency: '%s',
                        name: 'Simhastha Connect',
                        description: '%s',
                        order_id: '%s',
                        prefill: { name: '%s', email: '%s', contact: '%s' },
                        notes: { bookingId: '%s', moduleType: '%s', internalPaymentId: '%s' },
                        modal: { ondismiss: function() { window.simhasthaPayment.cancelled(); } },
                        handler: function(response) {
                          window.simhasthaPayment.success(
                            response.razorpay_payment_id || '',
                            response.razorpay_order_id || '',
                            response.razorpay_signature || ''
                          );
                        },
                        theme: { color: '#b13b14' }
                      };
                      var checkout = new Razorpay(options);
                      checkout.on('payment.failed', function(response) {
                        var error = response && response.error ? response.error : {};
                        window.simhasthaPayment.failed(error.description || 'Payment failed.', error.code || 'PAYMENT_FAILED');
                      });
                      checkout.open();
                    }
                    window.onload = openCheckout;
                  </script>
                </body>
                </html>
                """.formatted(
                escapeJs(order.keyId()),
                order.amount(),
                escapeJs(order.currency()),
                escapeJs(request.title()),
                escapeJs(order.razorpayOrderId()),
                escapeJs(request.customerName()),
                escapeJs(request.customerEmail()),
                escapeJs(request.customerPhone()),
                escapeJs(request.bookingId()),
                escapeJs(request.moduleType().name()),
                escapeJs(order.internalPaymentId()));
    }

    private String escapeJs(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
    }

    public static final class CheckoutBridge {
        private final Stage dialog;
        private final PaymentOrder order;
        private final CompletableFuture<PaymentResult> result;

        private CheckoutBridge(Stage dialog, PaymentOrder order, CompletableFuture<PaymentResult> result) {
            this.dialog = dialog;
            this.order = order;
            this.result = result;
        }

        public void success(String paymentId, String orderId, String signature) {
            if (!result.isDone()) {
                result.complete(new PaymentResult(order.internalPaymentId(), valueOr(orderId, order.razorpayOrderId()),
                        paymentId, signature, PaymentStatus.AUTHORIZED,
                        "Payment callback received. Awaiting secure verification.", ""));
            }
            Platform.runLater(dialog::close);
        }

        public void cancelled() {
            if (!result.isDone()) {
                result.complete(new PaymentResult(order.internalPaymentId(), order.razorpayOrderId(), "", "",
                        PaymentStatus.CANCELLED, "Payment checkout was cancelled.", "CHECKOUT_CANCELLED"));
            }
            Platform.runLater(dialog::close);
        }

        public void failed(String message, String errorCode) {
            if (!result.isDone()) {
                result.complete(new PaymentResult(order.internalPaymentId(), order.razorpayOrderId(), "", "",
                        PaymentStatus.FAILED, valueOr(message, "Payment failed."), valueOr(errorCode, "PAYMENT_FAILED")));
            }
            Platform.runLater(dialog::close);
        }

        private String valueOr(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }
}
