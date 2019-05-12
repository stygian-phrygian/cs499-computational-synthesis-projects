public class PM extends Osc {

    // Frequency of this oscillator relative to getFrequencyMod(): for example 2x the pitch  -- this should be a dial
    // we're going to break the contract the modules are from 0 - 1 here, because it's easier to view it this way
    // hence, this module (should) produce integer values representing divisions of frequency by multiples of 0.25
    // an enum as it were
    //         0 => 0.25
    //         1 => 0.50
    //         2 => 0.75
    //         and so on
    Module        relativeFrequency              = new ConstantValue(1.0);
    public void   setRelativeFrequency(Module p) { relativeFrequency = p;    }
    public Module getRelativeFrequency()         { return relativeFrequency; }

    // Strength of phase modulation affecting this oscillator -- this is typically a dial
    Module        phaseAmplifier                 = new ConstantValue(4);
    public void   setPhaseAmplifier(Module p)    { phaseAmplifier = p;    }
    public Module getPhaseAmplifier()            { return phaseAmplifier; }

    // Final amplification of this oscillator  -- this is typically an envelope
    Module        outputAmplitude                = new ConstantValue(1.0);
    public void   setOutputAmplitude(Module p)   { outputAmplitude = p;    }
    public Module getOutputAmplitude()           { return outputAmplitude; }

    public double tick(long tickCount) {
        double hz = Utils.valueToHz(getFrequencyMod().getValue()) * relativeFrequency.getValue();
        state    += hz * Config.INV_SAMPLING_RATE;
        if (state > 1) { state -= 1; }
        double w  = 2*Math.PI*state;
        return outputAmplitude.getValue()*Utils.fastSin(w);
    }
}
