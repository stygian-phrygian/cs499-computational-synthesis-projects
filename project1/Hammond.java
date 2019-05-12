import java.util.ArrayList;
import java.util.HashMap;

public class Hammond extends Module {
    private Osc[]    oscillators;
    private Module[] frequencyModulators;
    private Module[] amplitudeModulators;
    private Module   frequency;
    private int[][]  presets;
    private double   lastPreset = 0;
    private Module   presetModulator = new ConstantValue(lastPreset);

    public Hammond() {
        frequency = new ConstantValue(Utils.hzToValue(440));

        // populate the oscillators and their modulators
        int n                = 9;
        oscillators          = new Osc[n];
        frequencyModulators  = new Module[n];
        amplitudeModulators  = new Module[n];
        for (int i = 0; i < n; i++) {
            // create oscillator/frequency modulator/amplitude modulator
            oscillators[i]         = new Osc();
            frequencyModulators[i] = new ConstantValue(Utils.hzToValue(440));
            amplitudeModulators[i] = new ConstantValue(6); // drawbar range: 0 - 8
            // connect frequency modulator to oscillator
            oscillators[i].setFrequencyMod(frequencyModulators[i]);
        }

        recalibrateFrequencyModulators();

        instantiatePresets();
    }

    public void setFrequencyMod(Module m) { frequency = m; }
    public void setDrawbarMod(int n, Module m) {
        switch (n) {
            case 1: amplitudeModulators[0] = m; break;
            case 2: amplitudeModulators[1] = m; break;
            case 3: amplitudeModulators[2] = m; break;
            case 4: amplitudeModulators[3] = m; break;
            case 5: amplitudeModulators[4] = m; break;
            case 6: amplitudeModulators[5] = m; break;
            case 7: amplitudeModulators[6] = m; break;
            case 8: amplitudeModulators[7] = m; break;
            case 9: amplitudeModulators[8] = m; break;
        }
    }

    private void recalibrateFrequencyModulators() {
        // get fundamental frequency
        double f = frequency.getValue();
        // set the correct frequencies for the oscillators
        frequencyModulators[0].setValue(f*0.5);  // sub-octave
        frequencyModulators[1].setValue(f*1.5);  // perfect 5th
        frequencyModulators[2].setValue(f);      // fundamental
        frequencyModulators[3].setValue(f*2);    // 2nd harmonic
        frequencyModulators[4].setValue(f*3);    // 3rd harmonic
        frequencyModulators[5].setValue(f*4);    // 4th harmonic
        frequencyModulators[6].setValue(f*5);    // 5th harmonic
        frequencyModulators[7].setValue(f*6);    // 6th harmonic
        frequencyModulators[8].setValue(f*8);    // 8th harmonic
    }

    private void setAmplitudeValues(int[] amplitudeValues) {
        if (amplitudeValues.length < 9) { return; }
        amplitudeModulators[0].setValue(amplitudeValues[0]);
        amplitudeModulators[1].setValue(amplitudeValues[1]);
        amplitudeModulators[2].setValue(amplitudeValues[2]);
        amplitudeModulators[3].setValue(amplitudeValues[3]);
        amplitudeModulators[4].setValue(amplitudeValues[4]);
        amplitudeModulators[5].setValue(amplitudeValues[5]);
        amplitudeModulators[6].setValue(amplitudeValues[6]);
        amplitudeModulators[7].setValue(amplitudeValues[7]);
        amplitudeModulators[8].setValue(amplitudeValues[8]);
    }

    public double tick(long tickCount) {

        // maybe update preset
        if(presetModulator.getValue() != lastPreset &&
           presetModulator.getValue() <= presets.length) {
                int presetIndex = (int)presetModulator.getValue();
                setAmplitudeValues(presets[presetIndex]);
                lastPreset = presetIndex;
        }

        // update oscillators' frequency
        recalibrateFrequencyModulators();
        // tick oscillators and accumulate their output
        double out = 0.5;
        for(int i = 0; i < oscillators.length; ++i) {
            out += oscillators[i].tick(tickCount) * 0.125 * Math.min(Math.max(0, amplitudeModulators[i].getValue()), 8.0);
        }
        // remove clipping
        out *= 0.05;
        return out;
    }

