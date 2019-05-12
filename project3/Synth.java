// Copyright 2018 by George Mason University


import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import java.awt.*;

/**
 * DO NOT EDIT THIS SECTION BELOW (SEE BOTTOM OF FILES FOR WHERE TO UPDATE)
 * ==============================
 * VVVVVVVVVVVVVVVVVVVVVVVVVVVVVV
 **/
public class Synth {

    /**
     * Size of the Java SourceDataLine audio buffer.
     * Larger and we have more latency.  Smaller and the system can't handle it.
     * It appears &geq 1024 is required on the Mac for 44100 KHz
     */
    public static final int BATCH_SIZE = 32;
    private Midi midi;
    // The current Mixer
    private Mixer.Info mixer;
    // The Audio Format
    private AudioFormat audioFormat;
    // The audio output
    private SourceDataLine sdl;
    // current write position in buffer
    private int buffpos = 0;
    // Audio buffer, which the audio output drains.
    private byte[] audioBuffer = new byte[BATCH_SIZE];
    /*
      Random Number Generation

      Each Sound has its own random number generator.
      You can get a new, more or less statistically  independent generator from this method.
    */
    private Object randomLock = new Object[0];
    private long randomSeed;
    private Random rnd = getNewRandom();
    private ArrayList<Module> modules = new ArrayList<Module>();
    private Module outputModule = null;
    // Global tickcount
    private long tickCount = 0;

    public Synth() {
        randomSeed = System.currentTimeMillis();
        }

    public void setOutput(Module outputModule) { this.outputModule = outputModule; }
    public Module getOutput() { return outputModule; }
        
    public void setMidi(Midi midi) { this.midi = midi; }
    public Midi getMidi() { return midi; }
        

    public static int getInt(String s) {
        try {
            return (Integer.parseInt(s));
            } catch (NumberFormatException ex) {
            return -1;
            }
        }

    public static void showDevices(Midi midi, Mixer.Info[] mixers)
        {
        midi.displayDevices();
        System.err.println("\nAUDIO DEVICES:");
        for (int i = 0; i < mixers.length; i++)
            System.err.println("" + i + ":\t" + mixers[i].getName());

        System.err.println("\nFormat:\n\tjava Synth\t\t\t[displays available devices]\n\tjava Synth [midi] [audio]\t[runs synth with the given device numbers]");
        }

    public static void main(String[] args) 
        {
        Midi midi = new Midi();
        Synth synth = new Synth();
        synth.audioFormat = new AudioFormat(Config.SAMPLING_RATE, 16, 1, true, false);
        Mixer.Info[] mixers = synth.getSupportedMixers();

        if (args.length == 0) 
            {
            showDevices(midi, mixers);
            }
        else if (args.length == 2) 
            {
            int x = getInt(args[0]);
            ArrayList<Midi.MidiDeviceWrapper> in = midi.getInDevices();
            if (x >= 0 && x < in.size()) 
                {
                midi.setInDevice(in.get(x));
                System.err.println("MIDI: " + in.get(x));

                if (mixers == null)
                    {
                    System.err.println("No output found which supports the desired sampling rate and bit depth\n");
                    showDevices(midi, mixers);
                    System.exit(1);
                    }
                else
                    {
                    x = getInt(args[1]);
                    if (x >= 0 && x < mixers.length)
                        {
                        synth.setMixer(mixers[x]);
                        System.err.println("Audio: " + mixers[x].getName());                                            
                        synth.setMidi(midi);
                        synth.setup();
                        synth.go();
                        synth.sdl.drain();
                        synth.sdl.close();
                        }
                    else
                        {
                        System.err.println("Invalid Audio number " + args[1] + "\n");
                        showDevices(midi, mixers);
                        System.exit(1);
                        }
                    }
                }
            else 
                {
                System.err.println("Invalid MIDI number " + args[0] + "\n");
                showDevices(midi, mixers);
                System.exit(1);
                }
            }
        else
            {
            System.err.println("Invalid argument format\n");
            showDevices(midi, mixers);
            System.exit(1);
            }
        }

    /**
     * Returns the currently used Mixer
     */
    public Mixer.Info getMixer() {
        return mixer;
        }

