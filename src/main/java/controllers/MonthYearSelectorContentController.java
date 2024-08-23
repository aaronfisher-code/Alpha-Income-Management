package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleGroup;
import javafx.scene.text.Text;
import java.time.LocalDate;
import java.time.Month;
import java.util.EnumMap;
import java.util.Map;

public class MonthYearSelectorContentController extends PageController {

    @FXML private Text yearValue;
    @FXML private MFXButton backwardButton, forwardButton;
    @FXML private MFXRectangleToggleNode janNode, febNode, marNode, aprNode, mayNode, junNode,
            julNode, augNode, sepNode, octNode, novNode, decNode;
    private DateSelectController parent;
    private Map<Month, MFXRectangleToggleNode> monthNodes;
    private ToggleGroup monthToggleGroup;

    public void setParent(DateSelectController m) {
        this.parent = m;
    }

    @Override
    public void fill() {
        initializeMonthNodes();
        updateYearDisplay();
        setCurrentMonthSelected();
        setupYearButtons();
        setupMonthButtons();
    }

    private void initializeMonthNodes() {
        monthToggleGroup = new ToggleGroup();
        monthNodes = new EnumMap<>(Month.class);
        monthNodes.put(Month.JANUARY, janNode);
        monthNodes.put(Month.FEBRUARY, febNode);
        monthNodes.put(Month.MARCH, marNode);
        monthNodes.put(Month.APRIL, aprNode);
        monthNodes.put(Month.MAY, mayNode);
        monthNodes.put(Month.JUNE, junNode);
        monthNodes.put(Month.JULY, julNode);
        monthNodes.put(Month.AUGUST, augNode);
        monthNodes.put(Month.SEPTEMBER, sepNode);
        monthNodes.put(Month.OCTOBER, octNode);
        monthNodes.put(Month.NOVEMBER, novNode);
        monthNodes.put(Month.DECEMBER, decNode);
        monthNodes.values().forEach(node -> node.setToggleGroup(monthToggleGroup));
    }

    private void updateYearDisplay() {
        yearValue.setText(String.valueOf(main.getCurrentDate().getYear()));
    }

    private void setCurrentMonthSelected() {
        Month currentMonth = main.getCurrentDate().getMonth();
        MFXRectangleToggleNode currentNode = monthNodes.get(currentMonth);
        if (currentNode != null) {
            currentNode.setSelected(true);
        }
    }

    private void setupYearButtons() {
        forwardButton.setOnAction(_ -> updateYear(1));
        backwardButton.setOnAction(_ -> updateYear(-1));
    }

    private void updateYear(int yearDelta) {
        parent.setDate(main.getCurrentDate().plusYears(yearDelta));
        updateYearDisplay();
    }

    private void setupMonthButtons() {
        monthNodes.forEach((month, node) -> {
            node.setOnAction(_ -> {
                if (!node.isSelected()) {
                    node.setSelected(true);
                }
                updateDate(month);
            });
        });
    }

    private void updateDate(Month newMonth) {
        LocalDate currentDate = main.getCurrentDate();
        int day = currentDate.getDayOfMonth();
        int year = currentDate.getYear();
        parent.setDate(LocalDate.of(year, newMonth, day));
    }
}
