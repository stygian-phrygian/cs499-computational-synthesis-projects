public class LPF extends Filter {

    Module frequencyMod, resonanceMod;
    double lastFrequencyModValue, lastResonanceModValue;

    LPF(Module input, Module frequencyMod, Module resonanceMod) {
        super(input, new double[2], new double[2], 0.0);
        lastFrequencyModValue = frequencyMod.getValue();
        lastResonanceModValue = resonanceMod.getValue();
        this.frequencyMod     = frequencyMod;
        this.resonanceMod     = resonanceMod;
        calculateCoefficients();
    }

    public void setFrequencyMod(Module frequencyMod) { this.frequencyMod = frequencyMod; }
    public void setResonanceMod(Module resonanceMod) { this.resonanceMod = resonanceMod; }

    @Override
    public double tick(long tickCount) {

        // if cutoff/resonance changed, calculate coefficients
        if ( (frequencyMod.getValue() != lastFrequencyModValue) ||
             (resonanceMod.getValue() != lastResonanceModValue)) {
            calculateCoefficients();
        }
        // save last cutoff and resonance values for next tick
        lastFrequencyModValue = frequencyMod.getValue();
        lastResonanceModValue = resonanceMod.getValue();

        // get coefficients
        double a1 = a[0];
        double a2 = a[1];
        double b1 = b[0];
        double b2 = b[1];
        // get input
        double x0 = input.getValue();
        // get saved input and output
        double x1 = x[0];
        double x2 = x[1];
        double y1 = y[0];
        double y2 = y[1];

        // perform calculation
        double y0 = b0*x0 + b1*x1 + b2*x2 - a1*y1 - a2*y2;

        // low cutoff or hiqh Q = violence thus clamping
        y0 = clamp(y0);

        // shift saved input/output
        x[0] = x2;
        x[1] = x0;
        y[0] = y2;
        y[1] = y0;

//        //DEBUG
//        System.out.printf("x: %.3f, y: %.3f, \n", x0, y0);

        // return the output (clamped)
        return y0;
    }

    private double clamp(double x) {
        // hopefully java can figure out this is 1 x86 instruction... probably not
        return Math.max(0.0, Math.min(x, 1.0));
    }


    private void calculateCoefficients() {

        // see lecture notes for 2nd order butterworth
        // b0 == (1/J) * M
        // b1 == (1/J) * 2M
        // b2 == (1/J) * M
        // a1 == (1/J) * (-8Q + 2M)
        // a2 == (1/J) * (4Q - 2w0T + M)
        // where
        //      w0 == cutoff
        //      Q  == resonance
        //      J  == 4*Q + 2*w0*T + M
        //      M  == w0^2*Q*T^2
        // NB.
        //      w0 and Q are prohibited from being 0
        //      w0 is 2pi * cutoffFrequencyInHZ
        //      Q  is somewhere between sqrt(1/2) and (rather arbitrarily) 10ish corresponding to no and full resonance

        // intermediary
        double T        = Config.INV_SAMPLING_RATE;
        double hz       = Utils.valueToHz(frequencyMod.getValue());
        double w0       = hz>0 ? 2*Math.PI*hz : 0.0001;
        double Q        = resonanceMod.getValue()*10 + Math.sqrt(0.5);
        double M        = w0*w0*Q*T*T;
        double J        = 4*Q + 2*w0*T + M;
        double oneOverJ = 1/J;

        // update coefficients
        this.b0   = oneOverJ*M;                  // b0
        this.b[0] = oneOverJ*2*M;                // b1
        this.b[1] = oneOverJ*M;                  // b2
        this.a[0] = oneOverJ*(-8*Q + 2*M);       // a1
        this.a[1] = oneOverJ*(4*Q - 2*w0*T + M); // a2


//        //DEBUG
//        System.out.printf("b0: %.3f, b1: %.3f, b2: %.3f, a1: %.3f, a2: %.3f\n", b0, b[0], b[1], a[0], a[1]);
//        System.out.printf("Q: %.3f, \n", Q);
    }
}
