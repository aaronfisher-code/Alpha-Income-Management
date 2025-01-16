package strategies;

public class MedschecksStrategy implements GaugeTargetStrategy {
    @Override
    public double getMinValue() {
        return 0;
    }

    @Override
    public double getMaxValue() {
        return 5;
    }

    @Override
    public double getActualValue() {
        return 4.38;
    }
}