    public void setPresetMod(Module m) {
        this.presetModulator = m;
    }

    private void instantiatePresets() {
        presets = new int[][]{
                // "Alone in the City"
                new int[]{0, 0, 7, 7, 4, 0, 0, 3, 4},
                // "America (Gospel) (U)"
                new int[]{8, 8, 7, 7, 2, 4, 1, 1, 0},
                // "America (Gospel) (L)"
                new int[]{0, 0, 6, 6, 0, 6, 0, 0, 0},
                // "Blues"
                new int[]{8, 8, 5, 3, 2, 4, 5, 8, 8},
                // "Booker T. Jones 1"
                new int[]{8, 8, 8, 8, 0, 0, 0, 0, 0},
                // "Booker T. Jones 2"
                new int[]{8, 8, 8, 6, 3, 0, 0, 0, 0},
                // "Born to B3 (Gospel) (U)"
                new int[]{8, 8, 8, 8, 0, 8, 0, 0, 8},
                // "Born to B3 (Gospel) (L)"
                new int[]{0, 0, 7, 7, 2, 5, 4, 0, 0},
                // "Brian Auger 1"
                new int[]{8, 8, 8, 1, 1, 0, 0, 0, 0},
                // "Brian Auger 2"
                new int[]{8, 8, 8, 8, 0, 5, 0, 0, 0},
                // "Bright Comping"
                new int[]{8, 7, 8, 0, 0, 0, 4, 5, 6},
                // "Brother Jack"
                new int[]{8, 0, 0, 0, 0, 0, 8, 8, 8},
                // "Dark Comping"
                new int[]{8, 4, 3, 0, 0, 0, 0, 0, 0},
                // "Dark Solo A (U)"
                new int[]{8, 8, 8, 8, 8, 8, 8, 8, 8},
                // "Dark Solo A (L)"
                new int[]{6, 6, 2, 0, 0, 0, 0, 0, 0},
                // "Dark Solo B (U)"
                new int[]{8, 2, 8, 2, 0, 0, 0, 0, 2},
                // "Dark Solo B (L)"
                new int[]{6, 0, 6, 0, 0, 0, 0, 0, 0},
                // "Fat"
                new int[]{8, 8, 8, 0, 0, 0, 8, 8, 8},
                // "Fifth Organ (Gospel) (U)"
                new int[]{0, 8, 0, 0, 8, 0, 8, 8, 3},
                // "Fifth Organ (Gospel) (L)"
                new int[]{0, 0, 8, 8, 0, 6, 0, 0, 0},
                // "Flutes"
                new int[]{0, 0, 6, 8, 0, 2, 0, 0, 0},
                // "Full and High"
                new int[]{8, 8, 8, 6, 6, 6, 8, 8, 8},
                // "Full and Sweet"
                new int[]{8, 6, 8, 8, 6, 8, 0, 6, 8},
                // "Full Organ"
                new int[]{8, 8, 8, 8, 8, 8, 8, 8, 8},
                // "Funky Comping"
                new int[]{6, 8, 8, 6, 0, 0, 0, 0, 4},
                // "Gimme Some Loving"
                new int[]{8, 8, 8, 8, 0, 0, 0, 0, 0},
                // "Gospel 1"
                new int[]{8, 0, 8, 8, 0, 8, 0, 0, 8},
                // "Gospel 2"
                new int[]{8, 8, 8, 0, 0, 0, 0, 0, 8},
                // "Greg Allman 1"
                new int[]{8, 6, 8, 6, 6, 6, 5, 6, 8},
                // "Greg Allman 2"
                new int[]{8, 8, 8, 6, 0, 0, 0, 0, 0},
                // "Greg Allman 3"
                new int[]{8, 8, 6, 0, 0, 0, 0, 4, 0},
                // "Greg Rolie 1"
                new int[]{8, 8, 8, 8, 0, 0, 0, 8, 8},
                // "Greg Rolie 2"
                new int[]{8, 8, 6, 4, 0, 0, 0, 0, 0},
                // "Greg Rolie 4"
                new int[]{8, 8, 8, 8, 8, 6, 6, 6, 6},
                // "Groove Holmes (Gospel) (U)"
                new int[]{8, 8, 8, 4, 2, 0, 0, 8, 0},
                // "Groove Holmes (Gospel) (L)"
                new int[]{0, 0, 0, 5, 0, 5, 0, 0, 0},
                // "House Bass (Gospel) (U)"
                new int[]{8, 8, 0, 0, 0, 0, 0, 0, 0},
                // "House Bass (Gospel) (L)"
                new int[]{0, 0, 8, 0, 8, 0, 0, 0, 0},
                // "Jimmy McGriff 1"
                new int[]{8, 6, 8, 6, 0, 0, 0, 0, 6},
                // "Jimmy McGriff 2 (Gospel) (U)"
                new int[]{8, 8, 3, 2, 0, 0, 1, 2, 5},
                // "Jimmy McGriff 2 (Gospel) (L)"
                new int[]{4, 4, 8, 6, 5, 0, 0, 0, 0},
                // "Jimmy Smith 1 (U)"
                new int[]{8, 8, 8, 8, 8, 8, 8, 8, 8},
                // "Jimmy Smith 1 (L)"
                new int[]{0, 0, 7, 5, 0, 0, 0, 0, 0},
                // "Jimmy Smith 2 (U)"
                new int[]{8, 8, 8, 0, 0, 0, 0, 0, 0},
                // "Jimmy Smith 2 (L)"
                new int[]{8, 3, 8, 0, 0, 0, 0, 0, 0},
                // "Jimmy Smith 3 (U)"
                new int[]{8, 8, 8, 0, 0, 0, 0, 0, 0},
                // "Jimmy Smith 3 (L)"
                new int[]{8, 0, 8, 0, 0, 0, 0, 0, 0},
                // "Joey DeFrancesco"
                new int[]{8, 8, 8, 4, 0, 0, 0, 8, 0},
                // "Jon Lord"
                new int[]{8, 8, 4, 4, 0, 0, 0, 0, 0},
                // "Latin (Gospel) (U)"
                new int[]{8, 8, 0, 0, 6, 0, 0, 0, 0},
                // "Latin (Gospel) (L)"
                new int[]{0, 0, 6, 6, 7, 6, 0, 0, 0},
                // "Matthew Fisher"
                new int[]{8, 0, 0, 8, 0, 8, 0, 0, 0},
                // "Melvin Crispel"
                new int[]{8, 6, 8, 8, 0, 0, 0, 0, 4},
                // "Mellow"
                new int[]{8, 0, 3, 6, 0, 0, 0, 0, 0},
                // "Meditation Time (Gospel) (U)"
                new int[]{0, 0, 7, 8, 0, 0, 4, 5, 3},
                // "Meditation Time (Gospel) (L)"
                new int[]{0, 0, 6, 7, 0, 0, 5, 4, 0},
                // "Paul Shaffer 1"
                new int[]{8, 8, 6, 8, 0, 0, 3, 0, 0},
                // "Paul Shaffer 2"
                new int[]{8, 8, 8, 7, 6, 8, 8, 8, 8},
                // "Paul Shaffer 3"
                new int[]{8, 8, 8, 8, 7, 8, 6, 7, 8},
                // "Pink Floyd"
                new int[]{8, 5, 0, 0, 0, 5, 0, 0, 0},
                // "Power Chords"
                new int[]{8, 8, 8, 8, 0, 0, 0, 0, 0},
                // "Progessive (Gospel) (U)"
                new int[]{8, 8, 8, 8, 0, 0, 0, 0, 0},
                // "Progessive (Gospel) (L)"
                new int[]{0, 0, 8, 8, 8, 4, 0, 0, 0},
                // "Ray Charles"
                new int[]{0, 0, 6, 8, 7, 6, 4, 0, 0},
                // "Reggae"
                new int[]{8, 0, 8, 0, 0, 0, 0, 0, 8},
                // "Rock R&B (U)
                new int[]{8, 8, 8, 8, 0, 0, 0, 0, 0},
                // "Rock R&B (L)
                new int[]{8, 4, 8, 0, 0, 0, 0, 0, 0},
                // "Screaming (Gospel) (U)"
                new int[]{8, 0, 0, 3, 8, 8, 8, 8, 8},
                // "Screaming (Gospel) (L)"
                new int[]{0, 0, 7, 0, 3, 3, 3, 3, 3},
                // "Shirley Scott"
                new int[]{0, 0, 8, 8, 8, 8, 8, 0, 0},
                // "Simmering"
                new int[]{8, 3, 0, 0, 0, 0, 3, 7, 8},
                // "Shouting 1"
                new int[]{8, 7, 6, 5, 5, 6, 7, 8, 8},
                // "Shouting 2"
                new int[]{6, 6, 8, 8, 4, 8, 5, 8, 8},
                // "Shouting 3 (Gospel) (U)"
                new int[]{8, 7, 8, 6, 4, 5, 4, 6, 6},
                // "Shouting 3 (Gospel) (L)"
                new int[]{8, 8, 8, 8, 0, 0, 0, 0, 0},
                // "Slow Balllad"
                new int[]{0, 0, 8, 4, 0, 0, 0, 0, 0},
                // "Slowly"
                new int[]{0, 6, 8, 8, 4, 0, 0, 0, 3},
                // "Soft Backing (Gospel) (U)"
                new int[]{8, 8, 8, 7, 0, 0, 0, 0, 0},
                // "Soft Backing (Gospel) (L)"
                new int[]{5, 5, 5, 4, 0, 0, 0, 0, 0},
                // "Soft Chords"
                new int[]{8, 0, 8, 4, 0, 0, 0, 0, 8},
                // "Speaker Talking (Gospel) (U)"
                new int[]{6, 7, 8, 4, 0, 4, 2, 3, 1},
                // "Speaker Talking (Gospel) (L)"
                new int[]{0, 0, 6, 6, 0, 2, 0, 2, 4},
                // "Steppenwolf"
                new int[]{8, 8, 8, 6, 4, 3, 2, 0, 0},
                // "Steve Winwood"
                new int[]{8, 8, 8, 8, 7, 6, 7, 8, 8},
                // "Strings"
                new int[]{8, 7, 6, 5, 4, 3, 2, 1, 1},
                // "Sweet"
                new int[]{0, 0, 8, 0, 0, 0, 0, 0, 0},
                // "Testimony Service  (Gospel) (U)"
                new int[]{7, 8, 7, 7, 4, 6, 0, 4, 6},
                // "Testimony Service  (Gospel) (L)"
                new int[]{0, 0, 8, 8, 0, 0, 6, 7, 3},
                // "Theatre Organ (Gospel) (U)"
                new int[]{8, 7, 8, 6, 5, 6, 4, 6, 7},
                // "Theatre Organ (Gospel) (L)"
                new int[]{0, 0, 8, 8, 4, 4, 0, 0, 0},
                // "Tom Coster"
                new int[]{8, 8, 8, 8, 0, 0, 0, 0, 0},
                // "Whistle 1"
                new int[]{8, 0, 0, 0, 0, 0, 0, 0, 8},
                // "Whistle 2"
                new int[]{8, 8, 8, 0, 0, 0, 0, 0, 8},
                // "Whiter Shade Of Pale 1 (U)"
                new int[]{6, 8, 8, 6, 0, 0, 0, 0, 0},
                // "Whiter Shade Of Pale 1 (L)"
                new int[]{8, 8, 0, 0, 7, 0, 7, 7, 0},
                // "Whiter Shade Of Pale 2 (U)"
                new int[]{8, 8, 8, 8, 0, 8, 0, 0, 6},
                // "Whiter Shade Of Pale 2 (L)"
                new int[]{0, 0, 4, 4, 4, 0, 0, 0, 0},
                // "Wide Leslie"
                new int[]{8, 6, 6, 8, 0, 0, 0, 0, 0}
        };
    }
}
