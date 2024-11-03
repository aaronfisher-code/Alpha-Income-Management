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
    public final static String CASH_REGEX = "-?[0-9]+\\.?[0-9]?[0-9]?";
    public final static String CASH_EMPTY_REGEX = "^(-?[0-9]+\\.?[0-9]?[0-9]?)?$";
    public final static String CASH_ERROR = "Please enter a valid amount";
    public final static String BLANK_REGEX = "(.|\\s)*\\S(.|\\s)*";
    public final static String BLANK_ERROR = "This field must not be blank";
    public final static String INT_REGEX = "-?[0-9]*";
    public final static String INT_ERROR = "Please enter a valid number";
    public final static String DATE_REGEX = "^(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)?\\d\\d)$";
    public final static String DATE_ERROR = "Please enter a valid date";
    public final static String TIME_REGEX = "^(1[0-2]|0?[1-9]):[0-5][0-9] ([Aa][Mm]|[Pp][Mm])$";
    public final static String TIME_ERROR = "Please enter a valid time";


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
                field.setStyle("-fx-background-color: white;-fx-text-fill: black;");
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
                    field.setStyle("-fx-background-color: red;-fx-text-fill: white;");
                    validationLabel.setText(constraints.get(0).getMessage());
                    validationLabel.setStyle("-fx-text-fill: red;");
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
                field.commit(field.getText());
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
