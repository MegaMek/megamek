/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import megamek.common.enums.MPBoosters;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import org.apache.logging.log4j.LogManager;

import java.util.*;

public class LandAirMech extends BipedMech implements IAero, IBomber {
    private static final long serialVersionUID = -8118673802295814548L;

    public static final int CONV_MODE_MECH = 0;
    public static final int CONV_MODE_AIRMECH = 1;
    public static final int CONV_MODE_FIGHTER = 2;

    public static final int LAM_AVIONICS = 15;

    public static final int LAM_LANDING_GEAR = 16;

    public static final String[] systemNames = { "Life Support", "Sensors", "Cockpit", "Engine", "Gyro", null, null,
            "Shoulder", "Upper Arm", "Lower Arm", "Hand", "Hip", "Upper Leg", "Lower Leg", "Foot", "Avionics",
            "Landing Gear" };

    public static final int LAM_UNKNOWN = -1;
    public static final int LAM_STANDARD = 0;
    public static final int LAM_BIMODAL = 1;

    public static final String[] LAM_STRING = { "Standard", "Bimodal" };

    /** Locations for capital fighter weapons groups */
    public static final int LOC_CAPITAL_NOSE = 8;
    public static final int LOC_CAPITAL_AFT = 9;
    public static final int LOC_CAPITAL_WINGS = 10;

    /**
     * Translate a 'Mech location to the equivalent Aero location.
     */
    public static int getAeroLocation(int loc) {
        switch (loc) {
            case LOC_HEAD:
            case LOC_CT:
            case LOC_CAPITAL_NOSE:
                return Aero.LOC_NOSE;
            case LOC_RT:
            case LOC_RARM:
                return Aero.LOC_RWING;
            case LOC_LT:
            case LOC_LARM:
                return Aero.LOC_LWING;
            case LOC_RLEG:
            case LOC_LLEG:
            case LOC_CAPITAL_AFT:
                return Aero.LOC_AFT;
            case LOC_CAPITAL_WINGS:
                return Aero.LOC_WINGS;
            default:
                return LOC_NONE;
        }
    }

