import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

/**
 * This is a class which does a lot of midi missage processing
 * It sets a few values internally which are useful to extract in other modules
 * By default, as a module, it outputs the pitch (with bend) as a value from 0 to 1
 **/
public class MidiModule extends Module {
    private static final double base = Math.pow(2, 1 / 12.0);
    Midi midi;
    private int lastnote = 0;
    private double pitch;
    private double velocity;
    private double bend = 1.0;
    private double rawBend;
    //private double rawVal;

    public MidiModule(Midi midi) {
        this.midi = midi;
        }

    // Processes a PITCH BEND message.
    void processPitchBend(ShortMessage sm) {
        int lsb = sm.getData1();
        int msb = sm.getData2();

        // Linux Java distros have a bug: pitch bend data is treated
        // as a signed two's complement integer, which is wrong, wrong, wrong.
        // So we have to special-case it here. See:
        //
        // https://bugs.openjdk.java.net/browse/JDK-8075073
        // https://bugs.launchpad.net/ubuntu/+source/openjdk-8/+bug/1755640

        if (Utils.isUnix()) {
            if (msb >= 64) {
                msb = msb - 64;
                } else {
                msb = msb + 64;
                }
            }

        int rawBend = (lsb + msb * 128) - 8192;

        if (rawBend < -8191) {
            rawBend = -8191;
            }

        // The 2 at the end is the octave
        this.rawBend = rawBend / 8191.0 * 24;
        bend = Utils.hybridpow(2.0, rawBend / 8191.0 * 2);

        }


    @Override
    public double tick(long tickCount) {
        MidiMessage[] messages = midi.getNextMessages();
        int note = 0;
        int vel = 0;
        for (MidiMessage message : messages) {
            if (message instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) message;
                switch (sm.getCommand()) {
                case ShortMessage.NOTE_ON:
                    note = sm.getData1();
                    vel = sm.getData2();
                    break;
                case ShortMessage.NOTE_OFF:
                    note = sm.getData1();
                    if (note == lastnote) {
                        vel = 0;
                        } else {
                        continue;
                        }
                    break;
                case ShortMessage.PITCH_BEND:
                    processPitchBend(sm);
                default:
                    note = lastnote;
                    vel = (int) (velocity * 127);
                    break;

                    }
                lastnote = note;
                velocity = vel / 127.0;
                pitch = Utils.hzToValue(440.0 * Math.pow(base, note - 69) * bend);
                //rawVal = note + rawBend;
                }


            }
        return pitch;
        }

    /*
      public double getRawVal() {
      return rawVal;
      }

      public double getVal() {
      return Utils.hzToValue(val);
      }
    */
    
    public double getBend() {
        return bend;
        }
        
    public double getPitch() {
        return pitch;
        }

    public double getVelocity() {
        return velocity;
        }

    public double getGate() {
        return velocity > 0 ? 1 : 0;
        }

    }
