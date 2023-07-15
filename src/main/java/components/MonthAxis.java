package components;

import javafx.scene.chart.ValueAxis;

import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MonthAxis extends ValueAxis<Number> {

    private static final double[] TICK_VALUES = new double[]{0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365};

    @Override
    protected List<Number> calculateMinorTickMarks() {
        return Collections.emptyList();
    }

    @Override
    protected void setRange(Object o, boolean b) {

    }

    @Override
    protected Object getRange() {
        return null;
    }

    @Override
    protected List<Number> calculateTickValues(double length, Object range) {
        return Arrays.stream(TICK_VALUES).boxed().collect(Collectors.toList());
    }

    @Override
    protected String getTickMarkLabel(Number value) {
        int index = 0;
        for (int i = 0; i < TICK_VALUES.length; i++) {
            if (value.doubleValue() <= TICK_VALUES[i]) {
                index = i;
                break;
            }
        }
        if(index == 0) {
            return "JAN"; // or any label you want for 0
        } else if(index == 12) {
            return "";
        } else {
            return Month.of(index).plus(1).name().substring(0, 3);
        }
    }

}