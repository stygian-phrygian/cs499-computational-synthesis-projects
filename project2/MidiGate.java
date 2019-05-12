public class MidiGate extends Module {
    private MidiModule midiModule;

    public MidiGate(MidiModule midiModule) {
        this.midiModule = midiModule;
        }

    @Override
    public double tick(long tickCount) {
        return this.midiModule.getGate();
        }
    }
