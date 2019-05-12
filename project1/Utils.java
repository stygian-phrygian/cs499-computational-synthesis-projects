public class Utils {
    public static final double WELL_ABOVE_SUBNORMALS = 1.0e-200;
    public static final double T = Config.INV_SAMPLING_RATE;
    static final int SIN_TABLE_LENGTH = 65536; //  * 16;
    static final double SIN_MULTIPLIER = SIN_TABLE_LENGTH / Math.PI / 2;
    static double[] sinTable = new double[SIN_TABLE_LENGTH];

    static {
        for (int i = 0; i < SIN_TABLE_LENGTH; ++i) {
            sinTable[i] = (double) Math.sin((double) i * Math.PI * (2.0 / SIN_TABLE_LENGTH));
            }

        }

    /**
     * Maps linearly from domain 0 -> NYQUIST_LIMIT to range 0 -> 1.0
     **/
    public static final double hzToValue(double hz) {
        if (hz > Config.SAMPLING_RATE / 2.0) {
            return 1.0;
            }
        if (hz < 0) {
            return 0.0;
            }
        return hz * Config.INV_NYQUIST_LIMIT;
        }

    /**
     * Maps linearly from domain 0 -> 1.0 to range 0 -> NYQUIST_LIMIT
     **/
    public static final double valueToHz(double cv) {
        if (cv <= 0)
            return 0;
        if (cv >= 1.0)
            return Config.NYQUIST_LIMIT;
        return cv * Config.NYQUIST_LIMIT;
        }

    /**
     * A fast (3.5x) approximation of a^b.
     */
    // About 3.5 times faster
    public static double fastpow(final double a, final double b) {
        if (b == 0) {
            return 1.0;
            } else if (b == 1) {
            return a;
            } else if (b < 0) {
            return 1.0 / fastpow(a, -b);
            } else if (b <= 10 && b == (int) b) {
            double res = a;
            for (int i = 1; i < b; i++) {
                res = res * a;
                }
            return res;
            } else {
            double r = 1.0;
            double base = a;
            int exp = (int) b;

            // exponentiation by squaring
            while (exp != 0) {
                if ((exp & 1) != 0) {
                    r *= base;
                    }
                base *= base;
                exp >>= 1;
                }

            // use the IEEE 754 trick for the fraction of the exponent
            final double b_faction = b - (int) b;
            final long tmp = Double.doubleToLongBits(a);
            final long tmp2 = (long) (b_faction * (tmp - 4606921280493453312L)) + 4606921280493453312L;
            return r * Double.longBitsToDouble(tmp2);
            }
        }

    /**
     * An approximation of a^b which uses Math.pow for values of b < 1,
     * and uses fastpow for values >= 1.  We presently use this in filters
     * but its implementation may change in the future.
     */
    // This should remove some of the weirdness in the LPF sounds
    public static double hybridpow(final double a, final double b) {
        if (Math.abs(b) < 1.0)
            return Math.pow(a, b);
        return fastpow(a, b);
        }

    private static String OS() {
        return System.getProperty("os.name").toLowerCase();
        }

    public static boolean isUnix() {
        return (OS().indexOf("nix") >= 0 || OS().indexOf("nux") >= 0 || OS().indexOf("aix") > 0);
        }

    /**
     * A fast approximation of Sine using a lookup table.
     */
    public static final double fastSin(double f) {
        /*
        // interpolating version -- seems to make little to no difference
        double v = f * SIN_MULTIPLIER;
        int conv = (int) v;
        double alpha = v - conv;
        int slot1 = conv & (SIN_TABLE_LENGTH - 1);
        int slot2 = (slot1 + 1) & SIN_TABLE_LENGTH;
        return sinTable[slot2] * alpha + sinTable[slot1] * (1.0 - alpha);
        */
        return sinTable[(int) (f * SIN_MULTIPLIER) & (SIN_TABLE_LENGTH - 1)];
        }

    public static double lerp(double v1, double v2, double ctime, double btime, double etime) {
        double alpha = (ctime - btime) / (etime - btime);
        return (v2 - v1) * alpha + v1;
        }
    }
