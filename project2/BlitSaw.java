public class BlitSaw extends Blit {
    double prev = 0.0;

    protected double tick(long tickCount, double d) {
        double hz = Utils.valueToHz(getFrequencyMod().getValue());

        hz  = Math.min(Math.max(1.0, hz), Config.NYQUIST_LIMIT); // clamp so we don't get INF below
        double P  = Config.SAMPLING_RATE / hz;
        double alpha = 1 - 1/P;
        double val = alpha*prev + super.tick(tickCount, d) - 1/P;
        prev = val;
        // handle DC offset issue
        val += 0.5;
        return val;
    }
}
