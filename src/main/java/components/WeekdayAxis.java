package components;

import javafx.scene.chart.ValueAxis;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WeekdayAxis extends ValueAxis<Number> {

    private static final double[] TICK_VALUES = new double[]{0, 1, 2, 3, 4, 5, 6, 7};

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
        int index = (int)value.doubleValue();
        if(index == 0) {
            return "MON"; // or any label you want for 0
        } else {
            return DayOfWeek.of(index).plus(1).name().substring(0, 3);
        }
    }
}
