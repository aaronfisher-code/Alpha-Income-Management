package components;

import com.dlsc.gemsfx.FilterView;
import com.dlsc.gemsfx.ChipView;
import com.dlsc.gemsfx.ChipsViewContainer;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;

/**
 * Keeps the pre-upgrade FilterView behavior when used with GemsFX 4.x.
 *
 * <p>The 4.x skin clears its structured chip list but renders text-search
 * chips directly as children. Clearing an already-empty list is a no-op, so
 * typing can leave one chip per keystroke. It also allows the control to
 * shrink below the padded height used by the 1.x skin.</p>
 */
public class CompatibleFilterView<T> extends FilterView<T> {

    public CompatibleFilterView() {
        setMinHeight(Region.USE_COMPUTED_SIZE);
        skinProperty().addListener((_, _, _) -> scheduleFilterChipRepair());
        filterTextProperty().addListener((_, _, _) -> scheduleFilterChipRepair());
        filtersProperty().addListener((_, _, _) -> scheduleFilterChipRepair());
        sceneProperty().addListener((_, _, scene) -> {
            if (scene != null) {
                scheduleFilterChipRepair();
            }
        });
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        Skin<?> skin = super.createDefaultSkin();
        scheduleFilterChipRepair();
        return skin;
    }

    private void scheduleFilterChipRepair() {
        Platform.runLater(this::repairFilterChipVisibility);
    }

    private void repairFilterChipVisibility() {
        Node filtersPane = findNode(this, "filters");
        if (filtersPane == null) {
            return;
        }

        if (!(filtersPane instanceof Parent parent)) {
            return;
        }

        if (filtersPane instanceof ChipsViewContainer chipsViewContainer) {
            removeDuplicateTextFilterNodes(chipsViewContainer);
        }

        BooleanBinding hasActiveFilter = Bindings.isNotEmpty(parent.getChildrenUnmodifiable());

        filtersPane.visibleProperty().unbind();
        filtersPane.managedProperty().unbind();
        filtersPane.visibleProperty().bind(hasActiveFilter);
        filtersPane.managedProperty().bind(hasActiveFilter);
    }

    private void removeDuplicateTextFilterNodes(ChipsViewContainer chipsViewContainer) {
        ObservableList<Node> children = chipsViewContainer.getChildren();
        Node lastTextChip = null;
        Node lastClearLabel = null;
        boolean hasTextFilter = getFilterText() != null && !getFilterText().isBlank();
        boolean hasStructuredFilters = !getFilters().isEmpty();

        for (Node child : children) {
            if (child instanceof ChipView<?> && !chipsViewContainer.getChips().contains(child)) {
                lastTextChip = child;
            } else if (child.getStyleClass().contains("clear-filter-label")) {
                lastClearLabel = child;
            }
        }

        Node textChipToKeep = hasTextFilter ? lastTextChip : null;
        Node clearLabelToKeep = hasTextFilter || hasStructuredFilters ? lastClearLabel : null;
        children.removeIf(child ->
                (child instanceof ChipView<?> && !chipsViewContainer.getChips().contains(child)
                        && child != textChipToKeep)
                        || (child.getStyleClass().contains("clear-filter-label")
                        && child != clearLabelToKeep));
    }

    private Node findNode(Node node, String styleClass) {
        if (node.getStyleClass().contains(styleClass)) {
            return node;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = findNode(child, styleClass);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
}
