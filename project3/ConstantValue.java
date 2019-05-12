public class ConstantValue extends Module {
    public ConstantValue(double value) {
        setValue(value);
    }

    public double tick(long tickCount) {
        return getValue();
    }
}
