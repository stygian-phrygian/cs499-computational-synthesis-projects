public class Config {
    /**
     * Sampling rate of sound
     */
    public static final float SAMPLING_RATE = 48000f;
    public static final double INV_SAMPLING_RATE = 1.0 / SAMPLING_RATE;
    public static final double NYQUIST_LIMIT = SAMPLING_RATE / 2.0;
    public static final double INV_NYQUIST_LIMIT = 1.0 / NYQUIST_LIMIT;
    }
