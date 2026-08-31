package com.simhastha.payment.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import com.simhastha.payment.CheckoutHandler;
import com.simhastha.payment.PaymentOrder;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.payment.PaymentResult;
import com.simhastha.payment.PaymentStatus;
import com.simhastha.payment.api.PaymentJson;
import com.simhastha.view.ThemeManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

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

        Label title = new Label("Complete the secure payment in your browser");
        title.getStyleClass().add("payment-title");
        Label status = new Label("Opening Razorpay Checkout...");
        status.getStyleClass().add("payment-status-text");
        status.setWrapText(true);
        Hyperlink checkoutLink = new Hyperlink();
        checkoutLink.setVisible(false);
        checkoutLink.setManaged(false);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("back-button");
        HBox actions = new HBox(10, cancel);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, title, status, checkoutLink, actions);
        root.setPadding(new Insets(20));
        root.setPrefWidth(430);
        root.getStyleClass().add("payment-dialog");

        dialog.setOnCloseRequest(event -> {
            if (!result.isDone()) {
                result.complete(new PaymentResult(order.internalPaymentId(), order.razorpayOrderId(), "", "",
                        PaymentStatus.CANCELLED, "Payment checkout was cancelled.", "CHECKOUT_CANCELLED"));
            }
        });
        cancel.setOnAction(event -> {
            if (!result.isDone()) {
                result.complete(new PaymentResult(order.internalPaymentId(), order.razorpayOrderId(), "", "",
                        PaymentStatus.CANCELLED, "Payment checkout was cancelled.", "CHECKOUT_CANCELLED"));
            }
            dialog.close();
        });

        Scene scene = new Scene(root);
        ThemeManager.addTheme(scene, this);
        ThemeManager.addListener(() -> ThemeManager.applyTo(root));
        dialog.setScene(scene);
        dialog.show();

        try {
            BrowserCheckoutServer checkoutServer = new BrowserCheckoutServer(request, order, result, dialog);
            URI checkoutUri = checkoutServer.start();
            result.whenComplete((paymentResult, throwable) -> checkoutServer.stop());
            checkoutLink.setText(checkoutUri.toString());
            checkoutLink.setOnAction(event -> openBrowser(checkoutUri, result, dialog));
            openBrowser(checkoutUri, result, dialog);
            status.setText("Razorpay Checkout opened in your browser. Keep this window open until payment finishes.");
            checkoutLink.setVisible(true);
            checkoutLink.setManaged(true);
        } catch (IOException exception) {
            result.complete(PaymentResult.failed("Secure checkout could not be opened.", "CHECKOUT_LOAD_FAILED"));
            dialog.close();
        }
    }

    private void openBrowser(URI checkoutUri, CompletableFuture<PaymentResult> result, Stage dialog) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IOException("Desktop browser is unavailable.");
            }
            Desktop.getDesktop().browse(checkoutUri);
        } catch (IOException | RuntimeException exception) {
            if (!result.isDone()) {
                result.complete(PaymentResult.failed("Secure checkout could not be opened.", "CHECKOUT_BROWSER_FAILED"));
            }
            dialog.close();
        }
    }

    private String checkoutHtml(PaymentRequest request, PaymentOrder order, String token) {
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
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
                      <div class="detail" id="status">Complete checkout to continue. Final confirmation will happen after secure verification.</div>
                      <button onclick="openCheckout()">Pay Securely</button>
                      <div class="muted">Do not close the Simhastha Connect payment window while payment is processing.</div>
                    </div>
                  </div>
                  <script>
                    var callbackToken = '%s';
                    function notifyApp(path, payload) {
                      return fetch(path + '?token=' + encodeURIComponent(callbackToken), {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload || {})
                      }).then(function() {
                        document.getElementById('status').textContent = 'Payment response received. You can return to Simhastha Connect.';
                      }).catch(function() {
                        document.getElementById('status').textContent = 'Payment response could not reach Simhastha Connect. Please keep this page open and try again.';
                      });
                    }
                    function openCheckout() {
                      if (!window.Razorpay) {
                        notifyApp('/failure', { message: 'Razorpay checkout is unavailable.', code: 'RAZORPAY_UNAVAILABLE' });
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
                        modal: { ondismiss: function() { notifyApp('/cancelled', {}); } },
                        handler: function(response) {
                          notifyApp('/callback', {
                            razorpay_payment_id: response.razorpay_payment_id || '',
                            razorpay_order_id: response.razorpay_order_id || '',
                            razorpay_signature: response.razorpay_signature || ''
                          });
                        },
                        theme: { color: '#b13b14' }
                      };
                      var checkout = new Razorpay(options);
                      checkout.on('payment.failed', function(response) {
                        var error = response && response.error ? response.error : {};
                        notifyApp('/failure', {
                          message: error.description || 'Payment failed.',
                          code: error.code || 'PAYMENT_FAILED'
                        });
                      });
                      checkout.open();
                    }
                    window.onload = openCheckout;
                  </script>
                </body>
                </html>
                """.formatted(
                escapeJs(token),
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
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\r", " ").replace("\n", " ");
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private final class BrowserCheckoutServer {
        private final PaymentRequest request;
        private final PaymentOrder order;
        private final CompletableFuture<PaymentResult> result;
        private final Stage dialog;
        private final String token = UUID.randomUUID().toString();
        private HttpServer server;

        private BrowserCheckoutServer(PaymentRequest request, PaymentOrder order, CompletableFuture<PaymentResult> result,
                Stage dialog) {
            this.request = request;
            this.order = order;
            this.result = result;
            this.dialog = dialog;
        }

        private URI start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/checkout", this::handleCheckout);
            server.createContext("/callback", this::handleCallback);
            server.createContext("/cancelled", this::handleCancelled);
            server.createContext("/failure", this::handleFailure);
            server.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "simhastha-razorpay-checkout");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/checkout");
        }

        private void stop() {
            if (server != null) {
                server.stop(0);
            }
        }

        private void handleCheckout(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "Method not allowed.", "text/plain");
                return;
            }
            send(exchange, 200, checkoutHtml(request, order, token), "text/html; charset=utf-8");
        }

        private void handleCallback(HttpExchange exchange) throws IOException {
            if (!isValidPost(exchange)) {
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String paymentId = PaymentJson.value(body, "razorpay_payment_id");
            String orderId = PaymentJson.value(body, "razorpay_order_id");
            String signature = PaymentJson.value(body, "razorpay_signature");
            send(exchange, 200, "Payment response received. Return to Simhastha Connect.", "text/plain");
            if (!result.isDone()) {
                result.complete(new PaymentResult(order.internalPaymentId(), valueOr(orderId, order.razorpayOrderId()),
                        paymentId, signature, PaymentStatus.AUTHORIZED,
                        "Payment callback received. Awaiting secure verification.", ""));
            }
            Platform.runLater(dialog::close);
        }

        private void handleCancelled(HttpExchange exchange) throws IOException {
            if (!isValidPost(exchange)) {
                return;
            }
            send(exchange, 200, "Payment cancelled. Return to Simhastha Connect.", "text/plain");
            if (!result.isDone()) {
                result.complete(new PaymentResult(order.internalPaymentId(), order.razorpayOrderId(), "", "",
                        PaymentStatus.CANCELLED, "Payment checkout was cancelled.", "CHECKOUT_CANCELLED"));
            }
            Platform.runLater(dialog::close);
        }

        private void handleFailure(HttpExchange exchange) throws IOException {
            if (!isValidPost(exchange)) {
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String message = valueOr(PaymentJson.value(body, "message"), "Payment failed.");
            String code = valueOr(PaymentJson.value(body, "code"), "PAYMENT_FAILED");
            send(exchange, 200, escapeHtml(message), "text/plain");
            if (!result.isDone()) {
                result.complete(new PaymentResult(order.internalPaymentId(), order.razorpayOrderId(), "", "",
                        PaymentStatus.FAILED, message, code));
            }
            Platform.runLater(dialog::close);
        }

        private boolean isValidPost(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "Method not allowed.", "text/plain");
                return false;
            }
            if (!("token=" + token).equals(exchange.getRequestURI().getRawQuery())) {
                send(exchange, 403, "Forbidden.", "text/plain");
                return false;
            }
            return true;
        }

        private void send(HttpExchange exchange, int status, String body, String contentType) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }

        private String valueOr(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }
}
