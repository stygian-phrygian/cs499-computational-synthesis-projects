/*
 * This is a module which allows one to modulate the volume of another module
 *
 */
public class Amplifier extends Module {

    private Module input = new ConstantValue(1.0);
    private Module amplitudeMod = new ConstantValue(1.0);
    
    public Amplifier(Module input){
        this.input = input;
        }

    // Set the amplitude module
    public void setAmplitudeMod(Module amplitudeMod){
        this.amplitudeMod = amplitudeMod;
        }

    public Module getAmplitudeMod() { return amplitudeMod; }

    public Module getInput() { return input; }

    public double tick(long tickCount) {
        return input.getValue() * amplitudeMod.getValue();
        }
    }
