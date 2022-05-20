package components;

import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXFilterComboBoxCell;
import io.github.palexdev.materialfx.skins.MFXFilterComboBoxSkin;
import io.github.palexdev.materialfx.i18n.I18N;
import io.github.palexdev.virtualizedfx.flow.simple.SimpleVirtualFlow;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import io.github.palexdev.materialfx.collections.TransformableList;
import io.github.palexdev.materialfx.controls.BoundTextField;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.virtualizedfx.cell.Cell;
import javafx.geometry.Pos;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

public class ActionableFilterComboBoxSkin<T> extends MFXFilterComboBoxSkin<T> {


    public ActionableFilterComboBoxSkin(MFXFilterComboBox comboBox, BoundTextField boundField, MFXButton actionButton, MFXButton secondaryButton) {
        super(comboBox, boundField);
        this.popup.setContent(createPopupContent(actionButton,secondaryButton));
    }

    protected Node createPopupContent(MFXButton actionButton, MFXButton secondaryButton) {
        MFXFilterComboBox<T> comboBox = getComboBox();
        TransformableList<T> filterList = comboBox.getFilterList();

        MFXTextField searchField = new MFXTextField("", I18N.getOrDefault("filterCombo.search"));
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().bindBidirectional(comboBox.searchTextProperty());
        searchField.setMaxWidth(Double.MAX_VALUE);

        SimpleVirtualFlow<T, Cell<T>> virtualFlow = new SimpleVirtualFlow<>(
                filterList,
                t -> new MFXFilterComboBoxCell<>(comboBox, filterList, t),
                Orientation.VERTICAL
        );
        virtualFlow.cellFactoryProperty().bind(comboBox.cellFactoryProperty());
        virtualFlow.prefWidthProperty().bind(comboBox.widthProperty());
        virtualFlow.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (popup.isShowing()) {
                popup.hide();
            }
        });
        Region divider = new Region();
        divider.setMinHeight(0.5);
        divider.setMaxHeight(0.5);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: #D3D3D3;");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setTextAlignment(TextAlignment.LEFT);
        actionButton.setAlignment(Pos.BASELINE_LEFT);
        EventHandler oldEvent = actionButton.getOnAction();
        actionButton.setOnAction(actionEvent -> {
            oldEvent.handle(actionEvent);
            this.popup.hide();
        });
        secondaryButton.setMaxWidth(Double.MAX_VALUE);
        secondaryButton.setTextAlignment(TextAlignment.LEFT);
        secondaryButton.setAlignment(Pos.BASELINE_LEFT);
        EventHandler oldSecondaryEvent = secondaryButton.getOnAction();
        secondaryButton.setOnAction(actionEvent -> {
            oldSecondaryEvent.handle(actionEvent);
            this.popup.hide();
        });

        VBox container = new VBox(10, searchField, virtualFlow,divider, actionButton, secondaryButton);
        VBox.setMargin(divider,new Insets(0,-6,-6,-6));
        VBox.setMargin(actionButton,new Insets(0,-6,0,-6));
        VBox.setMargin(secondaryButton,new Insets(-10,-6,0,-6));
        container.getStyleClass().add("search-container");
        container.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, Color.web("#000000",0.8),10,0.0,0.0,2.0));
        container.setAlignment(Pos.TOP_CENTER);
        return container;
    }
}
