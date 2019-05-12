public class BPBlit extends Blit {

    Module phaseMod = new ConstantValue(0.5);
    Blit offPhaseBlit = new Blit();

    public void setPhaseMod(Module phaseMod) {
        this.phaseMod = phaseMod;
    }

    public Module getPhaseMod() {
        return this.phaseMod;
    }

    @Override
    public void setFrequencyMod(Module frequencyMod) {
        super.setFrequencyMod(frequencyMod);
        offPhaseBlit.setFrequencyMod(frequencyMod);
    }

    protected double tick(long tickCount, double d) {
        double hz = Utils.valueToHz(getFrequencyMod().getValue());
        hz = Math.min(Math.max(1.0, hz), Config.NYQUIST_LIMIT); // clamp so we don't get INF below
        double p = Config.SAMPLING_RATE / hz;
        double val = super.tick(tickCount, d) - offPhaseBlit.tick(tickCount, p*(1.0-d));
        return val;
    }
}
