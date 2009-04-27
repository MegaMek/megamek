package megamek.common;

/**
 * This lass represents Land Air Mechs. It is to be considered experimental, and
 * not yet ready for release.
 * 
 * @author dirk
 */
public class LandAirMech extends BipedMech {

    /**
     * 
     */
    private static final long serialVersionUID = 3527907120230165939L;
    public static final int MODE_MECH = 0x001;
    public static final int MODE_AIRMECH = 0x002;
    public static final int MODE_AIRCRAFT = 0x004; // this is supposed to be an
                                                    // areospace fighter. don't
                                                    // know their rules though

    // conversion modes: all conversions take a turn and the mech uses an
    // intermediate mode in them.
    /**
     * this is a placeholder for generic conversions. All conversion modes must
     * be greater than this.
     */
    public static final int MODE_CONVERT = 0x100;

    public static final int MODE_CONVERT_MECH_TO_AIRMECH = 0x112;
    public static final int MODE_CONVERT_MECH_TO_AIRCRAFT = 0x114; // illegal
                                                                    // conversion

    public static final int MODE_CONVERT_AIRMECH_TO_MECH = 0x121;
    public static final int MODE_CONVERT_AIRMECH_TO_AIRCRAFT = 0x124;

    public static final int MODE_CONVERT_AIRCRAFT_TO_MECH = 0x141; // illegal
                                                                    // conversion
    public static final int MODE_CONVERT_AIRCRAFT_TO_AIRMECH = 0x142;

    private int mode = MODE_MECH;
    private int startMode = MODE_MECH;
    private boolean landed = true;

    public LandAirMech(String inGyroType, String inCockpitType) {
        super(inGyroType, inCockpitType);
    }

    public LandAirMech() {
        super();
    }

