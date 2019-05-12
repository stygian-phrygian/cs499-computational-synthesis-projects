// this is intended solely to scale input from the UI
public class Scaler extends Module {
    private double multiplier, offset;
    private Module inputModule;

    public Scaler(Module inputModule, double multiplier, double offset) {
        this.inputModule = inputModule;
        this.multiplier = multiplier;
        this.offset = multiplier;
    }

    public double getValue() {
        return multiplier * (inputModule.getValue() + offset);
    }

    public double tick(long tickCount) {
        return getValue();
    }
}
