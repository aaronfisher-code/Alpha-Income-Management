package utils;

import components.ActionableFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.validation.Constraint;
import io.github.palexdev.materialfx.validation.Severity;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.regex.Pattern;

import static io.github.palexdev.materialfx.validation.Validated.INVALID_PSEUDO_CLASS;

public class ValidatorUtils {
    public final static String CASH_REGEX = "[0-9]+\\.?[0-9]?[0-9]?";
    public final static String CASH_ERROR = "Please enter a valid dollar amount";
    public final static String BLANK_REGEX = "(.|\\s)*\\S(.|\\s)*";
    public final static String BLANK_ERROR = "This field must not be blank";
    public final static String INT_REGEX = "[0-9]*";
    public final static String INT_ERROR = "Please enter a valid number";

    public static void setupRegexValidation(MFXTextField field, Label validationLabel, String regex, String errorMessage, String measureUnit, MFXButton disableButton) {
        Constraint digitConstraint = Constraint.Builder.build()
                .setSeverity(Severity.ERROR)
                .setMessage(errorMessage)
                .setCondition(Bindings.createBooleanBinding(
                        () -> Pattern.matches(regex, field.getText()),
                        field.textProperty()
                ))
                .get();
        if (measureUnit != null)
            field.setLeadingIcon(new Label(measureUnit));
        field.getValidator().constraint(digitConstraint);
        field.getValidator().validProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                validationLabel.setVisible(false);
                if(disableButton!=null)
                    disableButton.setDisable(false);
                field.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
            }
        });

        field.delegateFocusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                List<Constraint> constraints = field.validate();
                if (!constraints.isEmpty()) {
                    field.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, true);
                    validationLabel.setText(constraints.get(0).getMessage());
                    validationLabel.setVisible(true);
                    if(disableButton!=null)
                        disableButton.setDisable(true);
                }
            }
        });
    }
    public static void setupRegexValidation(ActionableFilterComboBox field, Label validationLabel, String regex, String errorMessage, String measureUnit, MFXButton disableButton) {
        Constraint digitConstraint = Constraint.Builder.build()
                .setSeverity(Severity.ERROR)
                .setMessage(errorMessage)
                .setCondition(Bindings.createBooleanBinding(
                        () -> Pattern.matches(regex, field.getText()),
                        field.textProperty()
                ))
                .get();
        if (measureUnit != null)
            field.setLeadingIcon(new Label(measureUnit));
        field.getValidator().constraint(digitConstraint);
        field.getValidator().validProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                validationLabel.setVisible(false);
                if(disableButton!=null)
                    disableButton.setDisable(false);
                field.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
            }
        });

        field.delegateFocusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                List<Constraint> constraints = field.validate();
                if (!constraints.isEmpty()) {
                    field.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, true);
                    validationLabel.setText(constraints.get(0).getMessage());
                    validationLabel.setVisible(true);
                    if(disableButton!=null)
                        disableButton.setDisable(true);
                }
            }
        });
    }

    public static void setupRegexValidation(MFXDatePicker field, Label validationLabel, String regex, String errorMessage, String measureUnit, MFXButton disableButton) {
        Constraint digitConstraint = Constraint.Builder.build()
                .setSeverity(Severity.ERROR)
                .setMessage(errorMessage)
                .setCondition(Bindings.createBooleanBinding(
                        () -> Pattern.matches(regex, field.getText()),
                        field.textProperty()
                ))
                .get();
        if (measureUnit != null)
            field.setLeadingIcon(new Label(measureUnit));
        field.getValidator().constraint(digitConstraint);
        field.getValidator().validProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                validationLabel.setVisible(false);
                if(disableButton!=null)
                    disableButton.setDisable(false);
                field.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
            }
        });

        field.delegateFocusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                List<Constraint> constraints = field.validate();
                if (!constraints.isEmpty()) {
                    field.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, true);
                    validationLabel.setText(constraints.get(0).getMessage());
                    validationLabel.setVisible(true);
                    if(disableButton!=null)
                        disableButton.setDisable(true);
                }
            }
        });
    }
}
