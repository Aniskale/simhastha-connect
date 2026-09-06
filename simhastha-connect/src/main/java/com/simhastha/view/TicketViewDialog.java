package com.simhastha.view;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class TicketViewDialog {

    private TicketViewDialog() {
    }

    public static void show(AppDataStore.TicketRecord ticket) {
        Stage stage = new Stage();
        stage.setTitle("Simhastha Connect E-Ticket");
        VBox ticketNode = ticketNode(ticket);
        Button print = new Button("Print");
        print.getStyleClass().add("primary-button");
        print.setOnAction(event -> print(ticketNode));
        VBox root = new VBox(12, ticketNode, print);
        root.getStyleClass().add("ticket-dialog");
        Scene scene = new Scene(root, 520, 650);
        java.net.URL css = AppResources.url(TicketViewDialog.class, "/css/simhastha-theme.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        ThemeManager.applyTo(root);
        stage.setScene(scene);
        stage.show();
    }

    private static VBox ticketNode(AppDataStore.TicketRecord ticket) {
        VBox details = new VBox(7,
                row("Ticket ID", ticket.ticketId),
                row("Booking ID", ticket.bookingId),
                row("Service", ticket.title),
                row("Module", ticket.moduleType),
                row("Customer", ticket.customerName),
                row("Date", ticket.dateText),
                row("Location", ticket.location),
                row("Amount", formatAmount(ticket.amountPaise)),
                row("Payment", "PAID"),
                row("Status", ticket.status),
                row("Issued", issued(ticket.issuedAt)));
        VBox ticketNode = new VBox(12,
                label("SIMHASTHA CONNECT", "ticket-brand"),
                label("E-Ticket / Booking Pass", "ticket-title"),
                details,
                qrPattern(ticket.qrVerificationReference),
                label("QR Ref: " + ticket.qrVerificationReference, "ticket-small"));
        ticketNode.getStyleClass().add("ticket-card");
        return ticketNode;
    }

    private static HBox row(String name, String value) {
        Label left = label(name, "ticket-key");
        Label right = label(value == null ? "" : value, "ticket-value");
        HBox row = new HBox(10, left, AppUi.spacer(), right);
        return row;
    }

    private static GridPane qrPattern(String payload) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("ticket-qr");
        int hash = payload == null ? 0 : payload.hashCode();
        for (int row = 0; row < 21; row++) {
            for (int col = 0; col < 21; col++) {
                StackPane cell = new StackPane();
                boolean finder = (row < 6 && col < 6) || (row < 6 && col > 14) || (row > 14 && col < 6);
                boolean filled = finder || (((hash >> Math.floorMod(row + col, 16)) ^ (row * 31 + col * 17)) & 1) == 1;
                cell.getStyleClass().add(filled ? "ticket-qr-cell-on" : "ticket-qr-cell-off");
                cell.setPrefSize(7, 7);
                grid.add(cell, col, row);
            }
        }
        return grid;
    }

    private static void print(Node node) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(node.getScene().getWindow()) && job.printPage(node)) {
            job.endJob();
        }
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private static String formatAmount(long paise) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(paise / 100.0);
    }

    private static String issued(String millis) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(millis)).atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
        } catch (RuntimeException exception) {
            return millis;
        }
    }
}
