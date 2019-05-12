// mixer takes three inputs I1, I2, I3, and three amplitudes A1, A2, A3
// and performs I1 * A1 + I2 * A2 + I3 * A3
public class Mixer3 extends Module {
    Module i1 = new ConstantValue(0.0);
    Module i2 = new ConstantValue(0.0);
    Module i3 = new ConstantValue(0.0);
    Module a1 = new ConstantValue(1.0);
    Module a2 = new ConstantValue(1.0);
    Module a3 = new ConstantValue(1.0);
    void setInput1(Module i1) { this.i1 = i1; }
    void setInput2(Module i2) { this.i2 = i2; }
    void setInput3(Module i3) { this.i3 = i3; }
    void setAmplitude1(Module a1) { this.a1 = a1; }
    void setAmplitude2(Module a2) { this.a2 = a2; }
    void setAmplitude3(Module a3) { this.a3 = a3; }

    public double tick(long tickCount) {
        double v = a1.getValue() * i1.getValue() + a2.getValue() * i2.getValue() + a3.getValue() * i3.getValue();
        v *= 0.2; // headroom
        return v;
    }
}