    /**
     * Sets the currently used Mixer
     */
    public void setMixer(Mixer.Info mixer) {
        try {
            if (sdl != null)
                sdl.stop();
            if (mixer == null) {
                Mixer.Info[] m = getSupportedMixers();
                if (m.length > 0)
                    mixer = m[0];
                }
            if (mixer == null)
                sdl = AudioSystem.getSourceDataLine(audioFormat);
            else
                sdl = AudioSystem.getSourceDataLine(audioFormat, mixer);
            sdl.open(audioFormat, bufferSize);
            sdl.start();
            this.mixer = mixer;
            } catch (LineUnavailableException ex) {
            throw new RuntimeException(ex);
            }
        }

    /**
     * Returns the available mixers which support the given audio format.
     */
    public Mixer.Info[] getSupportedMixers() {
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        Mixer.Info[] info = AudioSystem.getMixerInfo();
        int count = 0;
        for (int i = 0; i < info.length; i++) {
            Mixer m = AudioSystem.getMixer(info[i]);
            if (m.isLineSupported(lineInfo)) {
                count++;
                }
            }

        Mixer.Info[] options = new Mixer.Info[count];
        count = 0;
        for (int i = 0; i < info.length; i++) {
            Mixer m = AudioSystem.getMixer(info[i]);
            if (m.isLineSupported(lineInfo))
                options[count++] = info[i];
            }
        return options;
        }

    public static final int RANDOM_INCREASE = 17;
    Random getNewRandom() {
        synchronized (randomLock) {
            randomSeed += RANDOM_INCREASE;  // or whatever
            return new Random(randomSeed);
            }
        }

    /**
     * Sample ranges from 0 to 1
     */
    public void emitSample(double d) {
        int val = 0;
        if (d > 1.0) d = 1.0;
        if (d < 0.0) d = 0.0;
        d -= 0.5;

        val = (int) (d * 65536);
        if (val > 32767) val = 32767;
        if (val < -32768) val = -32768; 

        audioBuffer[buffpos] = (byte) (val & 255);
        audioBuffer[buffpos + 1] = (byte) ((val >> 8) & 255);
        buffpos += 2;
        if (buffpos + 1 >= BATCH_SIZE) {
            sdl.write(audioBuffer, 0, buffpos);
            buffpos = 0;
            }
        }

    private void go() {
        if (this.outputModule == null) {
            System.err.println("No output module defined: exiting");
            return;
            }
        System.out.println("NO AUDIO FOR THE FIRST 3 SECONDS WHILE THE JIT KICKS IN!");
        while (true) {
            for (Module m : this.modules) {
                m.doUpdate(tickCount);
                }
            emitSample(this.outputModule.getValue());
            tickCount++;
            }
        }

    /**
       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       ==============================
       DO NOT EDIT THIS SECTION ABOVE
    **/


    // ADJUST THIS VALUE IF YOU GET A LOT OF GLITCHY SOUND
    private static int numSamples = 2048;

    // DON'T ADJUST THIS VALUE
    private static int bufferSize = numSamples * 2;

