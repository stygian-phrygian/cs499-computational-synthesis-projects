public class ADSR extends Module {
    private int       attackDurationInTicks, decayDurationInTicks, releaseDurationInTicks;
    private double    sustainLevel;
    private int       currentStageTick  = 0;    // used to determine how long to stay within a stage
    private double    stageRate         = 1.0;  // used to inc/dec the current output value in a stage
    private double    previousGateValue = 0.0;  // used to determine gate change (entering attack or release stage)
    private Module gate;                     // the module to pull gate values from
    private enum      ADSRStage { OFF_STAGE, ATTACK_STAGE, DECAY_STAGE, SUSTAIN_STAGE, RELEASE_STAGE; }
    private ADSRStage currentStage      = ADSRStage.OFF_STAGE;
    private double    out               = 0.0;  // the adsr envelope's final output level for each tick
    // keeps track of prior values in case we changed and need a recompute
    private double priorAttackDurationInSeconds  = 0.3;
    private double priorDecayDurationInSeconds   = 0.3;
    private double priorSustainLevel             = 1.0;
    private double priorReleaseDurationInSeconds = 0.3;
    // the modules which control everything
    private Module attackModule;
    private Module decayModule;
    private Module sustainModule;
    private Module releaseModule;
    // multiplier gain at the end of the signal processing
    private Module multiplierModule;
    public ADSR(Module gate) {
        // save our gate module
        this.gate = gate;
        // provide default adsr durations
        this.attackModule  = new ConstantValue(0.1);
        this.decayModule   = new ConstantValue(1.0);
        this.sustainModule = new ConstantValue(0.3);
        this.releaseModule = new ConstantValue(1.0);
        // save their last values
        this.priorAttackDurationInSeconds  = this.attackModule.getValue();
        this.priorDecayDurationInSeconds   = this.decayModule.getValue();
        this.priorSustainLevel             = this.sustainModule.getValue();
        this.priorReleaseDurationInSeconds = this.releaseModule.getValue();
        // set them internally to tick counts we can utilize
        setAttack(priorAttackDurationInSeconds);
        setDecay(priorDecayDurationInSeconds);
        setSustain(priorSustainLevel);
        setRelease(priorReleaseDurationInSeconds);
        // set multiplier
        this.multiplierModule = new ConstantValue(1.0);
    }
    private void setAttack(double durationInSeconds) {
        // update attack time
        attackDurationInTicks = (int) (durationInSeconds * Config.SAMPLING_RATE);
        // update attack stage rate (if we're in this stage)
        if(currentStage == ADSRStage.ATTACK_STAGE) { calculateStageRate(currentStage); }
    }
    private void setDecay(double durationInSeconds) {
        // update decay time
        decayDurationInTicks = (int) (durationInSeconds * Config.SAMPLING_RATE);
        // update decay stage rate (if we're in this stage)
        if(currentStage == ADSRStage.DECAY_STAGE) { calculateStageRate(currentStage); }
    }
    private void setSustain(double sustainLevel) {
        this.sustainLevel = Math.min(Math.max(0.0, sustainLevel), 1.0); // clamp between 0 and 1
        // update decay stage rate (if we're in decay stage)
        if(currentStage == ADSRStage.DECAY_STAGE) { calculateStageRate(currentStage); }
    }
    private void setRelease(double durationInSeconds) {
        // update release time
        releaseDurationInTicks = (int) (durationInSeconds * Config.SAMPLING_RATE);
        // update release stage rate (if we're in this stage)
        if(currentStage == ADSRStage.RELEASE_STAGE) { calculateStageRate(currentStage); }
    }


    // calculates the stage's rate of increase/decrease
    // should be called at stage changes or mutation/initialization of a stage
    private void calculateStageRate(ADSRStage stage) {
        switch(stage) {
            case ATTACK_STAGE:
                stageRate = (1.0 - out) / (attackDurationInTicks - currentStageTick);
                break;
            case DECAY_STAGE:
                stageRate = -(1.0 - sustainLevel) / (decayDurationInTicks - currentStageTick);
                break;
            case RELEASE_STAGE:
                stageRate = -out / (releaseDurationInTicks - currentStageTick);
                break;
            case SUSTAIN_STAGE:
            case OFF_STAGE:
                // do nothing
        }
    }

    public double tick(long tickCount) {

        // update times (if they changed)
        if(attackModule.getValue() != priorAttackDurationInSeconds) {
            setAttack(attackModule.getValue());
            priorAttackDurationInSeconds = attackModule.getValue();
        }
        if(decayModule.getValue() != priorDecayDurationInSeconds) {
            setDecay(decayModule.getValue());
            priorDecayDurationInSeconds = decayModule.getValue();
        }
        if(sustainModule.getValue() != priorSustainLevel) {
            setSustain(sustainModule.getValue());
            priorSustainLevel = sustainModule.getValue();
        }
        if(releaseModule.getValue() != priorReleaseDurationInSeconds) {
            setRelease(releaseModule.getValue());
            priorReleaseDurationInSeconds = releaseModule.getValue();
        }

        // get the current gate value and compare it
        // to the previous gate value to ascertain
        // if we should trigger the attack or release stage
        double currentGateValue = gate.getValue();
        if(previousGateValue != currentGateValue) {
            // reset the stage's current tick
            currentStageTick = 0;
            // set which stage we're at
            if (previousGateValue == 0) {
                currentStage = ADSRStage.ATTACK_STAGE;
            } else {
                currentStage = ADSRStage.RELEASE_STAGE;
            }
            // calculate the relevant stage rate
            calculateStageRate(currentStage);
        }

        // save current gate value for later comparisons
        previousGateValue = currentGateValue;

        // determine which stage we're at and adjust adsr output accordingly
        // we're really only checking for the attack/decay/release stages here
        // as the sustain/off stages do nothing to the current output
        switch(currentStage) {
            case ATTACK_STAGE:
                if (currentStageTick < attackDurationInTicks) {
                    out += stageRate;
                } else {
                    // set decay stage
                    currentStage = ADSRStage.DECAY_STAGE;
                    // reset the stage's current tick (for next stage)
                    currentStageTick = 0;
                    // and of course, reach a 1.0
                    out = 1.0;
                    // calculate the relevant stage rate
                    calculateStageRate(currentStage);
                }
                break;
            case DECAY_STAGE:
                if (currentStageTick < decayDurationInTicks) {
                    out +=  stageRate;
                } else {
                    // set sustain stage
                    currentStage = ADSRStage.SUSTAIN_STAGE;
                    // and of course, reach sustain
                    out = sustainLevel;
                }
                break;
            case RELEASE_STAGE:
                if (currentStageTick < releaseDurationInTicks) {
                    out += stageRate;
                } else {
                    // set off stage
                    currentStage = ADSRStage.OFF_STAGE;
                    // and of course, reach 0.0
                    out = 0.0;
                }
                break;
            case SUSTAIN_STAGE:
            case OFF_STAGE:
                // do nothing
        }

        // update current stage tick
        currentStageTick += 1;

        return out * multiplierModule.getValue();
    }

    //
    public void setAttackModule(Module m)     { this.attackModule     = m; }
    public void setDecayModule(Module m)      { this.decayModule      = m; }
    public void setSustainModule(Module m)    { this.sustainModule    = m; }
    public void setReleaseModule(Module m)    { this.releaseModule    = m; }
    //
    public void setMultiplierModule(Module m) { this.multiplierModule = m; }
}
