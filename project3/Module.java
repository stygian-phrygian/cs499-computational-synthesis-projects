public abstract class Module {
    private double value = 0.0;

    public double getValue() {
        return value;
        }

    // Set the value to be recalled later
    public void setValue(double value) {
        this.value = value;
        }

    // The method that must be overwritten by each module
    protected abstract double tick(long tickCount);

    // Does the tick and sets the value for quick recal later
    public void doUpdate(long tickCount) {
        setValue(tick(tickCount));
        }
    }
