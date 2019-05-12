public abstract class Filter extends Module {
    Module input;
    double b0;
    double[] b;
    double[] a;
    double[] x;
    double[] y;
    double x0;

    // provide input and coefficients
    public Filter(Module input, double[] a, double[] b, double b0) {
        this.input = input;
        this.a = a;
        this.b = b;
        this.b0 = b0;
        this.y = new double[a.length];
        this.x = new double[b.length];
    }

    public void setInput(Module input) { this.input = input; }
}
