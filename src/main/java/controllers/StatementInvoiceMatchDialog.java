package controllers;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import models.DocumentAiModels.StatementInvoice;
import models.ScannedInvoice;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Explicit choice of a saved invoice; cancellation leaves the statement unchanged. */
final class StatementInvoiceMatchDialog extends Dialog<StatementInvoice> {
    StatementInvoiceMatchDialog(ScannedInvoice statement, List<StatementInvoice> candidates,
            Function<StatementInvoice, String> problem) {
        setTitle("Choose matching invoice");
        setHeaderText(statement.supplierName() + " · Statement reference " + statement.invoiceNo());
        setResizable(true);
        var currency = NumberFormat.getCurrencyInstance(Locale.US);
        var date = DateTimeFormatter.ofPattern("dd/MM/uuuu");
        var summary = new Label("Statement date: " + (statement.invoiceDate() == null ? "Unavailable" : date.format(statement.invoiceDate()))
                + "    Amount: " + currency.format(statement.amount())
                + "\nChoose the saved " + statement.documentType().name().toLowerCase(Locale.ROOT) + " that belongs to this statement row.");
        summary.setWrapText(true);
        var table = new TableView<StatementInvoice>();
        table.setId("statement-match-candidates");
        table.getItems().setAll(candidates);
        table.setPrefSize(900, 250);
        table.setPlaceholder(new Label("No eligible saved invoices or credits were found. Reopen the statement after correcting its source records."));
        table.getColumns().add(column("Saved reference", 145, row -> row.values().invoiceNo()));
        table.getColumns().add(column("Date", 100, row -> row.values().invoiceDate() == null ? "Unavailable" : date.format(row.values().invoiceDate())));
        table.getColumns().add(column("Saved amount", 110, row -> currency.format(row.values().amount())));
        table.getColumns().add(column("Difference", 100, row -> currency.format((row.values().amountCents() - statement.amountCents()) / 100.0)));
        table.getColumns().add(column("Source PDF", 170, row -> row.documentId() > 0 ? row.values().sourceFile() : "No linked PDF"));
        table.getColumns().add(column("Availability", 250, row -> problem.apply(row).isEmpty() ? "Available" : problem.apply(row)));
        var explanation = new Label(candidates.isEmpty()
                ? "A repeated statement row can also cause this warning. Check the statement PDF before rescanning it."
                : "Select a row to review the match. Your choice is saved when you complete the statement.");
        explanation.setId("statement-match-explanation");
        explanation.setWrapText(true);
        var details = new Label(); details.setWrapText(true);
        var content = new VBox(12, summary, table, details, explanation);
        content.setPadding(new Insets(12));
        getDialogPane().setContent(content);
        var use = new ButtonType("Use selected invoice", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(use, ButtonType.CANCEL);
        var confirm = getDialogPane().lookupButton(use);
        confirm.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((_, _, selected) -> {
            String reason = selected == null ? "Select an invoice first." : problem.apply(selected);
            confirm.setDisable(selected == null || !reason.isEmpty());
            explanation.setText(reason.isEmpty() ? "Review this invoice after selecting it. Complete the statement to save the match." : reason);
            details.setText(selected == null ? "" : "Notes: " + (selected.notes().isBlank() ? "None" : selected.notes()));
        });
        setResultConverter(button -> button == use ? table.getSelectionModel().getSelectedItem() : null);
    }

    private static TableColumn<StatementInvoice, String> column(String title, double width, Function<StatementInvoice, String> value) {
        var column = new TableColumn<StatementInvoice, String>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setCellFactory(_ -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTooltip(empty || item == null ? null : new Tooltip(item));
            }
        });
        return column;
    }
}
