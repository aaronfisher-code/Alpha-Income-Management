package components.layouts;

import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;

public class BootstrapPane extends GridPane {

    private final List<BootstrapRow> rows = new ArrayList<>();
    private Breakpoint currentWindowSize = Breakpoint.XSMALL;
    private double requestedHgap;
    private double requestedVgap;
    private boolean normalizingGaps;

    public BootstrapPane() {
        super();
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setAlignment(Pos.TOP_CENTER);
        setColumnConstraints();
        installGapHandling();
        setWidthEventHandlers();
    }

    public BootstrapPane(MFXScrollPane p) {
        super();
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setAlignment(Pos.TOP_CENTER);
        setColumnConstraints();
        installGapHandling();
        setWidthEventHandlers(p);
        this.setPrefHeight(p.getHeight());
    }

    private void installGapHandling() {
        hgapProperty().addListener((_, _, newValue) -> {
            if (normalizingGaps) return;
            requestedHgap = Math.max(0, newValue.doubleValue());
            normalizeGaps();
        });
        vgapProperty().addListener((_, _, newValue) -> {
            if (normalizingGaps) return;
            requestedVgap = Math.max(0, newValue.doubleValue());
            normalizeGaps();
        });
    }

    /**
     * JavaFX 26 no longer includes gaps for the empty columns inside a
     * column-spanning child. Bootstrap's 12-column layout relies on those
     * gaps being part of each span, so represent them as half-margins while
     * leaving the actual grid gap at zero.
     */
    private void normalizeGaps() {
        normalizingGaps = true;
        try {
            setHgap(0);
            setVgap(0);
        } finally {
            normalizingGaps = false;
        }
        Insets margin = new Insets(
                requestedVgap / 2,
                requestedHgap / 2,
                requestedVgap / 2,
                requestedHgap / 2
        );
        for (Node child : getChildren()) {
            GridPane.setMargin(child, margin);
        }
    }

    private void setWidthEventHandlers() {
        widthProperty().addListener((_, _, newValue) -> updateBreakpoint(newValue.doubleValue()));
    }

    private void setWidthEventHandlers(MFXScrollPane p) {
        widthProperty().addListener((_, _, newValue) -> updateBreakpoint(newValue.doubleValue()));
        p.heightProperty().addListener((_, _, newValue) -> setPrefHeight(newValue.doubleValue()));
    }

    private void updateBreakpoint(double width) {
        Breakpoint newBreakpoint = breakpointForWidth(width);
        if (newBreakpoint != currentWindowSize) {
            currentWindowSize = newBreakpoint;
            calculateNodePositions();
            requestLayout();
        }
    }

    private Breakpoint breakpointForWidth(double width) {
        if (width > 1200) return Breakpoint.XLARGE;
        if (width > 992) return Breakpoint.LARGE;
        if (width > 768) return Breakpoint.MEDIUM;
        if (width > 576) return Breakpoint.SMALL;
        return Breakpoint.XSMALL;
    }

    @Override
    protected void layoutChildren() {
        // A nested pane can be resized by its parent after the scroll-pane width
        // notification. Re-evaluate from the pane's real allocated width so its
        // column constraints cannot remain stuck at a stale breakpoint.
        updateBreakpoint(getWidth());
        super.layoutChildren();
    }

    private void setColumnConstraints() {
        //Remove all current columns.
        getColumnConstraints().clear();

        //Create 12 equally sized columns for layout
        double width = 100.0 / 12.0;
        for (int i = 0; i < 12; i++) {
            ColumnConstraints columnConstraints = new ColumnConstraints();
            columnConstraints.setMinWidth(0);
            columnConstraints.setPrefWidth(0);
            columnConstraints.setMaxWidth(Double.MAX_VALUE);
            columnConstraints.setPercentWidth(width);
            columnConstraints.setHgrow(Priority.ALWAYS);
            columnConstraints.setFillWidth(true);
            getColumnConstraints().add(columnConstraints);
        }
    }

    public void calculateNodePositions() {
        int currentGridPaneRow = 0;
        for (BootstrapRow row : rows) {
            currentGridPaneRow += row.calculateRowPositions(currentGridPaneRow, currentWindowSize);
        }
    }

    /**
     * Add a BootstrapRow to the layout.
     * New BootstrapRows will automatically start on a new row.
     *
     * @param row the row to be added
     */
    public void addRow(BootstrapRow row) {
        if (rows.contains(row)) return; //prevent duplicate children error

        rows.add(row);
        updateBreakpoint(getWidth());
        calculateNodePositions();

        for (BootstrapColumn column : row.getColumns()) {
            getChildren().add(column.getContent());
            GridPane.setFillWidth(column.getContent(), true);
            GridPane.setFillHeight(column.getContent(), true);
            GridPane.setHgrow(column.getContent(), Priority.ALWAYS);
            GridPane.setVgrow(column.getContent(), Priority.ALWAYS);
        }
        normalizeGaps();
    }

    /**
     * Remove a BootstrapRow from the layout.
     *
     * @param row the row to be removed
     */
    public void removeRow(BootstrapRow row) {
        rows.remove(row);
        calculateNodePositions();

        for (BootstrapColumn column : row.getColumns()) {
            getChildren().remove(column.getContent());
        }
    }
}