    public LandAirMech(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);
    }

    /**
     * Returns the elevation of the LAM, correct in Mech mode, but needs fixing
     * in other modes.
     */
    public int getElevation() {
        if (mode == MODE_MECH) {
            return super.getElevation();
        }

        return elevation; // TODO use Entities elevation code instead.
    }

    /**
     * is it possible to go up, or are we at maximum altitude? assuming passed
     * elevation.
     */
    public boolean canGoUp(int assumedElevation, Coords assumedPos) {
        IHex hex = getGame().getBoard().getHex(assumedPos);
        int altitude = assumedElevation + hex.surface();

        int maxAlt = hex.surface();
        if (mode == MODE_AIRMECH) {
            // Arbitarty upper limit of +50 for AirMeks, same as VTOL's,
            maxAlt += 50;
        } else if (mode == MODE_AIRCRAFT) {
            // areospace fighters don't have an upper limit...
            return true;
        }

        return (altitude < maxAlt);
    }

    /**
     * is it possible to go down, or are we landed/just above the
     * water/treeline? assuming passed elevation. What are the rules regarding
     * airmechs in water? I'll assume that airmechs can't go underwater, and
     * that fighters follow the rules for VTOLS
     */
    public boolean canGoDown(int assumedElevation, Coords assumedPos) {
        IHex hex = getGame().getBoard().getHex(assumedPos);
        int altitude = assumedElevation + hex.surface();
        int minAlt = hex.surface();

        if (isInMode(MODE_AIRCRAFT)) {
            minAlt = hex.ceiling();
            if (hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.WATER)
                    || hex.containsTerrain(Terrains.JUNGLE)) {
                minAlt++;
            }
        } else if (isInMode(MODE_MECH)) {
            minAlt = hex.floor();
        }

        return (altitude > minAlt);
    }

    /**
     * Returns the Mode this mech is on.
     * 
     * @return One of the MODE_* or the MODE_CONVERT_* constants.
     */
    public int getMode() {
        return mode;
    }

    /**
     * Returns true if the mech is in the given mode, false otherwise.
     * 
     * @param mode One of the MODE_* or the MODE_CONVERT_* constants.
     * @return true iff the mech is in that mode.
     */
    public boolean isInMode(int mode) {
        return this.mode == mode;
    }

    /**
     * Returns true if the mech can transform into mech mode, false otherwise.
     * 
     * @return true iff the mech can go in that mode.
     */
    public boolean canConvertToMech() {
        if (!isDeployed()) {
            return true;
        }
        if (startMode == MODE_AIRMECH && landed
                && isSystemIntact(Mech.ACTUATOR_LOWER_LEG)
                && isSystemIntact(Mech.ACTUATOR_UPPER_LEG)
                && isSystemIntact(Mech.ACTUATOR_HIP)
                && isSystemIntact(Mech.SYSTEM_GYRO)) {
            return true;
        }

        if (startMode == MODE_MECH && mode != startMode) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if the mech can transform into airmek mode, false otherwise.
     * 
     * @return true iff the mech can go in that mode.
     */
    public boolean canConvertToAirmech() {
        if (!isDeployed()) {
            return true;
        }
        if (startMode == MODE_MECH && isSystemIntact(Mech.ACTUATOR_LOWER_LEG)
                && isSystemIntact(Mech.ACTUATOR_UPPER_LEG)
                && isSystemIntact(Mech.ACTUATOR_HIP)
                && isSystemIntact(Mech.SYSTEM_GYRO)) {
            return true;
        } else if (startMode == MODE_AIRCRAFT
                && isSystemIntact(Mech.ACTUATOR_LOWER_LEG)
                && isSystemIntact(Mech.ACTUATOR_UPPER_LEG)) {
            return true;
        }

        if (startMode == MODE_AIRMECH && mode != startMode) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if the mech can transform into fighter mode, false
     * otherwise.
     * 
     * @return true iff the mech can go in that mode.
     */
    public boolean canConvertToAircraft() {
        if (!isDeployed()) {
            return true;
        }
        if (!landed && startMode == MODE_AIRMECH
                && isSystemIntact(Mech.ACTUATOR_LOWER_LEG)
                && isSystemIntact(Mech.ACTUATOR_UPPER_LEG)
                && isSystemIntact(Mech.ACTUATOR_SHOULDER)
                && isSystemIntact(Mech.ACTUATOR_UPPER_ARM)) {
            return true;
        }

        if (startMode == MODE_AIRCRAFT && mode != startMode) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if the mech can transform into the given mode, false
     * otherwise.
     * 
     * @return true iff the mech can go in that mode.
     */
    public boolean canConvertToMode(int mode) {
        switch (mode) {
            case MODE_MECH:
                return canConvertToMech();
            case MODE_AIRMECH:
                return canConvertToAirmech();
            case MODE_AIRCRAFT:
                return canConvertToAircraft();
        }
        throw new IllegalArgumentException("Not a valid mode.");
    }

    /**
     * Tries to convert the mech to the given mode, following all the
     * restrictions.
     * 
     * @param newMode
     */
    public void convertToMode(int newMode) {
        if (!isDeployed()) { // during deployment we can change our mode at
                                // will.
            mode = newMode;
            startMode = mode;
            return;
        }

        if (startMode == newMode) {
            mode = startMode;
        } else if (canConvertToMode(newMode)) {
            switch (newMode) {
                case MODE_MECH:
                    mode = MODE_CONVERT_AIRMECH_TO_MECH;
                    break;
                case MODE_AIRMECH:
                    if (startMode == MODE_MECH) {
                        mode = MODE_CONVERT_MECH_TO_AIRMECH;
                    } else if (startMode == MODE_AIRCRAFT) {
                        mode = MODE_CONVERT_AIRCRAFT_TO_AIRMECH;
                    }
                    break;
                case MODE_AIRCRAFT:
                    mode = MODE_CONVERT_AIRMECH_TO_AIRCRAFT;
                    break;
            }
        }
    }

    public int getMaxElevationChange() {
        if (mode == MODE_MECH)
            return super.getMaxElevationChange();
        return 999;
    }

    public void newRound(int roundNumber) {
        if (mode >= MODE_CONVERT) { // if we are undergoing a conversiuon
            switch (mode) {
                case MODE_CONVERT_AIRMECH_TO_MECH:
                case MODE_CONVERT_AIRCRAFT_TO_MECH:
                    // I am not sure if changing back while in the air to mech
                    // form is allowed.
                    // It is almost always a bad idea however. I'll assume the
                    // mech crashes prone.
                    mode = MODE_MECH;
                    if (getElevation() > 0) { // the mech was up in the air,
                                                // now it comes crashing down.
                        // TODO do crash damage.
                    }
                    setProne(true);
                    landed = true;
                    break;
                case MODE_CONVERT_MECH_TO_AIRMECH:
                case MODE_CONVERT_AIRCRAFT_TO_AIRMECH:
                    mode = MODE_AIRMECH;
                    break;
                case MODE_CONVERT_AIRMECH_TO_AIRCRAFT:
                case MODE_CONVERT_MECH_TO_AIRCRAFT:
                    mode = MODE_AIRCRAFT;
                    break;
                default:
                    // unknown mode, don't know what to do.
            }
        }
        startMode = mode; // set the mode we start the turn in. Can never be
                            // on of the conversion modes.

        super.newRound(roundNumber);
    }

    /**
     * This function returns the original jump MP for the mode the mech is in.
     */
    public int getOriginalJumpMP() {
        int base = super.getOriginalJumpMP();
        switch (mode) {
            case MODE_AIRMECH:
            case MODE_CONVERT_AIRMECH_TO_AIRCRAFT:
                base *= 3;
                break;
            case MODE_CONVERT_AIRMECH_TO_MECH:
                base *= 1.5;
                break;
            case MODE_CONVERT_MECH_TO_AIRMECH:
                base *= 0.5;
                break;
            case MODE_AIRCRAFT:
            case MODE_CONVERT_AIRCRAFT_TO_AIRMECH:
                return 0; // aircraft can't jump.
        }
        return base;
    }

    /**
     * How much heat is gained from jumping, airmechs don't gain jumping heat.
     */
    public int getJumpHeat(int movedMP) {
        if (mode == MODE_MECH || mode == MODE_CONVERT_MECH_TO_AIRMECH) {
            return super.getJumpHeat(movedMP);
        }
        return 0;
    }

    /**
     * Can this entity fire this turn?
     */
    public boolean isEligibleForFiring() {
        // can't shoot if converting to or from aircraft.
        if (mode == MODE_CONVERT_AIRMECH_TO_AIRCRAFT
                || mode == MODE_CONVERT_AIRCRAFT_TO_AIRMECH)
            return false;

        return super.isEligibleForFiring();
    }

    /**
     * Returns the original walking MP for that mode or that conversion.
     */
    public int getOriginalWalkMP() {
        int base = super.getOriginalWalkMP();
        switch (mode) {
            case MODE_AIRMECH:
            case MODE_CONVERT_AIRMECH_TO_AIRCRAFT:
                base = (int) Math.ceil(base / 3.0);
                break;
            case MODE_CONVERT_AIRMECH_TO_MECH:
                base = (int) Math.ceil(base / 6.0);
                break;
            case MODE_CONVERT_MECH_TO_AIRMECH:
                base = (int) Math.ceil(base / 2.0);
                break;
        }
        return base;
    }

    /**
     * returns true if the mech is flying.
     * 
     * @return true iff the mech is flying
     */
    public boolean isFlying() {
        return !landed;
    }

    // //-----*******--------- WARNING DIRTY HACK BELOW
    // -------******-------////////
    // the way jump mps get calculated need to be refactored.
    public int getJumpMP() {
        return getOriginalJumpMP(); // ignoring terrain for now.
    }

    public int getJumpMPWithTerrain() {
        return getJumpMP(); // ignoring terrain for now.
    }
}
