// Copyright 2018 by George Mason University


import javax.sound.sampled.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

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

    public void setOutput(Module outputModule) {
        this.outputModule = outputModule;
    }

    public Module getOutput() {
        return outputModule;
    }

    public void setMidi(Midi midi) {
        this.midi = midi;
    }

    public Midi getMidi() {
        return midi;
    }


    public static int getInt(String s) {
        try {
            return (Integer.parseInt(s));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static void showDevices(Midi midi, Mixer.Info[] mixers) {
        midi.displayDevices();
        System.err.println("\nAUDIO DEVICES:");
        for (int i = 0; i < mixers.length; i++)
            System.err.println("" + i + ":\t" + mixers[i].getName());

        System.err.println("\nFormat:\n\tjava Synth\t\t\t[displays available devices]\n\tjava Synth [midi] [audio]\t[runs synth with the given device numbers]");
    }

    public static void main(String[] args) {
        Midi midi = new Midi();
        Synth synth = new Synth();
        synth.audioFormat = new AudioFormat(Config.SAMPLING_RATE, 16, 1, true, false);
        Mixer.Info[] mixers = synth.getSupportedMixers();

        if (args.length == 0) {
            showDevices(midi, mixers);
        } else if (args.length == 2) {
            int x = getInt(args[0]);
            ArrayList<Midi.MidiDeviceWrapper> in = midi.getInDevices();
            if (x >= 0 && x < in.size()) {
                midi.setInDevice(in.get(x));
                System.err.println("MIDI: " + in.get(x));

                if (mixers == null) {
                    System.err.println("No output found which supports the desired sampling rate and bit depth\n");
                    showDevices(midi, mixers);
                    System.exit(1);
                } else {
                    x = getInt(args[1]);
                    if (x >= 0 && x < mixers.length) {
                        synth.setMixer(mixers[x]);
                        System.err.println("Audio: " + mixers[x].getName());
                        synth.setMidi(midi);
                        synth.setup();
                        synth.go();
                        synth.sdl.drain();
                        synth.sdl.close();
                    } else {
                        System.err.println("Invalid Audio number " + args[1] + "\n");
                        showDevices(midi, mixers);
                        System.exit(1);
                    }
                }
            } else {
                System.err.println("Invalid MIDI number " + args[0] + "\n");
                showDevices(midi, mixers);
                System.exit(1);
            }
        } else {
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

    Random getNewRandom() {
        synchronized (randomLock) {
            randomSeed += 10729347;  // or whatever
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


        if (tickCount >= Config.SAMPLING_RATE * 3) {
            val = (int) (d * 65535);
        } else {
            val = (int) (d * 0.0001 * 65535);
        }

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
     * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     * ==============================
     * DO NOT EDIT THIS SECTION ABOVE
     **/


    // ADJUST THIS VALUE IF YOU GET A LOT OF GLITCHY SOUND
    private static int numSamples = 1024;

    // DON'T ADJUST THIS VALUE
    private static int bufferSize = numSamples * 2;

    /**
     * MAKE YOUR EDITS TO THIS METHOD
     **/
    public void setup() {


        // 2 oscillators = 1 square 1 saw
        // 1 lfo - 2 dials depth / rate
        // 1 mixer - 3 dials for each osc + 1 gain
        // filter adsr - 4 dials
        // amplitude adsr - 4 dials

        JFrame frame   = new JFrame("Project 2");
        Box box        = new Box(BoxLayout.X_AXIS);

        Box boxOscil   = new Box(BoxLayout.Y_AXIS);
        Oscilloscope oscilloscope = new Oscilloscope();
        boxOscil.add(oscilloscope);
        boxOscil.add(boxOscil.createGlue());

        Box boxSquare  = new Box(BoxLayout.Y_AXIS);
        boxSquare.add(new JLabel("  blit sqr  "));
        boxSquare.add(new JLabel("amp"));
        Dial squareAmp = new Dial(1.0);
        boxSquare.add(squareAmp);
        boxSquare.add(boxSquare.createGlue());
        frame.add(boxSquare);

        Box boxSaw     = new Box(BoxLayout.Y_AXIS);
        boxSaw.add(new JLabel("  blit saw  "));
        boxSaw.add(new JLabel("amp"));
        Dial sawAmp    = new Dial(1.0);
        boxSaw.add(sawAmp);
        boxSaw.add(boxSaw.createGlue());
        frame.add(boxSaw);

        Box boxFilter = new Box(BoxLayout.Y_AXIS);
        boxFilter.add(new JLabel("  filter adsr  "));
        Dial filterADSRAttack  = new Dial(0.1);
        Dial filterADSRDecay   = new Dial(0.2);
        Dial filterADSRSustain = new Dial(0.8);
        Dial filterADSRRelease = new Dial(1.0);
        boxFilter.add(filterADSRAttack);
        boxFilter.add(filterADSRDecay);
        boxFilter.add(filterADSRSustain);
        boxFilter.add(filterADSRRelease);
        boxFilter.add(boxFilter.createGlue());
        frame.add(boxFilter);

        Box boxAmp          = new Box(BoxLayout.Y_AXIS);
        boxAmp.add(new JLabel("  amp ADSR  "));
        Dial ampADSRAttack  = new Dial(0.1);
        Dial ampADSRDecay   = new Dial(0.2);
        Dial ampADSRSustain = new Dial(0.8);
        Dial ampADSRRelease = new Dial(1.0);
        boxAmp.add(ampADSRAttack);
        boxAmp.add(ampADSRDecay);
        boxAmp.add(ampADSRSustain);
        boxAmp.add(ampADSRRelease);
        boxAmp.add(boxAmp.createGlue());
        frame.add(boxAmp);


        box.add(boxSquare);
        box.add(boxSaw);
        box.add(boxFilter);
        box.add(boxAmp);
        box.add(boxOscil);
        box.add(box.createGlue());
        frame.add(box);

        frame.pack();
        frame.setVisible(true);

        // create a midi listening module
        MidiModule midiFrequencyMod = new MidiModule(getMidi());
        modules.add(midiFrequencyMod);

        // get a midi note gate
        MidiGate midiNoteOnGateMod = new MidiGate(midiFrequencyMod);
        modules.add(midiNoteOnGateMod);

        // create an amplitude adsr
        ADSR adsr = new ADSR(midiNoteOnGateMod);
        adsr.setAttackModule(ampADSRAttack.getModule());
        adsr.setDecayModule(ampADSRDecay.getModule());
        adsr.setSustainModule(ampADSRSustain.getModule());
        adsr.setReleaseModule(ampADSRRelease.getModule());
        modules.add(adsr);

        // create an filter adsr
        ADSR filterADSR = new ADSR(midiNoteOnGateMod);
        filterADSR.setAttackModule(filterADSRAttack.getModule());
        filterADSR.setDecayModule(filterADSRDecay.getModule());
        filterADSR.setSustainModule(filterADSRSustain.getModule());
        filterADSR.setReleaseModule(filterADSRRelease.getModule());
        modules.add(filterADSR);

        // create osc1 & osc2
        BlitSquare osc1 = new BlitSquare();
        osc1.setFrequencyMod(midiFrequencyMod);
        BlitSaw osc2 = new BlitSaw();
        osc2.setFrequencyMod(midiFrequencyMod);
        modules.add(osc1);
        modules.add(osc2);

        // add them together in a mixer
        Mixer3 mixer = new Mixer3();
        mixer.setInput1(osc1);
        mixer.setInput2(osc2);
        mixer.setAmplitude1(squareAmp.getModule());
        mixer.setAmplitude2(sawAmp.getModule());
        modules.add(mixer);

        // set up the filter adsr
        LPF filter = new LPF(mixer, filterADSR, new ConstantValue(0.9));
        modules.add(filter);

        // set up the amplitude adsr
        Amplifier amp = new Amplifier(mixer);
        amp.setAmplitudeMod(adsr);
        modules.add(amp);

        oscilloscope.setDelay(1);
        Oscilloscope.OModule oModule = oscilloscope.getModule();
        oModule.setAmplitudeModule(amp);
        modules.add(oModule);

        setOutput(amp);




















        //===============


//        // create a midi listening module
//        MidiModule midiFrequencyMod = new MidiModule(getMidi());
//        modules.add(midiFrequencyMod);
//
//        // get a midi note gate
//        MidiGate midiNoteOnGateMod = new MidiGate(midiFrequencyMod);
//        modules.add(midiNoteOnGateMod);
//
//        // create an adsr
//        ADSR adsr = new ADSR(midiNoteOnGateMod);
//        modules.add(adsr);
//
//        // signal
//        BlitSaw  s1 = new BlitSaw();
//        BlitSaw  s2 = new BlitSaw();
//        BlitSaw  s3 = new BlitSaw();
//        Function f1 = new Function();
//        Function f2 = new Function();
//        // set s1's frequency at midi
//        s1.setFrequencyMod(midiFrequencyMod);
//        // set s2's frequency a p5 above s1's
//        f1.setType(Function.Type.MULTIPLY);
//        f1.setA(midiFrequencyMod);
//        f1.setB(new ConstantValue(1.5));
//        s2.setFrequencyMod(f1);
//        // set s3's frequency a p5 above s2's
//        f2.setType(Function.Type.MULTIPLY);
//        f2.setA(f1);
//        f2.setB(new ConstantValue(1.5));
//        s3.setFrequencyMod(f2);
//        // add modules
//        modules.add(s1);
//        modules.add(s2);
//        modules.add(s3);
//        modules.add(f1);
//        modules.add(f2);
//
//        Mixer3 mixer3       = new Mixer3();
//        mixer3.setInput1(s1); mixer3.setAmplitude1(0.1);
//        mixer3.setInput2(s2); mixer3.setAmplitude2(0.1);
//        mixer3.setInput3(s3); mixer3.setAmplitude3(0.1);
//        modules.add(mixer3);
//
//        // create a "vca"
//        Amplifier vca = new Amplifier(mixer3);
//        // modulate it by the envelope
//        vca.setAmplitudeMod(adsr);
//        modules.add(vca);
//
//        // gain reduction thus we don't blow speakers
//        Amplifier gain = new Amplifier(vca);
//        gain.setAmplitudeMod(new ConstantValue(0.1));
//        modules.add(gain);
//
//        setOutput(gain);
    }
}


