public class ThreeOperatorPM extends Module {

    // possible algorithms
    enum AlgorithmType {PARALLEL, SERIAL, BRANCH, MERGE;}
    /*

    parallel:

        1 2 3

    serial:

        1
        |
        2
        |
        3

    branch:

          1
         / \
        2   3

    merge:

        1   2
         \ /
          3
    */

    private Module frequencyMod = new ConstantValue(Utils.hzToValue(220));
    private PM operator1        = new PM();
    private PM operator2        = new PM();
    private PM operator3        = new PM();
    private Module algorithmMod = new ConstantValue(AlgorithmType.PARALLEL.ordinal());
    private AlgorithmType[] alg = AlgorithmType.values(); // hack... you'll see

    // an operator is an amplifier/envelope and relative frequency
    // you can only alter the relative frequency and envelope
    // you cannot alter its base frequency
    ThreeOperatorPM() {
        // set operators' base frequency
        setFrequencyMod(getFrequencyMod());
    }

    // base frequency get/set
    public Module getFrequencyMod() {
        return frequencyMod;
    }
    public void setFrequencyMod(Module m) {
        frequencyMod = m;
        operator1.setFrequencyMod(m);
        operator2.setFrequencyMod(m);
        operator3.setFrequencyMod(m);
    }

    public void setAlgorithmMod(Module m) { this.algorithmMod = m; }

    public void setOperatorRelativeFrequencyMod(int operatorN, Module m) {
      switch(operatorN) {
        case 1:
          operator1.setRelativeFrequency(m);
          break;
        case 2:
          operator2.setRelativeFrequency(m);
          break;
        case 3:
          operator3.setRelativeFrequency(m);
          break;
      }
    }

    // for envelopes
    public void setOperatorOutputAmplitudeMod(int operatorN, Module m) {
        switch(operatorN) {
            case 1:
                operator1.setOutputAmplitude(m);
                break;
            case 2:
                operator2.setOutputAmplitude(m);
                break;
            case 3:
                operator3.setOutputAmplitude(m);
                break;
        }
    }


    // reads the current value of algorithmMod and converts to an enum
    private AlgorithmType getAlgorithmType() {
        // and here's our hack baby
        return alg[(int) Math.min(Math.max(0, algorithmMod.getValue()), alg.length-1)];
    }

    public double tick(long tickCount) {
        double hz    = Utils.valueToHz(getFrequencyMod().getValue());
        double value = 0.0;
        // get our output operator's angular momentum from base frequency
        // switch on which algorithm to utilize
        operator1.tick(tickCount);
        operator2.tick(tickCount);
        operator3.tick(tickCount);

        switch (getAlgorithmType()) {
            case PARALLEL: {
                double f1 = hz * operator1.getRelativeFrequency().getValue();
                double f2 = hz * operator2.getRelativeFrequency().getValue();
                double f3 = hz * operator3.getRelativeFrequency().getValue();
                double e1 = operator1.getOutputAmplitude().getValue();
                double e2 = operator2.getOutputAmplitude().getValue();
                double e3 = operator3.getOutputAmplitude().getValue();
                double  x = 2 * Math.PI * tickCount * Config.INV_SAMPLING_RATE;
                // parallel is just additive synthesis
                value     = 0.333*(e3*Utils.fastSin(f3*x)+e2*Utils.fastSin(f2*x)+e1*Utils.fastSin(f1*x));
            }
                break;
            case SERIAL: {
                double f1 = hz * operator1.getRelativeFrequency().getValue();
                double f2 = hz * operator2.getRelativeFrequency().getValue();
                double f3 = hz * operator3.getRelativeFrequency().getValue();
                double e1 = operator1.getOutputAmplitude().getValue();
                double e2 = operator2.getOutputAmplitude().getValue();
                double e3 = operator3.getOutputAmplitude().getValue();
                double p1 = operator1.getPhaseAmplifier().getValue();
                double p2 = operator2.getPhaseAmplifier().getValue();
                double p3 = operator3.getPhaseAmplifier().getValue();
                double  x = 2 * Math.PI * tickCount * Config.INV_SAMPLING_RATE;
                value     = e3 * p3 * Utils.fastSin(f3 * x +
                            e2 * p2 * Utils.fastSin(f2 * x +
                            e1 * p1 * Utils.fastSin(f1 * x)));
            }
                break;
            case BRANCH: {
                double f2 = hz * operator2.getRelativeFrequency().getValue();
                double f3 = hz * operator3.getRelativeFrequency().getValue();
                double e2 = operator2.getOutputAmplitude().getValue();
                double e3 = operator3.getOutputAmplitude().getValue();
                double p2 = operator2.getPhaseAmplifier().getValue();
                double p3 = operator3.getPhaseAmplifier().getValue();
                double  x = 2 * Math.PI * tickCount * Config.INV_SAMPLING_RATE;
                value    += e2 * p2 * Utils.fastSin(f2 * x + operator1.getValue());
                value    += e3 * p3 * Utils.fastSin(f3 * x + operator1.getValue());
                value    *= 0.5;
            }
                break;
            case MERGE: {
                double f3 = hz * operator3.getRelativeFrequency().getValue();
                double e3 = operator3.getOutputAmplitude().getValue();
                double p3 = operator3.getPhaseAmplifier().getValue();
                double x  = 2 * Math.PI * tickCount * Config.INV_SAMPLING_RATE;
                value     = e3 * p3 * Utils.fastSin(f3 * x + operator1.getValue() + operator2.getValue());
            }
                break;
        }
        value = (value+1.0)*0.5; // [-1 to +1] -> [0 to +1]
        return value;
    }
}