    /**
     * MAKE YOUR EDITS TO THIS METHOD
     **/
    public void setup() {



        // create a new frame
        JFrame frame        = new JFrame("Project 3");
        frame.setLayout(new FlowLayout());
        // create dials/options/oscilloscope
        //
        // algorithm:
        Box boxAlgorithm          = new Box(BoxLayout.X_AXIS);
        frame.add(boxAlgorithm);
        Options algorithmOptions  = new Options("Algorithms", new String[]{"Parallel", "Serial", "Branch", "Merge"}, 1);
        boxAlgorithm.add(algorithmOptions);
        boxAlgorithm.add(boxAlgorithm.createGlue());
        // operator1: relative frequency option, phase amp dial, 4 x envelope dials
        Box boxOperator1                          = new Box(BoxLayout.X_AXIS);
        frame.add(boxOperator1);
        Options operator1RelativeFrequencyOptions = new Options(
                "Operator 1 Relative Frequency",
                new String[]{"0.25", "0.50", "0.75", "1.00", "1.25", "1.50", "1.75", "2.00", "2.25", "2.50", "2.75", "3.00"},
                3);
        Options operator1PhaseAmplitudeOptions    = new Options(
                "Operator 1 Phase Amplitude",
                new String[]{"0.25", "0.50", "0.75", "1.00", "1.25", "1.50", "1.75", "2.00", "2.25", "2.50", "2.75", "3.00"},
                3);
        Dial operator1AttackDial                  = new Dial(0.1);
        Dial operator1DecayDial                   = new Dial(1.0);
        Dial operator1SustainDial                 = new Dial(0.3);
        Dial operator1ReleaseDial                 = new Dial(1.0);
        boxOperator1.add(operator1RelativeFrequencyOptions);
        boxOperator1.add(operator1PhaseAmplitudeOptions);
        boxOperator1.add(new JLabel("  Op 1 ADSR   "));
        boxOperator1.add(operator1AttackDial);
        boxOperator1.add(operator1DecayDial);
        boxOperator1.add(operator1SustainDial);
        boxOperator1.add(operator1ReleaseDial);
        boxOperator1.add(boxOperator1.createGlue());
        // operator2: relative frequency option, phase amp dial, 4 x envelope dials
        Box boxOperator2                          = new Box(BoxLayout.X_AXIS);
        frame.add(boxOperator2);
        Options operator2RelativeFrequencyOptions = new Options(
                "Operator 2 Relative Frequency",
                new String[]{"0.25", "0.50", "0.75", "1.00", "1.25", "1.50", "1.75", "2.00", "2.25", "2.50", "2.75", "3.00"},
                3);
        Options operator2PhaseAmplitudeOptions    = new Options(
                "Operator 2 Phase Amplitude",
                new String[]{"0.25", "0.50", "0.75", "1.00", "1.25", "1.50", "1.75", "2.00", "2.25", "2.50", "2.75", "3.00"},
                3);
        Dial operator2AttackDial                  = new Dial(0.1);
        Dial operator2DecayDial                   = new Dial(1.0);
        Dial operator2SustainDial                 = new Dial(0.3);
        Dial operator2ReleaseDial                 = new Dial(1.0);
        boxOperator2.add(operator2RelativeFrequencyOptions);
        boxOperator2.add(operator2PhaseAmplitudeOptions);
        boxOperator2.add(new JLabel("  Op 2 ADSR  "));
        boxOperator2.add(operator2AttackDial);
        boxOperator2.add(operator2DecayDial);
        boxOperator2.add(operator2SustainDial);
        boxOperator2.add(operator2ReleaseDial);
        boxOperator2.add(boxOperator2.createGlue());
        // operator3: relative frequency option, phase amp dial, 4 x envelope dials
        Box boxOperator3                          = new Box(BoxLayout.X_AXIS);
        frame.add(boxOperator3);
        Options operator3RelativeFrequencyOptions = new Options(
                "Operator3 Relative Frequency",
                new String[]{"0.25", "0.50", "0.75", "1.00", "1.25", "1.50", "1.75", "2.00", "2.25", "2.50", "2.75", "3.00"},
                3);
        Options operator3PhaseAmplitudeOptions    = new Options(
                "Operator 3 Phase Amplitude",
                new String[]{"0.25", "0.50", "0.75", "1.00", "1.25", "1.50", "1.75", "2.00", "2.25", "2.50", "2.75", "3.00"},
                3);
        Dial operator3AttackDial                  = new Dial(0.1);
        Dial operator3DecayDial                   = new Dial(1.0);
        Dial operator3SustainDial                 = new Dial(0.3);
        Dial operator3ReleaseDial                 = new Dial(1.0);
        boxOperator3.add(operator3RelativeFrequencyOptions);
        boxOperator3.add(operator3PhaseAmplitudeOptions);
        boxOperator3.add(new JLabel("  Op3 ADSR  "));
        boxOperator3.add(operator3AttackDial);
        boxOperator3.add(operator3DecayDial);
        boxOperator3.add(operator3SustainDial);
        boxOperator3.add(operator3ReleaseDial);
        boxOperator3.add(boxOperator3.createGlue());
        // oscilloscope
        Box boxOscilloscope = new Box(BoxLayout.X_AXIS);
        frame.add(boxOscilloscope);
        Oscilloscope oscilloscope = new Oscilloscope();
        oscilloscope.setDelay(1);
        boxOscilloscope.add(oscilloscope);
        boxOscilloscope.add(boxOscilloscope.createGlue());
        // master adsr
        Box boxMasterADSR = new Box(BoxLayout.X_AXIS);
        frame.add(boxMasterADSR);
        boxMasterADSR.add(new JLabel("  ADSR  "));
        Dial masterADSRAttackDial                 = new Dial(0.1);
        Dial masterADSRDecayDial                  = new Dial(1.0);
        Dial masterADSRSustainDial                = new Dial(0.3);
        Dial masterADSRReleaseDial                = new Dial(1.0);
        boxMasterADSR.add(masterADSRAttackDial);
        boxMasterADSR.add(masterADSRDecayDial);
        boxMasterADSR.add(masterADSRSustainDial);
        boxMasterADSR.add(masterADSRReleaseDial);
        boxMasterADSR.add(boxMasterADSR.createGlue());
        frame.pack();
        frame.setVisible(true);

        // get midi pitches
        MidiModule midimod = new MidiModule(getMidi());
        modules.add(midimod);

        // get midi gate
        MidiGate gate = new MidiGate(midimod);
        modules.add(gate);


        // create 4 x adsr:
        // 3 for operators' amplitudes
        // 1 for master synth amplitude
        ADSR operator1ADSR = new ADSR(gate);
        ADSR operator2ADSR = new ADSR(gate);
        ADSR operator3ADSR = new ADSR(gate);
        ADSR masterADSR    = new ADSR(gate);
        // add them to be updated
        modules.add(operator1ADSR);
        modules.add(operator2ADSR);
        modules.add(operator3ADSR);
        modules.add(masterADSR);
        // wire them to the UI modules
        operator1ADSR.setAttackModule(operator1AttackDial.getModule());
        operator1ADSR.setDecayModule(operator1DecayDial.getModule());
        operator1ADSR.setSustainModule(operator1SustainDial.getModule());
        operator1ADSR.setReleaseModule(operator1ReleaseDial.getModule());
        operator1ADSR.setMultiplierModule(new Scaler(operator1PhaseAmplitudeOptions.getModule(), 0.25, 1.0));
        //
        operator2ADSR.setAttackModule(operator2AttackDial.getModule());
        operator2ADSR.setDecayModule(operator2DecayDial.getModule());
        operator2ADSR.setSustainModule(operator2SustainDial.getModule());
        operator2ADSR.setReleaseModule(operator2ReleaseDial.getModule());
        operator2ADSR.setMultiplierModule(new Scaler(operator2PhaseAmplitudeOptions.getModule(), 0.25, 1.0));
        //
        operator3ADSR.setAttackModule(operator3AttackDial.getModule());
        operator3ADSR.setDecayModule(operator3DecayDial.getModule());
        operator3ADSR.setSustainModule(operator3SustainDial.getModule());
        operator3ADSR.setReleaseModule(operator3ReleaseDial.getModule());
        operator2ADSR.setMultiplierModule(new Scaler(operator2PhaseAmplitudeOptions.getModule(), 0.25, 1.0));
        //
        masterADSR.setAttackModule(masterADSRAttackDial.getModule());
        masterADSR.setDecayModule(masterADSRDecayDial.getModule());
        masterADSR.setSustainModule(masterADSRSustainDial.getModule());
        masterADSR.setReleaseModule(masterADSRReleaseDial.getModule());

        // create a new pm synth
        ThreeOperatorPM h = new ThreeOperatorPM();
        modules.add(h);
        // hook up midi
        h.setFrequencyMod(midimod);
        // hook up UI to algorithms
        h.setAlgorithmMod(algorithmOptions.getModule());
        // hook up UI to operator relative frequency
        h.setOperatorRelativeFrequencyMod(1, new Scaler(operator1RelativeFrequencyOptions.getModule(), 0.25, 1.0));
        h.setOperatorRelativeFrequencyMod(2, new Scaler(operator2RelativeFrequencyOptions.getModule(), 0.25, 1.0));
        h.setOperatorRelativeFrequencyMod(3, new Scaler(operator3RelativeFrequencyOptions.getModule(), 0.25, 1.0));
        // hook up UI to operator adsr
        h.setOperatorOutputAmplitudeMod(1, operator1ADSR);
        h.setOperatorOutputAmplitudeMod(2, operator2ADSR);
        h.setOperatorOutputAmplitudeMod(3, operator3ADSR);
        // hook up synth into master adsr and oscilloscope
        Amplifier amp = new Amplifier(h);
        amp.setAmplitudeMod(masterADSR);
        modules.add(amp);
        Oscilloscope.OModule oModule = oscilloscope.getModule();
        modules.add(oModule);
        oModule.setAmplitudeModule(amp);
        setOutput(amp);
    }
}