    private static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso", "Right Arm",
            "Left Arm", "Right Leg", "Left Leg", "Nose", "Aft", "Wings" };

    private static final String[] LOCATION_ABBRS = { "HD", "CT", "RT", "LT", "RA", "LA", "RL", "LL", "NOS", "AFT",
            "WNG" };

    private static final int[] NUM_OF_SLOTS = { 6, 12, 12, 12, 12, 12, 6, 6, 100, 100, 100 };

    /**
     * Returns a vector of slot counts for all locations
     */
    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Returns a vector of names for all locations
     */
    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns a vector of abbreviations for all locations
     */
    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    private int lamType;

    /** Fighter mode **/
    // out of control
    private boolean outControl = false;
    private boolean outCtrlHeat = false;
    private boolean randomMove = false;

    // set up movement
    private int currentVelocity = 0;
    private int nextVelocity = currentVelocity;
    private boolean accLast = false;
    private boolean rolled = false;
    private boolean failedManeuver = false;
    private boolean accDecNow = false;
    private int straightMoves = 0;
    private int altLoss = 0;
    private int altLossThisRound = 0;

    //Autoejection
    private boolean critThresh = false;

    // Bomb choices

    protected int[] intBombChoices = new int[BombType.B_NUM];
    protected int[] extBombChoices = new int[BombType.B_NUM];

    private Targetable airmechBombTarget = null;

    private int fuel;
    private int currentfuel;
    private int whoFirst;

    // Capital Fighter stuff
    private int capitalArmor = 0;
    private int capitalArmor_orig = 2;
    private int fatalThresh = 0;
    private int currentDamage = 0;
    private Map<String, Integer> weaponGroups = new HashMap<>();

    public LandAirMech(int inGyroType, int inCockpitType, int inLAMType) {
        super(inGyroType, inCockpitType);
        lamType = inLAMType;

        setTechLevel(TechConstants.T_IS_ADVANCED);
        setCritical(Mech.LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_LT, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_RT, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_LT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
        setCritical(Mech.LOC_RT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
        for (int i = 0; i < getNumberOfCriticals(Mech.LOC_CT); i++) {
            if (null == getCritical(Mech.LOC_CT, i)) {
                setCritical(Mech.LOC_CT, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
                break;
            }
        }

        previousMovementMode = movementMode;
        setCrew(new LAMPilot(this));
    }

    @Override
    public void setCrew(Crew newCrew) {
        if (newCrew instanceof LAMPilot) {
            super.setCrew(newCrew);
        } else {
            super.setCrew(LAMPilot.convertToLAMPilot(this, newCrew));
        }
    }

    @Override
    public String getSystemName(int index) {
        if (index == SYSTEM_GYRO) {
            return Mech.getGyroDisplayString(gyroType);
        }
        if (index == SYSTEM_COCKPIT) {
            return Mech.getCockpitDisplayString(cockpitType);
        }
        return systemNames[index];
    }

    @Override
    public String getRawSystemName(int index) {
        return systemNames[index];
    }

    public int getLAMType() {
        return lamType;
    }

    public void setLAMType(int lamType) {
        this.lamType = lamType;
    }

    public String getLAMTypeString(int lamType) {
        if (lamType < 0 || lamType >= LAM_STRING.length) {
            LogManager.getLogger().error("Attempted to get LAM Type string for unknown type " + lamType + " returning standard.");
            return LAM_STRING[LAM_STANDARD];
        }
        return LAM_STRING[lamType];
    }

    public String getLAMTypeString() {
        return getLAMTypeString(getLAMType());
    }

    public static int getLAMTypeForString(String inType) {
        if ((inType == null) || (inType.length() < 1)) {
            return LAM_UNKNOWN;
        }
        for (int x = 0; x < LAM_STRING.length; x++) {
            if (inType.equals(LAM_STRING[x])) {
                return x;
            }
        }
        return LAM_UNKNOWN;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return getConversionMode() != CONV_MODE_FIGHTER;
    }

    @Override
    public boolean doomedInSpace() {
        return getConversionMode() != CONV_MODE_FIGHTER;
    }

    /**
     * Current MP is calculated differently depending on the LAM's mode. AirMech
     * mode returns cruise/flank; walk/run is treated as a special case of WiGE
     * ground movement.
     */
    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp;
        if (!mpCalculationSetting.ignoreConversion && (getConversionMode() == CONV_MODE_FIGHTER)) {
            mp = getFighterModeWalkMP(mpCalculationSetting);
        } else if (!mpCalculationSetting.ignoreConversion && (getConversionMode() == CONV_MODE_AIRMECH)) {
            mp = getAirMechCruiseMP(mpCalculationSetting);
        } else {
            mp = super.getWalkMP(mpCalculationSetting);
        }
        if (!mpCalculationSetting.ignoreConversion && convertingNow) {
            mp /= 2;
        }
        return mp;
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        int mp;
        if (!mpCalculationSetting.ignoreConversion && (getConversionMode() == CONV_MODE_FIGHTER)) {
            mp = getFighterModeRunMP(mpCalculationSetting);
        } else if (!mpCalculationSetting.ignoreConversion && (getConversionMode() == CONV_MODE_AIRMECH)) {
            mp = getAirMechFlankMP(mpCalculationSetting);
        } else {
            // conversion reduction has already been done at this point
            return super.getRunMP(mpCalculationSetting);
        }
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }

    @Override
    public int getSprintMP(MPCalculationSetting mpCalculationSetting) {
        if (!mpCalculationSetting.ignoreConversion && (getConversionMode() == CONV_MODE_FIGHTER)) {
            return getRunMP();
        } else if (!mpCalculationSetting.ignoreConversion && (getConversionMode() == CONV_MODE_AIRMECH)) {
            if (hasHipCrit()) {
                return getAirMechRunMP(mpCalculationSetting);
            }
            if (!mpCalculationSetting.ignoreMASC) {
                return getArmedMPBoosters().calculateSprintMP(getAirMechWalkMP(mpCalculationSetting));
            }
        }
        return super.getSprintMP(mpCalculationSetting);
    }

    public int getAirMechCruiseMP(MPCalculationSetting mpCalculationSetting) {
        if (game != null && game.getBoard().inAtmosphere()
                && (isLocationBad(Mech.LOC_LT) || isLocationBad(Mech.LOC_RT))) {
            return 0;
        }
        return getJumpMP(mpCalculationSetting) * 3;
    }

    public int getAirMechFlankMP(MPCalculationSetting mpCalculationSetting) {
        if (game != null && game.getBoard().inAtmosphere()
                && (isLocationBad(Mech.LOC_LT) || isLocationBad(Mech.LOC_RT))) {
            return 0;
        }
        return (int) Math.ceil(getAirMechCruiseMP(mpCalculationSetting) * 1.5);
    }

    public int getAirMechWalkMP() {
        return getAirMechWalkMP(MPCalculationSetting.STANDARD);
    }

    public int getAirMechWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = (int) Math.ceil(super.getWalkMP(mpCalculationSetting) * 0.33);
        if (!mpCalculationSetting.ignoreConversion && convertingNow) {
            mp /= 2;
        }
        return mp;
    }

    public int getAirMechRunMP() {
        return getAirMechRunMP(MPCalculationSetting.STANDARD);
    }

    public int getAirMechRunMP(MPCalculationSetting mpCalculationSetting) {
        int mp = (int) Math.ceil(getAirMechWalkMP(mpCalculationSetting) * 1.5);
        if (!mpCalculationSetting.ignoreConversion && convertingNow) {
            mp /= 2;
        }
        return mp;
    }

    public int getFighterModeWalkMP(MPCalculationSetting mpCalculationSetting) {
        int thrust = getCurrentThrust(mpCalculationSetting);
        if (!mpCalculationSetting.ignoreGrounded && !isAirborne()) {
            thrust /= 2;
        }
        return thrust;
    }

    public int getFighterModeRunMP(MPCalculationSetting mpCalculationSetting) {
        int walk = getFighterModeWalkMP(mpCalculationSetting);
        if (mpCalculationSetting.ignoreGrounded || isAirborne()) {
            return (int) Math.ceil(walk * 1.5);
        } else {
            return walk; // Grounded asfs cannot use flanking movement
        }
    }

    @Override
    public int getCurrentThrust() {
        // Cannot fly in atmosphere with missing side torso
        if (!isSpaceborne() && (isLocationBad(LOC_RT) || isLocationBad(LOC_LT))) {
            return 0;
        }
        int j = getJumpMP();
        if (null != game) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            if (weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            }

            if(getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)
                    && conditions.getWeather().isClear()
                    && conditions.getWind().isTornadoF1ToF3()) {
                j += 1;
            }
        }
        return j;
    }

    public int getCurrentThrust(MPCalculationSetting mpCalculationSetting) {
        // Cannot fly in atmosphere with missing side torso
        if (!isSpaceborne() && (isLocationBad(LOC_RT) || isLocationBad(LOC_LT))) {
            return 0;
        }
        int j = getJumpMP();
        if (!mpCalculationSetting.ignoreWeather && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            j = Math.max(j + weatherMod, 0);

            if(getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)
                    && conditions.getWeather().isClear()
                    && conditions.getWind().isTornadoF1ToF3()) {
                j += 1;
            }
        }
        return j;
    }

    public int getAirMechCruiseMP() {
        return getAirMechCruiseMP(MPCalculationSetting.STANDARD);
    }

    public int getAirMechFlankMP() {
        return getAirMechFlankMP(MPCalculationSetting.STANDARD);
    }

    /**
     * LAMs cannot benefit from MASC in AirMech or fighter mode and cannot mount
     * a supercharger.
     */
    @Override
    public MPBoosters getMPBoosters() {
        return (getConversionMode() == CONV_MODE_MECH) ? super.getMPBoosters() : MPBoosters.NONE;
    }

    @Override
    public MPBoosters getArmedMPBoosters() {
        return (getConversionMode() == CONV_MODE_MECH) ? super.getArmedMPBoosters() : MPBoosters.NONE;
    }

    @Override
    public boolean isImmobile() {
        if (getConversionMode() == CONV_MODE_FIGHTER && (isAirborne() || isSpaceborne())) {
            return false;
        }
        return super.isImmobile();
    }

    @Override
    public int getWalkHeat() {
        if (moved == EntityMovementType.MOVE_VTOL_WALK) {
            return getAirMechHeat();
        }
        return super.getWalkHeat();
    }

    @Override
    public int getRunHeat() {
        if (moved == EntityMovementType.MOVE_VTOL_RUN) {
            return getAirMechHeat();
        }
        return super.getRunHeat();
    }

    public int getAirMechHeat() {
        int mod = bDamagedCoolantSystem ? 1 : 0;
        return mod + (int) Math.round(getJumpHeat(mpUsed) / 3.0);
    }

    @Override
    public int getEngineCritHeat() {
        // Engine crit heat follows ASF rules in fighter mode.
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            return 2 * getEngineHits();
        } else {
            return super.getEngineCritHeat();
        }
    }

    @Override
    public int getHeatSinks() {
        return getActiveSinks();
    }

    @Override
    public boolean usesTurnMode() {
        // Turn mode rule is not optional for LAMs in AirMech mode.
        return getConversionMode() == CONV_MODE_AIRMECH;
    }

    /**
     * When cycling through possible movement modes, we need to know if we've
     * returned to the previous mode, which means that no conversion is actually
     * going to take place.
     *
     * @return The movement mode on the previous turn.
     */
    public EntityMovementMode getPreviousMovementMode() {
        return previousMovementMode;
    }

    public int getPreviousConversionMode() {
        switch (previousMovementMode) {
            case AERODYNE:
            case WHEELED:
                return CONV_MODE_FIGHTER;
            case WIGE:
                return CONV_MODE_AIRMECH;
            case BIPED:
            default:
                return CONV_MODE_MECH;
        }
    }

    @Override
    public void setMovementMode(EntityMovementMode mode) {
        int prevMode = getConversionMode();
        if (mode == EntityMovementMode.AERODYNE || mode == EntityMovementMode.WHEELED) {
            setConversionMode(CONV_MODE_FIGHTER);
        } else if (mode == EntityMovementMode.WIGE) {
            setConversionMode(CONV_MODE_AIRMECH);
        } else {
            setConversionMode(CONV_MODE_MECH);
        }
        super.setMovementMode(mode);

        if (getConversionMode() != prevMode) {
            if (getConversionMode() == CONV_MODE_FIGHTER) {
                setRapidFire();
            } else if (prevMode == CONV_MODE_FIGHTER) {
                for (Mounted m : getTotalWeaponList()) {
                    WeaponType wtype = (WeaponType) m.getType();
                    if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                        m.setMode("");
                        m.setModeSwitchable(true);
                    } else if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA) {
                        m.setMode("");
                        m.setModeSwitchable(true);
                    } else if (wtype.hasIndirectFire()) {
                        m.setModeSwitchable(true);
                    }
                }
            }

            resetBombAttacks();
        }
    }

    @Override
    public void setConversionMode(int mode) {
        if (mode == getConversionMode()) {
            return;
        }
        if (mode == CONV_MODE_MECH) {
            super.setMovementMode(EntityMovementMode.BIPED);
        } else if (mode == CONV_MODE_AIRMECH) {
            super.setMovementMode(EntityMovementMode.WIGE);
        } else if (mode == CONV_MODE_FIGHTER) {
            super.setMovementMode(EntityMovementMode.AERODYNE);
        } else {
            return;
        }
        super.setConversionMode(mode);
    }

    @Override
    public boolean canAssaultDrop() {
        return getConversionMode() != CONV_MODE_FIGHTER;
    }

    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        // Fighter mode has the same terrain restrictions as ASFs.
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            if (isAirborne()) {
                return false;
            }
            Hex hex = game.getBoard().getHex(c);

            // Additional restrictions for hidden units
            if (isHidden()) {
                // Can't deploy in paved hexes
                if (hex.containsTerrain(Terrains.PAVEMENT) || hex.containsTerrain(Terrains.ROAD)) {
                    return true;
                }
                // Can't deploy on a bridge
                if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation) && hex.containsTerrain(Terrains.BRIDGE)) {
                    return true;
                }
                // Can't deploy on the surface of water
                if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                    return true;
                }
            }

            // grounded aeros have the same prohibitions as wheeled tanks
            return hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.ROUGH)
                    || ((hex.terrainLevel(Terrains.WATER) > 0) && !hex.containsTerrain(Terrains.ICE))
                    || hex.containsTerrain(Terrains.RUBBLE) || hex.containsTerrain(Terrains.MAGMA)
                    || hex.containsTerrain(Terrains.JUNGLE) || (hex.terrainLevel(Terrains.SNOW) > 1)
                    || (hex.terrainLevel(Terrains.GEYSER) == 2);
        } else if (getConversionMode() == CONV_MODE_AIRMECH && currElevation > 0) {
            // Cannot enter woods or a building hex in AirMech mode unless using
            // ground movement
            // or flying over the terrain.
            Hex hex = game.getBoard().getHex(c);
            return (hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE)
                    || hex.containsTerrain(Terrains.BLDG_ELEV)) && hex.ceiling() > currElevation;
        } else {
            // Mech mode or AirMech mode using ground MP have the same
            // restrictions as Biped Mech.
            return super.isLocationProhibited(c, currElevation);
        }
    }

    @Override
    public String getMovementString(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_WALK:
                if (getConversionMode() == CONV_MODE_FIGHTER) {
                    return "Cruised";
                } else {
                    return "Walked";
                }
            case MOVE_RUN:
                if (getConversionMode() == CONV_MODE_FIGHTER) {
                    return "Flanked";
                } else {
                    return "Ran";
                }
            case MOVE_VTOL_WALK:
                return "Cruised";
            case MOVE_VTOL_RUN:
                return "Flanked";
            case MOVE_SAFE_THRUST:
                return "Safe Thrust";
            case MOVE_OVER_THRUST:
                return "Over Thrust";
            default:
                return super.getMovementString(mtype);
        }
    }

    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_WALK:
                if (getConversionMode() == CONV_MODE_FIGHTER) {
                    return "C";
                } else {
                    return "W";
                }
            case MOVE_RUN:
                if (getConversionMode() == CONV_MODE_FIGHTER) {
                    return "F";
                } else {
                    return "R";
                }
            case MOVE_VTOL_WALK:
                return "C";
            case MOVE_VTOL_RUN:
                return "F";
            case MOVE_NONE:
                return "N";
            case MOVE_SAFE_THRUST:
                return "S";
            case MOVE_OVER_THRUST:
                return "O";
            default:
                return super.getMovementAbbr(mtype);
        }
    }

    /**
     * What's the range of the ECM equipment?
     *
     * @return the <code>int</code> range of this unit's ECM. This value will be
     *         <code>Entity.NONE</code> if no ECM is active.
     */
    @Override
    public int getECMRange() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                || !game.getBoard().inSpace()) {
            return super.getECMRange();
        }
        return Math.min(super.getECMRange(), 0);
    }

    /**
     * Add in any piloting skill mods
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        // Modifier for pilot hits applies in all modes.
        if (getCrew().getHits(0) > 0) {
            roll.addModifier(getCrew().getHits(0), "pilot hits");
        }

        if (getConversionMode() != CONV_MODE_FIGHTER && !isAirborneVTOLorWIGE()) {
            return super.addEntityBonuses(roll);
        }

        // In fighter mode a destroyed gyro gives +6 to the control roll.
        int gyroHits = getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
        if (gyroHits > 0) {
            if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
                if (gyroHits == 1) {
                    roll.addModifier(1, "HD Gyro damaged once");
                } else if (gyroHits == 2) {
                    roll.addModifier(3, "HD Gyro damaged twice");
                } else {
                    roll.addModifier(6, "Gyro destroyed");
                }
            } else {
                if (gyroHits == 1) {
                    roll.addModifier(3, "Gyro damaged");
                } else {
                    roll.addModifier(6, "Gyro destroyed");
                }
            }
        }

        // EI bonus?
        if (hasActiveEiCockpit()) {
            roll.addModifier(-1, "Enhanced Imaging");
        }

        // VDNI bonus?
        if (hasAbility(OptionsConstants.MD_VDNI)
                && !hasAbility(OptionsConstants.MD_BVDNI)) {
            roll.addModifier(-1, "VDNI");
        }

        // Small/torso-mounted cockpit penalty?
        if ((getCockpitType() == Mech.COCKPIT_SMALL)
                && !hasAbility(OptionsConstants.MD_BVDNI)
                && !hasAbility(OptionsConstants.UNOFF_SMALL_PILOT)) {
            roll.addModifier(1, "Small Cockpit");
        }

        if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT) && !hasAbility(OptionsConstants.UNOFF_SMALL_PILOT)) {
            roll.addModifier(1, "cramped cockpit");
        }

        int avionicsHits = getAvionicsHits();
        if (avionicsHits > 2) {
            roll.addModifier(5, "avionics destroyed");
        } else if (avionicsHits > 0) {
            roll.addModifier(avionicsHits, "avionics damage");
        }

        if (getConversionMode() == CONV_MODE_FIGHTER) {
            if (moved == EntityMovementType.MOVE_OVER_THRUST) {
                roll.addModifier(+1, "Used more than safe thrust");
            }

            int vel = getCurrentVelocity();
            int vmod = vel - (2 * getWalkMP());
            if (!getGame().getBoard().inSpace() && (vmod > 0)) {
                roll.addModifier(vmod, "Velocity greater than 2x safe thrust");
            }

            PlanetaryConditions conditions = game.getPlanetaryConditions();
            // add in atmospheric effects later
            boolean spaceOrVacuum = game.getBoard().inSpace()
                    || conditions.getAtmosphere().isVacuum();
            if (!spaceOrVacuum
                    && isAirborne()) {
                roll.addModifier(+1, "Atmospheric operations");
            }

            if (hasQuirk(OptionsConstants.QUIRK_POS_ATMO_FLYER) && !game.getBoard().inSpace()) {
                roll.addModifier(-1, "atmospheric flyer");
            }
            if (hasQuirk(OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY) && !game.getBoard().inSpace()) {
                roll.addModifier(+1, "atmospheric flight instability");
            }
        }

        return roll;
    }

    /**
     * Landing in AirMech mode requires a control roll only if the gyro or any
     * of the hip or leg actuators are damaged.
     *
     * @return The control roll that must be passed to land safely.
     */
    public PilotingRollData checkAirMechLanding() {
        // Base piloting skill
        PilotingRollData roll = new PilotingRollData(getId(), getCrew().getPiloting(), "Base piloting skill");

        addEntityBonuses(roll);

        // Landing in AirMech mode only requires a roll if gyro or hip/leg
        // actuators are damaged.
        int gyroHits = getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
        if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            gyroHits--;
        }

        boolean required = gyroHits > 0;

        for (int loc = 0; loc < locations(); loc++) {
            if (locationIsLeg(loc)) {
                if (isLocationBad(loc)) {
                    roll.addModifier(5, getLocationName(loc) + " destroyed");
                    required = true;
                } else {
                    // check for damaged hip actuators
                    if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0) {
                        roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                        if (!game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                            continue;
                        }
                        required = true;
                    }
                    // upper leg actuators?
                    if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc) > 0) {
                        roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                        required = true;
                    }
                    // lower leg actuators?
                    if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc) > 0) {
                        roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                        required = true;
                    }
                    // foot actuators?
                    if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc) > 0) {
                        roll.addModifier(1, getLocationName(loc) + " Foot Actuator destroyed");
                        required = true;
                    }
                }
            }
        }
        if (required) {
            roll.addModifier(0, "landing with gyro or leg damage");
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check not required for landing");
        }
        return roll;
    }

    @Override
    public int getMaxElevationDown(int currElevation) {
        // Cannot spend AirMech MP above altitude 3 (level 30) so we use that as
        // max descent.
        if ((currElevation > 0) && (getConversionMode() == CONV_MODE_AIRMECH)) {
            return 30;
        }
        return super.getMaxElevationDown(currElevation);
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        if (getConversionMode() == CONV_MODE_MECH) {
            return super.canChangeSecondaryFacing();
        } else {
            return false;
        }
    }

    /**
     * Start a new round
     *
     * @param roundNumber
     *            the <code>int</code> number of the new round
     */
    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);
        previousMovementMode = movementMode;

        // reset threshold critted
        setCritThresh(false);

        // reset maneuver status
        setFailedManeuver(false);
        // reset acc/dec this turn
        setAccDecNow(false);

        updateBays();

        // update recovery turn if in recovery
        if (getRecoveryTurn() > 0) {
            setRecoveryTurn(getRecoveryTurn() - 1);
        }

        airmechBombTarget = null;

        if (getConversionMode() == CONV_MODE_FIGHTER) {
            // if in atmosphere, then halve next turn's velocity
            if (!game.getBoard().inSpace() && isDeployed() && (roundNumber > 0)) {
                setNextVelocity((int) Math.floor(getNextVelocity() / 2.0));
            }

            // update velocity
            setCurrentVelocity(getNextVelocity());

            // if they are out of control due to heat, then apply this and reset
            if (isOutCtrlHeat()) {
                setOutControl(true);
                setOutCtrlHeat(false);
            }

            // get new random whofirst
            setWhoFirst();

            resetAltLossThisRound();
        }
    }

    /**
     * Cannot make any physical attacks in fighter mode except ramming, which is
     * handled in the movement phase.
     */
    @Override
    public boolean isEligibleForPhysical() {
        return getConversionMode() != CONV_MODE_FIGHTER && super.isEligibleForPhysical();
    }

    @Override
    public boolean canCharge() {
        if (getConversionMode() == CONV_MODE_FIGHTER
                || ((getConversionMode() == CONV_MODE_AIRMECH) && isAirborneVTOLorWIGE())) {
            return false;
        } else {
            return super.canCharge();
        }
    }

    @Override
    public boolean canRam() {
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            return !isImmobile() && (getWalkMP() > 0);
        } else if (getConversionMode() == CONV_MODE_AIRMECH) {
            return isAirborneVTOLorWIGE();
        } else {
            return false;
        }
    }

    /*
     * Cycling through conversion modes for LAMs in 'Mech or fighter mode is
     * simple toggling between two states. LAMs in AirMech mode have three
     * possible states.
     */
    @Override
    public EntityMovementMode nextConversionMode(EntityMovementMode afterMode) {
        boolean inSpace = game != null && game.getBoard().inSpace();
        if (previousMovementMode == EntityMovementMode.WIGE) {
            if (afterMode == EntityMovementMode.WIGE) {
                return EntityMovementMode.AERODYNE;
            } else if (afterMode == EntityMovementMode.AERODYNE || afterMode == EntityMovementMode.WHEELED) {
                return originalMovementMode;
            } else {
                return EntityMovementMode.WIGE;
            }
        } else if (afterMode == EntityMovementMode.WIGE) {
            return inSpace ? EntityMovementMode.AERODYNE : previousMovementMode;
        } else if (afterMode == EntityMovementMode.AERODYNE || afterMode == EntityMovementMode.WHEELED) {
            return (inSpace || lamType == LAM_BIMODAL) ? originalMovementMode : EntityMovementMode.WIGE;
        } else {
            return lamType == LAM_BIMODAL ? EntityMovementMode.AERODYNE : EntityMovementMode.WIGE;
        }
    }

    public boolean canConvertTo(EntityMovementMode toMode) {
        return canConvertTo(getConversionMode(), getConversionModeFor(toMode));
    }

    public boolean canConvertTo(int fromMode, EntityMovementMode toMode) {
        return canConvertTo(fromMode, getConversionModeFor(toMode));
    }

    /**
     * Determines whether it is possible to assume a particular mode based on
     * damage and type of map.
     *
     * @param fromMode
     *            The mode to convert from (one of CONV_MODE_MECH,
     *            CONV_MODE_AIRMECH, or CONV_MODE_FIGHTER)
     * @param toMode
     *            The mode to convert to (one of CONV_MODE_MECH,
     *            CONV_MODE_AIRMECH, or CONV_MODE_FIGHTER)
     * @return true if it is possible for the LAM to convert to the given mode.
     */
    public boolean canConvertTo(int fromMode, int toMode) {
        // Cannot convert with any gyro damage
        int gyroHits = getBadCriticals(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO, LOC_CT);
        if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            gyroHits--;
        }
        if (gyroHits > 0) {
            return false;
        }

        // Cannot convert to or from mech mode with damage shoulder or arm
        // actuators
        if ((toMode == CONV_MODE_MECH || fromMode == CONV_MODE_MECH)
                && (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, LOC_RARM)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, LOC_RARM)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, LOC_RARM)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, LOC_LARM)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, LOC_LARM)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, LOC_LARM) > 0)) {
            return false;
        }

        // Cannot convert to or from fighter mode with damage hip or leg
        // actuators
        if ((toMode == CONV_MODE_FIGHTER || fromMode == CONV_MODE_FIGHTER)
                && (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, LOC_RLEG)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, LOC_RLEG)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, LOC_RLEG)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, LOC_LLEG)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, LOC_LLEG)
                        + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, LOC_LLEG) > 0)) {
            return false;
        }

        if (toMode == CONV_MODE_AIRMECH) {
            if (getLAMType() == LAM_BIMODAL) {
                return false;
            }
        } else if (toMode == CONV_MODE_FIGHTER) {
            // Standard LAMs can convert from mech to fighter mode in a single
            // round on a space map
            if (fromMode == CONV_MODE_MECH) {
                return getLAMType() == LAM_BIMODAL || game.getBoard().inSpace();
            }
        } else if (toMode == CONV_MODE_MECH) {
            // Standard LAMs can convert from fighter to mech mode in a single
            // round on a space map
            if (fromMode == CONV_MODE_FIGHTER) {
                return getLAMType() == LAM_BIMODAL || game.getBoard().inSpace();
            }
        }
        return true;
    }

    public int getConversionModeFor(EntityMovementMode mmode) {
        if (mmode == EntityMovementMode.AERODYNE || mmode == EntityMovementMode.WHEELED) {
            return CONV_MODE_FIGHTER;
        } else if (mmode == EntityMovementMode.WIGE) {
            return CONV_MODE_AIRMECH;
        } else {
            return CONV_MODE_MECH;
        }
    }

    @Override
    public boolean canFall(boolean gyroLegDamage) {
        return getConversionMode() != CONV_MODE_FIGHTER && !isAirborneVTOLorWIGE() && super.canFall(gyroLegDamage);
    }

    private static final TechAdvancement[] TA_LAM = {
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(2683, 2688, DATE_NONE, 3085)
                .setClanAdvancement(DATE_NONE, 2688, DATE_NONE, 2825)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setTechRating(RATING_D).setAvailability(RATING_D, RATING_E, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL), //standard
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(2680, 2684, DATE_NONE, 2781)
                .setClanAdvancement(DATE_NONE, 2684, DATE_NONE, 2801)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_F, RATING_X, RATING_X)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL) // bimodal
    };

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_LAM[lamType];
    }

    @Override
    public int height() {
        if (getConversionMode() == CONV_MODE_MECH) {
            return super.height();
        }
        return 0;
    }

    /**
     * LAMs can only carry mechanized BA in mech mode
     *
     * @return
     */
    @Override
    public boolean canLoad(Entity unit, boolean checkElev) {
        return (getConversionMode() == CONV_MODE_MECH) && super.canLoad(unit, checkElev);
    }

    /**
     * Bomb ordnance is treated as inferno ammo for purposes of avoiding
     * explosion due to heat.
     *
     * @return <code>true</code> if the unit is still loaded with inferno rounds
     *         or bomb ordnance.
     */
    @Override
    public boolean hasInfernoAmmo() {
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_BOMB_BAY) && m.getLinked() != null) {
                Mounted bomb = m.getLinked();
                // We may have to go through a launcher to get to the ordnance
                if (bomb.getLinked() != null) {
                    bomb = bomb.getLinked();
                }
                if (bomb.getExplosionDamage() > 0) {
                    return true;
                }
            }
        }
        return super.hasInfernoAmmo();
    }

    /** Fighter Mode **/

    public void setWhoFirst() {
        whoFirst = Compute.randomInt(500);
    }

    public int getWhoFirst() {
        return whoFirst;
    }

    public int getMaxBombPoints() {
        return getMaxExtBombPoints() + getMaxIntBombPoints();
    }

    @Override
    public int getMaxExtBombPoints() {
        return 0;
    }
    @Override
    public int getMaxIntBombPoints() {
        return countWorkingMisc(MiscType.F_BOMB_BAY);
    }

    /**
     *
     * @return Largest empty bay size
     */
    @Override
    public int getMaxIntBombSize() {
        return Math.max(emptyBaysInLoc(LOC_CT), Math.max(emptyBaysInLoc(LOC_RT), emptyBaysInLoc(LOC_LT)));
    }

    @Override
    public int[] getIntBombChoices() {
        return intBombChoices.clone();
    }

    @Override
    public void setIntBombChoices(int[] bc) {
        if (bc.length == intBombChoices.length) {
            intBombChoices = bc.clone();
        }
    }

    @Override
    public void setUsedInternalBombs(int b){
        // Do nothing; LAMs don't take internal bomb bay hits like this
    }

    @Override
    public void increaseUsedInternalBombs(int b){
        // Do nothing
    }

    @Override
    public int getUsedInternalBombs() {
        // Currently not possible
        return 0;
    }

    @Override
    public int[] getExtBombChoices() {
        return extBombChoices;
    }

    @Override
    public void setExtBombChoices(int[] bc) {
    }

    @Override
    public void clearBombChoices() {
        Arrays.fill(intBombChoices, 0);
    }

    @Override
    public int reduceMPByBombLoad(int t) {
        // bombs don't impact movement
        return t;
    }

    @Override
    public Targetable getVTOLBombTarget() {
        return airmechBombTarget;
    }

    @Override
    public void setVTOLBombTarget(Targetable t) {
        airmechBombTarget = t;
    }

    @Override
    public boolean isMakingVTOLGroundAttack() {
        return airmechBombTarget != null;
    }

    @Override
    public boolean isNightwalker() {
        if (isAirborne()) {
            return false;
        } else {
            return getCrew().getOptions().booleanOption(OptionsConstants.PILOT_TM_NIGHTWALKER);
        }
    }

    @Override
    public int getCurrentVelocity() {
        // if using advanced movement then I just want to sum up
        // the different vectors
        if ((game != null) && game.useVectorMove()) {
            return getVelocity();
        }
        return currentVelocity;
    }

    @Override
    public void setCurrentVelocity(int velocity) {
        currentVelocity = velocity;
    }

    @Override
    public int getNextVelocity() {
        return nextVelocity;
    }

    @Override
    public void setNextVelocity(int velocity) {
        nextVelocity = velocity;
    }

    @Override
    public int getCurrentVelocityActual() {
        return currentVelocity;
    }

    @Override
    public boolean isRolled() {
        return rolled;
    }

    @Override
    public boolean isOutControlTotal() {
        return outControl || shutDown || getCrew().isUnconscious();
    }

    @Override
    public boolean isOutControl() {
        return outControl;
    }

    @Override
    public void setOutControl(boolean ocontrol) {
        outControl = ocontrol;
    }

    @Override
    public boolean isOutCtrlHeat() {
        return outCtrlHeat;
    }

    @Override
    public void setOutCtrlHeat(boolean octrlheat) {
        outCtrlHeat = octrlheat;
    }

    @Override
    public boolean isRandomMove() {
        return randomMove;
    }

    @Override
    public void setRandomMove(boolean randmove) {
        randomMove = randmove;
    }

    @Override
    public void setRolled(boolean roll) {
        rolled = roll;
    }

    @Override
    public boolean didAccLast() {
        return accLast;
    }

    @Override
    public void setAccLast(boolean b) {
        accLast = b;
    }

    @Override
    public void setSI(int si) {
        setInternal(si, LOC_CT);
    }

    @Override
    public int getSI() {
        return getInternal(LOC_CT);
    }

    @Override
    public int get0SI() {
        return getOInternal(LOC_CT);
    }

    @Override
    public boolean hasLifeSupport() {
        return getGoodCriticals(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT, LOC_HEAD) > 0;
    }

    /**
     * Used to determine modifier for landing.
     */
    @Override
    public int getNoseArmor() {
        return getArmor(LOC_CT);
    }

    /**
     * returns exposure or breached flag for location
     */
    @Override
    public int getLocationStatus(int loc) {
        switch (loc) {
            case LOC_CAPITAL_NOSE:
                return Math.max(super.getLocationStatus(LOC_HEAD), super.getLocationStatus(LOC_CT));
            case LOC_CAPITAL_AFT:
                return Math.max(super.getLocationStatus(LOC_RLEG), super.getLocationStatus(LOC_LLEG));
            case LOC_CAPITAL_WINGS:
                return Math.max(Math.max(super.getLocationStatus(LOC_RT), super.getLocationStatus(LOC_RARM)),
                        Math.max(super.getLocationStatus(LOC_LT), super.getLocationStatus(LOC_LARM)));
            default:
                return super.getLocationStatus(loc);
        }
    }

    @Override
    public int getAvionicsHits() {
        int hits = 0;
        for (int loc = 0; loc < locations(); loc++) {
            hits += getBadCriticals(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS, loc);
        }
        return hits;
    }

    @Override
    public int getSensorHits() {
        return getBadCriticals(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS, LOC_HEAD);
    }

    @Override
    public int getFCSHits() {
        return 0;
    }

    @Override
    public int getLeftThrustHits() {
        return 0;
    }

    @Override
    public int getRightThrustHits() {
        return 0;
    }

    @Override
    public void setGearHit(boolean hit) {
        if (hit) {
            List<CriticalSlot> gearSlots = new ArrayList<>();
            for (int loc = 0; loc < locations(); loc++) {
                for (int i = 0; i < crits[loc].length; i++) {
                    final CriticalSlot slot = crits[loc][i];
                    if (slot != null && slot.getType() == CriticalSlot.TYPE_SYSTEM
                            && slot.getIndex() == LAM_LANDING_GEAR && !slot.isDestroyed()) {
                        gearSlots.add(slot);
                    }
                }
            }

            if (!gearSlots.isEmpty()) {
                int index = Compute.randomInt(gearSlots.size());
                gearSlots.get(index).setDestroyed(true);
            }
        }
    }

    /**
     * Modifier to landing or vertical takeoff roll for landing gear damage.
     *
     * @param vTakeoff
     *            true if this is for a vertical takeoff, false if for a landing
     * @return the control roll modifier
     */
    @Override
    public int getLandingGearMod(boolean vTakeoff) {
        int hits = 0;
        for (int loc = 0; loc < locations(); loc++) {
            hits += getBadCriticals(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR, loc);
        }
        if (vTakeoff) {
            return hits > 0 ? 1 : 0;
        } else {
            return hits > 3 ? 5 : hits;
        }
    }

    //Landing mods for partial repairs
    @Override
    public int getLandingGearPartialRepairs() {
        if (getPartialRepairs().booleanOption("aero_gear_crit")) {
        return 2;
        } else if (getPartialRepairs().booleanOption("aero_gear_replace")) {
        return 1;
        } else {
        return 0;
        }
    }

    //Avionics mods for partial repairs
    @Override
    public int getAvionicsMisreplaced() {
        if (getPartialRepairs().booleanOption("aero_avionics_replace")) {
        return 1;
        } else {
        return 0;
        }
    }

    @Override
    public int getAvionicsMisrepaired() {
        if (getPartialRepairs().booleanOption("aero_avionics_crit")) {
        return 1;
        } else {
        return 0;
        }
    }

    /**
     * In fighter mode the weapon arcs need to be translated to Aero arcs.
     */
    @Override
    public int getWeaponArc(int wn) {
        if (getConversionMode() != CONV_MODE_FIGHTER) {
            return super.getWeaponArc(wn);
        }
        final Mounted mounted = getEquipment(wn);
        if (mounted.getType().hasFlag(WeaponType.F_SPACE_BOMB) || mounted.getType().hasFlag(WeaponType.F_DIVE_BOMB)
                || mounted.getType().hasFlag(WeaponType.F_ALT_BOMB)) {
            return Compute.ARC_360;
        }
        // We use Aero locations for weapon groups for fighter squadron
        // compatibility
        if (mounted.isWeaponGroup()) {
            return (mounted.getLocation() == Aero.LOC_AFT) ? Compute.ARC_AFT : Compute.ARC_NOSE;
        }
        int arc = Compute.ARC_NOSE;
        switch (mounted.getLocation()) {
            case LOC_HEAD:
                arc = Compute.ARC_NOSE;
                break;
            case LOC_CT:
                if (mounted.isRearMounted()) {
                    arc = Compute.ARC_AFT;
                } else {
                    arc = Compute.ARC_NOSE;
                }
                break;
            case LOC_RT:
                if (mounted.isRearMounted()) {
                    arc = Compute.ARC_RWINGA;
                } else {
                    arc = Compute.ARC_RWING;
                }
                break;
            case LOC_LT:
                if (mounted.isRearMounted()) {
                    arc = Compute.ARC_LWINGA;
                } else {
                    arc = Compute.ARC_LWING;
                }
                break;
            case LOC_RARM:
                arc = Compute.ARC_RWING;
                break;
            case LOC_LARM:
                arc = Compute.ARC_LWING;
                break;
            case LOC_RLEG:
            case LOC_LLEG:
                arc = Compute.ARC_AFT;
                break;
            default:
                arc = Compute.ARC_360;
                break;
        }

        return rollArcs(arc);
    }

    /**
     * Hit location table for fighter mode
     */
    @Override
    public HitData rollHitLocation(int table, int side) {
        if (getConversionMode() != CONV_MODE_FIGHTER) {
            return super.rollHitLocation(table, side);
        }

        int roll = Compute.d6(2);

        // first check for above/below
        if ((table == ToHitData.HIT_ABOVE) || (table == ToHitData.HIT_BELOW)) {

            // have to decide which arm/leg
            int armloc = LOC_RARM;
            int legloc = LOC_RLEG;
            int wingroll = Compute.d6(1);
            if (wingroll > 3) {
                armloc = LOC_LARM;
                legloc = LOC_LLEG;
            }
            switch (roll) {
                case 2:
                case 6:
                    return new HitData(LOC_RT, false, HitData.EFFECT_NONE);
                case 3:
                case 4:
                case 10:
                case 11:
                    return new HitData(armloc, false, HitData.EFFECT_NONE);
                case 5:
                case 9:
                    return new HitData(legloc, false, HitData.EFFECT_NONE);
                case 7:
                    return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
                case 8:
                case 12:
                    return new HitData(LOC_LT, false, HitData.EFFECT_NONE);
            }
        }

        if (side == ToHitData.SIDE_FRONT) {
            // normal front hits
            switch (roll) {
                case 2:
                case 7:
                case 12:
                    return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
                case 3:
                case 6:
                    return new HitData(LOC_RT, false, HitData.EFFECT_NONE);
                case 4:
                case 5:
                    return new HitData(LOC_RARM, false, HitData.EFFECT_NONE);
                case 8:
                case 11:
                    return new HitData(LOC_LT, false, HitData.EFFECT_NONE);
                case 9:
                case 10:
                    return new HitData(LOC_LARM, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_LEFT) {
            // normal left-side hits
            switch (roll) {
                case 2:
                    return new HitData(LOC_HEAD, false, HitData.EFFECT_NONE);
                case 3:
                case 7:
                case 11:
                    return new HitData(LOC_LARM, false, HitData.EFFECT_NONE);
                case 4:
                case 5:
                    return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
                case 6:
                case 8:
                    return new HitData(LOC_LT, false, HitData.EFFECT_NONE);
                case 9:
                case 10:
                case 12:
                    return new HitData(LOC_LLEG, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_RIGHT) {
            // normal right-side hits
            switch (roll) {
                case 2:
                    return new HitData(LOC_HEAD, false, HitData.EFFECT_NONE);
                case 3:
                case 7:
                case 11:
                    return new HitData(LOC_RARM, false, HitData.EFFECT_NONE);
                case 4:
                case 5:
                    return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
                case 6:
                case 8:
                    return new HitData(LOC_RT, false, HitData.EFFECT_NONE);
                case 9:
                case 10:
                case 12:
                    return new HitData(LOC_RLEG, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_REAR) {
            // rear torso locations are only hit on a roll of 5-6 on d6
            boolean rear = Compute.d6() > 4;
            switch (roll) {
                case 2:
                case 12:
                    return new HitData(LOC_CT, rear, HitData.EFFECT_NONE);
                case 3:
                case 4:
                    return new HitData(LOC_RT, rear, HitData.EFFECT_NONE);
                case 5:
                    return new HitData(LOC_RARM, rear, HitData.EFFECT_NONE);
                case 6:
                    return new HitData(LOC_RLEG, rear, HitData.EFFECT_NONE);
                case 7:
                    if (Compute.d6() > 3) {
                        return new HitData(LOC_LLEG, rear, HitData.EFFECT_NONE);
                    } else {
                        return new HitData(LOC_RLEG, rear, HitData.EFFECT_NONE);
                    }
                case 8:
                    return new HitData(LOC_LLEG, rear, HitData.EFFECT_NONE);
                case 9:
                    return new HitData(LOC_LARM, rear, HitData.EFFECT_NONE);
                case 10:
                case 11:
                    return new HitData(LOC_LT, rear, HitData.EFFECT_NONE);
            }
        }
        return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
    }

    @Override
    public int getFuel() {
        if ((getPartialRepairs().booleanOption("aero_asf_fueltank_crit"))
            || (getPartialRepairs().booleanOption("aero_fueltank_crit"))) {
            return (int) (fuel * 0.9);
        } else {
        return fuel;
        }
    }

    @Override
    public int getCurrentFuel() {
        if ((getPartialRepairs().booleanOption("aero_asf_fueltank_crit"))
                || (getPartialRepairs().booleanOption("aero_fueltank_crit"))) {
                return (int) (currentfuel * 0.9);
        } else {
        return currentfuel;
        }
    }

    /**
     * Sets the number of fuel points.
     *
     * @param gas
     *            Number of fuel points.
     */
    @Override
    public void setFuel(int gas) {
        fuel = gas;
        currentfuel = gas;
    }

    @Override
    public void setCurrentFuel(int gas) {
        currentfuel = gas;
    }

    @Override
    public double getFuelPointsPerTon() {
        return 80;
    }

    /**
     * Set number of fuel points based on fuel tonnage.
     *
     * @param fuelTons
     *            The number of tons of fuel
     */
    @Override
    public void setFuelTonnage(double fuelTons) {
        double pointsPerTon = getFuelPointsPerTon();
        fuel = (int) Math.ceil(pointsPerTon * fuelTons);
    }

    /**
     * Gets the fuel for this Aero in terms of tonnage.
     *
     * @return The number of tons of fuel on this Aero.
     */
    @Override
    public double getFuelTonnage() {
        return fuel / getFuelPointsPerTon();
    }

    /**
     * Exceeding damage threshold does not result in critical, but requires
     * control roll.
     */
    @Override
    public int getThresh(int loc) {
        return getInternal(loc);
    }

    @Override
    public boolean wasCritThresh() {
        return critThresh;
    }

    @Override
    public void setCritThresh(boolean b) {
        critThresh = b;
    }

    @Override
    public boolean isSpheroid() {
        return false;
    }

    @Override
    public int getStraightMoves() {
        return straightMoves;
    }

    @Override
    public void setStraightMoves(int i) {
        straightMoves = i;
    }

    @Override
    public int getAltLoss() {
        return altLoss;
    }

    @Override
    public void setAltLoss(int i) {
        altLoss = i;
    }

    @Override
    public void resetAltLoss() {
        altLoss = 0;
    }

    @Override
    public int getAltLossThisRound() {
        return altLossThisRound;
    }

    @Override
    public void setAltLossThisRound(int i) {
        altLossThisRound = i;
    }

    @Override
    public void resetAltLossThisRound() {
        altLossThisRound = 0;
    }

    @Override
    public boolean isVSTOL() {
        return true;
    }

    @Override
    public boolean isSTOL() {
        return false;
    }

    @Override
    public int getElevation() {
        if ((game != null) && game.getBoard().inSpace()) {
            return 0;
        }
        // Altitude is not the same as elevation. If an aero is at 0 altitude,
        // then it is
        // grounded and uses elevation normally. Otherwise, just set elevation
        // to a very
        // large number so that a flying aero won't interact with the ground
        // maps in any way
        if (isAirborne()) {
            return 999;
        }
        return super.getElevation();
    }

    @Override
    public int getFuelUsed(int thrust) {
        int overThrust = Math.max(thrust - getWalkMP(), 0);
        int safeThrust = thrust - overThrust;
        return safeThrust + (2 * overThrust);
    }

    @Override
    public boolean didFailManeuver() {
        return failedManeuver;
    }

    @Override
    public void setFailedManeuver(boolean b) {
        failedManeuver = b;
    }

    @Override
    public void setAccDecNow(boolean b) {
        accDecNow = b;
    }

    @Override
    public boolean didAccDecNow() {
        return accDecNow;
    }

    @Override
    public int getCapArmor() {
        return capitalArmor;
    }

    @Override
    public void setCapArmor(int i) {
        capitalArmor = i;
    }

    @Override
    public int getCap0Armor() {
        return capitalArmor_orig;
    }

    @Override
    public int getFatalThresh() {
        return fatalThresh;
    }

    @Override
    public void autoSetCapArmor() {
        double divisor = 10.0;
        if ((null != game) && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            divisor = 1.0;
        }
        capitalArmor_orig = (int) Math.round(getTotalOArmor() / divisor);
        capitalArmor = (int) Math.round(getTotalArmor() / divisor);
    }

    @Override
    public void autoSetFatalThresh() {
        int baseThresh = 2;
        if ((null != game) && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            baseThresh = 20;
        }
        fatalThresh = Math.max(baseThresh, (int) Math.ceil(capitalArmor / 4.0));
    }

    @Override
    public int getCurrentDamage() {
        return currentDamage;
    }

    @Override
    public void setCurrentDamage(int i) {
        currentDamage = i;
    }

    @Override
    public Map<String, Integer> getWeaponGroups() {
        return weaponGroups;
    }

    /**
     * Provide weapon groups for capital fighters. For capital fighter purposes
     * we use Aero locations.
     */
    @Override
    public Map<String, Integer> groupWeaponsByLocation() {
        Map<String, Integer> groups = new HashMap<>();
        for (Mounted mounted : getTotalWeaponList()) {
            int loc = LOC_CAPITAL_WINGS;
            if ((loc == Mech.LOC_CT) || (loc == Mech.LOC_HEAD)) {
                loc = LOC_CAPITAL_NOSE;
            }
            if (mounted.isRearMounted() || (loc == Mech.LOC_LLEG) || (loc == Mech.LOC_RLEG)) {
                loc = LOC_CAPITAL_AFT;
            }
            String key = mounted.getType().getInternalName() + ":" + loc;
            groups.merge(key, mounted.getNWeapons(), Integer::sum);
        }
        return groups;
    }

    // Damage a fighter that was part of a squadron when splitting it. Per
    // StratOps pg. 32 & 34
    @Override
    public void doDisbandDamage() {

        int dealt = 0;
        // Check for critical threshold and if so damage one facing of the
        // fighter completely.
        if (isDestroyed() || isDoomed()) {
            // Note starting armor + internal, so we can compute how many damage
            // points were allocated
            // in this step.
            int start = getTotalArmor() + getTotalInternal();
            int side = Compute.randomInt(4);
            switch (side) {
                case 0: // Nose
                    destroyLocation(LOC_HEAD);
                    destroyLocation(LOC_CT);
                    break;
                case 1: // Left wing
                    destroyLocation(LOC_LT);
                    destroyLocation(LOC_LARM);
                    break;
                case 2: // Right wing
                    destroyLocation(LOC_RT);
                    destroyLocation(LOC_RARM);
                    break;
                case 3: // Aft
                    destroyLocation(LOC_LLEG);
                    destroyLocation(LOC_RLEG);
                    break;
            }
            // Also apply three engine hits
            int i = 0;
            int engineHits = getEngineHits();
            while (engineHits < 3 && i < getNumberOfCriticals(LOC_CT)) {
                final CriticalSlot slot = getCritical(LOC_CT, i);
                if (slot != null && slot.getType() == CriticalSlot.TYPE_SYSTEM && slot.getIndex() == SYSTEM_ENGINE
                        && !slot.isDamaged()) {
                    slot.setHit(true);
                    engineHits++;
                }
                i++;
            }

            dealt = start - getTotalArmor() - getTotalInternal();
        }

        // Move on to actual damage...
        int damage = getCap0Armor() - getCapArmor();
        if ((getGame() != null) && !getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            damage *= 10;
        }
        damage -= dealt;

        if (damage >= 0) {
            int hits = (int) Math.ceil(damage / 5.0);
            int damPerHit = 5;
            for (int i = 0; i < hits; i++) {
                int loc = rollHitLocation(ToHitData.HIT_ABOVE, ToHitData.SIDE_RANDOM).getLocation();
                setArmor(getArmor(loc) - Math.max(damPerHit, damage), loc);
                // We did too much damage, so we need to damage the internal
                // structure
                if (getArmor(loc) < 0) {
                    if (getInternal(loc) > 1) {
                        int internal = getInternal(loc) + getArmor(loc);
                        if (internal <= 0) {
                            if ((loc == LOC_CT || loc == LOC_HEAD) && !isDestroyed() && !isDoomed()) {
                                setInternal(1, loc);// We don't want to destroy
                                                    // the fighter if it didn't
                                                    // pass the fatal threshold
                            } else {
                                destroyLocation(loc);
                            }
                        }
                    }
                    setArmor(0, loc);
                }
                damage -= damPerHit;
            }
        }
        applyDamage();
    }

    @Override
    public String getTilesetModeString() {
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            return "_FIGHTER";
        } else if (getConversionMode() == CONV_MODE_AIRMECH) {
            return "_AIRMECH";
        } else {
            return "";
        }
    }

    @Override
    public boolean isAero() {
        return getConversionMode() == CONV_MODE_FIGHTER;
    }

    @Override
    public boolean isBomber() {
        return true;
    }

    /**
     * Try to find a location that has enough empty bomb bays to accommodate the
     * bomb size
     */
    @Override
    public int availableBombLocation(int cost) {
        if (emptyBaysInLoc(LOC_RT) >= cost) {
            return LOC_RT;
        } else if (emptyBaysInLoc(LOC_LT) >= cost) {
            return LOC_LT;
        } else if (emptyBaysInLoc(LOC_CT) >= cost) {
            return LOC_CT;
        } else {
            return LOC_NONE;
        }
    }

    private int emptyBaysInLoc(int loc) {
        int bays = 0;
        for (CriticalSlot slot : crits[loc]) {
            if ((slot != null) && (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) && slot.getMount2() == null
                    && slot.getMount().getType() instanceof MiscType
                    && slot.getMount().getType().hasFlag(MiscType.F_BOMB_BAY)) {
                bays++;
            }
        }
        return bays;
    }

    @Override
    protected void addBomb(Mounted mounted, int loc) throws LocationFullException {
        if ((loc < 0) || (loc >= crits.length)) {
            LogManager.getLogger().error("Cannot add bomb " + mounted.getName() + " at illegal location " + loc);
            return;
        }

        mounted.setLocation(loc, false);
        int slots = 1;
        if (mounted.getType() instanceof BombType) {
            slots = BombType.getBombCost(((BombType) mounted.getType()).getBombType());
        } else if (mounted.getType() instanceof WeaponType) {
            int type = BombType.getBombTypeForWeapon(mounted.getType());
            if (type >= 0) {
                slots = BombType.getBombCost(type);
            } else {
                slots = mounted.getCriticals();
            }
        }

        for (int i = 0; i < crits[loc].length; i++) {
            final CriticalSlot slot = crits[loc][i];
            if (slot != null && slot.getType() == CriticalSlot.TYPE_EQUIPMENT
                    && (slot.getMount().getType() instanceof MiscType)
                    && slot.getMount().getType().hasFlag(MiscType.F_BOMB_BAY) && (slot.getMount2() == null)) {
                slot.setMount2(mounted);
                slots--;
                // Link the bay to the slot so we can find the bomb to explode
                // when the slot is hit.
                slot.getMount().setLinked(mounted);
                if (slots == 0) {
                    break;
                }
            }
        }

        if (slots > 0) {
            throw new LocationFullException();
        }

        mounted.setBombMounted(true);

        if (mounted instanceof BombMounted) {
            bombList.add((BombMounted) mounted);
        }

        if (mounted instanceof WeaponMounted) {
            totalWeaponList.add((WeaponMounted) mounted);
            weaponList.add((WeaponMounted) mounted);
            if (mounted.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                aTracker.addWeapon(mounted);
            }
            if (mounted.getType().hasFlag(WeaponType.F_ONESHOT) && (AmmoType.getOneshotAmmo(mounted) != null)) {
                AmmoMounted m = (AmmoMounted) Mounted.createMounted(this, AmmoType.getOneshotAmmo(mounted));
                m.setShotsLeft(1);
                mounted.setLinked(m);
                // Oneshot ammo will be identified by having a location
                // of null. Other areas in the code will rely on this.
                addEquipment(m, Entity.LOC_NONE, false);
            }
        }
        if (mounted instanceof AmmoMounted) {
            ammoList.add((AmmoMounted) mounted);
        }
        if (mounted instanceof MiscMounted) {
            miscList.add((MiscMounted) mounted);
        }
        equipmentList.add(mounted);
    }

    @Override
    public Mounted<?> addEquipment(EquipmentType etype, int loc, boolean rearMounted) throws LocationFullException {
        if (etype instanceof BombType) {
            Mounted<?> mounted = Mounted.createMounted(this, etype);
            addBomb(mounted, loc);
            return mounted;
        } else {
            return super.addEquipment(etype, loc, rearMounted);
        }
    }

    @Override
    public boolean canSpot() {
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            boolean hiresLighted = hasWorkingMisc(MiscType.F_HIRES_IMAGER)
                    && game.getPlanetaryConditions().getLight().isDayOrDusk();
            return !isAirborne()
                    || hasWorkingMisc(MiscType.F_RECON_CAMERA)
                    || hasWorkingMisc(MiscType.F_INFRARED_IMAGER)
                    || hasWorkingMisc(MiscType.F_HYPERSPECTRAL_IMAGER)
                    || hiresLighted;
        } else {
            return super.canSpot();
        }
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_MECH | Entity.ETYPE_BIPED_MECH | Entity.ETYPE_LAND_AIR_MECH;
    }

    /**
     * A method to add/remove sensors that only work in space as we transition in and out of an atmosphere
     */
    @Override
    public void updateSensorOptions() {
        // Remove everything but Radar if we're not in space
        if (!isSpaceborne()) {
            Vector<Sensor> sensorsToRemove = new Vector<>();
            if (isAero()) {
                for (Sensor sensor : getSensors()) {
                    if (sensor.getType() == Sensor.TYPE_AERO_THERMAL) {
                        sensorsToRemove.add(sensor);
                    }
                }
            }
            getSensors().removeAll(sensorsToRemove);
            if (sensorsToRemove.size() >= 1) {
            setNextSensor(getSensors().firstElement());
            }
        }
        // If we are in space, add them back...
        if (isSpaceborne()) {
            if (isAero()) {
                // ASFs and small craft get thermal/optical sensors
                getSensors().add(new Sensor(Sensor.TYPE_AERO_THERMAL));
                setNextSensor(getSensors().firstElement());
            }
        }
    }
}
