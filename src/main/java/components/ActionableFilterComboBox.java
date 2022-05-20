package components;

import components.ActionableFilterComboBoxSkin;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import javafx.scene.control.Skin;

public class ActionableFilterComboBox<T> extends MFXFilterComboBox<T> {


    public ActionableFilterComboBox(MFXButton actionButton,MFXButton secondaryButton) {
        super();
        this.setSkin(new ActionableFilterComboBoxSkin<>(this,boundField,actionButton,secondaryButton));
    }


}
