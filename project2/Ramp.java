public class Ramp extends Module {
    public double state = 0;

    private Module frequencyMod = new ConstantValue(Utils.hzToValue(220));
    private Module amplitudeMod = new ConstantValue(1.0);
    public void    setFrequencyMod(Module frequencyMod) { this.frequencyMod = frequencyMod; }
    public Module  getFrequencyMod() { return this.frequencyMod; }
    public void    setAmplitudeMod(Module amplitudeMod) { this.amplitudeMod = amplitudeMod; }
    public Module  getAmplitudeMod() { return this.amplitudeMod; }

    public double tick(long tickCount) {
        double hz = Utils.valueToHz(getFrequencyMod().getValue());
        state += hz * Config.INV_SAMPLING_RATE;
        if (state > 1) { state -= 1; }
        return state;
    }
}
