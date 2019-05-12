// Copyright 2018 by George Mason University


import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.swing.JComponent;
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

        // create a new frame
        JFrame frame = new JFrame("Project 1");
        frame.setLayout(new FlowLayout());
        // create dials/options/oscilloscope
        Box box = new Box(BoxLayout.X_AXIS);
        Options drawbar1 = new Options("  drawbar 1  ", new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}, 6);
        Options drawbar2 = new Options("  drawbar 2  ", new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}, 6);
        Options drawbar3 = new Options("  drawbar 3  ", new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}, 6);
        Options drawbar4 = new Options("  drawbar 4  ", new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}, 6);
        Options drawbar5 = new Options("  drawbar 5  ", new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}, 6);
        Options drawbar6 = new Options("  drawbar 6  ", new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}, 6);
        Options drawbar7 = new Options("  drawbar 7  ", new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}, 6);
        Options drawbar8 = new Options("  drawbar 8  ", new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}, 6);
        Options drawbar9 = new Options("  drawbar 9  ", new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}, 6);
        Dial adsrAttack  = new Dial(0.1);
        Dial adsrDecay   = new Dial(0.1);
        Dial adsrSustain = new Dial(1.0);
        Dial adsrRelease = new Dial(0.4);
        Dial gain        = new Dial(1.0);
        Oscilloscope o   = new Oscilloscope();
        o.setDelay(1);
        Options PRESETS = new Options("PRESETS", new String[] {"Alone in the City", "America (Gospel) (U)", "America (Gospel) (L)", "Blues", "Booker T. Jones 1", "Booker T. Jones 2", "Born to B3 (Gospel) (U)", "Born to B3 (Gospel) (L)",
                "Brian Auger 1", "Brian Auger 2", "Bright Comping", "Brother Jack", "Dark Comping", "Dark Solo A (U)", "Dark Solo A (L)", "Dark Solo B (U)",
                "Dark Solo B (L)", "Fat", "Fifth Organ (Gospel) (U)", "Fifth Organ (Gospel) (L)", "Flutes", "Full and High", "Full and Sweet", "Full Organ",
                "Funky Comping", "Gimme Some Loving", "Gospel 1", "Gospel 2", "Greg Allman 1", "Greg Allman 2", "Greg Allman 3", "Greg Rolie 1",
                "Greg Rolie 2", "Greg Rolie 4", "Groove Holmes (Gospel) (U)", "Groove Holmes (Gospel) (L)", "House Bass (Gospel) (U)", "House Bass (Gospel) (L)", "Jimmy McGriff 1", "Jimmy McGriff 2 (Gospel) (U)",
                "Jimmy McGriff 2 (Gospel) (L)", "Jimmy Smith 1 (U)", "Jimmy Smith 1 (L)", "Jimmy Smith 2 (U)", "Jimmy Smith 2 (L)", "Jimmy Smith 3 (U)", "Jimmy Smith 3 (L)", "Joey DeFrancesco",
                "Jon Lord", "Latin (Gospel) (U)", "Latin (Gospel) (L)", "Matthew Fisher", "Melvin Crispel", "Mellow", "Meditation Time (Gospel) (U)", "Meditation Time (Gospel) (L)",
                "Paul Shaffer 1", "Paul Shaffer 2", "Paul Shaffer 3", "Pink Floyd", "Power Chords", "Progessive (Gospel) (U)", "Progessive (Gospel) (L)", "Ray Charles",
                "Reggae", "Rock R&B (U)", "Rock R&B (L)", "Screaming (Gospel) (U)", "Screaming (Gospel) (L)", "Shirley Scott", "Simmering", "Shouting 1",
                "Shouting 2", "Shouting 3 (Gospel) (U)", "Shouting 3 (Gospel) (L)", "Slow Balllad", "Slowly", "Soft Backing (Gospel) (U)", "Soft Backing (Gospel) (L)", "Soft Chords",
                "Speaker Talking (Gospel) (U)", "Speaker Talking (Gospel) (L)", "Steppenwolf", "Steve Winwood", "Strings", "Sweet", "Testimony Service  (Gospel) (U)", "Testimony Service  (Gospel) (L)",
                "Theatre Organ (Gospel) (U)", "Theatre Organ (Gospel) (L)", "Tom Coster", "Whistle 1", "Whistle 2", "Whiter Shade Of Pale 1 (U)", "Whiter Shade Of Pale 1 (L)", "Whiter Shade Of Pale 2 (U)",
                "Whiter Shade Of Pale 2 (L)", "Wide Leslie"},
                0);
        box.add(drawbar1);
        box.add(drawbar2);
        box.add(drawbar3);
        box.add(drawbar4);
        box.add(drawbar5);
        box.add(drawbar6);
        box.add(drawbar7);
        box.add(drawbar8);
        box.add(drawbar9);
        box.add(new JLabel("  ADSR  "));
        box.add(adsrAttack);
        box.add(adsrDecay);
        box.add(adsrSustain);
        box.add(adsrRelease);
        box.add(new JLabel("  Volume  "));
        box.add(gain);
        box.add(PRESETS);
        box.add(o);
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

        // create an adsr
        ADSR adsr = new ADSR(midiNoteOnGateMod);
        adsr.setAttackModule(adsrAttack.getModule());
        adsr.setDecayModule(adsrDecay.getModule());
        adsr.setSustainModule(adsrSustain.getModule());
        adsr.setReleaseModule(adsrRelease.getModule());
        adsr.setMultiplierModule(gain.getModule());
        modules.add(adsr);

        // create our hammond instrument
        Hammond h = new Hammond();
        // set its frequency with the midi frequency
        h.setFrequencyMod(midiFrequencyMod);
        h.setPresetMod(PRESETS.getModule());
        h.setDrawbarMod(1, drawbar1.getModule());
        h.setDrawbarMod(2, drawbar2.getModule());
        h.setDrawbarMod(3, drawbar3.getModule());
        h.setDrawbarMod(4, drawbar4.getModule());
        h.setDrawbarMod(5, drawbar5.getModule());
        h.setDrawbarMod(6, drawbar6.getModule());
        h.setDrawbarMod(7, drawbar7.getModule());
        h.setDrawbarMod(8, drawbar8.getModule());
        h.setDrawbarMod(9, drawbar9.getModule());
        modules.add(h);

        // create an amp
        Amplifier amp = new Amplifier(h);
        // modulate it by the envelope
        amp.setAmplitudeMod(adsr);
        modules.add(amp);

        // display on oscilloscope on amp
        Oscilloscope.OModule oModule = o.getModule();
        oModule.setAmplitudeModule(amp);
        modules.add(oModule);

        setOutput(amp);

    }

}

