/*
* MegaMek -
* Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
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

import megamek.common.enums.AimingMode;
import megamek.common.loaders.MtfFile;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.common.weapons.autocannons.HVACWeapon;
import megamek.common.weapons.autocannons.LBXACWeapon;
import megamek.common.weapons.autocannons.UACWeapon;
import megamek.common.weapons.gaussrifles.GaussWeapon;
import megamek.common.weapons.lasers.CLImprovedHeavyLaserLarge;
import megamek.common.weapons.lasers.CLImprovedHeavyLaserMedium;
import megamek.common.weapons.lasers.CLImprovedHeavyLaserSmall;
import megamek.common.weapons.lasers.ISRISCHyperLaser;
import megamek.common.weapons.other.ISMekTaser;
import megamek.common.weapons.other.TSEMPWeapon;
import megamek.common.weapons.ppc.PPCWeapon;
import megamek.common.weapons.prototypes.*;
import org.apache.logging.log4j.LogManager;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * You know what mechs are, silly.
 */
public abstract class Mech extends Entity {
    private static final long serialVersionUID = -1929593228891136561L;

    // system designators for critical hits
    public static final int SYSTEM_LIFE_SUPPORT = 0;

    public static final int SYSTEM_SENSORS = 1;

    public static final int SYSTEM_COCKPIT = 2;

    public static final int SYSTEM_ENGINE = 3;

    public static final int SYSTEM_GYRO = 4;

    // actuators are systems too, for now
    public static final int ACTUATOR_SHOULDER = 7;

    public static final int ACTUATOR_UPPER_ARM = 8;

    public static final int ACTUATOR_LOWER_ARM = 9;

    public static final int ACTUATOR_HAND = 10;

    public static final int ACTUATOR_HIP = 11;

    public static final int ACTUATOR_UPPER_LEG = 12;

    public static final int ACTUATOR_LOWER_LEG = 13;

    public static final int ACTUATOR_FOOT = 14;

    public static final String[] systemNames = { "Life Support", "Sensors",
            "Cockpit", "Engine", "Gyro", null, null, "Shoulder", "Upper Arm",
            "Lower Arm", "Hand", "Hip", "Upper Leg", "Lower Leg", "Foot"};

    // locations
    public static final int LOC_HEAD = 0;

    public static final int LOC_CT = 1;

    public static final int LOC_RT = 2;

    public static final int LOC_LT = 3;

    public static final int LOC_RARM = 4;

    public static final int LOC_LARM = 5;

    public static final int LOC_RLEG = 6;

    public static final int LOC_LLEG = 7;

    // center leg, for tripods
    public static final int LOC_CLEG = 8;

    // cockpit status
    public static final int COCKPIT_OFF = 0;

    public static final int COCKPIT_ON = 1;

    public static final int COCKPIT_AIMED_SHOT = 2;

    // gyro types
    public static final int GYRO_UNKNOWN = -1;

    public static final int GYRO_STANDARD = 0;

    public static final int GYRO_XL = 1;

    public static final int GYRO_COMPACT = 2;

    public static final int GYRO_HEAVY_DUTY = 3;

    public static final int GYRO_NONE = 4;

    public static final int GYRO_SUPERHEAVY = 5;

    public static final String[] GYRO_STRING = { "Standard Gyro", "XL Gyro",
            "Compact Gyro", "Heavy Duty Gyro", "None", "Superheavy Gyro" };

    public static final String[] GYRO_SHORT_STRING = { "Standard", "XL",
            "Compact", "Heavy Duty", "None", "Superheavy" };

    // cockpit types
    public static final int COCKPIT_UNKNOWN = -1;

    public static final int COCKPIT_STANDARD = 0;

    public static final int COCKPIT_SMALL = 1;

    public static final int COCKPIT_COMMAND_CONSOLE = 2;

    public static final int COCKPIT_TORSO_MOUNTED = 3;

    public static final int COCKPIT_DUAL = 4;

    public static final int COCKPIT_INDUSTRIAL = 5;

    public static final int COCKPIT_PRIMITIVE = 6;

    public static final int COCKPIT_PRIMITIVE_INDUSTRIAL = 7;

    public static final int COCKPIT_SUPERHEAVY = 8;

    public static final int COCKPIT_SUPERHEAVY_TRIPOD = 9;

    public static final int COCKPIT_TRIPOD = 10;

    public static final int COCKPIT_INTERFACE = 11;

    public static final int COCKPIT_VRRP = 12;

    public static final int COCKPIT_QUADVEE = 13;

    public static final int COCKPIT_SUPERHEAVY_INDUSTRIAL = 14;
    
    public static final int COCKPIT_SUPERHEAVY_COMMAND_CONSOLE = 15;
    
    public static final int COCKPIT_SMALL_COMMAND_CONSOLE = 16;

    public static final String[] COCKPIT_STRING = { "Standard Cockpit",
            "Small Cockpit", "Command Console", "Torso-Mounted Cockpit",
            "Dual Cockpit", "Industrial Cockpit", "Primitive Cockpit",
            "Primitive Industrial Cockpit", "Superheavy Cockpit",
            "Superheavy Tripod Cockpit", "Tripod Cockpit", "Interface Cockpit",
            "Virtual Reality Piloting Pod", "QuadVee Cockpit",
            "Superheavy Industrial Cockpit", "Superheavy Command Console", 
	    "Small Command Console"};

    public static final String[] COCKPIT_SHORT_STRING = { "Standard", "Small",
            "Command Console", "Torso Mounted", "Dual", "Industrial",
            "Primitive", "Primitive Industrial", "Superheavy",
            "Superheavy Tripod", "Tripod", "Interface", "VRRP", "Quadvee",
            "Superheavy Industrial", "Superheavy Command", 
            "Small Command"};

    public static final String FULL_HEAD_EJECT_STRING = "Full Head Ejection System";

    // jump types
    public static final int JUMP_UNKNOWN = -1;
    public static final int JUMP_NONE = 0;
    public static final int JUMP_STANDARD = 1;
    public static final int JUMP_IMPROVED = 2;
    public static final int JUMP_PROTOTYPE = 3;
    public static final int JUMP_BOOSTER = 4;
    public static final int JUMP_DISPOSABLE = 5;
    // Type for Improved Jumpjet Prototype
    public static final int JUMP_PROTOTYPE_IMPROVED = 6;

    // Some "has" items only need be determined once
    public static final int HAS_FALSE = -1;

    public static final int HAS_UNKNOWN = 0;

    public static final int HAS_TRUE = 1;
    // rear armor
    private int[] rearArmor;

    private int[] orig_rearArmor;

    private boolean[] rearHardenedArmorDamaged;

    // for Harjel II/III
    private boolean[] armorDamagedThisTurn;

    private int sinksOn = -1;

    private int sinksOnNextRound = -1;

    private boolean autoEject = true;

    private boolean condEjectAmmo = true;

    private boolean condEjectEngine = true;

    private boolean condEjectCTDest = true;

    private boolean condEjectHeadshot = true;

    private int cockpitStatus = COCKPIT_ON;

    private int cockpitStatusNextRound = COCKPIT_ON;

    private int jumpType = JUMP_UNKNOWN;

    protected int gyroType = GYRO_STANDARD;

    protected int cockpitType = COCKPIT_STANDARD;

    private int cowlArmor = 3;

    private int hasLaserHeatSinks = HAS_UNKNOWN;

    // For grapple attacks
    private int grappled_id = Entity.NONE;

    private boolean isGrappleAttacker = false;

    private int grappledSide = Entity.GRAPPLE_BOTH;

    private boolean grappledThisRound = false;

    private boolean shouldDieAtEndOfTurnBecauseOfWater = false;

    private boolean justMovedIntoIndustrialKillingWater = false;

    private boolean stalled = false;

    private boolean stalledThisTurn = false;

    private boolean checkForCrit = false;

    private int levelsFallen = 0;

    private boolean fullHeadEject = false;

    protected static int[] EMERGENCY_COOLANT_SYSTEM_FAILURE = {3, 5, 7, 10, 13, 13, 13};

    // nCoolantSystemLevel is the # of turns RISC emergency coolant system has been used previously
    protected int nCoolantSystemLevel = 0;

    protected boolean bCoolantWentUp = false;

    protected boolean bUsedCoolantSystem = false; // Has emergency coolant system been used?

    protected boolean bDamagedCoolantSystem = false; // is the emergency coolant system damaged?

    protected int nCoolantSystemMOS = 0;

    // Cooling System Flaws quirk
    private boolean coolingFlawActive = false;

    // QuadVees, LAMs, and tracked 'Mechs can change movement mode.
    protected EntityMovementMode originalMovementMode = EntityMovementMode.BIPED;

    /**
     * Construct a new, blank, mech.
     */
    public Mech() {
        this(Mech.GYRO_STANDARD, Mech.COCKPIT_STANDARD);
    }

    public Mech(int inGyroType, int inCockpitType) {
        super();

        gyroType = inGyroType;
        cockpitType = inCockpitType;

        rearArmor = new int[locations()];
        orig_rearArmor = new int[locations()];
        rearHardenedArmorDamaged = new boolean[locations()];
        armorDamagedThisTurn = new boolean[locations()];

        for (int i = 0; i < locations(); i++) {
            if (!hasRearArmor(i)) {
                initializeRearArmor(IArmorState.ARMOR_NA, i);
            }
        }

        // Standard leg crits
        setCritical(LOC_RLEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                ACTUATOR_HIP));
        setCritical(LOC_RLEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                ACTUATOR_UPPER_LEG));
        setCritical(LOC_RLEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                ACTUATOR_LOWER_LEG));
        setCritical(LOC_RLEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                ACTUATOR_FOOT));

        setCritical(LOC_LLEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                ACTUATOR_HIP));
        setCritical(LOC_LLEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                ACTUATOR_UPPER_LEG));
        setCritical(LOC_LLEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                ACTUATOR_LOWER_LEG));
        setCritical(LOC_LLEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                ACTUATOR_FOOT));

        // Player setting specify whether their Meks' automatic
        // ejection systems are disabled by default or not.
        autoEject = !PreferenceManager.getClientPreferences()
                .defaultAutoejectDisabled();

        switch (inCockpitType) {
            case COCKPIT_TRIPOD:
                setCrew(new Crew(CrewType.TRIPOD));
                break;
            case COCKPIT_SUPERHEAVY_TRIPOD:
                setCrew(new Crew(CrewType.SUPERHEAVY_TRIPOD));
                break;
            case COCKPIT_DUAL:
                setCrew(new Crew(CrewType.DUAL));
                break;
            case COCKPIT_COMMAND_CONSOLE:
            case COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
            case COCKPIT_SMALL_COMMAND_CONSOLE:
                setCrew(new Crew(CrewType.COMMAND_CONSOLE));
                break;
            case COCKPIT_QUADVEE:
                setCrew(new Crew(CrewType.QUADVEE));
                break;
        }
    }

    @Override
    public int getUnitType() {
        return UnitType.MEK;
    }

    /**
     * @return if this mech cannot stand up from hulldown
     */
    public abstract boolean cannotStandUpFromHullDown();

    public int getCowlArmor() {
        if (hasCowl()) {
            return cowlArmor;
        }
        return 0;
    }

    public boolean hasCowl() {
        return hasQuirk(OptionsConstants.QUIRK_POS_COWL);
    }

    /**
     * Damage the cowl. Returns amount of excess damage
     *
     * @param amount
     * @return
     */
    public int damageCowl(int amount) {
        if (hasCowl()) {
            if (amount < cowlArmor) {
                cowlArmor -= amount;
                return 0;
            }
            amount -= cowlArmor;
            cowlArmor = 0;
            return amount;
        }
        return amount; // No cowl - return full damage
    }

    /**
     * Returns the location that transferred damage or crits will go to from a
     * given location.
     */
    public static int getInnerLocation(int location) {
        switch (location) {
            case Mech.LOC_RT:
            case Mech.LOC_LT:
            case Mech.LOC_CLEG:
                return Mech.LOC_CT;
            case Mech.LOC_LLEG:
            case Mech.LOC_LARM:
                return Mech.LOC_LT;
            case Mech.LOC_RLEG:
            case Mech.LOC_RARM:
                return Mech.LOC_RT;
            default:
                return location;
        }
    }

    /**
     * Returns the location with the most restrictive firing arc for a weapon.
     */
    public static int mostRestrictiveLoc(int location1, int location2) {
        if (location1 == location2) {
            return location1;
        } else if (Mech.restrictScore(location1) >= Mech
                .restrictScore(location2)) {
            return location1;
        } else {
            return location2;
        }
    }

    /**
     * find the least restrictive location of the two locations passed in
     *
     * @param location1
     * @param location2
     * @return
     */
    public static int leastRestrictiveLoc(int location1, int location2) {
        if (location1 == location2) {
            return location2;
        } else if (Mech.restrictScore(location1) >= Mech
                .restrictScore(location2)) {
            return location2;
        } else {
            return location1;
        }
    }

    /**
     * Helper function designed to give relative restrictiveness of locations.
     * Used for finding the most restrictive firing arc for a weapon.
     */
    public static int restrictScore(int location) {
        switch (location) {
            case Mech.LOC_RARM:
            case Mech.LOC_LARM:
                return 0;
            case Mech.LOC_RT:
            case Mech.LOC_LT:
                return 1;
            case Mech.LOC_CT:
                return 2;
            default:
                return 3;
        }
    }

    /**
     * OmniMechs have handles for Battle Armor squads to latch onto. Please
     * note, this method should only be called during this Mech's construction.
     * <p/>
     * Overrides <code>Entity#setOmni(boolean)</code>
     */
    @Override
    public void setOmni(boolean omni) {

        // Perform the superclass' action.
        super.setOmni(omni);

        // Add BattleArmorHandles to OmniMechs.
        if (omni && !hasBattleArmorHandles()) {
            addTransporter(new BattleArmorHandles());
        }
    }

    // Set whether a non-omni should have BA Grab Bars.
    public void setBAGrabBars() {
        if (isOmni()) {
            return;
        }
        // TODO: I really hate this optional rule - what if some units are
        // already loaded?
        // if ba_grab_bars is on, then we need to add battlearmor handles,
        // otherwise clamp mounts
        // but first clear out whatever we have
        // Removed the removal of transporters so that loaded units don't get ditched
        // This is an unofficial rule and it doesn't work well anyway as changing the option
        // does not affect units that are in the game
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_BA_GRAB_BARS)) {
            addTransporter(new BattleArmorHandles());
        } else {
            addTransporter(new ClampMountMech());
        }
    }

    public void setProtomechClampMounts() {
        boolean front = false;
        boolean rear = false;
        for (Transporter t: getTransports()) {
            if (t instanceof ProtomechClampMount) {
                front |= !((ProtomechClampMount) t).isRear();
                rear |= ((ProtomechClampMount) t).isRear();
            }
        }
        if (!front) {
            addTransporter(new ProtomechClampMount(false));
        }
        if (!rear) {
            addTransporter(new ProtomechClampMount(true));
        }
    }

    @Override
    public void load(Entity unit, boolean checkElev, int bayNumber) {
        if (unit.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
            boolean rear = bayNumber > 0;
            for (Transporter t : getTransports()) {
                if ((t instanceof ProtomechClampMount)
                        && t.canLoad(unit)
                        && (!checkElev || (unit.getElevation() == getElevation()))
                        && (((ProtomechClampMount) t).isRear() == rear)) {
                    t.load(unit);
                    unit.setTargetBay(-1);
                    return;
                }
            }
        }
        super.load(unit, checkElev, bayNumber);
    }

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
        return 8;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#newRound(int)
     */
    @Override
    public void newRound(int roundNumber) {
        // Walk through the Mech's miscellaneous equipment before
        // we apply our parent class' newRound() functionality
        // because Mek Stealth is set by the Entity#newRound() method.
        for (Mounted m : getMisc()) {
            MiscType mtype = (MiscType) m.getType();

            // Stealth can not be turned on if it's ECM is destroyed.
            if (mtype.hasFlag(MiscType.F_STEALTH)
                    && m.getLinked().isDestroyed()
                    && m.getLinked().isBreached()) {
                m.setMode("Off");
            }
        } // Check the next piece of equipment.

        super.newRound(roundNumber);
        // If MASC was used last turn, increment the counter,
        // otherwise decrement. Then, clear the counter
        if (usedMASC) {
            nMASCLevel++;
            bMASCWentUp = true;
        } else {
            nMASCLevel = Math.max(0, nMASCLevel - 1);
            if (bMASCWentUp) {
                nMASCLevel = Math.max(0, nMASCLevel - 1);
                bMASCWentUp = false;
            }
        }

        // Clear the MASC flag
        usedMASC = false;

        // If emergency cooling system was used last turn, increment the counter,
        // otherwise decrement. Then, clear the counter
        if (bUsedCoolantSystem) {
            nCoolantSystemLevel++;
            bCoolantWentUp = true;
        } else {
            nCoolantSystemLevel = Math.max(0, nCoolantSystemLevel - 1);
            if (bCoolantWentUp) {
                nCoolantSystemLevel = Math.max(0, nCoolantSystemLevel - 1);
                bCoolantWentUp = false;
            }
        }

        // Clear the coolant system flag
        bUsedCoolantSystem = false;


        setSecondaryFacing(getFacing());

        // set heat sinks
        sinksOn = sinksOnNextRound;

        // update cockpit status
        cockpitStatus = cockpitStatusNextRound;

        if (isJustMovedIntoIndustrialKillingWater()) {
            shouldDieAtEndOfTurnBecauseOfWater = true;
        } else {
            shouldDieAtEndOfTurnBecauseOfWater = false;
        }
        if (stalledThisTurn) {
            stalledThisTurn = false;
        }
        levelsFallen = 0;
        checkForCrit = false;

        grappledThisRound = false;

        // clear HarJel "took damage this turn" flags
        for (int loc = 0; loc < locations(); ++loc) {
            setArmorDamagedThisTurn(loc, false);
        }
    } // End public void newRound()

    /**
     * Returns true if the location in question is a torso location
     */
    public boolean locationIsTorso(int loc) {
        return (loc == LOC_CT) || (loc == LOC_RT) || (loc == LOC_LT);
    }

    /**
     * Returns true if the location in question is a leg
     */
    @Override
    public boolean locationIsLeg(int loc) {
        return (loc == LOC_LLEG) || (loc == LOC_RLEG);
    }

    /**
     * Count the number of destroyed or breached legs on the mech
     */
    public int countBadLegs() {
        int badLegs = 0;

        for (int i = 0; i < locations(); i++) {
            badLegs += (locationIsLeg(i) && isLocationBad(i)) ? 1 : 0;
        }

        return badLegs;
    }

    /**
     * Returns true if the entity has a hip crit.
     */
    @Override
    public boolean hasHipCrit() {
        for (int loc = 0; loc < locations(); loc++) {
            if (legHasHipCrit(loc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true is the location is a leg and has a hip crit
     */
    public boolean legHasHipCrit(int loc) {
        if (isLocationBad(loc)) {
            return false;
        }

        if (locationIsLeg(loc)) {
            return (getGoodCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.ACTUATOR_HIP, loc) == 0);
        }

        return false;
    }

    /**
     * This function returns true iff the system is in perfect condition.
     *
     * @param system
     *            the system to check
     * @return false if the system is damaged.
     */
    public boolean isSystemIntact(int system) {
        for (int loc = 0; loc < locations(); loc++) {
            int numCrits = getNumberOfCriticals(loc);
            for (int i = 0; i < numCrits; i++) {
                CriticalSlot ccs = getCritical(loc, i);

                if ((ccs != null)
                        && (ccs.getType() == CriticalSlot.TYPE_SYSTEM)
                        && (ccs.getIndex() == system)) {
                    if (ccs.isDamaged() || ccs.isBreached()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Count non-hip leg actuator crits
     */
    public int countLegActuatorCrits(int loc) {
        if (isLocationBad(loc)) {
            return 0;
        }

        int legCrits = 0;

        if (locationIsLeg(loc)) {
            if (getGoodCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.ACTUATOR_UPPER_LEG, loc) == 0) {
                legCrits++;
            }
            if (getGoodCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.ACTUATOR_LOWER_LEG, loc) == 0) {
                legCrits++;
            }
            if (getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT,
                    loc) == 0) {
                legCrits++;
            }
        }

        return legCrits;
    }

    /**
     * does this mech have composite internal structure?
     *
     * @return
     */
    public boolean hasCompositeStructure() {
        return (getStructureType() == EquipmentType.T_STRUCTURE_COMPOSITE);
    }

    /**
     * does this mech have reinforced internal structure?
     *
     * @return
     */
    public boolean hasReinforcedStructure() {
        return (getStructureType() == EquipmentType.T_STRUCTURE_REINFORCED);
    }

    /**
     * does this mech mount MASC?
     *
     * @return
     */
    public boolean hasMASC() {
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_MASC) && !mEquip.isInoperable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks if a mech has both a normal MASC system and a supercharger,
     * regardless of arming status
     */
    public boolean hasMASCAndSuperCharger() {
        boolean hasMASC = false;
        boolean hasSuperCharger = false;
        for (Mounted m : getEquipment()) {
            if (!m.isInoperable() && (m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_MASC)
                    && m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
                hasSuperCharger = true;
            }
            if (!m.isInoperable() && (m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_MASC)
                    && !m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
                hasMASC = true;
            }
        }
        return hasMASC && hasSuperCharger;
    }

    /**
     * does this mech have working jump boosters?
     *
     * @return
     */
    public boolean hasJumpBoosters() {
        boolean jumpBoosters = false;
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_JUMP_BOOSTER)) {

                // one crit destroyed they all all screwed
                // --Torren
                if (mEquip.isBreached() || mEquip.isDestroyed()
                        || mEquip.isMissing()) {
                    return false;
                }
                jumpBoosters = true;
            }
        }
        return jumpBoosters;
    }

    /**
     * Checks if a mech has an armed MASC system. Note that the mech will have
     * to exceed its normal run to actually engage the MASC system
     */
    public boolean hasArmedMASC() {
        for (Mounted m : getEquipment()) {
            if (!m.isDestroyed() && !m.isBreached()
                    && (m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_MASC)
                    && m.curMode().equals("Armed")) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks if a mech has both a normal armed MASC system and a armed super-
     * charger.
     */
    public boolean hasArmedMASCAndSuperCharger() {
        boolean hasMASC = false;
        boolean hasSuperCharger = false;
        for (Mounted m : getEquipment()) {
            if (!m.isDestroyed() && !m.isBreached()
                    && (m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_MASC)
                    && m.curMode().equals("Armed")
                    && m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
                hasSuperCharger = true;
            }
            if (!m.isDestroyed() && !m.isBreached()
                    && (m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_MASC)
                    && m.curMode().equals("Armed")
                    && !m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
                hasMASC = true;
            }
        }
        return hasMASC && hasSuperCharger;
    }

    /**
     * Does this mech have an extended retractable blade in working condition?
     */
    public boolean hasExtendedRetractableBlade() {
        for (Mounted m : getEquipment()) {
            if (!m.isInoperable() && (m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_CLUB)
                    && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE)
                    && m.curMode().equals("extended")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does the entity have a retracted blade in the given location? Only true
     * for biped mechs
     */
    public boolean hasRetractedBlade(int loc) {
        return false;
    }

    /**
     * Check for whether the mech has triple strength myomer
     * @param includePrototype Whether to include prototype TSM in the check.
     *                         Prototype TSM does not have a movement bonus or
     *                         a required heat level.
     * @return Whether the mech has TSM
     */
    public boolean hasTSM(boolean includePrototype) {
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_TSM)
                    && (includePrototype || !m.getType().hasFlag(MiscType.F_PROTOTYPE))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean antiTSMVulnerable() {
        for (Mounted m : getMisc()) {
            if ((m.getType().hasFlag(MiscType.F_TSM) && m.getType().hasFlag(MiscType.F_PROTOTYPE))
                    || (m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM) && (getYear() <= 3050))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActiveTSM() {
        return hasActiveTSM(true);
    }

    /**
     * Checks whether any type of TSM is active. Industrial and prototype
     * are always on. Standard is on when heat level is >= 9.
     *
     * @param includeIndustrial Whether to include industrial TSM in the check
     * @return Whether the mech has some form of TSM and it's active
     */
    public boolean hasActiveTSM(boolean includeIndustrial) {
        for (Mounted m : getMisc()) {
            if (includeIndustrial && m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                return true;
            } else if (m.getType().hasFlag(MiscType.F_TSM)) {
                return (heat >=9) || m.getType().hasFlag(MiscType.F_PROTOTYPE);
            }
        }
        return false;
    }

    /**
     * does this mech have SCM?
     */
    public boolean hasSCM() {
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_SCM)) {
                return true;
            }
        }
        return false;
    }

    /**
     * does this mech have industrial TSM=
     *
     * @return
     */
    public boolean hasIndustrialTSM() {
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                return true;
            }
        }
        return false;
    }

    /**
     * does this mech have a null-sig-system?
     *
     * @return
     */
    public boolean hasNullSig() {
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_NULLSIG)) {
                // The Mek has Null-Sig
                return true;
            }
        }
        return false;
    }

    /**
     * does this mech have a void-sig-system?
     *
     * @return
     */
    public boolean hasVoidSig() {
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_VOIDSIG)) {
                // The Mek has Void-Sig
                return true;
            }
        }
        return false;
    }

    /**
     * Does this mech have tracks? Used for tracks as industrial equipment; QuadVees return false.
     *
     * @return
     */
    public boolean hasTracks() {
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_TRACKS)) {
                // The Mek has tracks
                return true;
            }
        }
        return false;
    }

    /**
     * does this mech have a chameleon light polarization shield?
     *
     * @return
     */
    public boolean hasChameleonShield() {
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                // The Mek has Chameleon Light Polarization Field
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getStandingHeat()
     */
    @Override
    public int getStandingHeat() {
        return hasEngine() ? getEngine().getStandingHeat() : 0;
    }

    /**
     * set this mech's <code>Engine</code>
     *
     * @param e
     *            the <code>Engine</code> to set
     */
    @Override
    public void setEngine(Engine e) {
        super.setEngine(e);
        if (hasEngine() && getEngine().engineValid) {
            setOriginalWalkMP(calculateWalk());
        }
    }

    /**
     * Used to set this Mech's original walk mp
     *
     * @return this units calculated walking speed, dependent on engine rating
     *         and weight
     */
    protected int calculateWalk() {
        if (!hasEngine()) {
            return 0;
        }
        if (isPrimitive()) {
            double rating = getEngine().getRating();
            rating /= 1.2;
            if ((rating % 5) != 0) {
                return (int) ((rating - (rating % 5)) + 5) / (int) weight;
            }
            return (int) (rating / (int) weight);

        }
        return getEngine().getRating() / (int) weight;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getWalkHeat()
     */
    @Override
    public int getWalkHeat() {
        int extra = bDamagedCoolantSystem?1:0;
        return extra + (hasEngine() ? getEngine().getWalkHeat(this) : 0);
    }

    /**
     * Returns whether this mech should use conditional ejection
     *
     * @return
     */
    /*
     * public boolean shouldUseConditionalEject() { if (game !=null &&
     * game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)) { return true; }
     *
     * return false; }
     */

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getRunMP(boolean, boolean, boolean)
     */
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        if (hasArmedMASCAndSuperCharger()) {
            return ((int) Math.ceil(getWalkMP(gravity, ignoreheat,
                    ignoremodulararmor) * 2.5))
                    - (hasMPReducingHardenedArmor() ? 1 : 0);
        }
        if (hasArmedMASC()) {
            return (getWalkMP(gravity, ignoreheat, ignoremodulararmor) * 2)
                    - (hasMPReducingHardenedArmor() ? 1 : 0);
        }
        return Math.max(0, super.getRunMP(gravity, ignoreheat, ignoremodulararmor)
                - (hasMPReducingHardenedArmor() ? 1 : 0));
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getRunMPwithoutMASC(boolean, boolean, boolean)
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        return super.getRunMP(gravity, ignoreheat, ignoremodulararmor)
                - (hasMPReducingHardenedArmor() ? 1 : 0);
    }

    /**
     * @return The mech's run MP without MASC or supercharger, but with any reduction
     *         due to hardened armor.
     */
    public int getOriginalRunMPwithoutMASC() {
        return super.getOriginalRunMP()
                - (hasMPReducingHardenedArmor() ? 1 : 0);
    }

    /**
     * Returns this entity's running/flank mp as a string.
     */
    @Override
    public String getRunMPasString() {
        if (hasArmedMASC()) {
            return getRunMPwithoutMASC() + "(" + getRunMP() + ")";
        }
        return Integer.toString(getRunMP());
    }

    /**
     * Depends on engine type
     */
    @Override
    public int getRunHeat() {
        int extra = bDamagedCoolantSystem?1:0;
        return extra + (hasEngine() ? getEngine().getRunHeat(this) : 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getSprintMP()
     */
    @Override
    public int getSprintMP() {
        if (hasHipCrit()) {
            return getRunMP();
        }
        return getSprintMP(true, false, false);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getSprintMP(boolean, boolean, boolean)
     */
    @Override
    public int getSprintMP(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        if (hasHipCrit()) {
            return getRunMP(gravity, ignoreheat, ignoremodulararmor);
        }
        if (hasArmedMASCAndSuperCharger()) {
            return ((int) Math.ceil(getWalkMP(gravity, ignoreheat,
                    ignoremodulararmor) * 3.0))
                    - (hasMPReducingHardenedArmor() ? 1 : 0);
        }
        if (hasArmedMASC()) {
            return ((int) Math.ceil(getWalkMP(gravity, ignoreheat,
                    ignoremodulararmor) * 2.5))
                    - (hasMPReducingHardenedArmor() ? 1 : 0);
        }
        return getSprintMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getSprintMPwithoutMASC(boolean, boolean)
     */
    @Override
    public int getSprintMPwithoutMASC() {
        return getSprintMPwithoutMASC(true, false, false);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getSprintMPwithoutMASC(boolean, boolean,
     * boolean)
     */
    @Override
    public int getSprintMPwithoutMASC(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        if (hasHipCrit()) {
            return getRunMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
        }
        return ((int) Math.ceil(getWalkMP(gravity, ignoreheat,
                ignoremodulararmor) * 2.0))
                - (hasMPReducingHardenedArmor() ? 1 : 0);
    }

    public int getOriginalSprintMPwithoutMASC() {
        return ((int) Math.ceil(getOriginalWalkMP() * 2.0)) - (hasMPReducingHardenedArmor() ? 1 : 0);
    }

    /**
     * Returns this entity's Sprint mp as a string.
     */
    @Override
    public String getSprintMPasString() {
        if (hasArmedMASC()) {
            return getRunMPwithoutMASC() + "(" + getSprintMP() + ")";
        }
        return Integer.toString(getSprintMP());
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getRunningGravityLimit()
     */
    @Override
    public int getRunningGravityLimit() {
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_SPRINT)) {
            return getSprintMP(false, false, false);
        }
        return getRunMP(false, false, false);
    }

    /**
     * Depends on engine type
     */
    @Override
    public int getSprintHeat() {
        int extra = bDamagedCoolantSystem?1:0;
        return extra + (hasEngine() ? getEngine().getSprintHeat() : 0);
    }

    /**
     * This mech's jumping MP modified for missing jump jets and gravity
     */
    @Override
    public int getJumpMP() {
        return getJumpMP(true);
    }

    /**
     * This mech's jumping MP modified for missing jump jets and possibly
     * gravity
     */
    @Override
    public int getJumpMP(boolean gravity) {
        return getJumpMP(gravity, false);
    }

    public int getJumpMP(boolean gravity, boolean ignoremodulararmor) {
        int jump = 0;

        if (hasShield() && (getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return 0;
        }

        for (Mounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_JUMP_JET)
                    && !mounted.isDestroyed() && !mounted.isBreached()) {
                jump++;
            } else if (mounted.getType().hasFlag(MiscType.F_JUMP_BOOSTER)
                    && !mounted.isDestroyed() && !mounted.isBreached()) {
                jump = getOriginalJumpMP();
                break;
            }
        }

        // apply Partial Wing bonus if we have the ability to jump
        if (jump > 0) {
            for (Mounted mount : getMisc()) {
                if (mount.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                    jump += getPartialWingJumpBonus(mount);
                    break;
                }
            }
        }
        // Medium shield reduces jump mp by 1/shield
        jump -= getNumberOfShields(MiscType.S_SHIELD_MEDIUM);

        if (hasModularArmor() && !ignoremodulararmor) {
            jump--;
        }
        
        if (gravity) {
            return Math.max(applyGravityEffectsOnMP(jump), 0);
        }
        return Math.max(jump, 0);
    }

    /**
     * Gives the bonus to Jump MP conferred by a mech partial wing.
     *
     * @param mount
     *            The mounted location of the Wing
     * @return The Jump MP bonus conferred by the wing
     */
    public int getPartialWingJumpBonus(Mounted mount) {
        int bonus;
        if (game != null) {
            if ((getWeightClass() <= EntityWeightClass.WEIGHT_MEDIUM)) {
                switch (game.getPlanetaryConditions().getAtmosphere()) {
                    case PlanetaryConditions.ATMO_VACUUM:
                    case PlanetaryConditions.ATMO_TRACE:
                        bonus = 0;
                        break;
                    case PlanetaryConditions.ATMO_THIN:
                        bonus = 1;
                        break;
                    case PlanetaryConditions.ATMO_VHIGH:
                        bonus = 3;
                        break;
                    case PlanetaryConditions.ATMO_STANDARD:
                    case PlanetaryConditions.ATMO_HIGH:
                    default:
                        bonus = 2;
                        break;
                }
            } else {
                switch (game.getPlanetaryConditions().getAtmosphere()) {
                    case PlanetaryConditions.ATMO_VACUUM:
                    case PlanetaryConditions.ATMO_TRACE:
                    case PlanetaryConditions.ATMO_THIN:
                        bonus = 0;
                        break;
                    case PlanetaryConditions.ATMO_HIGH:
                    case PlanetaryConditions.ATMO_VHIGH:
                        bonus = 2;
                        break;
                    case PlanetaryConditions.ATMO_STANDARD:
                    default:
                        bonus = 1;
                }
            }
        } else {
            if ((getWeightClass() <= EntityWeightClass.WEIGHT_MEDIUM)) {
                bonus = 2;
            } else {
                bonus = 1;
            }
        }

        // subtract jumping bonus for damaged criticals
        bonus -= getBadCriticals(CriticalSlot.TYPE_EQUIPMENT,
                getEquipmentNum(mount), Mech.LOC_RT);
        bonus -= getBadCriticals(CriticalSlot.TYPE_EQUIPMENT,
                getEquipmentNum(mount), Mech.LOC_LT);

        return Math.max(bonus, 0);
    }

    /**
     * Gives the heat capacity bonus conferred by a mech partial wing.
     *
     * @return the heat capacity bonus provided by the wing
     */
    private int getPartialWingHeatBonus() {
        int bonus;
        if (game != null) {
            switch (game.getPlanetaryConditions().getAtmosphere()) {
                case PlanetaryConditions.ATMO_VACUUM:
                    bonus = 0;
                    break;
                case PlanetaryConditions.ATMO_TRACE:
                    bonus = 1;
                    break;
                case PlanetaryConditions.ATMO_THIN:
                    bonus = 2;
                    break;
                case PlanetaryConditions.ATMO_STANDARD:
                case PlanetaryConditions.ATMO_HIGH:
                case PlanetaryConditions.ATMO_VHIGH:
                default:
                    bonus = 3;
                    break;
            }
        } else {
            bonus = 3;
        }

        return bonus;
    }

    /**
     * Returns the type of jump jet system the mech has.
     */
    @Override
    public int getJumpType() {
        jumpType = JUMP_NONE;
        for (Mounted m : miscList) {
            if (m.getType().hasFlag(MiscType.F_JUMP_JET)) {
                if (m.getType().hasSubType(MiscType.S_IMPROVED)
                        && m.getType().hasSubType(MiscType.S_PROTOTYPE)) {
                    jumpType = JUMP_PROTOTYPE_IMPROVED;
                } else if (m.getType().hasSubType(MiscType.S_IMPROVED)) {
                    jumpType = JUMP_IMPROVED;
                } else if (m.getType().hasSubType(MiscType.S_PROTOTYPE)) {
                    jumpType = JUMP_PROTOTYPE;
                } else {
                    jumpType = JUMP_STANDARD;
                }
                break;
            } else if (m.getType().hasFlag(MiscType.F_JUMP_BOOSTER)) {
                jumpType = JUMP_BOOSTER;
                break;
            }
        }
        return jumpType;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getJumpHeat(int)
     */
    @Override
    public int getJumpHeat(int movedMP) {

        int extra = bDamagedCoolantSystem?1:0;

        // don't count movement granted by Partial Wing
        for (Mounted mount : getMisc()) {
            if (mount.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                movedMP -= getPartialWingJumpBonus(mount);
                break;
            }
        }

        switch (getJumpType()) {
            case JUMP_IMPROVED:
                return extra + (hasEngine() ? getEngine().getJumpHeat((movedMP / 2) + (movedMP % 2)) : 0);
            case JUMP_PROTOTYPE_IMPROVED:
                // min 6 heat, otherwise 2xJumpMp, XTRO:Succession Wars pg17
                return extra + (hasEngine() ? Math.max(6, getEngine().getJumpHeat(movedMP * 2)) : 0);
            case JUMP_BOOSTER:
            case JUMP_DISPOSABLE:
                return extra;
            case JUMP_NONE:
                return 0;
            default:
                return extra + (hasEngine() ? getEngine().getJumpHeat(movedMP) : 0);
        }
    }

    /**
     * Returns this mech's jumping MP, modified for missing & underwater jets
     * and gravity.
     */
    @Override
    public int getJumpMPWithTerrain() {
        if ((getPosition() == null) || (game.getBoard().getHex(getPosition()) == null) || (getJumpType() == JUMP_BOOSTER)) {
            return getJumpMP();
        }
        int waterLevel = 0;
        if (!isOffBoard()) {
            waterLevel = game.getBoard().getHex(getPosition()).terrainLevel(Terrains.WATER);
        }
        if ((waterLevel <= 0) || (getElevation() >= 0)) {
            return getJumpMP();
        } else if (waterLevel > 1) {
            return 0;
        } else { // waterLevel == 1
            return applyGravityEffectsOnMP(torsoJumpJets());
        }
    }

    /**
     * Returns the number of (working) jump jets mounted in the torsos.
     */
    public int torsoJumpJets() {
        int jump = 0;

        for (Mounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_JUMP_JET)
                    && !mounted.isDestroyed() && !mounted.isBreached()
                    && locationIsTorso(mounted.getLocation())) {
                jump++;
            }
        }

        // apply Partial Wing bonus if we have the ability to jump
        if (jump > 0) {
            for (Mounted mount : getMisc()) {
                if (mount.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                    jump += getPartialWingJumpBonus(mount);
                    break;
                }
            }
        }

        return jump;
    }

    @Override
    public boolean isEligibleForPavementBonus() {
        //eligible if using Mech tracks
        return movementMode == EntityMovementMode.TRACKED;
    }

    @Override
    public EntityMovementMode nextConversionMode(EntityMovementMode afterMode) {
        if (hasTracks() && afterMode != EntityMovementMode.TRACKED) {
            return EntityMovementMode.TRACKED;
        } else {
            return originalMovementMode;
        }
    }

    /**
     * QuadVees and LAMs may not have to make PSRs to avoid falling depending on their mode,
     * and Mechs using tracks for movement do not have to make PSRs for damage to gyro or leg
     * actuators.
     *
     * @param gyroLegDamage Whether the PSR is due to damage to gyro or leg actuators
     * @return              true if the Mech can fall due to failed PSR.
     */
    @Override
    public boolean canFall(boolean gyroLegDamage) {
        return !isProne() && !(gyroLegDamage && movementMode == EntityMovementMode.TRACKED);
    }

    /**
     * Return the height of this mech above the terrain.
     */
    @Override
    public int height() {
        return isProne() ? 0 : isSuperHeavy() ? 2 : 1;
    }

    /**
     * Adds heat sinks to the engine. Uses clan/normal depending on the
     * currently set techLevel
     */
    public void addEngineSinks(int totalSinks, BigInteger heatSinkFlag) {
        addEngineSinks(totalSinks, heatSinkFlag, isClan());
    }

    /**
     * Adds heat sinks to the engine. Adds either the engine capacity, or the
     * entire number of heat sinks, whichever is less
     */
    public void addEngineSinks(int totalSinks, BigInteger heatSinkFlag,
            boolean clan) {
        if (heatSinkFlag == MiscType.F_DOUBLE_HEAT_SINK) {
            addEngineSinks(totalSinks, clan ? EquipmentTypeLookup.CLAN_DOUBLE_HS
                    : EquipmentTypeLookup.IS_DOUBLE_HS);
        } else if (heatSinkFlag == MiscType.F_COMPACT_HEAT_SINK) {
            addEngineSinks(totalSinks, EquipmentTypeLookup.COMPACT_HS_1);
        } else if (heatSinkFlag == MiscType.F_LASER_HEAT_SINK) {
            addEngineSinks(totalSinks, EquipmentTypeLookup.LASER_HS);
        } else {
            addEngineSinks(totalSinks, EquipmentTypeLookup.SINGLE_HS);
        }
    }

    /**
     * base for adding engine sinks. Newer method allows externals to say how
     * much are engine HS.
     *
     * @param totalSinks
     *            the amount of heatsinks to add to the engine
     * @param sinkName
     *            the <code>String</code> determining the type of heatsink to
     *            add. must be a lookupname of a heatsinktype
     */
    public void addEngineSinks(int totalSinks, String sinkName) {
        if (!hasEngine()) {
            return;
        }
        EquipmentType sinkType = EquipmentType.get(sinkName);

        if (sinkType == null) {
            System.out.println("Mech: can't find heat sink to add to engine");
        }

        int toAllocate = Math.min(
                totalSinks,
                getEngine().integralHeatSinkCapacity(
                        sinkType.hasFlag(MiscType.F_COMPACT_HEAT_SINK)));
        addEngineSinks(sinkName, toAllocate);
    }

    /**
     * add heat sinks into the engine
     *
     * @param sinkName
     *            the <code>String</code> determining the type of heatsink to
     *            add. must be a lookupname of a heatsinktype
     * @param toAllocate
     *            Number of hs to add to the Engine.
     */
    public void addEngineSinks(String sinkName, int toAllocate) {
        // this relies on these being the correct internalNames for these items
        EquipmentType sinkType = EquipmentType.get(sinkName);

        if (sinkType == null) {
            System.out.println("Mech: can't find heat sink to add to engine");
        }

        for (int i = 0; i < toAllocate; i++) {
            try {
                addEquipment(new Mounted(this, sinkType), Entity.LOC_NONE,
                        false);
            } catch (LocationFullException ex) {
                // um, that's impossible.
            }
        }
    }

    /**
     * Returns extra heat generated by engine crits
     */
    @Override
    public int getEngineCritHeat() {
        if (!hasEngine()) {
            return 0;
        }
        int engineCritHeat = 0;
        if (!isShutDown() && getEngine().isFusion()) {
            engineCritHeat += 5 * getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_ENGINE, Mech.LOC_CT);
            engineCritHeat += 5 * getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_ENGINE, Mech.LOC_LT);
            engineCritHeat += 5 * getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_ENGINE, Mech.LOC_RT);
        }
        // Partial Repairs of the engine cause additional heat
        if (getPartialRepairs().booleanOption("mech_reactor_3_crit")) {
            engineCritHeat += 8;
        }
        if (getPartialRepairs().booleanOption("mech_reactor_2_crit")) {
            engineCritHeat += 5;
        }
        if (getPartialRepairs().booleanOption("mech_reactor_1_crit")) {
            engineCritHeat += 3;
        }
        if (getPartialRepairs().booleanOption("mech_engine_replace")) {
            engineCritHeat += 1;
        }

        // add the partial repair heat here.
        return engineCritHeat;
    }

    /**
     * Returns the number of heat sinks, functional or not.
     */
    public int heatSinks() {
        return heatSinks(true);
    }

    /**
     * Returns the number of heat sinks, functional or not.
     *
     * @param countPrototypes
     *            Set TRUE to include Prototype Heat Sinks in the total.
     */
    public int heatSinks(boolean countPrototypes) {
        int sinks = 0;
        for (Mounted mounted : getMisc()) {
            EquipmentType etype = mounted.getType();
            if (etype.hasFlag(MiscType.F_COMPACT_HEAT_SINK)
                    && (etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK) || (etype
                            .hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE) && countPrototypes))) {
                sinks += 2;
            } else if (etype.hasFlag(MiscType.F_HEAT_SINK)
                    || etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                    || (etype.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE) && countPrototypes)) {
                sinks++;
            }
        }
        return sinks;
    }

    /**
     * Returns the number of destroyed heat sinks.
     */
    public int damagedHeatSinks() {
        int sinks = 0;
        for (Mounted mounted : getMisc()) {
            EquipmentType etype = mounted.getType();
            if (!mounted.isDestroyed()) {
                continue;
            }
            if (etype.hasFlag(MiscType.F_HEAT_SINK)
                    || etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                    || etype.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                sinks++;
            }
        }
        return sinks;
    }

    /**
     * Returns the about of heat that the entity can sink each turn.
     */
    @Override
    public int getHeatCapacity() {
        return getHeatCapacity(true, true);
    }

    @Override
    public int getHeatCapacity(boolean radicalHeatSink) {
        return getHeatCapacity(true, radicalHeatSink);
    }

    /**
     * Returns the name of the heat sinks mounted on this 'mech.
     *
     * @return
     */
    public String getHeatSinkTypeName() {
        for (Mounted m : getMisc()) {
            // The MiscType name for compact heat sinks is formatted differently
            if (m.getType().hasFlag(MiscType.F_COMPACT_HEAT_SINK)) {
                return "Compact Heat Sink";
            }
            if (m.getType().hasFlag(MiscType.F_HEAT_SINK)
                    || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                    || m.getType().hasFlag(MiscType.F_LASER_HEAT_SINK)) {
                return m.getName();
            }
        }
        
        // if a mech has no heat sink equipment, we pretend like it has standard heat sinks.
        return "Heat Sink";
    }

    public int getHeatCapacity(boolean includePartialWing,
            boolean includeRadicalHeatSink) {
        int capacity = 0;
        int activeCount = getActiveSinks();

        for (Mounted mounted : getMisc()) {
            if (mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }
            if ((activeCount > 0)
                    && mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                capacity++;
                activeCount--;
            } else if ((activeCount > 0)
                    && mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                activeCount--;
                capacity += 2;
            } else if (mounted.getType().hasFlag(
                    MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                capacity += 2;
            } else if (includePartialWing
                    && mounted.getType().hasFlag(MiscType.F_PARTIAL_WING)
                    && // unless
                       // all crits
                       // are
                       // destroyed,
                       // we get
                       // the bonus
                    ((getGoodCriticals(CriticalSlot.TYPE_EQUIPMENT,
                            getEquipmentNum(mounted), Mech.LOC_RT) > 0) || (getGoodCriticals(
                            CriticalSlot.TYPE_EQUIPMENT,
                            getEquipmentNum(mounted), Mech.LOC_LT) > 0))) {
                capacity += getPartialWingHeatBonus();
                includePartialWing = false; // Only count the partial wing bonus
                                            // once.
            }
        }
        // AirMech mode for LAMs confers the same heat benefits as a partial wing.
        if (includePartialWing && movementMode == EntityMovementMode.WIGE) {
            capacity += getPartialWingHeatBonus();
        }
        if (includeRadicalHeatSink
                && hasWorkingMisc(MiscType.F_RADICAL_HEATSINK)) {
            capacity += (int) Math.ceil(getActiveSinks() * 0.4);
        }

        return capacity;
    }

    /**
     * Returns the about of heat that the entity can sink each turn, factoring
     * for water.
     */
    @Override
    public int getHeatCapacityWithWater() {
        if (hasLaserHeatSinks()) {
            return getHeatCapacity(true, false);
        }
        return getHeatCapacity(true, false) + Math.min(sinksUnderwater(), 6);
    }

    /**
     * Gets the number of heat sinks that are underwater.
     */
    private int sinksUnderwater() {
        if ((getPosition() == null) || isOffBoard()) {
            return 0;
        }

        Hex curHex = game.getBoard().getHex(getPosition());
        // are we even in water? is it depth 1+
        if ((curHex.terrainLevel(Terrains.WATER) <= 0) || (getElevation() >= 0)) {
            return 0;
        }

        // are we entirely underwater?
        if (isProne() || (curHex.terrainLevel(Terrains.WATER) >= 2)) {
            return getHeatCapacity();
        }

        // okay, count leg sinks
        int sinksUnderwater = 0;
        for (Mounted mounted : getMisc()) {
            if (mounted.isDestroyed() || mounted.isBreached()
                    || !locationIsLeg(mounted.getLocation())) {
                continue;
            }
            if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                sinksUnderwater++;
            } else if (mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                    || mounted.getType().hasFlag(
                            MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                sinksUnderwater += 2;
            }
        }
        return sinksUnderwater;
    }
    
    @Override
    public boolean tracksHeat() {
        return true;
    }

    /**
     * Returns the name of the type of movement used. This is mech-specific.
     */
    @Override
    public String getMovementString(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_SKID:
                return "Skidded";
            case MOVE_NONE:
            case MOVE_CAREFUL_STAND:
                return "None";
            case MOVE_WALK:
                return "Walked";
            case MOVE_RUN:
                return "Ran";
            case MOVE_JUMP:
                return "Jumped";
            case MOVE_SPRINT:
                return "Sprinted";
            //LAM AirMech modes
            case MOVE_VTOL_WALK:
                return "Cruised";
            case MOVE_VTOL_RUN:
                return "Flanked";
            default:
                return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used. This is mech-specific.
     */
    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_SKID:
                return "S";
            case MOVE_NONE:
                return "N";
            case MOVE_WALK:
                return "W";
            case MOVE_RUN:
                return "R";
            case MOVE_JUMP:
                return "J";
            case MOVE_SPRINT:
                return "Sp";
            //LAM AirMech modes
            case MOVE_VTOL_WALK:
                return "C";
            case MOVE_VTOL_RUN:
                return "F";
            default:
                return "?";
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#canChangeSecondaryFacing()
     */
    @Override
    public boolean canChangeSecondaryFacing() {
        if (hasQuirk(OptionsConstants.QUIRK_NEG_NO_TWIST)) {
            return false;
        }
        return !isProne() && !isBracing();
    }

    /**
     * Can this mech torso twist in the given direction?
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            if (hasQuirk(OptionsConstants.QUIRK_POS_EXT_TWIST)) {
                return (rotate == 0) || (rotate == 1) || (rotate == 2)
                        || (rotate == -1) || (rotate == -2) || (rotate == -5)
                        || (rotate == -4) || (rotate == 5) || (rotate == 4);
            }
            return (rotate == 0) || (rotate == 1) || (rotate == -1)
                    || (rotate == -5) || (rotate == 5);
        }
        return rotate == 0;
    }

    /**
     * Return the nearest valid direction to torso twist in
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        if (isValidSecondaryFacing(dir)) {
            return dir;
        }
        // can't twist while prone
        if (!canChangeSecondaryFacing()) {
            return getFacing();
        }
        // otherwise, twist once in the appropriate direction
        final int rotate = (dir + (6 - getFacing())) % 6;
        if ((rotate == 3) && hasQuirk(OptionsConstants.QUIRK_POS_EXT_TWIST)) {
            // if the unit can do an extended torso twist and the area chosen
            // was directly behind them, then just rotate one way
            return (getFacing() + 2) % 6;
        }
        return rotate >= 3 ? (getFacing() + 5) % 6 : (getFacing() + 1) % 6;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#hasRearArmor(int)
     */
    @Override
    public boolean hasRearArmor(int loc) {
        return (loc == LOC_CT) || (loc == LOC_RT) || (loc == LOC_LT);
    }

    /**
     * Returns the amount of armor in the location specified. Mech version,
     * handles rear armor.
     */
    @Override
    public int getArmor(int loc, boolean rear) {
        if (isLocationBlownOff(loc)) {
            return IArmorState.ARMOR_DESTROYED;
        }
        return getArmorForReal(loc, rear);
    }

    @Override
    public int getArmorForReal(int loc, boolean rear) {
        if (rear && hasRearArmor(loc)) {
            return rearArmor[loc];
        }
        return super.getArmorForReal(loc, rear);
    }

    /**
     * Returns the original amount of armor in the location specified. Mech
     * version, handles rear armor.
     */
    @Override
    public int getOArmor(int loc, boolean rear) {
        if (rear && hasRearArmor(loc)) {
            return orig_rearArmor[loc];
        }
        return super.getOArmor(loc, rear);
    }

    /**
     * Sets the amount of armor in the location specified. Mech version, handles
     * rear armor.
     */
    @Override
    public void setArmor(int val, int loc, boolean rear) {
        if (rear && hasRearArmor(loc)) {
            rearArmor[loc] = val;
        } else {
            super.setArmor(val, loc, rear);
        }
    }

    /**
     * Initializes the rear armor on the mech. Sets the original and starting
     * point of the armor to the same number.
     */
    public void initializeRearArmor(int val, int loc) {
        orig_rearArmor[loc] = val;
        setArmor(val, loc, true);
    }

    @Override
    public void setHardenedArmorDamaged(HitData hit, boolean damaged) {
        if (hit.isRear() && hasRearArmor(hit.getLocation())) {
            rearHardenedArmorDamaged[hit.getLocation()] = damaged;
        } else {
            hardenedArmorDamaged[hit.getLocation()] = damaged;
        }
    }

    @Override
    public boolean isHardenedArmorDamaged(HitData hit) {
        if (hit.isRear() && hasRearArmor(hit.getLocation())) {
            return rearHardenedArmorDamaged[hit.getLocation()];
        } else {
            return hardenedArmorDamaged[hit.getLocation()];
        }
    }

    /**
     * did the armor in this location take damage which did not destroy it
     * at least once this turn?
     * this is used to decide whether to trigger Harjel II/III
     */
    public void setArmorDamagedThisTurn(int loc, boolean tookdamage) {
        armorDamagedThisTurn[loc] = tookdamage;
    }

    /**
     * did the armor in this location take damage which did not destroy it
     * at least once this turn?
     * this is used to decide whether to trigger Harjel II/III
     */
    public boolean isArmorDamagedThisTurn(int loc) {
        return armorDamagedThisTurn[loc];
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);

        // B-Pods need to be special-cased, the have 360 firing arc
        if ((mounted.getType() instanceof WeaponType)
                && mounted.getType().hasFlag(WeaponType.F_B_POD)) {
            return Compute.ARC_360;
        }
        // VGLs base arc on their facing
        if (mounted.getType().hasFlag(WeaponType.F_VGL)) {
            return Compute.firingArcFromVGLFacing(mounted.getFacing());
        }
        // rear mounted?
        if (mounted.isRearMounted()) {
            return Compute.ARC_REAR;
        }
        // front mounted
        switch (mounted.getLocation()) {
            case LOC_HEAD:
            case LOC_CT:
            case LOC_RT:
            case LOC_LT:
            case LOC_RLEG:
            case LOC_LLEG:
                return Compute.ARC_FORWARD;
            case LOC_RARM:
                return getArmsFlipped() ? Compute.ARC_REAR
                        : Compute.ARC_RIGHTARM;
            case LOC_LARM:
                return getArmsFlipped() ? Compute.ARC_REAR
                        : Compute.ARC_LEFTARM;
            default:
                return Compute.ARC_360;
        }
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc. If
     * false, assume it fires into the primary.
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        // leg-mounted weapons fire into the primary arc, always
        if ((getEquipment(weaponId).getLocation() == LOC_RLEG)
                || (getEquipment(weaponId).getLocation() == LOC_LLEG)) {
            return false;
        }
        // other weapons into the secondary
        return true;
    }

    @Override
    public String joinLocationAbbr(List<Integer> locations, int limit) {
        // If all locations are torso, strip the T from all but the last location.
        // e.g. R/L/CT
        if ((locations.size() > limit) && locations.stream().allMatch(this::locationIsTorso)) {
            return locations.stream().map(l -> getLocationAbbr(l).replace("T", ""))
                    .collect(Collectors.joining("/")) + "T";
        } else {
            return super.joinLocationAbbr(locations, limit);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#rollHitLocation(int, int)
     */
    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, AimingMode.NONE, LosEffects.COVER_NONE);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#rollHitLocation(int, int, int, int)
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
                                   int cover) {
        int roll = -1;

        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {
            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
        }

        if ((table == ToHitData.HIT_NORMAL) || (table == ToHitData.HIT_PARTIAL_COVER)) {
            roll = Compute.d6(2);
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable thrown) {
                thrown.printStackTrace();
            }
            if (side == ToHitData.SIDE_FRONT) {
                // normal front hits
                switch (roll) {
                    case 2:
                        if ((getCrew().hasEdgeRemaining() && getCrew()
                                .getOptions().booleanOption(OptionsConstants.EDGE_WHEN_TAC))
                                && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(tac(table, side,
                                    Mech.LOC_CT, cover, false));
                            return result;
                        } // if
                        return tac(table, side, Mech.LOC_CT, cover, false);
                    case 3:
                    case 4:
                        return new HitData(Mech.LOC_RARM);
                    case 5:
                        return new HitData(Mech.LOC_RLEG);
                    case 6:
                        return new HitData(Mech.LOC_RT);
                    case 7:
                        return new HitData(Mech.LOC_CT);
                    case 8:
                        return new HitData(Mech.LOC_LT);
                    case 9:
                        return new HitData(Mech.LOC_LLEG);
                    case 10:
                    case 11:
                        return new HitData(Mech.LOC_LARM);
                    case 12:
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(
                                        OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mech.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_LEFT) {
                // normal left side hits
                switch (roll) {
                    case 2:
                        if ((getCrew().hasEdgeRemaining() && getCrew()
                                .getOptions().booleanOption(OptionsConstants.EDGE_WHEN_TAC))
                                && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(tac(table, side,
                                    Mech.LOC_LT, cover, false));
                            return result;
                        } // if
                        return tac(table, side, Mech.LOC_LT, cover, false);
                    case 3:
                        return new HitData(Mech.LOC_LLEG);
                    case 4:
                    case 5:
                        return new HitData(Mech.LOC_LARM);
                    case 6:
                        return new HitData(Mech.LOC_LLEG);
                    case 7:
                        return new HitData(Mech.LOC_LT);
                    case 8:
                        if (game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MECH_HIT_LOCATIONS)) {
                            return new HitData(Mech.LOC_CT, true);
                        }
                        return new HitData(Mech.LOC_CT);
                    case 9:
                        if (game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MECH_HIT_LOCATIONS)) {
                            return new HitData(Mech.LOC_RT, true);
                        }
                        return new HitData(Mech.LOC_RT);
                    case 10:
                        return new HitData(Mech.LOC_RARM);
                    case 11:
                        return new HitData(Mech.LOC_RLEG);
                    case 12:
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(
                                        OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mech.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_RIGHT) {
                // normal right side hits
                switch (roll) {
                    case 2:
                        if ((getCrew().hasEdgeRemaining() && getCrew()
                                .getOptions().booleanOption(OptionsConstants.EDGE_WHEN_TAC))
                                && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(tac(table, side,
                                    Mech.LOC_RT, cover, false));
                            return result;
                        } // if
                        return tac(table, side, Mech.LOC_RT, cover, false);
                    case 3:
                        return new HitData(Mech.LOC_RLEG);
                    case 4:
                    case 5:
                        return new HitData(Mech.LOC_RARM);
                    case 6:
                        return new HitData(Mech.LOC_RLEG);
                    case 7:
                        return new HitData(Mech.LOC_RT);
                    case 8:
                        if (game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MECH_HIT_LOCATIONS)) {
                            return new HitData(Mech.LOC_CT, true);
                        }
                        return new HitData(Mech.LOC_CT);
                    case 9:
                        if (game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MECH_HIT_LOCATIONS)) {
                            return new HitData(Mech.LOC_LT, true);
                        }
                        return new HitData(Mech.LOC_LT);
                    case 10:
                        return new HitData(Mech.LOC_LARM);
                    case 11:
                        return new HitData(Mech.LOC_LLEG);
                    case 12:
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(
                                        OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mech.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_REAR) {
                // normal rear hits
                if (game.getOptions().booleanOption(
                        OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MECH_HIT_LOCATIONS)
                        && isProne()) {
                    switch (roll) {
                        case 2:
                            if ((getCrew().hasEdgeRemaining() && getCrew()
                                    .getOptions()
                                    .booleanOption(OptionsConstants.EDGE_WHEN_TAC))
                                    && !game.getOptions().booleanOption(
                                            OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side,
                                        aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side,
                                        Mech.LOC_CT, cover, true));
                                return result;
                            } // if
                            return tac(table, side, Mech.LOC_CT, cover, true);
                        case 3:
                            return new HitData(Mech.LOC_RARM, true);
                        case 4:
                        case 5:
                            return new HitData(Mech.LOC_RLEG, true);
                        case 6:
                            return new HitData(Mech.LOC_RT, true);
                        case 7:
                            return new HitData(Mech.LOC_CT, true);
                        case 8:
                            return new HitData(Mech.LOC_LT, true);
                        case 9:
                        case 10:
                            return new HitData(Mech.LOC_LLEG, true);
                        case 11:
                            return new HitData(Mech.LOC_LARM, true);
                        case 12:
                            if (getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(
                                            OptionsConstants.EDGE_WHEN_HEADHIT)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side,
                                        aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(
                                        Mech.LOC_HEAD, true));
                                return result;
                            } // if
                            return new HitData(Mech.LOC_HEAD, true);
                    }
                } else {
                    switch (roll) {
                        case 2:
                            if ((getCrew().hasEdgeRemaining() && getCrew()
                                    .getOptions()
                                    .booleanOption(OptionsConstants.EDGE_WHEN_TAC))
                                    && !game.getOptions().booleanOption(
                                            OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side,
                                        aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side,
                                        Mech.LOC_CT, cover, true));
                                return result;
                            } // if
                            return tac(table, side, Mech.LOC_CT, cover, true);
                        case 3:
                        case 4:
                            return new HitData(Mech.LOC_RARM, true);
                        case 5:
                            return new HitData(Mech.LOC_RLEG, true);
                        case 6:
                            return new HitData(Mech.LOC_RT, true);
                        case 7:
                            return new HitData(Mech.LOC_CT, true);
                        case 8:
                            return new HitData(Mech.LOC_LT, true);
                        case 9:
                            return new HitData(Mech.LOC_LLEG, true);
                        case 10:
                        case 11:
                            return new HitData(Mech.LOC_LARM, true);
                        case 12:
                            if (getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(
                                            OptionsConstants.EDGE_WHEN_HEADHIT)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side,
                                        aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(
                                        Mech.LOC_HEAD, true));
                                return result;
                            } // if
                            return new HitData(Mech.LOC_HEAD, true);
                    }
                }
            }
        }
        if (table == ToHitData.HIT_PUNCH) {
            roll = Compute.d6(1);
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences()
                        .getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable thrown) {
                thrown.printStackTrace();
            }
            if (side == ToHitData.SIDE_FRONT) {
                // front punch hits
                switch (roll) {
                    case 1:
                        return new HitData(Mech.LOC_LARM);
                    case 2:
                        return new HitData(Mech.LOC_LT);
                    case 3:
                        return new HitData(Mech.LOC_CT);
                    case 4:
                        return new HitData(Mech.LOC_RT);
                    case 5:
                        return new HitData(Mech.LOC_RARM);
                    case 6:
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(
                                        OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mech.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_LEFT) {
                // left side punch hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mech.LOC_LT);
                    case 3:
                        return new HitData(Mech.LOC_CT);
                    case 4:
                    case 5:
                        return new HitData(Mech.LOC_LARM);
                    case 6:
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(
                                        OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mech.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_RIGHT) {
                // right side punch hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mech.LOC_RT);
                    case 3:
                        return new HitData(Mech.LOC_CT);
                    case 4:
                    case 5:
                        return new HitData(Mech.LOC_RARM);
                    case 6:
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(
                                        OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mech.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_REAR) {
                // rear punch hits
                switch (roll) {
                    case 1:
                        return new HitData(Mech.LOC_LARM, true);
                    case 2:
                        return new HitData(Mech.LOC_LT, true);
                    case 3:
                        return new HitData(Mech.LOC_CT, true);
                    case 4:
                        return new HitData(Mech.LOC_RT, true);
                    case 5:
                        return new HitData(Mech.LOC_RARM, true);
                    case 6:
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(
                                        OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side,
                                    aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD,
                                    true));
                            return result;
                        } // if
                        return new HitData(Mech.LOC_HEAD, true);
                }
            }
        }
        if (table == ToHitData.HIT_KICK) {
            roll = Compute.d6(1);
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences()
                        .getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable thrown) {
                thrown.printStackTrace();
            }
            if ((side == ToHitData.SIDE_FRONT) || (side == ToHitData.SIDE_REAR)) {
                // front/rear kick hits
                switch (roll) {
                    case 1:
                    case 2:
                    case 3:
                        return new HitData(Mech.LOC_RLEG,
                                (side == ToHitData.SIDE_REAR));
                    case 4:
                    case 5:
                    case 6:
                        return new HitData(Mech.LOC_LLEG,
                                (side == ToHitData.SIDE_REAR));
                }
            }
            if (side == ToHitData.SIDE_LEFT) {
                // left side kick hits
                return new HitData(Mech.LOC_LLEG);
            }
            if (side == ToHitData.SIDE_RIGHT) {
                // right side kick hits
                return new HitData(Mech.LOC_RLEG);
            }
        }
        if ((table == ToHitData.HIT_SWARM)
                || (table == ToHitData.HIT_SWARM_CONVENTIONAL)) {
            roll = Compute.d6(2);
            int effects;
            if (table == ToHitData.HIT_SWARM_CONVENTIONAL) {
                effects = HitData.EFFECT_NONE;
            } else {
                effects = HitData.EFFECT_CRITICAL;
            }
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences()
                        .getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable thrown) {
                thrown.printStackTrace();
            }
            // Swarm attack locations.
            switch (roll) {
                case 2:
                    if (getCrew().hasEdgeRemaining()
                            && getCrew().getOptions().booleanOption(
                                    OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side,
                                aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD,
                                false, effects));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD, false, effects);
                case 3:
                    return new HitData(Mech.LOC_CT, true, effects);
                case 4:
                    return new HitData(Mech.LOC_RT, true, effects);
                case 5:
                    return new HitData(Mech.LOC_RT, false, effects);
                case 6:
                    return new HitData(Mech.LOC_RARM, false, effects);
                case 7:
                    return new HitData(Mech.LOC_CT, false, effects);
                case 8:
                    return new HitData(Mech.LOC_LARM, false, effects);
                case 9:
                    return new HitData(Mech.LOC_LT, false, effects);
                case 10:
                    return new HitData(Mech.LOC_LT, true, effects);
                case 11:
                    return new HitData(Mech.LOC_CT, true, effects);
                case 12:
                    if (getCrew().hasEdgeRemaining()
                            && getCrew().getOptions().booleanOption(
                                    OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side,
                                aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD,
                                false, effects));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD, false, effects);
            }
        }
        if (table == ToHitData.HIT_ABOVE) {
            roll = Compute.d6(1);
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences()
                        .getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable thrown) {
                thrown.printStackTrace();
            }
            // Hits from above.
            switch (roll) {
                case 1:
                    return new HitData(Mech.LOC_LARM,
                            (side == ToHitData.SIDE_REAR));
                case 2:
                    return new HitData(Mech.LOC_LT,
                            (side == ToHitData.SIDE_REAR));
                case 3:
                    return new HitData(Mech.LOC_CT,
                            (side == ToHitData.SIDE_REAR));
                case 4:
                    return new HitData(Mech.LOC_RT,
                            (side == ToHitData.SIDE_REAR));
                case 5:
                    return new HitData(Mech.LOC_RARM,
                            (side == ToHitData.SIDE_REAR));
                case 6:
                    if (getCrew().hasEdgeRemaining()
                            && getCrew().getOptions().booleanOption(
                                    OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side,
                                aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD,
                                (side == ToHitData.SIDE_REAR)));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD,
                            (side == ToHitData.SIDE_REAR));
            }
        }
        if (table == ToHitData.HIT_BELOW) {
            roll = Compute.d6(1);
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences()
                        .getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable thrown) {
                thrown.printStackTrace();
            }
            // Hits from below.
            switch (roll) {
                case 1:
                case 2:
                    return new HitData(Mech.LOC_LLEG, (side == ToHitData.SIDE_REAR));
                case 3:
                    return new HitData(Mech.LOC_LT, (side == ToHitData.SIDE_REAR));
                case 4:
                    return new HitData(Mech.LOC_RT, (side == ToHitData.SIDE_REAR));
                case 5:
                case 6:
                    return new HitData(Mech.LOC_RLEG, (side == ToHitData.SIDE_REAR));
            }
        }
        return null;
    }

    /**
     * Called when a thru-armor-crit is rolled. Checks the game options and
     * either returns no critical hit, rolls a floating crit, or returns a TAC
     * in the specified location.
     */
    protected HitData tac(int table, int side, int location, int cover,
            boolean rear) {
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
            return new HitData(location, rear);
        } else if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_FLOATING_CRITS)) {
            HitData hd = rollHitLocation(table, side);
            // check for cover and keep rolling until you get something without
            // cover
            int i = 0;
            while (removePartialCoverHits(hd.getLocation(), cover, side)
                    && (i < 500)) {
                hd = rollHitLocation(table, side);
                i++;
            }
            return new HitData(hd.getLocation(), hd.isRear(),
                    HitData.EFFECT_CRITICAL);
        } else {
            return new HitData(location, rear, HitData.EFFECT_CRITICAL);
        }
    }

    /**
     * Gets the location that excess damage transfers to
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        switch (hit.getLocation()) {
            case LOC_RT:
            case LOC_LT:
            case LOC_CLEG:
                return new HitData(LOC_CT, hit.isRear(), hit.getEffect(),
                        hit.hitAimedLocation(), hit.getSpecCritMod(),
                        hit.getSpecCrit(), hit.isFromFront(),
                        hit.getGeneralDamageType(), hit.glancingMod());
            case LOC_LLEG:
            case LOC_LARM:
                return new HitData(LOC_LT, hit.isRear(), hit.getEffect(),
                        hit.hitAimedLocation(), hit.getSpecCritMod(),
                        hit.getSpecCrit(), hit.isFromFront(),
                        hit.getGeneralDamageType(), hit.glancingMod());
            case LOC_RLEG:
            case LOC_RARM:
                return new HitData(LOC_RT, hit.isRear(), hit.getEffect(),
                        hit.hitAimedLocation(), hit.getSpecCritMod(),
                        hit.getSpecCrit(), hit.isFromFront(),
                        hit.getGeneralDamageType(), hit.glancingMod());
            case LOC_HEAD:
                if (getCockpitType() == COCKPIT_TORSO_MOUNTED) {
                    return new HitData(LOC_NONE); // not destroyed by head loss
                }
                return new HitData(LOC_DESTROYED);
            case LOC_CT:
            default:
                return new HitData(LOC_DESTROYED);
        }
    }

    /**
     * Gets the location that is destroyed recursively
     */
    @Override
    public int getDependentLocation(int loc) {
        switch (loc) {
            case LOC_RT:
                return LOC_RARM;
            case LOC_LT:
                return LOC_LARM;
            case LOC_LLEG:
            case LOC_LARM:
            case LOC_RLEG:
            case LOC_RARM:
            case LOC_HEAD:
            case LOC_CT:
            default:
                return LOC_NONE;
        }
    }

    /**
     * Sets the internal structure for the mech.
     *
     * @param head
     *            head
     * @param ct
     *            center torso
     * @param t
     *            right/left torso
     * @param arm
     *            right/left arm
     * @param leg
     *            right/left leg
     */
    public abstract void setInternal(int head, int ct, int t, int arm, int leg);

    /**
     * Set the internal structure to the appropriate value for the mech's weight
     * class
     */
    @Override
    public void autoSetInternal() {
        // stupid irregular table... grr.
        switch ((int) weight) {
        // H, CT,TSO,ARM,LEG
            case 10:
                setInternal(3, 4, 3, 1, 2);
                break;
            case 15:
                setInternal(3, 5, 4, 2, 3);
                break;
            case 20:
                setInternal(3, 6, 5, 3, 4);
                break;
            case 25:
                setInternal(3, 8, 6, 4, 6);
                break;
            case 30:
                setInternal(3, 10, 7, 5, 7);
                break;
            case 35:
                setInternal(3, 11, 8, 6, 8);
                break;
            case 40:
                setInternal(3, 12, 10, 6, 10);
                break;
            case 45:
                setInternal(3, 14, 11, 7, 11);
                break;
            case 50:
                setInternal(3, 16, 12, 8, 12);
                break;
            case 55:
                setInternal(3, 18, 13, 9, 13);
                break;
            case 60:
                setInternal(3, 20, 14, 10, 14);
                break;
            case 65:
                setInternal(3, 21, 15, 10, 15);
                break;
            case 70:
                setInternal(3, 22, 15, 11, 15);
                break;
            case 75:
                setInternal(3, 23, 16, 12, 16);
                break;
            case 80:
                setInternal(3, 25, 17, 13, 17);
                break;
            case 85:
                setInternal(3, 27, 18, 14, 18);
                break;
            case 90:
                setInternal(3, 29, 19, 15, 19);
                break;
            case 95:
                setInternal(3, 30, 20, 16, 20);
                break;
            case 100:
                setInternal(3, 31, 21, 17, 21);
                break;
            case 105:
                setInternal(4, 32, 22, 17, 22);
                break;
            case 110:
                setInternal(4, 33, 23, 18, 23);
                break;
            case 115:
                setInternal(4, 35, 24, 19, 24);
                break;
            case 120:
                setInternal(4, 36, 25, 20, 25);
                break;
            case 125:
                setInternal(4, 38, 26, 21, 26);
                break;
            case 130:
                setInternal(4, 39, 27, 21, 27);
                break;
            case 135:
                setInternal(4, 41, 28, 22, 28);
                break;
            case 140:
                setInternal(4, 42, 29, 23, 29);
                break;
            case 145:
                setInternal(4, 44, 31, 24, 31);
                break;
            case 150:
                setInternal(4, 45, 32, 25, 32);
                break;
            case 155:
                setInternal(4, 47, 33, 26, 33);
                break;
            case 160:
                setInternal(4, 48, 34, 26, 34);
                break;
            case 165:
                setInternal(4, 50, 35, 27, 35);
                break;
            case 170:
                setInternal(4, 51, 36, 28, 36);
                break;
            case 175:
                setInternal(4, 53, 37, 29, 37);
                break;
            case 180:
                setInternal(4, 54, 38, 30, 38);
                break;
            case 185:
                setInternal(4, 56, 39, 31, 39);
                break;
            case 190:
                setInternal(4, 57, 40, 31, 40);
                break;
            case 195:
                setInternal(4, 59, 41, 32, 41);
                break;
            case 200:
                setInternal(4, 60, 42, 33, 42);
                break;
        }
    }

    /**
     * Adds clan CASE in every location
     */
    public void addClanCase() {
        boolean explosiveFound = false;
        EquipmentType clCase = EquipmentType.get(EquipmentTypeLookup.CLAN_CASE);
        for (int i = 0; i < locations(); i++) {
            explosiveFound = false;
            for (Mounted m : getEquipment()) {
                if (m.getType().isExplosive(m) && (m.getLocation() == i)) {
                    explosiveFound = true;
                }
            }
            if (explosiveFound) {
                try {
                    addEquipment(new Mounted(this, clCase), i, false);
                } catch (LocationFullException ex) {
                    // um, that's impossible.
                }
            }
        }
    }

    public Mounted addEquipment(EquipmentType etype, EquipmentType etype2,
            int loc,  boolean omniPod, boolean armored) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        Mounted mounted2 = new Mounted(this, etype2);
        mounted.setOmniPodMounted(omniPod);
        mounted2.setOmniPodMounted(omniPod);
        mounted.setArmored(armored);
        mounted2.setArmored(armored);
        // check criticals for space
        if (getEmptyCriticals(loc) < 1) {
            throw new LocationFullException(mounted.getName() + " and "
                    + mounted2.getName() + " do not fit in "
                    + getLocationAbbr(loc) + " on " + getDisplayName()
                    + "\n        free criticals in location: "
                    + getEmptyCriticals(loc) + ", criticals needed: " + 1);
        }
        super.addEquipment(mounted, loc, false);
        super.addEquipment(mounted2, loc, false);
        CriticalSlot cs = new CriticalSlot(mounted);
        cs.setMount2(mounted2);
        addCritical(loc, cs);
        return mounted;
    }

    /**
     * Mounts the specified weapon in the specified location.
     */
    @Override
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        addEquipment(mounted,loc,rearMounted,-1);
    }

    /**
     * Mounts the specified weapon in the specified location.
     */
    @Override
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted,
            int critSlot)
            throws LocationFullException {
        // if there's no actual location or this is a LAM capital fighter weapons group,
        // or ammo for a LAM bomb weapon then don't add criticals
        if ((loc == LOC_NONE) || mounted.isWeaponGroup()
                || (mounted.getType() instanceof BombType)) {
            super.addEquipment(mounted, loc, rearMounted);
            return;
        }

        // spreadable or split equipment only gets added to 1 crit at a time,
        // since we don't know how many are in this location
        int reqSlots = mounted.getCriticals();
        if (mounted.getType().isSpreadable() || mounted.isSplitable()) {
            reqSlots = 1;
        }
        if (isSuperHeavy()) {
            reqSlots = (int) Math.ceil(((double) reqSlots / 2.0f));
        }
        // gauss and AC weapons on omni arms means no arm actuators, so we
        // remove them
        if (isOmni()
                && (this instanceof BipedMech)
                && ((loc == LOC_LARM) || (loc == LOC_RARM))
                && ((mounted.getType() instanceof GaussWeapon)
                        || (mounted.getType() instanceof ACWeapon)
                        || (mounted.getType() instanceof UACWeapon)
                        || (mounted.getType() instanceof LBXACWeapon) || (mounted
                            .getType() instanceof PPCWeapon))) {
            if (hasSystem(Mech.ACTUATOR_LOWER_ARM, loc)) {
                setCritical(loc, 2, null);
            }
            if (hasSystem(Mech.ACTUATOR_HAND, loc)) {
                setCritical(loc, 3, null);
            }
        }

        // check criticals for space
        if (getEmptyCriticals(loc) < reqSlots) {
            throw new LocationFullException(mounted.getName()
                    + " does not fit in " + getLocationAbbr(loc) + " on "
                    + getDisplayName()
                    + "\n        free criticals in location: "
                    + getEmptyCriticals(loc) + ", criticals needed: "
                    + reqSlots);
        }
        // add it
        if (getEquipmentNum(mounted) == -1) {
            super.addEquipment(mounted, loc, rearMounted);
        }

        // add criticals
        if (critSlot == -1) {
            for (int i = 0; i < reqSlots; i++) {
                CriticalSlot cs = new CriticalSlot(mounted);
                addCritical(loc, cs);
            }
        } else {
            // Need to ensure that we have enough contiguous critical slots
            int iterations = 0;
            while ((getContiguousNumberOfCrits(loc, critSlot) < reqSlots) &&
                    (iterations < getNumberOfCriticals(loc))) {
                critSlot = (critSlot + 1) % getNumberOfCriticals(loc);
                iterations++;
            }
            if (iterations >= getNumberOfCriticals(loc)) {
                throw new LocationFullException(mounted.getName()
                        + " does not fit in " + getLocationAbbr(loc) + " on "
                        + getDisplayName()
                        + "\n    needs "
                        + getEmptyCriticals(loc)
                        + " free contiguous criticals");
            }
            for (int i = 0; i < reqSlots; i++) {
                CriticalSlot cs = new CriticalSlot(mounted);
                addCritical(loc, cs, critSlot);
                critSlot = (critSlot + 1) % getNumberOfCriticals(loc);
            }
        }
    }

    //From IO pg 50
    public static TechAdvancement getTechAdvancement(long etype, boolean primitive, boolean industrial, int weightClass) {
        if ((etype & ETYPE_TRIPOD_MECH) != 0) {
            if (weightClass != EntityWeightClass.WEIGHT_SUPER_HEAVY) {
                return new TechAdvancement(TECH_BASE_IS)
                        .setISAdvancement(2585, 2602).setISApproximate(true).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setTechRating(RATING_D)
                        .setAvailability(RATING_F, RATING_F, RATING_F, RATING_E)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED);
            } else {
                return new TechAdvancement(TECH_BASE_IS)
                        .setISAdvancement(2930, 2940).setISApproximate(true).setPrototypeFactions(F_FW)
                        .setProductionFactions(F_FW).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_F, RATING_X, RATING_F)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED);
            }
        } else if (primitive && industrial) {
            return new TechAdvancement(TECH_BASE_IS)
                    .setISAdvancement(2300, 2350, 2425, 2520).setPrototypeFactions(F_TA)
                    .setProductionFactions(F_TH).setTechRating(RATING_D)
                    .setAvailability(RATING_D, RATING_X, RATING_F, RATING_F)
                    .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else if (primitive) {
            return new TechAdvancement(TECH_BASE_IS)
                    .setISAdvancement(2439, 2443, 2470, 2520).setPrototypeFactions(F_TH)
                    .setProductionFactions(F_TH).setTechRating(RATING_C)
                    .setAvailability(RATING_C, RATING_X, RATING_F, RATING_F)
                    .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else if (industrial && (EntityWeightClass.WEIGHT_SUPER_HEAVY == weightClass)) {
            // Superheavy industrialmechs don't have a separate entry on the tech advancement
            // table in IO, but the dates for the superheavy tripod are based on the
            // three-man digging machine, which is an industrialmech.
            return new TechAdvancement(TECH_BASE_IS)
                    .setAdvancement(2930, 2940).setPrototypeFactions(F_FW)
                    .setProductionFactions(F_FW).setTechRating(RATING_D)
                    .setAvailability(RATING_X, RATING_F, RATING_X, RATING_F)
                    .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else if (industrial) {
            return new TechAdvancement(TECH_BASE_ALL)
                    .setAdvancement(2460, 2470, 2500).setPrototypeFactions(F_TH)
                    .setProductionFactions(F_TH).setTechRating(RATING_C)
                    .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
                    .setStaticTechLevel(SimpleTechLevel.STANDARD);
        } else if (EntityWeightClass.WEIGHT_ULTRA_LIGHT == weightClass) {
            return new TechAdvancement(TECH_BASE_ALL)
                    .setAdvancement(2500, 2519, 3075).setPrototypeFactions(F_TH, F_FW)
                    .setProductionFactions(F_FW).setApproximate(true, false, true)
                    .setTechRating(RATING_D)
                    .setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                    .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else if (EntityWeightClass.WEIGHT_SUPER_HEAVY == weightClass) {
            return new TechAdvancement(TECH_BASE_IS)
                    .setISAdvancement(3077, 3078).setPrototypeFactions(F_WB)
                    .setProductionFactions(F_WB).setTechRating(RATING_D)
                    .setAvailability(RATING_X, RATING_F, RATING_F, RATING_F)
                    .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else {
            return new TechAdvancement(TECH_BASE_ALL)
                    .setAdvancement(2460, 2470, 2500).setPrototypeFactions(F_TH)
                    .setProductionFactions(F_TH).setTechRating(RATING_D)
                    .setAvailability(RATING_C, RATING_E, RATING_D, RATING_C)
                    .setStaticTechLevel(SimpleTechLevel.INTRO);
        }
    }

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return getTechAdvancement(getEntityType(), isPrimitive(), isIndustrial(), getWeightClass());
    }

    private static final TechAdvancement[] GYRO_TA =  {
            new TechAdvancement(TECH_BASE_ALL).setAdvancement(2300, 2350, 2505)
                .setApproximate(true, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.INTRO), //Standard
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(3055, 3067, 3072)
                .setISApproximate(true, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //XL
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(3055, 3068, 3072)
                .setISApproximate(true, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //Compact
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(3055, 3067, 3072)
                .setISApproximate(true, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //Heavy duty
            new TechAdvancement(TECH_BASE_IS).setAdvancement(DATE_NONE)
                .setTechRating(RATING_A)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //None (placeholder)
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(2905, 2940)
                .setISApproximate(true, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_F, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Superheavy
    };

    private static final TechAdvancement[] COCKPIT_TA = {
            new TechAdvancement(TECH_BASE_ALL).setAdvancement(2468, 2470, 2487)
                .setApproximate(true, false, false).setTechRating(RATING_D)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.INTRO), //Standard
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(3060, 3067, 3080)
                .setISApproximate(true, false, false)
                .setClanAdvancement(DATE_NONE, 3080, 3080).setTechRating(RATING_E)
                .setPrototypeFactions(F_FS).setProductionFactions(F_FS, F_CJF)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //Small
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(2625, 2631, DATE_NONE, 2850, 3030)
                .setISApproximate(true, false, false, true, true)
                .setClanAdvancement(2625, 2631).setClanApproximate(true, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setReintroductionFactions(F_FS).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Cockpit command console
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(3053, 3080, 3100)
                .setClanAdvancement(3055, 3080, 3100)
                .setPrototypeFactions(F_FS, F_LC, F_CSJ).setProductionFactions(F_LC)
                .setApproximate(false, true, false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL), //Torso mounted
            //FIXME: Dual is unofficial; these are stats for standard
            new TechAdvancement(TECH_BASE_ALL).setAdvancement(2468, 2470, 2487)
                .setApproximate(true, false, false).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL), //Dual
            new TechAdvancement(TECH_BASE_ALL).setAdvancement(2469, 2470, 2490)
                .setApproximate(true, false, false).setTechRating(RATING_C)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setAvailability(RATING_B, RATING_C, RATING_C, RATING_B)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //Industrial
            new TechAdvancement(TECH_BASE_ALL).setAdvancement(2430, 2439)
                .setApproximate(true, false).setTechRating(RATING_D)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setAvailability(RATING_D, RATING_X, RATING_X, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Primitive
            new TechAdvancement(TECH_BASE_ALL).setAdvancement(2300, 2350, DATE_NONE, 2520)
                .setApproximate(true, false, false).setTechRating(RATING_C)
                .setPrototypeFactions(F_TA).setProductionFactions(F_TH)
                .setAvailability(RATING_C, RATING_X, RATING_X, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Primitive industrial
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(3060, 3076)
                .setISApproximate(true, false).setTechRating(RATING_E)
                .setPrototypeFactions(F_WB).setProductionFactions(F_WB)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Superheavy
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(3130, 3135)
                .setISApproximate(true, false).setTechRating(RATING_E)
                .setPrototypeFactions(F_RS).setProductionFactions(F_RS)
                .setAvailability(RATING_X, RATING_F, RATING_X, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Superheavy tripod
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(2590, 2702)
                .setISApproximate(true, false).setTechRating(RATING_F)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Tripod
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(3074).setClanAdvancement(3083)
                .setApproximate(true).setTechRating(RATING_E)
                .setPrototypeFactions(F_WB, F_CHH)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL), //Cockpit interface
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(3052, DATE_NONE, DATE_NONE, 3055)
                .setPrototypeFactions(F_FS, F_LC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL), //VRRP
            new TechAdvancement(TECH_BASE_CLAN).setClanAdvancement(3130, 3135)
                .setClanApproximate(true, false).setTechRating(RATING_F)
                .setPrototypeFactions(F_CHH).setProductionFactions(F_CHH)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //QuadVee
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(2905, 2940)
                .setISApproximate(true, false).setTechRating(RATING_D)
                .setPrototypeFactions(F_FW).setProductionFactions(F_FW)
                .setAvailability(RATING_X, RATING_F, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Superheavy industrial
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(3060, 3076)
                .setISApproximate(true, false).setTechRating(RATING_E)
                .setPrototypeFactions(F_WB).setProductionFactions(F_WB)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Superheavy command console
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(3060, 3067, 3080)
                .setISApproximate(true, false, false)
                .setClanAdvancement(DATE_NONE, 3080, 3080).setTechRating(RATING_E)
                .setPrototypeFactions(F_FS).setProductionFactions(F_FS, F_CJF)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Small Command Console
    };

    // Advanced fire control for industrial mechs is implemented with a standard cockpit,
    // but the tech progression is different.
    public static TechAdvancement getIndustrialAdvFireConTA() {
        return new TechAdvancement(TECH_BASE_ALL).setAdvancement(2469, 2470, 2491)
                .setApproximate(true, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH).setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_E, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    public static TechAdvancement getCockpitTechAdvancement(int cockpitType) {
        if (cockpitType >= 0 && cockpitType < COCKPIT_TA.length) {
            return new TechAdvancement(COCKPIT_TA[cockpitType]);
        }
        return null;
    }

    public TechAdvancement getCockpitTechAdvancement() {
        if (isIndustrial() && (getCockpitType() == COCKPIT_STANDARD)) {
            return getIndustrialAdvFireConTA();
        }
        return getCockpitTechAdvancement(getCockpitType());
    }

    public static TechAdvancement getGyroTechAdvancement(int gyroType) {
        if ((gyroType >= 0) && (gyroType < GYRO_TA.length)) {
            return new TechAdvancement(GYRO_TA[gyroType]);
        }
        return null;
    }

    public TechAdvancement getGyroTechAdvancement() {
        return getGyroTechAdvancement(getGyroType());
    }

    public static TechAdvancement getFullHeadEjectAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL)
        		.setISAdvancement(DATE_NONE, 3020, 3023,DATE_NONE, DATE_NONE)
        		.setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3052, DATE_NONE, DATE_NONE).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC, F_CWF).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        // battlemechs with non-fusion engines are experimental
        if (hasEngine() && !isIndustrial() && !getEngine().isFusion()) {
            ctl.addComponent(new TechAdvancement().setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL));
        }
        if (getGyroTechAdvancement() != null) {
            ctl.addComponent(getGyroTechAdvancement());
        }
        if (getCockpitTechAdvancement() != null) {
            ctl.addComponent(getCockpitTechAdvancement());
        }
        if (hasFullHeadEject()) {
            ctl.addComponent(getFullHeadEjectAdvancement());
        }
        //FIXME: Clan interface cockpit has higher tech rating
        //if (getCockpitType() == COCKPIT_INTERFACE && isClan()) {
        //    techAdvancement.setTechRating(Math.max(techAdvancement.getTechRating(), RATING_F));
        //}
    }

    /**
     * This method will return the number of contiguous criticals in the given
     * location, starting at the given critical slot
     *
     * @param loc           The location on the unit to check slots on
     * @param startingSlot  The critical slot to start at
     * @return
     */
    private int getContiguousNumberOfCrits(int loc, int startingSlot) {

        int numCritSlots = getNumberOfCriticals(loc);
        int contiguousCrits = 0;

        for (int slot = startingSlot; slot < numCritSlots; slot++) {
            if (getCritical(loc, slot) == null) {
                contiguousCrits++;
            } else {
               break;
            }
        }
        return contiguousCrits;
    }

    /**
     * Used in BV calculations. Any equipment that will destroy the unit or leg it if it explodes
     * decreases the defensive battle rating. This is anything in the head, CT, or leg,
     * or side torso if it has >= 3 engine crits, or any location that can transfer damage to that
     * location.
     *
     * @param loc The location index
     * @return    Whether explosive equipment in the location should decrease BV
     */
    private boolean hasExplosiveEquipmentPenalty(int loc) {
        if ((loc == Entity.LOC_NONE) || hasCASEII(loc)) {
            return false;
        }
        if (!entityIsQuad() && ((loc == Mech.LOC_RARM) || (loc == Mech.LOC_LARM))) {
            return !locationHasCase(loc) && hasExplosiveEquipmentPenalty(getTransferLocation(loc));
        } else if ((loc == Mech.LOC_RT) || (loc == Mech.LOC_LT)) {
            return !locationHasCase(loc) || (getEngine().getSideTorsoCriticalSlots().length >= 3);
        } else {
            return true;
        }
    }

    /**
     * Calculates the battle value of this mech
     */
    @Override
    public int calculateBattleValue() {
        if (useManualBV) {
            return manualBV;
        }
        return calculateBattleValue(false, false);
    }

    /**
     * Calculates the battle value of this mech. If the parameter is true, then
     * the battle value for c3 won't be added whether the mech is currently part
     * of a network or not.
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        if (useManualBV) {
            return manualBV;
        }

        bvText = new StringBuffer(
                "<HTML><BODY><CENTER><b>Battle Value Calculations For ");

        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

        double dbv = 0; // defensive battle value
        double obv; // offensive bv

        double armorMultiplier = 1.0;
        bvText.append(startTable);
        for (int loc = 0; loc < locations(); loc++) {
            // total armor points

            switch (getArmorType(loc)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    armorMultiplier = 0.5;
                    break;
                case EquipmentType.T_ARMOR_HARDENED:
                    armorMultiplier = 2.0;
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                case EquipmentType.T_ARMOR_REFLECTIVE:
                case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                    armorMultiplier = 1.5;
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    armorMultiplier = 1.2;
                    break;
                case EquipmentType.T_ARMOR_HEAT_DISSIPATING:
                    armorMultiplier = 1.1;
                    break;
                default:
                    armorMultiplier = 1.0;
                    break;
            }

            if (hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
                armorMultiplier += 0.2;
            }
            if (countWorkingMisc(MiscType.F_HARJEL_II, loc) > 0) {
                armorMultiplier *= 1.1;
            }
            if (countWorkingMisc(MiscType.F_HARJEL_III, loc) > 0) {
                armorMultiplier *= 1.2;
            }

            // BV for torso mounted cockpit.
            if ((getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)
                    && (loc == LOC_CT)) {
                bvText.append(startRow);
                bvText.append(startColumn);
                double cockpitArmor = this.getArmor(Mech.LOC_CT)
                        + this.getArmor(Mech.LOC_CT, true);
                cockpitArmor *= armorMultiplier;
                bvText.append("extra BV for torso mounted cockpit");
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(cockpitArmor);
                bvText.append(endColumn);
                bvText.append(endRow);
                dbv += cockpitArmor;
            }
            int modularArmor = 0;
            for (Mounted mounted : getMisc()) {
                if (mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                        && (mounted.getLocation() == loc)) {
                    modularArmor += mounted.getBaseDamageCapacity()
                            - mounted.getDamageTaken();
                }
            }
            int armor = getArmor(loc)
                    + (hasRearArmor(loc) ? getArmor(loc, true) : 0);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total Armor "
                    + this.getLocationAbbr(loc)
                    + " ("
                    + armor
                    + (modularArmor > 0 ? " +" + modularArmor + " modular" : "")
                    + ") x ");
            bvText.append(armorMultiplier);
            bvText.append(endColumn);
            bvText.append(startColumn);
            double armorBV = (armor + modularArmor) * armorMultiplier;
            dbv += armorBV;
            bvText.append(armorBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total modified armor BV x 2.5 ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        dbv *= 2.5;
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        // total internal structure
        double internalMultiplier = 1.0;
        if ((getStructureType() == EquipmentType.T_STRUCTURE_INDUSTRIAL)
                || (getStructureType() == EquipmentType.T_STRUCTURE_COMPOSITE)) {
            internalMultiplier = 0.5;
        } else if (getStructureType() == EquipmentType.T_STRUCTURE_REINFORCED) {
            internalMultiplier = 2.0;
        }
        if (hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
            internalMultiplier += 0.2;
        }

        dbv += getTotalInternal() * internalMultiplier * 1.5
                * (hasEngine() ? getEngine().getBVMultiplier() : 1.0);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total I.S. Points x IS Multipler x 1.5 x Engine Multipler");

        bvText.append(endColumn + startColumn);
        bvText.append(getTotalInternal());
        bvText.append(" x ");
        bvText.append(internalMultiplier);
        bvText.append(" x ");
        bvText.append("1.5 x ");
        bvText.append(hasEngine() ? getEngine().getBVMultiplier() : 1.0);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(getTotalInternal() * internalMultiplier * 1.5
                * (hasEngine() ? getEngine().getBVMultiplier() : 1.0));
        bvText.append(endColumn);
        bvText.append(endRow);

        // add gyro
        dbv += getWeight() * getGyroMultiplier();

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Weight x Gyro Multipler ");
        bvText.append(endColumn + startColumn);
        bvText.append(getWeight());
        bvText.append(" x ");
        bvText.append(getGyroMultiplier());
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(getWeight() * getGyroMultiplier());
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Defensive Equipment:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(endColumn);
        bvText.append(endRow);
        double amsAmmoBV = 0;
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();
            if ((atype.getAmmoType() == AmmoType.T_AMS) || (atype.getAmmoType() == AmmoType.T_APDS)) {
                amsAmmoBV += atype.getBV(this);
            }
        }
        double amsBV = 0;
        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype
                    .hasFlag(WeaponType.F_AMS)
                    || etype.hasFlag(WeaponType.F_M_POD) || etype
                        .hasFlag(WeaponType.F_B_POD)))
                    || ((etype instanceof MiscType) && (etype
                            .hasFlag(MiscType.F_ECM)
                            || etype.hasFlag(MiscType.F_BAP)
                            || etype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)
                            || etype.hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)
                            || etype.hasFlag(MiscType.F_AP_POD)
                            || etype.hasFlag(MiscType.F_MASS)
                            || etype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                            || etype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                            || etype.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                            || etype.hasFlag(MiscType.F_CHAFF_POD)
                            || etype.hasFlag(MiscType.F_HARJEL_II)
                            || etype.hasFlag(MiscType.F_HARJEL_III)
                            || etype.hasFlag(MiscType.F_SPIKES) || (etype
                            .hasFlag(MiscType.F_CLUB) && (etype
                            .hasSubType(MiscType.S_SHIELD_LARGE)
                            || etype.hasSubType(MiscType.S_SHIELD_MEDIUM) || etype
                                .hasSubType(MiscType.S_SHIELD_SMALL)))))) {
                double bv = etype.getBV(this);
                if (etype instanceof WeaponType) {
                    WeaponType wtype = (WeaponType) etype;
                    if (wtype.hasFlag(WeaponType.F_AMS)
                            && ((wtype.getAmmoType() == AmmoType.T_AMS) || (wtype.getAmmoType() == AmmoType.T_APDS))) {
                        amsBV += bv;
                    }
                }
                dEquipmentBV += bv;
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(mounted.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);

                bvText.append("+");
                bvText.append(bv);
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        if (amsAmmoBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("AMS Ammo (to a maximum of AMS BV)");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+");
            bvText.append(Math.min(amsBV, amsAmmoBV));
            bvText.append(endColumn);
            bvText.append(endRow);
            dEquipmentBV += Math.min(amsBV, amsAmmoBV);
        }

        dbv += dEquipmentBV;

        bvText.append(startRow);
        bvText.append(startColumn);

        double armoredBVCal = getArmoredComponentBV();

        if (armoredBVCal > 0) {
            bvText.append("Armored Components BV Modification");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append("+");
            bvText.append(armoredBVCal);
            bvText.append(endColumn);
            bvText.append(endRow);
            dbv += armoredBVCal;
        }

        bvText.append("Total BV of all Defensive Equipment ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(dEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        // subtract for explosive ammo
        double ammoPenalty = 0;
        for (Mounted mounted : getEquipment()) {
            int loc = mounted.getLocation();
            int toSubtract = 15;
            EquipmentType etype = mounted.getType();

            // only count explosive ammo
            if (!etype.isExplosive(mounted, true)) {
                continue;
            }

            // don't count oneshot ammo
            if (loc == LOC_NONE) {
                continue;
            }

            if (!hasExplosiveEquipmentPenalty(loc) && !hasExplosiveEquipmentPenalty(mounted.getSecondLocation())) {
                continue;
            }

            // gauss rifles only subtract 1 point per slot, same for HVACs and
            // iHeavy Lasers and mektasers
            if ((etype instanceof GaussWeapon) || (etype instanceof HVACWeapon)
                    || (etype instanceof CLImprovedHeavyLaserLarge)
                    || (etype instanceof CLImprovedHeavyLaserMedium)
                    || (etype instanceof CLImprovedHeavyLaserSmall)
                    || (etype instanceof ISRISCHyperLaser)
                    || (etype instanceof TSEMPWeapon)
                    || (etype instanceof ISMekTaser)
                    || (etype.hasFlag(WeaponType.F_B_POD)
                    || (etype.hasFlag(WeaponType.F_M_POD)))) {
                toSubtract = 1;
            }

         // PPCs with capacitors subtract 1
            if (etype instanceof PPCWeapon) {
                if (mounted.getLinkedBy() != null) {
                    toSubtract = 1;
                } else {
                    continue;
                }
            }
            if ((etype instanceof MiscType)
                    && (etype.hasFlag(MiscType.F_PPC_CAPACITOR)
                            || etype.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)
                            || etype.hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)
                            || etype.hasFlag(MiscType.F_JUMP_JET))) {
                toSubtract = 1;
            }

            if (etype instanceof AmmoType
                    && ((AmmoType) mounted.getType()).getAmmoType() == AmmoType.T_COOLANT_POD) {
                toSubtract = 1;
            }


            if ((etype instanceof MiscType)
                    && etype.hasFlag(MiscType.F_BLUE_SHIELD)) {
                // blue shield needs to be special cased, because it's one
                // mounted with lots of locations,
                // and some of those could be protected by cas
                toSubtract = 0;
            }

            // RACs, LACs and ACs don't really count
            if ((etype instanceof WeaponType) && ((((WeaponType) etype).getAmmoType() == AmmoType.T_AC_ROTARY)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_LAC)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC_IMP)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC_PRIMITIVE)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_PAC))) {
                toSubtract = 0;
            }

            // empty ammo shouldn't count
            if ((etype instanceof AmmoType)
                    && (mounted.getUsableShotsLeft() == 0)) {
                continue;
            }

            // we subtract per critical slot
            int criticals;
            if (mounted.isSplit()) {
                criticals = 0;
                for (int l = 0; l < locations(); l++) {
                    if (((l == mounted.getLocation()) || (l == mounted.getSecondLocation()))
                            && hasExplosiveEquipmentPenalty(l)) {
                        for (int i = 0; i < getNumberOfCriticals(l); i++) {
                            CriticalSlot slot = getCritical(l, i);
                            if ((slot != null) && mounted.equals(slot.getMount())) {
                                criticals++;
                            }
                        }
                    }
                }
            } else if (mounted.getType() instanceof HVACWeapon) {
                // HVAC are only -1 total, regardless of number of crits. None are large enough to be splittable.
                criticals = 1;
            } else {
                criticals = mounted.getCriticals();
            }
            toSubtract *= criticals;
            ammoPenalty += toSubtract;
        }
        // special case for blueshield, need to check each non-head location
        // seperately for CASE
        if (hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
            int unProtectedCrits = 0;
            for (int loc = LOC_CT; loc <= LOC_LLEG; loc++) {
                if (hasCASEII(loc)) {
                    continue;
                }
                if (isClan()) {
                    // Clan mechs only count ammo in ct, legs or head (per
                    // BMRr).
                    // Also count ammo in side torsos if mech has xxl engine
                    // (extrapolated from rule intent - not covered in rules)
                    if (((loc != LOC_CT) && (loc != LOC_RLEG)
                            && (loc != LOC_LLEG) && (loc != LOC_HEAD))
                            && !(((loc == LOC_RT) || (loc == LOC_LT)) && hasEngine() && (getEngine()
                                    .getSideTorsoCriticalSlots().length > 2))) {
                        continue;
                    }
                } else {
                    // inner sphere with XL or XXL counts everywhere
                    if (hasEngine() && (getEngine().getSideTorsoCriticalSlots().length <= 2)) {
                        // without XL or XXL, only count torsos if not CASEed,
                        // and arms if arm & torso not CASEed
                        if (((loc == LOC_RT) || (loc == LOC_LT))
                                && locationHasCase(loc)) {
                            continue;
                        } else if ((loc == LOC_LARM)
                                && (locationHasCase(loc) || locationHasCase(LOC_LT))) {
                            continue;
                        } else if ((loc == LOC_RARM)
                                && (locationHasCase(loc) || locationHasCase(LOC_RT))) {
                            continue;
                        }
                    }
                }
                unProtectedCrits++;
            }
            ammoPenalty += unProtectedCrits;
        }
        dbv = Math.max(1, dbv - ammoPenalty);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Explosive Weapons/Equipment Penalty ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= -");
        bvText.append(ammoPenalty);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        // adjust for target movement modifier
        // we use full possible movement, ignoring gravity, heat and modular
        // armor, but taking into account hit actuators
        int bvWalk = getWalkMP(false, true, true);
        int airmechMP = 0;
        if (((getEntityType() & ETYPE_LAND_AIR_MECH) != 0)) {
            bvWalk = ((LandAirMech) this).getBVWalkMP();
            if (((LandAirMech) this).getLAMType() == LandAirMech.LAM_STANDARD) {
                airmechMP = ((LandAirMech) this).getAirMechFlankMP();
            }
        } else if (((getEntityType() & ETYPE_QUADVEE) != 0)
                && (getMovementMode() == EntityMovementMode.WHEELED)) {
            // Don't use bonus cruise MP in calculating BV
            bvWalk = Math.max(0, walkMP);
        }
        int runMP;
        if (hasTSM(false)) {
            bvWalk++;
        }
        if (hasMASCAndSuperCharger()) {
            runMP = (int) Math.ceil(bvWalk * 2.5);
        } else if (hasMASC()) {
            runMP = bvWalk * 2;
        } else {
            runMP = (int) Math.ceil(bvWalk * 1.5);
        }
        if (hasMPReducingHardenedArmor()) {
            runMP--;
        }
        int tmmRan = Compute.getTargetMovementModifier(runMP, false, false,
                game).getValue();
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Run MP");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(runMP);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Target Movement Modifier For Run");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(tmmRan);
        bvText.append(endColumn);
        bvText.append(endRow);

        // Calculate modifiers for jump and UMU movement where applicable.
        final int jumpMP = Math.max(getJumpMP(false, true), airmechMP);
        final int tmmJumped = (jumpMP > 0) ? Compute.
                getTargetMovementModifier(jumpMP, true, false, game).getValue()
                : 0;

        final int umuMP = getActiveUMUCount();
        final int tmmUMU = (umuMP > 0) ? Compute.
                getTargetMovementModifier(umuMP, false, false, game).getValue()
                : 0;

        bvText.append(startRow);
        bvText.append(startColumn);

        if (airmechMP == 0) {
            bvText.append("Target Movement Modifier For Jumping");
        } else {
            bvText.append("Target Movement Modifier For AirMech Flank");
        }
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(tmmJumped);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append("Target Movement Modifier For UMUs");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(tmmUMU);
        bvText.append(endColumn);
        bvText.append(endRow);

        double targetMovementModifier = Math.max(tmmRan, Math.max(tmmJumped,
                tmmUMU));

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Target Movement Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(targetMovementModifier);
        bvText.append(endColumn);
        bvText.append(endRow);

        // Try to find a Mek Stealth or similar system.
        if (hasStealth() || hasNullSig()) {
            targetMovementModifier += 2;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Stealth +2");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append("+2");
            bvText.append(endColumn);
            bvText.append(endRow);

        }
        if (hasChameleonShield()) {
            targetMovementModifier += 2;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Chameleon +2");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append("+2");
            bvText.append(endColumn);
            bvText.append(endRow);

        }
        if (hasVoidSig()) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Void Sig");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(endColumn);
            bvText.append(startColumn);

            if (targetMovementModifier < 3) {
                targetMovementModifier = 3;
                bvText.append("3");
            } else if (targetMovementModifier == 3) {
                targetMovementModifier++;
                bvText.append("+1");
            } else {
                bvText.append("-");
            }

            bvText.append(endColumn);
            bvText.append(endRow);
        }
        double tmmFactor = 1 + (targetMovementModifier / 10);
        dbv *= tmmFactor;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Multiply by Defensive Movement Factor of ");
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(tmmFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" x ");
        bvText.append(tmmFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Defensive Battle Value");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("<b>Offensive Battle Rating Calculation:</b>");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);
        // calculate heat efficiency
        int mechHeatEfficiency = 6 + getHeatCapacity();
        if ((this instanceof LandAirMech) && (((LandAirMech) this).getLAMType() == LandAirMech.LAM_STANDARD)) {
            mechHeatEfficiency += 3;
        }

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Base Heat Efficiency ");

        double coolantPods = 0;
        for (Mounted ammo : getAmmo()) {
            if (((AmmoType) ammo.getType()).getAmmoType() == AmmoType.T_COOLANT_POD) {
                coolantPods++;
            }
        }

        // account for coolant pods
        if (coolantPods > 0) {
            mechHeatEfficiency += (int) Math.ceil((getNumberOfSinks() * coolantPods) / 5d);
            bvText.append(" + Coolant Pods ");
        }
        if (hasWorkingMisc(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
            mechHeatEfficiency += 4;
            bvText.append(" + RISC Emergency Coolant System");
        }

        int moveHeat;
        if ((this instanceof LandAirMech) && (((LandAirMech) this).getLAMType()  == LandAirMech.LAM_STANDARD)) {
            moveHeat = (int) Math.round(((LandAirMech) this).getAirMechFlankMP(false, true) / 3d);
        } else if ((getJumpMP(false, true) > 0)
                && (getJumpHeat(getJumpMP(false, true)) > getRunHeat())) {
            moveHeat = getJumpHeat(getJumpMP(false, true));
            bvText.append(" - Jump Heat ");
        } else {
            moveHeat = getRunHeat();
            if (hasSCM()) {
                moveHeat = 0;
            }
            bvText.append(" - Run Heat ");
        }
        mechHeatEfficiency -= moveHeat;
        if (hasStealth()) {
            mechHeatEfficiency -= 10;
            bvText.append(" - Stealth Heat ");
        }
        if (hasChameleonShield()) {
            mechHeatEfficiency -= 6;
            bvText.append(" - Chameleon LPS Heat ");
        }
        if (hasNullSig()) {
            mechHeatEfficiency -= 10;
            bvText.append(" - Null-signature system Heat ");
        }
        if (hasVoidSig()) {
            mechHeatEfficiency -= 10;
            bvText.append(" - Void-signature system Heat ");
        }

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(6 + getHeatCapacity());

        if (coolantPods > 0) {
            bvText.append(" + ");
            bvText.append(Math.ceil((getNumberOfSinks() * coolantPods) / 5));
        }

        if (hasWorkingMisc(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
            mechHeatEfficiency += 4;
            bvText.append(" + 4");
        }

        bvText.append(" - ").append(moveHeat);
        if (hasStealth()) {
            bvText.append(" - 10");
        }

        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(mechHeatEfficiency);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Unmodified Weapon BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        double weaponBV = 0;
        boolean hasTargComp = hasTargComp();
        // first, add up front-faced and rear-faced unmodified BV,
        // to know wether front- or rear faced BV should be halved
        double bvFront = 0, bvRear = 0, nonArmFront = 0, nonArmRear = 0, bvTurret = 0;
        ArrayList<Mounted> weapons = getWeaponList();
        for (Mounted weapon : weapons) {
            WeaponType wtype = (WeaponType) weapon.getType();
            if (wtype.hasFlag(WeaponType.F_B_POD)
                    || wtype.hasFlag(WeaponType.F_M_POD)) {
                continue;
            }
            double dBV = wtype.getBV(this);

            // don't count destroyed equipment
            if (weapon.isDestroyed()) {
                continue;
            }
            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgBV = 0;
                for (int eqNum : weapon.getBayWeapons()) {
                    Mounted mg = getEquipment(eqNum);
                    if ((mg != null) && (!mg.isDestroyed())) {
                        mgBV += mg.getType().getBV(this);
                    }
                }
                dBV = mgBV * 0.67;
            }
            String name = wtype.getName();
            // artemis bumps up the value
            // PPC caps do, too
            if (weapon.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) weapon.getLinkedBy().getType()).getBV(
                            this, weapon);
                    name = name.concat(" with Capacitor");
                }
                Mounted mLinker = weapon.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    name = name.concat(" with Artemis IV");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                    dBV *= 1.2;
                    name = name.concat(" with Artemis IV Prototype");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                    name = name.concat(" with Artemis V");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    name = name.concat(" with Apollo");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.15;
                    name = name.concat(" with RISC Laser Pulse Module");
                }
            }

            if (hasFunctionalArmAES(weapon.getLocation())) {
                dBV *= 1.25;
                name = name.concat(" augmented by AES");
            }

            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append(name);
            boolean rearVGL = false;
            if (weapon.getType().hasFlag(WeaponType.F_VGL)) {
                // vehicular grenade launchers facing to the rear sides count
                // for rear BV, too
                if ((weapon.getFacing() == 2) || (weapon.getFacing() == 4)) {
                    rearVGL = true;
                }
            }
            if (weapon.isMechTurretMounted()) {
                bvTurret += dBV;
                bvText.append(" (T)");
            } else if (weapon.isRearMounted() || rearVGL) {
                bvRear += dBV;
                bvText.append(" (R)");
            } else {
                bvFront += dBV;
            }
            if (!isArm(weapon.getLocation()) && !weapon.isMechTurretMounted()) {
                if (weapon.isRearMounted() || rearVGL) {
                    nonArmRear += dBV;
                } else {
                    nonArmFront += dBV;
                }
            }

            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(dBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Unmodified Front BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(bvFront);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Unmodfied Rear BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(bvRear);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Unmodfied Turret BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(bvTurret);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Unmodfied BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(bvRear + bvFront + bvTurret);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append("Unmodified Front non-arm BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(nonArmFront);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Unmodfied Rear non-arm BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(nonArmRear);
        bvText.append(endColumn);
        bvText.append(endRow);

        boolean halveRear = true;
        boolean turretFront = true;
        if (nonArmFront <= nonArmRear) {
            halveRear = false;
            turretFront = false;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("halving front instead of rear weapon BVs");
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("turret mounted weapon BVs count as rear firing");
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapon Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        // here we store the modified BV and heat of all heat-using weapons,
        // to later be sorted by BV
        ArrayList<ArrayList<Object>> heatBVs = new ArrayList<>();
        // BVs of non-heat-using weapons
        ArrayList<ArrayList<Object>> nonHeatBVs = new ArrayList<>();
        // total up maximum heat generated
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<>();
        double maximumHeat = 0;
        for (Mounted mounted : getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if (wtype.hasFlag(WeaponType.F_B_POD)
                    || wtype.hasFlag(WeaponType.F_M_POD)) {
                continue;
            }
            double weaponHeat = wtype.getHeat();

            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed()
                    || mounted.isBreached()) {
                continue;
            }

            // one shot weapons count 1/4
            if ((wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER)
                    || wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weaponHeat *= 0.25;
            }

            // double heat for ultras
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weaponHeat *= 2;
            }

            // Six times heat for RAC
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weaponHeat *= 6;
            }

            // 1d6 extra heat; add half for heat calculations (1d3/+2 for small pulse)
            if ((wtype instanceof ISERLaserLargePrototype)
                    || (wtype instanceof ISPulseLaserLargePrototype)
                    || (wtype instanceof ISPulseLaserMediumPrototype)
                    || (wtype instanceof ISPulseLaserMediumRecovered)) {
                weaponHeat += 3;
            } else if (wtype instanceof ISPulseLaserSmallPrototype) {
                weaponHeat += 2;
            }

            String name = wtype.getName();


            // RISC laser pulse module adds 2 heat
            if ((wtype.hasFlag(WeaponType.F_LASER)) && (mounted.getLinkedBy() != null)
                    && (mounted.getLinkedBy().getType() instanceof MiscType)
                    && (mounted.getLinkedBy().getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE))) {
                name = name.concat(" with RISC Laser Pulse Module");
                weaponHeat += 2;
            }

            // laser insulator reduce heat by 1, to a minimum of 1
            if (wtype.hasFlag(WeaponType.F_LASER)
                    && (mounted.getLinkedBy() != null)
                    && !mounted.getLinkedBy().isInoperable()
                    && (mounted.getLinkedBy().getType() instanceof MiscType)
                    && mounted.getLinkedBy().getType()
                            .hasFlag(MiscType.F_LASER_INSULATOR)) {
                weaponHeat -= 1;
                if (weaponHeat == 0) {
                    weaponHeat++;
                }
            }

            // half heat for streaks
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_MRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_IATM)) {
                weaponHeat *= 0.5;
            }
            // check to see if the weapon is a PPC and has a Capacitor attached
            // to it
            if (wtype.hasFlag(WeaponType.F_PPC)
                    && (mounted.getLinkedBy() != null)) {
                name = name.concat(" with Capacitor");
                weaponHeat += 5;
            }

            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append(name);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+ ");
            bvText.append(weaponHeat);
            bvText.append(endColumn);
            bvText.append(endRow);

            double dBV = wtype.getBV(this);
            if (hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                dBV *= 0.8;
            }
            String weaponName = mounted.getName()
                    + (mounted.isRearMounted() ? "(R)" : "");

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgBV = 0;
                for (int eqNum : mounted.getBayWeapons()) {
                    Mounted mg = getEquipment(eqNum);
                    if ((mg != null) && (!mg.isDestroyed())) {
                        mgBV += mg.getType().getBV(this);
                    }
                }
                dBV = mgBV * 0.67;
            }

            // artemis bumps up the value
            // PPC caps do, too
            if (mounted.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) mounted.getLinkedBy().getType()).getBV(
                            this, mounted);
                    weaponName = weaponName.concat(" with Capacitor");
                }
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    weaponName = weaponName.concat(" with Artemis IV");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                    weaponName = weaponName.concat(" with Artemis V");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    weaponName = weaponName.concat(" with Apollo");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.15;
                    weaponName = weaponName.concat(" with RISC Laser Pulse Module");
                }
            }
            // if linked to AES, multiply by 1.25
            if (hasFunctionalArmAES(mounted.getLocation())) {
                dBV *= 1.25;
            }
            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
            }
            // half for being rear mounted (or front mounted, when more rear-
            // than front-mounted un-modded BV
            // or for being turret mounted, when more rear-mounted BV than front
            // mounted BV
            if ((!isArm(mounted.getLocation())
                    && !mounted.isMechTurretMounted() && ((mounted
                    .isRearMounted() && halveRear) || (!mounted.isRearMounted() && !halveRear)))
                    || (mounted.isMechTurretMounted() && ((!turretFront && halveRear) || (turretFront && !halveRear)))) {
                dBV /= 2;
            }

            // ArrayList that stores weapon values
            // stores a double first (BV), then an Integer (heat),
            // then a String (weapon name)
            // for 0 heat weapons, just stores BV and name
            ArrayList<Object> weaponValues = new ArrayList<>();
            if (weaponHeat > 0) {
                // store heat and BV, for sorting a few lines down;
                weaponValues.add(dBV);
                weaponValues.add(weaponHeat);
                weaponValues.add(weaponName);
                heatBVs.add(weaponValues);
            } else {
                weaponValues.add(dBV);
                weaponValues.add(weaponName);
                nonHeatBVs.add(weaponValues);
            }

            maximumHeat += weaponHeat;
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !((wtype.getAmmoType() == AmmoType.T_PLASMA)
                    || (wtype.getAmmoType() == AmmoType.T_VEHICLE_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_HEAVY_FLAMER) || (wtype
                    .getAmmoType() == AmmoType.T_CHEMICAL_LASER)))
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype
                        .getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this)
                            + weaponsForExcessiveAmmo.get(key));
                }
            }
        }

        if (hasVibroblades()) {

            for (int location = Mech.LOC_RARM; location <= Mech.LOC_LARM; location++) {
                for (int slot = 0; slot < locations(); slot++) {
                    CriticalSlot cs = getCritical(location, slot);

                    if ((cs != null)
                            && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                        Mounted mount = cs.getMount();
                        if ((mount.getType() instanceof MiscType)
                                && mount.getType().hasFlag(MiscType.F_CLUB)
                                && ((MiscType) mount.getType()).isVibroblade()) {
                            ArrayList<Object> weaponValues = new ArrayList<>();
                            double dBV = mount.getType().getBV(this);
                            if (hasFunctionalArmAES(mount.getLocation())) {
                                dBV *= 1.25;
                            }
                            weaponValues.add(dBV);
                            weaponValues.add((double) getActiveVibrobladeHeat(location, true));
                            weaponValues.add(mount.getName());
                            heatBVs.add(weaponValues);

                            bvText.append(startRow);
                            bvText.append(startColumn);

                            bvText.append(mount.getName());
                            bvText.append(endColumn);
                            bvText.append(startColumn);
                            bvText.append(endColumn);
                            bvText.append(startColumn);
                            bvText.append("+ ");
                            bvText.append(getActiveVibrobladeHeat(location, true));
                            bvText.append(endColumn);
                            bvText.append(endRow);
                            maximumHeat += getActiveVibrobladeHeat(location, true);
                            break;
                        }
                    }
                }
            }
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(maximumHeat);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapons with no heat at full BV:");
        bvText.append(endColumn);
        bvText.append(endRow);
        // count heat-free weapons always at full modified BV
        for (ArrayList<Object> nonHeatWeapon : nonHeatBVs) {
            weaponBV += (Double) nonHeatWeapon.get(0);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(nonHeatWeapon.get(1));
            if (nonHeatWeapon.get(1).toString().length() < 8) {
                bvText.append("\t");
            }
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(nonHeatWeapon.get(0));
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Heat Modified Weapons BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        if (maximumHeat > mechHeatEfficiency) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("(Heat Exceeds Mech Heat Efficiency) ");

            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        if (maximumHeat <= mechHeatEfficiency) {
            // count all weapons equal
            for (ArrayList<Object> weaponValues : heatBVs) {
                // name
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(weaponValues.get(2));
                weaponBV += (Double) weaponValues.get(0);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);

                bvText.append(weaponValues.get(0));
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        } else {
            // this will count heat-generating weapons at full modified BV until
            // heat efficiency is reached or passed with one weapon

            // sort the heat-using weapons by modified BV
            heatBVs.sort((obj1, obj2) -> {
                Double obj1BV = (Double) obj1.get(0); // BV
                Double obj2BV = (Double) obj2.get(0); // BV

                // first element in the ArrayList is BV, second is heat
                // if same BV, lower heat first
                if (obj1BV.equals(obj2BV)) {
                    Double obj1Heat = (Double) obj1.get(1);
                    Double obj2Heat = (Double) obj2.get(1);

                    return Double.compare(obj1Heat, obj2Heat);
                }

                // higher BV first
                return Double.compare(obj2BV, obj1BV);
            });
            // count heat-generating weapons at full modified BV until
            // heat efficiency is reached or
            // passed with one weapon
            double heatAdded = 0;
            for (ArrayList<Object> weaponValues : heatBVs) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(weaponValues.get(2));
                bvText.append(endColumn);
                bvText.append(startColumn);

                double dBV = (Double) weaponValues.get(0);
                if (heatAdded >= mechHeatEfficiency) {
                    if (useReducedOverheatModifierBV()) {
                        dBV /= 10;
                    } else {
                        dBV /= 2;
                    }
                }
                if (heatAdded >= mechHeatEfficiency) {
                    if (useReducedOverheatModifierBV()) {
                        bvText.append("Heat efficiency reached, BV * 0.1");
                    } else {
                    bvText.append("Heat efficiency reached, half BV");
                    }
                }
                heatAdded += (Double) weaponValues.get(1);
                weaponBV += dBV;
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(dBV);
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append("Heat count: ").append(heatAdded);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Weapons BV Adjusted For Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Misc Offensive Equipment: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM -
        // BMR p152)
        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }
            // vibroblades have been counted under weapons
            if ((mounted.getType() instanceof MiscType)
                    && mounted.getType().hasFlag(MiscType.F_CLUB)
                    && ((MiscType) mounted.getType()).isVibroblade()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_ECM)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_AP_POD)
                    || mtype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)
                    || mtype.hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)
                    || mtype.hasFlag(MiscType.F_MASS)
                    || mtype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_CHAFF_POD)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)
                    || mtype.hasFlag(MiscType.F_SPIKES)
                    || mtype.hasFlag(MiscType.F_HARJEL_II)
                    || mtype.hasFlag(MiscType.F_HARJEL_III)
                    || (mtype.hasFlag(MiscType.F_CLUB) && (mtype
                            .hasSubType(MiscType.S_SHIELD_LARGE)
                            || mtype.hasSubType(MiscType.S_SHIELD_MEDIUM) || mtype
                                .hasSubType(MiscType.S_SHIELD_SMALL)))) {
                continue;
            }
            double bv = mtype.getBV(this);
            // if physical weapon linked to AES, multiply by 1.25
            if ((mtype.hasFlag(MiscType.F_CLUB) || mtype
                    .hasFlag(MiscType.F_HAND_WEAPON))
                    && hasFunctionalArmAES(mounted.getLocation())) {
                bv *= 1.25;
            }

            if (bv > 0) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(mounted.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(bv);
                bvText.append(endColumn);
                bvText.append(endRow);
            }

            oEquipmentBV += bv;
        }

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Misc Offensive Equipment BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(oEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        weaponBV += oEquipmentBV;

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on
        // our team
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<>();
        ArrayList<String> keys = new ArrayList<>();
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getUsableShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if ((atype.getAmmoType() == AmmoType.T_AMS) || (atype.getAmmoType() == AmmoType.T_APDS)) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED)
                    || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                Player tmpP = getOwner();

                if (tmpP != null) {
                    // Okay, actually check for friendly TAG.
                    if (tmpP.hasTAG()) {
                        tagBV += atype.getBV(this);
                    } else if ((tmpP.getTeam() != Player.TEAM_NONE) && (game != null)) {
                        for (Enumeration<Team> e = game.getTeams(); e.hasMoreElements();) {
                            Team m = e.nextElement();
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(game)) {
                                    tagBV += atype.getBV(this);
                                }
                                // A player can't be on two teams.
                                // If we check his team and don't give the
                                // penalty, that's it.
                                break;
                            }
                        }
                    }
                }
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize();
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, atype.getBV(this));
            } else {
                ammo.put(key, atype.getBV(this) + ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons
        // of that
        // type on the mech is reached.
        for (String key : keys) {

            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                } else {
                    ammoBV += ammo.get(key);
                }
            } else {
                // Ammo with no matching weapons counts 0, unless it's a coolant
                // pod
                // because coolant pods have no matching weapon
                if (key.equals(Integer.valueOf(AmmoType.T_COOLANT_POD).toString() + "1")) {
                    ammoBV += ammo.get(key);
                }
            }
        }
        weaponBV += ammoBV;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Ammo BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(ammoBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        double aesMultiplier = 1;
        if (hasFunctionalArmAES(Mech.LOC_LARM)) {
            aesMultiplier += 0.1;
        }
        if (hasFunctionalArmAES(Mech.LOC_RARM)) {
            aesMultiplier += 0.1;
        }
        if (hasFunctionalLegAES()) {
            if (this instanceof BipedMech) {
                aesMultiplier += 0.2;
            } else if (this instanceof QuadMech) {
                aesMultiplier += 0.4;
            }
        }

        double weight = this.weight * aesMultiplier;

        if (aesMultiplier > 1) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Weight x AES Multiplier ");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(this.weight);
            bvText.append(" x ");
            bvText.append(aesMultiplier);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(" = ");
            bvText.append(weight);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        // add tonnage, adjusted for TSM
        if (hasTSM(true)) {
            weaponBV += weight * 1.5;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Add weight + TSM Modifier");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(weight);
            bvText.append(" * 1.5");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(weight * 1.5);
            bvText.append(endColumn);
            bvText.append(endRow);
        } else if (hasIndustrialTSM()) {
            weaponBV += weight * 1.15;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Add weight + Industrial TSM Modifier");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(weight);
            bvText.append(" * 1.115");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(weight * 1.15);
            bvText.append(endColumn);
            bvText.append(endRow);
        } else {
            weaponBV += weight;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Add weight");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+");
            bvText.append(weight);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        if ((getCockpitType() == Mech.COCKPIT_INDUSTRIAL)
                || (getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            // industrial without advanced firing control get's 0.9 mod to
            // offensive BV
            bvText.append("Weapon BV * Firing Control Modifier");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(weaponBV);
            bvText.append(" * ");
            bvText.append("0.9");
            bvText.append(endColumn);
            weaponBV *= 0.9;
            bvText.append(startColumn);
            bvText.append(" = ");
            bvText.append(weaponBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        double speedFactor = Math
                .pow(1 + ((((double) runMP + (Math
                        .round(Math.max(jumpMP, umuMP) / 2.0))) - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Final Speed Factor: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        obv = weaponBV * speedFactor;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Weapons BV * Speed Factor ");
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(weaponBV);
        bvText.append(" * ");
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        if (useGeometricMeanBV()) {
            bvText.append("2 * sqrt(Offensive BV * Defensive BV");
        } else {
            bvText.append("Offensive BV + Defensive BV");
        }
        bvText.append(endColumn);
        bvText.append(startColumn);

        double finalBV;
        if (useGeometricMeanBV()) {
            finalBV = 2 * Math.sqrt(obv * dbv);
            if (finalBV == 0) {
                finalBV = dbv + obv;
            }
            bvText.append("2 * sqrt(");
            bvText.append(obv);
            bvText.append(" + ");
            bvText.append(dbv);
            bvText.append(")");
        } else {
            finalBV = dbv + obv;
            bvText.append(dbv);
            bvText.append(" + ");
            bvText.append(obv);
        }
        double totalBV = finalBV;

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        double cockpitMod = 1;
        if ((getCockpitType() == Mech.COCKPIT_SMALL)
                || (getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)
                || (getCockpitType() == Mech.COCKPIT_SMALL_COMMAND_CONSOLE)) {
            cockpitMod = 0.95;
            finalBV *= cockpitMod;
        } else if (hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            finalBV *= 0.95;
        } else if (getCockpitType() == Mech.COCKPIT_INTERFACE) {
            cockpitMod = 1.3;
            finalBV *= cockpitMod;
        }
        finalBV = Math.round(finalBV);
        bvText.append("Total BV * Cockpit Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(totalBV);
        bvText.append(" * ");
        bvText.append(cockpitMod);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Final BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        if (!ignoreC3) {
            xbv += getExtraC3BV((int) Math.round(finalBV));
        }
        finalBV = (int) Math.round(finalBV + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = getCrew().getBVSkillMultiplier(game);
        }

        return (int) Math.round(finalBV * pilotFactor);
    }

    /**
     * Calculate the C-bill cost of the mech. Passing null as the argument will
     * skip the detailed report processing.
     *
     * @return The cost in C-Bills of the 'Mech in question.
     */
    @Override
    public double getCost(boolean ignoreAmmo) {
        double[] costs = new double[17 + locations()];
        int i = 0;

        double cockpitCost;
        if (getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            cockpitCost = 750000;
        } else if (getCockpitType() == Mech.COCKPIT_DUAL) {
            // Solaris VII - The Game World (German) This is not actually
            // canonical as it
            // has never been repeated in any English language source including
            // Tech Manual
            cockpitCost = 40000;
        } else if (getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
            // Command Consoles are listed as a cost of 500,000.
            // That appears to be in addition to the primary cockpit.
            cockpitCost = 700000;
        } else if (getCockpitType() == Mech.COCKPIT_SMALL) {
            cockpitCost = 175000;
        } else if (getCockpitType() == Mech.COCKPIT_VRRP) {
            cockpitCost = 1250000;
        } else if (getCockpitType() == Mech.COCKPIT_INDUSTRIAL) {
            cockpitCost = 100000;
        } else if (getCockpitType() == Mech.COCKPIT_TRIPOD) {
            cockpitCost = 400000;
        } else if (getCockpitType() == Mech.COCKPIT_QUADVEE) {
            cockpitCost = 375000;
        } else if (getCockpitType() == Mech.COCKPIT_SUPERHEAVY) {
            cockpitCost = 300000;
        } else if (getCockpitType() == Mech.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE) {
            // The cost is the sum of both superheavy cockpit and command console
            cockpitCost = 800000;
        } else if (getCockpitType() == Mech.COCKPIT_SUPERHEAVY_TRIPOD) {
            cockpitCost = 500000;
        } else if (getCockpitType() == Mech.COCKPIT_SMALL_COMMAND_CONSOLE) {
            // The cost is the sum of both small and command console
            cockpitCost = 675000;            
        } else {
            cockpitCost = 200000;
        }
        if (hasEiCockpit()
                && ((null != getCrew()) && hasAbility(OptionsConstants.UNOFF_EI_IMPLANT))) {
            cockpitCost = 400000;
        }
        costs[i++] = cockpitCost;
        costs[i++] = 50000;// life support
        costs[i++] = weight * 2000;// sensors
        int muscCost = hasSCM() ? 10000 : hasTSM(false) ? 16000 :
                        hasTSM(true) ? 32000 : hasIndustrialTSM() ? 12000 : 2000;
        costs[i++] = muscCost * weight;// musculature
        double structureCost = EquipmentType.getStructureCost(structureType) * weight;// IS
        costs[i++] = structureCost;
        costs[i++] = getActuatorCost();// arm and/or leg actuators
        if (hasEngine()) {
            costs[i++] = (getEngine().getBaseCost() * getEngine().getRating() * weight) / 75.0;
        }
        if (getGyroType() == Mech.GYRO_XL) {
            costs[i++] = 750000 * (int) Math
                    .ceil((getOriginalWalkMP() * weight) / 100f) * 0.5;
        } else if (getGyroType() == Mech.GYRO_COMPACT) {
            costs[i++] = 400000 * (int) Math
                    .ceil((getOriginalWalkMP() * weight) / 100f) * 1.5;
        } else if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            costs[i++] = 500000 * (int) Math
                    .ceil((getOriginalWalkMP() * weight) / 100f) * 2;
        } else if (getGyroType() == Mech.GYRO_STANDARD) {
            costs[i++] = 300000 * (int) Math
                    .ceil((getOriginalWalkMP() * weight) / 100f);
        }
        double jumpBaseCost = 200;
        // You cannot have JJ's and UMU's on the same unit.
        if (hasUMU()) {
            costs[i++] = Math.pow(getAllUMUCount(), 2.0) * weight
                    * jumpBaseCost;
            // We could have Jump boosters
            if (getJumpType() == Mech.JUMP_BOOSTER) {
                jumpBaseCost = 150;
                costs[i++] = Math.pow(getOriginalJumpMP(), 2.0) * weight
                        * jumpBaseCost;
            }
        } else {
            if (getJumpType() == Mech.JUMP_BOOSTER) {
                jumpBaseCost = 150;
            } else if (getJumpType() == Mech.JUMP_IMPROVED) {
                jumpBaseCost = 500;
            }
            costs[i++] = Math.pow(getOriginalJumpMP(), 2.0) * weight
                    * jumpBaseCost;
        }
        // num of sinks we don't pay for
        int freeSinks = hasDoubleHeatSinks() ? 0 : 10;
        int sinkCost = hasDoubleHeatSinks() ? 6000 : 2000;
        // cost of sinks
        costs[i++] = sinkCost * (heatSinks() - freeSinks);
        costs[i++] = hasFullHeadEject() ? 1725000 : 0;
        // armored components
        int armoredCrits = 0;
        for (int j = 0; j < locations(); j++) {
            int numCrits = getNumberOfCriticals(j);
            for (int k = 0; k < numCrits; k++) {
                CriticalSlot ccs = getCritical(j, k);
                if ((ccs != null) && ccs.isArmored()
                        && (ccs.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    armoredCrits++;
                }
            }
        }
        costs[i++] = armoredCrits * 150000;

        // armor
        if (hasPatchworkArmor()) {
            for (int loc = 0; loc < locations(); loc++) {
                costs[i++] += getArmorWeight(loc)
                        * EquipmentType.getArmorCost(armorType[loc]);
            }
        } else {
            costs[i++] += getArmorWeight()
                    * EquipmentType.getArmorCost(armorType[0]);
        }

        double weaponCost = getWeaponsAndEquipmentCost(ignoreAmmo);
        costs[i++] = weaponCost;

        if (this instanceof LandAirMech) {
            costs[i++] = (structureCost + weaponCost)
                    * (((LandAirMech) this).getLAMType() == LandAirMech.LAM_BIMODAL? 0.65 : 0.75);
        } else if (this instanceof QuadVee) {
            costs[i++] = (structureCost + weaponCost) * 0.5;
        } else {
            costs[i++] = 0;
        }

        double cost = 0; // calculate the total
        for (int x = 0; x < i; x++) {
            cost += costs[x];
        }
        // TODO Decouple cost calculation from addCostDetails and eliminate duplicate code in getPriceMultiplier
        double quirkMultiplier = 0;
        if (hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_1)) {
        	quirkMultiplier = 1.1f;
        	cost *= quirkMultiplier;
        } else if (hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_2)) {
        	quirkMultiplier = 1.25f;
        	cost *= quirkMultiplier;
        }
        costs[i++] = -quirkMultiplier; // negative just marks it as multiplier

        double omniMultiplier = 0;
        if (isOmni()) {
            omniMultiplier = 1.25f;
            cost *= omniMultiplier;
        }
        costs[i++] = -omniMultiplier; // negative just marks it as multiplier

        double weightMultiplier = 1 + (weight / 100f);
        if (isIndustrial()) {
            weightMultiplier = 1 + (weight / 400f);
        }
        costs[i++] = -weightMultiplier; // negative just marks it as multiplier
        cost = Math.round(cost * weightMultiplier);
        addCostDetails(cost, costs);
        return cost;
    }

    @Override
    public double getPriceMultiplier() {
        double priceMultiplier = 1.0f;
        if (hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_1)) {
            priceMultiplier *= 1.1f;
        } else if (hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_2)) {
            priceMultiplier *= 1.25f;
        }
        // TODO Negative price quirks (Bad Reputation)

        if (isOmni()) {
            priceMultiplier *= 1.25f;
        }

        // Weight multiplier
        priceMultiplier *= 1 + (weight / 100f);
        if (isIndustrial()) {
            priceMultiplier = 1 + (weight / 400f);
        }
        return priceMultiplier;
    }

    @Override
    protected int implicitClanCASE() {
        if (!isClan()) {
            return 0;
        }
        int explicit = 0;
        Set<Integer> caseLocations = new HashSet<>();
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType) && (m.getType().hasFlag(MiscType.F_CASE))) {
                explicit++;
            } else if (m.getType().isExplosive(m)) {
                caseLocations.add(m.getLocation());
                if (m.getSecondLocation() >= 0) {
                    caseLocations.add(m.getSecondLocation());
                }
            }
        }
        return Math.max(0, caseLocations.size() - explicit);
    }

    private void addCostDetails(double cost, double... costs) {
        bvText = new StringBuffer();
        String[] left = { "Cockpit", "Life Support", "Sensors", "Myomer",
                "Structure", "Actuators", "Engine", "Gyro", "Jump Jets",
                "Heatsinks", "Full Head Ejection System",
                "Armored System Components", "Armor", "Equipment",
                "Conversion Equipment", "Quirk Multiplier", "Omni Multiplier", "Weight Multiplier" };

        NumberFormat commafy = NumberFormat.getInstance();

        bvText.append("<HTML><BODY><CENTER><b>Cost Calculations For ");
        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append(startTable);
        // find the maximum length of the columns.
        for (int l = 0; l < left.length; l++) {

            if (l == 13) {
                getWeaponsAndEquipmentCost(true);
            } else {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(left[l]);
                bvText.append(endColumn);
                bvText.append(startColumn);

                if (costs[l] == 0) {
                    bvText.append("N/A");
                } else if (costs[l] < 0) {
                    bvText.append("x ");
                    bvText.append(commafy.format(-costs[l]));
                } else {
                    bvText.append(commafy.format(costs[l]));

                }
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Cost:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(commafy.format(cost));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");
    }

    protected double getActuatorCost() {
        return getArmActuatorCost() + getLegActuatorCost();
    }

    protected abstract double getArmActuatorCost();

    protected abstract double getLegActuatorCost();

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7030);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(getCrew().getDescVector(false));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);

        if (isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if (killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if (killer != null) {
                r = new Report(7072, Report.PUBLIC);
                r.addDesc(killer);
            } else {
                r = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(r);
        } else if (getCrew().isEjected()) {
            r = new Report(7074, Report.PUBLIC);
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    /**
     * Add in any piloting skill mods
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        // gyro hit?
        if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                Mech.LOC_CT) > 0) {

            if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                        Mech.LOC_CT) == 1) {
                    roll.addModifier(1, "HD Gyro damaged once");
                } else {
                    roll.addModifier(3, "HD Gyro damaged twice");
                }
            } else {
                roll.addModifier(3, "Gyro damaged");
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
        if (((getCockpitType() == Mech.COCKPIT_SMALL) || (getCockpitType() == Mech.COCKPIT_SMALL_COMMAND_CONSOLE))
                && (!hasAbility(OptionsConstants.MD_BVDNI)
                && !hasAbility(OptionsConstants.UNOFF_SMALL_PILOT))) {
            roll.addModifier(1, "Small Cockpit");
        } else if (getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            roll.addModifier(1, "Torso-Mounted Cockpit");
            int sensorHits = getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                roll.addModifier(4,
                        "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                roll.addModifier(4,
                        "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        } else if (getCockpitType() == Mech.COCKPIT_DUAL) {
            //Dedicated pilot bonus is lost if pilot makes any attacks. Penalty for gunner acting as pilot.
            if (getCrew().getCurrentPilotIndex() != getCrew().getCrewType().getPilotPos()) {
                roll.addModifier(1, "dual cockpit without active pilot");
            } else if (getCrew().hasDedicatedGunner() || !isAttackingThisTurn()) {
                roll.addModifier(-1, "dedicated pilot");
            }
        }

        if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT) && !hasAbility(OptionsConstants.UNOFF_SMALL_PILOT)) {
            roll.addModifier(1, "cramped cockpit");
        }

        if (hasHardenedArmor()) {
            roll.addModifier(1, "Hardened Armor");
        }

        if (hasModularArmor()) {
            roll.addModifier(1, "Modular Armor");
        }
        if (hasIndustrialTSM()) {
            roll.addModifier(1, "Industrial TSM");
        }

        return roll;
    }

    @Override
    public int getMaxElevationChange() {
        if (movementMode == EntityMovementMode.TRACKED
                || movementMode == EntityMovementMode.WIGE) {
            return 1;
        }
        return 2;
    }

    @Override
    public int getMaxElevationDown(int currElevation) {
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEAPING)) {
            return UNLIMITED_JUMP_DOWN;
        }
        return getMaxElevationChange();
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth
     * can be active and not working when under ECCM)
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    @Override
    public boolean isStealthActive() {
        // Try to find a Mek Stealth system.
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_STEALTH)) {

                if (mEquip.curMode().equals("On")
                        && hasActiveECM()) {
                    // Return true if the mode is "On" and ECM is working
                    return true;
                }
            }
        }
        // No Mek Stealth or system inactive. Return false.
        return false;
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth
     * can be active and not working when under ECCM)
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    @Override
    public boolean isStealthOn() {
        // Try to find a Mek Stealth system.
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_STEALTH)) {
                if (mEquip.curMode().equals("On")) {
                    // Return true if the mode is "On"
                    return true;
                }
            }
        }
        // No Mek Stealth or system inactive. Return false.
        return false;
    }

    /**
     * Does the mech have a functioning null signature system, or a void sig
     * that is acting as a a null sig because of externally carried BA?
     */
    @Override
    public boolean isNullSigActive() {
        if (isVoidSigOn() && !isVoidSigActive()) {
            return true;
        }
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_NULLSIG)
                        && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isNullSigOn() {
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_NULLSIG)
                        && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the mech have a functioning void signature system?
     */
    @Override
    public boolean isVoidSigActive() {
        // per the rules questions forum, externally mounted BA invalidates Void
        // Sig
        if (getLoadedUnits().size() > 0) {
            return false;
        }
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_VOIDSIG)
                        && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the mech have a functioning void signature system?
     */
    @Override
    public boolean isVoidSigOn() {
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_VOIDSIG)
                        && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the mech have a functioning Chameleon Light Polarization Field? For
     * a CLPS to be functioning it must be on and the unit can't have mounted
     * mechanized BattleArmor.
     */
    @Override
    public boolean isChameleonShieldActive() {
        // TO pg 300 states that generates heat but doesn't operate if the unit
        //  has mounted BA
        if (getLoadedUnits().size() > 0) {
            return false;
        }
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_CHAMELEON_SHIELD)
                        && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the mech Chameleon Light Polarization Field turned on?  This is used
     * for heat generation purposes.  A CLPS can be on and generating heat but
     * not providing any benefit if the unit has mechanized BattleArmor.
     */
    @Override
    public boolean isChameleonShieldOn() {
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_CHAMELEON_SHIELD)
                        && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine the stealth modifier for firing at this unit from the given
     * range. If the value supplied for <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @param range
     *            - an <code>int</code> value that must match one of the
     *            <code>Compute</code> class range constants.
     * @param ae
     *            - entity making the attack
     * @return a <code>TargetRoll</code> value that contains the stealth
     *         modifier for the given range.
     */
    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // can't combine void sig and stealth or null-sig
        if (isVoidSigActive()) {
            int mmod = 3;
            if (delta_distance > 5) {
                mmod = 0;
            } else if (delta_distance > 2) {
                mmod = 1;
            } else if (delta_distance > 0) {
                mmod = 2;
            }
            return new TargetRoll(mmod, "void signature");
        }

        final boolean isInfantry = ae.isConventionalInfantry();
        // Stealth or null sig must be active.
        if (!isStealthActive() && !isNullSigActive() && !isChameleonShieldActive()) {
            result = new TargetRoll(0, "stealth not active");
        }
        // Determine the modifier based upon the range.
        // Infantry do not ignore Chameleon LPS!!!
        else {
            switch (range) {
                case RangeType.RANGE_MINIMUM:
                case RangeType.RANGE_SHORT:
                    if (isStealthActive() && !isInfantry) {
                        result = new TargetRoll(0, "stealth");
                    } else if (isChameleonShieldActive()) {
                        result = new TargetRoll(0, "chameleon");
                        if (isNullSigActive() && !isInfantry) {
                            result.addModifier(0, "null-sig");
                        }
                    } else if (isNullSigActive() && !isInfantry) {
                        result = new TargetRoll(0, "null-sig");
                    } else {
                        // must be infantry
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_MEDIUM:
                    if (isStealthActive() && !isInfantry) {
                        result = new TargetRoll(1, "stealth");
                    } else if (isChameleonShieldActive()) {
                        result = new TargetRoll(1, "chameleon");
                        if (isNullSigActive() && !isInfantry) {
                            result.addModifier(1, "null-sig");
                        }
                    } else if (isNullSigActive() && !isInfantry) {
                        result = new TargetRoll(1, "null-sig");
                    } else {
                        // must be infantry
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME:
                case RangeType.RANGE_LOS:
                    if (isStealthActive() && !isInfantry) {
                        result = new TargetRoll(2, "stealth");
                    } else if (isChameleonShieldActive()) {
                        result = new TargetRoll(2, "chameleon");
                        if (isNullSigActive() && !isInfantry) {
                            result.addModifier(2, "null-sig");
                        }
                    } else if (isNullSigActive() && !isInfantry) {
                        result = new TargetRoll(2, "null-sig");
                    } else {
                        // must be infantry
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_OUT:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown range constant: " + range);
            }
        }

        // Return the result.
        return result;

    } // End public TargetRoll getStealthModifier( char )

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return A <code>boolean</code> that is <code>true</code> if the unit can
     *         be repaired (given enough time and parts); if this value is
     *         <code>false</code>, the unit is only a source of spares.
     * @see Entity#isSalvage()
     */
    @Override
    public boolean isRepairable() {
        // A Mech is repairable if it is salvageable,
        // and its CT internals are not gone.
        int loc_is = this.getInternal(Mech.LOC_CT);
        return isSalvage() && (loc_is != IArmorState.ARMOR_DOOMED)
                && (loc_is != IArmorState.ARMOR_DESTROYED);
    }

    @Override
    public boolean canCharge() {
        // Mechs can charge, unless they are Clan and the "no clan physicals"
        // option is set
        return super.canCharge()
                && !(game.getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL) && isClan());
    }

    @Override
    public boolean canDFA() {
        // Mechs can DFA, unless they are Clan and the "no clan physicals"
        // option is set
        return super.canDFA()
                && !(game.getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL) && isClan());
    }

    // gives total number of sinks
    public int getNumberOfSinks() {
        int sinks = 0;
        for (Mounted mounted : getMisc()) {
            if (mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }
            if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                sinks++;
            } else if (mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                sinks++;
            }
        }
        return sinks;
    }

    public boolean hasDoubleHeatSinks() {
        for (Mounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                return false;
            } else if (mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasLaserHeatSinks() {
        if (hasLaserHeatSinks == HAS_UNKNOWN) {
            for (Mounted mounted : getMisc()) {
                if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                    hasLaserHeatSinks = HAS_FALSE;
                    break;
                } else if (mounted.getType()
                        .hasFlag(MiscType.F_LASER_HEAT_SINK)) {
                    hasLaserHeatSinks = HAS_TRUE;
                    break;
                }
            }
            if (hasLaserHeatSinks == HAS_UNKNOWN) {
                hasLaserHeatSinks = HAS_FALSE;
            }
        }
        return hasLaserHeatSinks == HAS_TRUE;
    }

    public void setActiveSinksNextRound(int sinks) {
        sinksOnNextRound = sinks;
    }

    public int getActiveSinks() {
        if (sinksOn < 0) {
            sinksOn = getNumberOfSinks();
            sinksOnNextRound = sinksOn;
        }
        return sinksOn;
    }

    public void resetSinks() {
        sinksOn = getNumberOfSinks();
    }

    public int getActiveSinksNextRound() {
        if (sinksOnNextRound < 0) {
            return getActiveSinks();
        }
        return sinksOnNextRound;
    }

    /**
     * @return Returns the autoEject.
     */
    public boolean isAutoEject() {
        boolean hasEjectSeat = true;
        if (getCockpitType() == COCKPIT_TORSO_MOUNTED
                || hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT)) {
            hasEjectSeat = false;
        }
        if (isIndustrial()) {
            // industrials can only eject when they have an ejection seat
            for (Mounted misc : miscList) {
                if (misc.getType().hasFlag(MiscType.F_EJECTION_SEAT)) {
                    hasEjectSeat = true;
                }
            }
        }
        return autoEject && hasEjectSeat;
    }

    /**
     * @param autoEject
     *            The autoEject to set.
     */
    public void setAutoEject(boolean autoEject) {
        this.autoEject = autoEject;
    }

    /**
     * @return Conditional Ejection Ammo
     */
    public boolean isCondEjectAmmo() {
        return condEjectAmmo;
    }

    /**
     * @param condEjectAmmo
     *            The condEjectAmmo to set.
     */
    public void setCondEjectAmmo(boolean condEjectAmmo) {
        this.condEjectAmmo = condEjectAmmo;
    }

    /**
     * @return Conditional Ejection Engine
     */
    public boolean isCondEjectEngine() {
        return condEjectEngine;
    }

    /**
     * @param condEjectEngine
     *            The condEjectEngine to set.
     */
    public void setCondEjectEngine(boolean condEjectEngine) {
        this.condEjectEngine = condEjectEngine;
    }

    /**
     * @return Conditional Ejection CTDest
     */
    public boolean isCondEjectCTDest() {
        return condEjectCTDest;
    }

    /**
     * @param condEjectCTDest
     *            The condEjectCTDest to set.
     */
    public void setCondEjectCTDest(boolean condEjectCTDest) {
        this.condEjectCTDest = condEjectCTDest;
    }

    /**
     * @return Conditional Ejection Headshot
     */
    public boolean isCondEjectHeadshot() {
        return condEjectHeadshot;
    }

    /**
     * @param condEjectHeadshot
     *            The condEjectHeadshot to set.
     */
    public void setCondEjectHeadshot(boolean condEjectHeadshot) {
        this.condEjectHeadshot = condEjectHeadshot;
    }

    @Override
    public boolean removePartialCoverHits(int location, int cover, int side) {
        // left and right cover are from attacker's POV.
        // if hitting front arc, need to swap them

        // Handle upper cover specially, as treating it as a bitmask will lead
        //  to every location being covered
        if (cover  == LosEffects.COVER_UPPER) {
            if ((location == Mech.LOC_LLEG) || (location == Mech.LOC_RLEG)) {
                return false;
            } else {
                return true;
            }
        }

        if (side == ToHitData.SIDE_FRONT) {
            if (((cover & LosEffects.COVER_LOWRIGHT) != 0)
                    && (location == Mech.LOC_LLEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOWLEFT) != 0)
                    && (location == Mech.LOC_RLEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0)
                    && ((location == Mech.LOC_LARM)
                            || (location == Mech.LOC_LT) || (location == Mech.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0)
                    && ((location == Mech.LOC_RARM)
                            || (location == Mech.LOC_RT) || (location == Mech.LOC_RLEG))) {
                return true;
            }
        } else {
            if (((cover & LosEffects.COVER_LOWLEFT) != 0)
                    && (location == Mech.LOC_LLEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOWRIGHT) != 0)
                    && (location == Mech.LOC_RLEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0)
                    && ((location == Mech.LOC_LARM)
                            || (location == Mech.LOC_LT) || (location == Mech.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0)
                    && ((location == Mech.LOC_LARM)
                            || (location == Mech.LOC_LT) || (location == Mech.LOC_LLEG))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doomedInExtremeTemp() {
        return false;
    }

    @Override
    public boolean doomedInVacuum() {
        return false;
    }

    @Override
    public boolean doomedOnGround() {
        return false;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return true;
    }

    @Override
    public boolean hasEiCockpit() {
        return isClan() || super.hasEiCockpit();
    }

    @Override
    public boolean hasActiveEiCockpit() {
        if (cockpitStatus == COCKPIT_OFF) {
            return false;
        }
        if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS,
                Mech.LOC_HEAD) > 0) {
            return false;
        }
        return super.hasActiveEiCockpit();
    }

    public int getCockpitStatus() {
        return cockpitStatus;
    }

    public int getCockpitStatusNextRound() {
        return cockpitStatusNextRound;
    }

    public void setCockpitStatus(int state) {
        cockpitStatusNextRound = state;
        // on/off allowed only in end phase
        if ((state != COCKPIT_OFF) && (cockpitStatus != COCKPIT_OFF)) {
            cockpitStatus = state;
        }
    }

    @Override
    public int getGyroType() {
        return gyroType;
    }

    public int getCockpitType() {
        return cockpitType;
    }

    public void setGyroType(int type) {
        gyroType = type;
    }

    public void setCockpitType(int type) {
        cockpitType = type;
    }

    public String getGyroTypeString() {
        return Mech.getGyroTypeString(getGyroType());
    }

    public String getCockpitTypeString() {
        return Mech.getCockpitTypeString(getCockpitType());
    }

    public static String getGyroTypeString(int inGyroType) {
        if ((inGyroType < 0) || (inGyroType >= GYRO_STRING.length)) {
            return "Unknown";
        }
        return GYRO_STRING[inGyroType];
    }

    public static String getGyroTypeShortString(int inGyroType) {
        if ((inGyroType < 0) || (inGyroType >= GYRO_SHORT_STRING.length)) {
            return "Unknown";
        }
        return GYRO_SHORT_STRING[inGyroType];
    }

    public static String getCockpitTypeString(int inCockpitType) {
        if ((inCockpitType < 0) || (inCockpitType >= COCKPIT_STRING.length)) {
            return "Unknown";
        }
        return COCKPIT_STRING[inCockpitType];
    }

    public static int getGyroTypeForString(String inType) {
        if ((inType == null) || (inType.length() < 1)) {
            return GYRO_UNKNOWN;
        }
        for (int x = 0; x < GYRO_STRING.length; x++) {
            if ((inType.equals(GYRO_STRING[x]))
                    || (inType.equals(GYRO_SHORT_STRING[x]))) {
                return x;
            }
        }
        return GYRO_UNKNOWN;
    }

    public static int getCockpitTypeForString(String inType) {
        if ((inType == null) || (inType.length() < 1)) {
            return COCKPIT_UNKNOWN;
        }
        for (int x = 0; x < COCKPIT_STRING.length; x++) {
            if ((inType.equals(COCKPIT_STRING[x]))
                    || (inType.equals(COCKPIT_SHORT_STRING[x]))) {
                return x;
            }
        }
        return COCKPIT_UNKNOWN;
    }

    public String getSystemName(int index) {
        if (index == SYSTEM_GYRO) {
            return Mech.getGyroDisplayString(gyroType);
        }
        if (index == SYSTEM_COCKPIT) {
            if (isIndustrial() && (cockpitType == Mech.COCKPIT_STANDARD)) {
                return "Industrial Cockpit (adv. FCS)";
            }
            return Mech.getCockpitDisplayString(cockpitType);
        }
        return systemNames[index];
    }

    public String getRawSystemName(int index) {
        return systemNames[index];
    }

    public static String getGyroDisplayString(int inType) {
        String inName;
        switch (inType) {
            case GYRO_XL:
                inName = "GYRO_XL";
                break;
            case GYRO_COMPACT:
                inName = "GYRO_COMPACT";
                break;
            case GYRO_HEAVY_DUTY:
                inName = "GYRO_HEAVY_DUTY";
                break;
            case GYRO_STANDARD:
                inName = "GYRO_STANDARD";
                break;
            case GYRO_NONE:
                inName = "GYRO_NONE";
                break;
            case GYRO_SUPERHEAVY:
                inName = "GYRO_SUPERHEAVY";
                break;
            default:
                inName = "GYRO_UNKNOWN";
        }
        String result = EquipmentMessages.getString("SystemType.Gyro." + inName);
        if (result != null) {
            return result;
        }
        return inName;
    }

    public static String getCockpitDisplayString(int inType) {
        String inName;
        switch (inType) {
            case COCKPIT_COMMAND_CONSOLE:
                inName = "COCKPIT_COMMAND_CONSOLE";
                break;
            case COCKPIT_SMALL:
                inName = "COCKPIT_SMALL";
                break;
            case COCKPIT_TORSO_MOUNTED:
                inName = "COCKPIT_TORSO_MOUNTED";
                break;
            case COCKPIT_DUAL:
                inName = "COCKPIT_DUAL";
                break;
            case COCKPIT_STANDARD:
                inName = "COCKPIT_STANDARD";
                break;
            case COCKPIT_INDUSTRIAL:
                inName = "COCKPIT_INDUSTRIAL";
                break;
            case COCKPIT_PRIMITIVE:
                inName = "COCKPIT_PRIMITIVE";
                break;
            case COCKPIT_PRIMITIVE_INDUSTRIAL:
                inName = "COCKPIT_PRIMITIVE_INDUSTRIAL";
                break;
            case COCKPIT_SUPERHEAVY:
                inName = "COCKPIT_SUPERHEAVY";
                break;
            case COCKPIT_SUPERHEAVY_TRIPOD:
                inName = "COCKPIT_SUPERHEAVY_TRIPOD";
                break;
            case COCKPIT_TRIPOD:
                inName = "COCKPIT_TRIPOD";
                break;
            case COCKPIT_INTERFACE:
                inName = "COCKPIT_INTERFACE";
                break;
            case COCKPIT_VRRP:
                inName = "COCKPIT_VRRP";
                break;
            case COCKPIT_QUADVEE:
                inName = "COCKPIT_QUADVEE";
                break;
            case COCKPIT_SUPERHEAVY_INDUSTRIAL:
                inName = "COCKPIT_SUPERHEAVY_INDUSTRIAL";
                break;
            case COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
                inName = "COCKPIT_SUPERHEAVY_COMMAND_CONSOLE";
                break;
            case COCKPIT_SMALL_COMMAND_CONSOLE:
                inName = "COCKPIT_SMALL_COMMAND_CONSOLE";
                break;
            default:
                inName = "COCKPIT_UNKNOWN";
        }
        String result = EquipmentMessages.getString("SystemType.Cockpit." + inName);
        if (result != null) {
            return result;
        }
        return inName;
    }

    @Override
    public boolean canAssaultDrop() {
        return true;
    }

    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        Hex hex = game.getBoard().getHex(c);
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if ((hex.containsTerrain(Terrains.PAVEMENT)
                    || hex.containsTerrain(Terrains.ROAD))
                    && (!hex.containsTerrain(Terrains.BUILDING)
                            && !hex.containsTerrain(Terrains.RUBBLE))) {
                return true;
            }
            // Can't deploy on a bridge
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation)
                    && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            // mechs can deploy in water if the water covers them entirely
            if (hex.containsTerrain(Terrains.WATER) && (hex.terrainLevel(Terrains.WATER) < (height() + 1))) {
                return true;
            }
            // Can't deploy in clear hex
            if (hex.isClearHex()) {
                return true;
            }
        }
        // Mechs using tracks and QuadVees in vehicle mode (or converting to or from) have the same
        // restrictions and combat vehicles with the exception that QuadVees can enter water hexes
        // except during conversion.
        if (movementMode == EntityMovementMode.TRACKED
                || (this instanceof QuadVee && convertingNow
                        && ((QuadVee) this).getMotiveType() == QuadVee.MOTIVE_TRACK)) {
                return (hex.terrainLevel(Terrains.WOODS) > 1)
                        || ((hex.terrainLevel(Terrains.WATER) > 0)
                                && !hex.containsTerrain(Terrains.ICE)
                                && (!(this instanceof QuadVee) || convertingNow))
                        || hex.containsTerrain(Terrains.JUNGLE)
                        || (hex.terrainLevel(Terrains.MAGMA) > 1)
                        || (hex.terrainLevel(Terrains.ROUGH) > 1)
                        || (hex.terrainLevel(Terrains.RUBBLE) > 5);
        }
        if (movementMode == EntityMovementMode.WHEELED
                || (this instanceof QuadVee && convertingNow
                        && ((QuadVee) this).getMotiveType() == QuadVee.MOTIVE_WHEEL)) {
            return hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.ROUGH)
                    || hex.containsTerrain(Terrains.RUBBLE)
                    || hex.containsTerrain(Terrains.MAGMA)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || (hex.terrainLevel(Terrains.SNOW) > 1)
                    || (hex.terrainLevel(Terrains.GEYSER) == 2)
                    || ((hex.terrainLevel(Terrains.WATER) > 0)
                            && !hex.containsTerrain(Terrains.ICE)
                            && convertingNow);
        }

        return (hex.terrainLevel(Terrains.WOODS) > 2)
                || (hex.terrainLevel(Terrains.JUNGLE) > 2);
    }

    /**
     * Get an '.mtf' file representation of the mech. This string can be
     * directly written to disk as a file and later loaded by the MtfFile class.
     * Known missing level 3 features: mixed tech, laser heatsinks
     */
    public String getMtf() {
        StringBuilder sb = new StringBuilder();
        String newLine = "\n";

        boolean standard = (getCockpitType() == Mech.COCKPIT_STANDARD)
                && (getGyroType() == Mech.GYRO_STANDARD);
        boolean fullHead = hasFullHeadEject();
        if (standard && !fullHead) {
            sb.append("Version:1.0").append(newLine);
        } else if (!fullHead) {
            sb.append("Version:1.1").append(newLine);
        } else {
            sb.append("Version:1.2").append(newLine);
        }
        sb.append(chassis).append(newLine);
        sb.append(model).append(newLine);
        sb.append(newLine);

        sb.append("Config:");
        if (this instanceof LandAirMech) {
            sb.append("LAM");
        } else if (this instanceof BipedMech) {
            sb.append("Biped");
        } else if (this instanceof QuadVee) {
            sb.append("QuadVee");
        } else if (this instanceof QuadMech) {
            sb.append("Quad");
        } else if (this instanceof TripodMech) {
            sb.append("Tripod");
        }

        if (isOmni()) {
            sb.append(" Omnimech");
        }

        sb.append(newLine);
        sb.append(MtfFile.TECH_BASE);
        if (isMixedTech()) {
            if (isClan()) {
                sb.append("Mixed (Clan Chassis)");
            } else {
                sb.append("Mixed (IS Chassis)");
            }
        } else {
            sb.append(TechConstants.getTechName(techLevel));
        }
        sb.append(newLine);
        sb.append(MtfFile.ERA).append(year).append(newLine);
        if ((source != null) && (source.trim().length() > 0)) {
            sb.append(MtfFile.SOURCE).append(source).append(newLine);
        }
        sb.append(MtfFile.RULES_LEVEL).append(
                TechConstants.T_SIMPLE_LEVEL[techLevel]);
        sb.append(newLine);
        sb.append(newLine);

        sb.append(MtfFile.MASS).append((int) weight).append(newLine);
        sb.append(MtfFile.ENGINE);
        if (hasEngine()) {
                sb.append(getEngine().getEngineName())
                .append(" Engine")
                .append(!(getEngine().hasFlag(Engine.CLAN_ENGINE) && isMixedTech()) ? ("(IS)")
                        : "");
        } else {
            sb.append("(none)");
        }
        sb.append(newLine);
        sb.append(MtfFile.STRUCTURE);
        sb.append(EquipmentType.getStructureTypeName(getStructureType(),
                TechConstants.isClan(structureTechLevel)));
        sb.append(newLine);

        sb.append(MtfFile.MYOMER);
        if (hasTSM(false)) {
            sb.append("Triple-Strength");
        } else if (hasTSM(true)) {
            sb.append("Prototype Triple-Strength");
        } else if (hasIndustrialTSM()) {
            sb.append("Industrial Triple-Strength");
        } else if (hasSCM()) {
            sb.append("Super-Cooled");
        } else {
            sb.append("Standard");
        }
        sb.append(newLine);

        if (this instanceof LandAirMech) {
            sb.append(MtfFile.LAM);
            sb.append(((LandAirMech) this).getLAMTypeString());
            sb.append(newLine);
        } else if (this instanceof QuadVee) {
            sb.append(MtfFile.MOTIVE);
            sb.append(((QuadVee) this).getMotiveTypeString());
            sb.append(newLine);
        }


        if (!standard) {
            sb.append(MtfFile.COCKPIT);
            sb.append(getCockpitTypeString());
            sb.append(newLine);

            sb.append(MtfFile.GYRO);
            sb.append(getGyroTypeString());
            sb.append(newLine);
        }
        if (hasFullHeadEject()) {
            sb.append(MtfFile.EJECTION);
            sb.append(Mech.FULL_HEAD_EJECT_STRING);
            sb.append(newLine);
        }
        sb.append(newLine);

        sb.append(MtfFile.HEAT_SINKS).append(heatSinks()).append(" ");
        Optional<EquipmentType> heatSink = getMisc().stream()
                .filter(m -> m.getType().hasFlag(MiscType.F_HEAT_SINK)
                    || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK))
                .map(Mounted::getType).findFirst();
        // If we didn't find any heat sinks we may have an ICE with no added sinks, or prototype
        // doubles (which have a different flag). In the latter case, we want to put single
        // here, since this determines what's installed as engine-integrated heat sinks.
        if (heatSink.isEmpty()) {
            sb.append(MtfFile.HS_SINGLE);
        } else if (heatSink.get().hasFlag(MiscType.F_LASER_HEAT_SINK)) {
            sb.append(MtfFile.HS_LASER);
        } else if (heatSink.get().hasFlag(MiscType.F_COMPACT_HEAT_SINK)) {
            sb.append(MtfFile.HS_COMPACT);
        } else if (heatSink.get().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
            sb.append(heatSink.get().isClan() ? MtfFile.TECH_BASE_CLAN : MtfFile.TECH_BASE_IS);
            sb.append(" ").append(MtfFile.HS_DOUBLE);
        } else {
            sb.append(MtfFile.HS_SINGLE);
        }
        sb.append(newLine);

        if (isOmni()) {
            sb.append(MtfFile.BASE_CHASSIS_HEAT_SINKS);
            sb.append(hasEngine() ? getEngine().getBaseChassisHeatSinks(hasCompactHeatSinks()) : 0);
            sb.append(newLine);
        }
        for (Mounted mounted : getMisc()) {
            if ((mounted.getCriticals() == 0)
                    && !mounted.getType().hasFlag(MiscType.F_CASE)
                    && !EquipmentType.isArmorType(mounted.getType())
                    && !EquipmentType.isStructureType(mounted.getType())) {
                sb.append(MtfFile.NO_CRIT).append(mounted.getType().getInternalName())
                        .append(":").append(getLocationAbbr(mounted.getLocation()))
                        .append(newLine);
            }
        }

        sb.append(MtfFile.WALK_MP).append(walkMP).append(newLine);
        sb.append(MtfFile.JUMP_MP).append(jumpMP).append(newLine);
        sb.append(newLine);

        if (hasPatchworkArmor()) {
            sb.append(MtfFile.ARMOR).append(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK));
        } else {
            sb.append(MtfFile.ARMOR).append(EquipmentType.getArmorTypeName(getArmorType(0)))
                .append("(").append(TechConstants.getTechName(getArmorTechLevel(0))).append(")");
        }
        sb.append(newLine);

        for (int element : MtfFile.locationOrder) {
            if ((element == Mech.LOC_CLEG) && !(this instanceof TripodMech)) {
                continue;
            }
            sb.append(getLocationAbbr(element)).append(" ").append(MtfFile.ARMOR);
            if (hasPatchworkArmor()) {
                sb.append(EquipmentType.getArmorTypeName(getArmorType(element), isClan()))
                        .append('(').append(TechConstants.getTechName(getArmorTechLevel(element)))
                        .append("):");
            }
            sb.append(getOArmor(element, false)).append(newLine);
        }
        for (int element : MtfFile.rearLocationOrder) {
            sb.append("RT").append(getLocationAbbr(element).charAt(0)).append(" ").append(MtfFile.ARMOR);
            sb.append(getOArmor(element, true)).append(newLine);
        }
        sb.append(newLine);

        sb.append("Weapons:").append(weaponList.size()).append(newLine);
        for (Mounted m : weaponList) {
            sb.append(m.getName()).append(", ")
                    .append(getLocationName(m.getLocation())).append(newLine);
        }
        sb.append(newLine);
        for (int l : MtfFile.locationOrder) {
            if ((l == Mech.LOC_CLEG) && !(this instanceof TripodMech)) {
                continue;
            }
            String locationName = getLocationName(l);
            sb.append(locationName).append(":");
            sb.append(newLine);
            for (int y = 0; y < 12; y++) {
                if (y < getNumberOfCriticals(l)) {
                    sb.append(decodeCritical(getCritical(l, y)))
                            .append(newLine);
                } else {
                    sb.append(MtfFile.EMPTY).append(newLine);
                }
            }
            sb.append(newLine);
        }

        if (getFluff().getOverview().trim().length() > 0) {
            sb.append(MtfFile.OVERVIEW);
            sb.append(getFluff().getOverview());
            sb.append(newLine);
        }

        if (getFluff().getCapabilities().trim().length() > 0) {
            sb.append(MtfFile.CAPABILITIES);
            sb.append(getFluff().getCapabilities());
            sb.append(newLine);
        }

        if (getFluff().getDeployment().trim().length() > 0) {
            sb.append(MtfFile.DEPLOYMENT);
            sb.append(getFluff().getDeployment());
            sb.append(newLine);
        }

        if (getFluff().getHistory().trim().length() > 0) {
            sb.append(MtfFile.HISTORY);
            sb.append(getFluff().getHistory());
            sb.append(newLine);
        }

        if (getFluff().getManufacturer().trim().length() > 0) {
            sb.append(MtfFile.MANUFACTURER);
            sb.append(getFluff().getManufacturer());
            sb.append(newLine);
        }

        if (getFluff().getPrimaryFactory().trim().length() > 0) {
            sb.append(MtfFile.PRIMARY_FACTORY);
            sb.append(getFluff().getPrimaryFactory());
            sb.append(newLine);
        }

        if (getFluff().getNotes().trim().length() > 0) {
            sb.append(MtfFile.NOTES);
            sb.append(getFluff().getNotes());
            sb.append(newLine);
        }

        if (getFluff().getMMLImagePath().trim().length() > 0) {
            sb.append(MtfFile.IMAGE_FILE);
            sb.append(getFluff().getMMLImagePath());
            sb.append(newLine);
        }

        for (EntityFluff.System system : EntityFluff.System.values()) {
        	if (getFluff().getSystemManufacturer(system).length() > 0) {
        		sb.append(MtfFile.SYSTEM_MANUFACTURER);
        		sb.append(system.toString()).append(":");
        		sb.append(getFluff().getSystemManufacturer(system));
        		sb.append(newLine);
        	}
        	if (getFluff().getSystemModel(system).length() > 0) {
        		sb.append(MtfFile.SYSTEM_MODEL);
        		sb.append(system.toString()).append(":");
        		sb.append(getFluff().getSystemModel(system));
        		sb.append(newLine);
        	}
        }

        if (getUseManualBV()) {
            sb.append(MtfFile.BV);
            sb.append(getManualBV());
            sb.append(newLine);
        }

        return sb.toString();
    }

    private String decodeCritical(CriticalSlot cs) {
        if (cs == null) {
            return MtfFile.EMPTY;
        }
        int type = cs.getType();
        int index = cs.getIndex();

        StringBuilder toReturn = new StringBuilder();
        if (type == CriticalSlot.TYPE_SYSTEM) {
            if ((getRawSystemName(index).contains("Upper"))
                    || (getRawSystemName(index).contains("Lower"))
                    || (getRawSystemName(index).contains("Hand"))
                    || (getRawSystemName(index).contains("Foot"))) {
                toReturn.append(getRawSystemName(index)).append(" Actuator");
            } else if (getRawSystemName(index).contains("Engine")) {
                toReturn.append("Fusion ").append(getRawSystemName(index));
            } else {
                toReturn.append(getRawSystemName(index));
            }
        } else if (type == CriticalSlot.TYPE_EQUIPMENT) {
            final Mounted m = cs.getMount();
            toReturn.append(m.getType().getInternalName());
            // Superheavy mechs can have a second ammo bin or heat sink in the same slot
            if (cs.getMount2() != null) {
                toReturn.append("|").append(cs.getMount2().getType().getInternalName());
            }
            if ((m.getType() instanceof WeaponType)
                    && m.getType().hasFlag(WeaponType.F_VGL)) {
                switch (m.getFacing()) {
                    case 1:
                        toReturn.append(" (FR)");
                        break;
                    case 2:
                        toReturn.append(" (RR)");
                        break;
                    // case 3:
                    // already handled by isRearMounted() above
                    case 4:
                        toReturn.append(" (RL)");
                        break;
                    case 5:
                        toReturn.append(" (FL)");
                        break;
                    default:
                        // forward facing
                        break;
                }
            }
            if (m.isRearMounted()) {
                toReturn.append(" (R)");
            }
            if (m.isMechTurretMounted()) {
                toReturn.append(" (T)");
            }
            if (m.isOmniPodMounted()) {
                toReturn.append(" ").append(MtfFile.OMNIPOD);
            }
            if (m.getType().isVariableSize()) {
                toReturn.append(MtfFile.SIZE).append(m.getSize());
            }
        } else {
            return "?" + index;
        }
        if (cs.isArmored()) {
            toReturn.append(" ").append(MtfFile.ARMORED);
        }
        return toReturn.toString();
    }

    /**
     * Add the critical slots necessary for a standard cockpit. Note: This is
     * part of the mek creation public API, and might not be referenced by any
     * MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addCockpit() {
        if (getEmptyCriticals(LOC_HEAD) < 5) {
            return false;
        }
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        if (isSuperHeavy()) {
            if (this instanceof TripodMech) {
            setCockpitType(COCKPIT_SUPERHEAVY_TRIPOD);
            } else if (isIndustrial()) {
                setCockpitType(COCKPIT_SUPERHEAVY_INDUSTRIAL);
            } else {
                setCockpitType(COCKPIT_SUPERHEAVY);
            }
        } else if (this instanceof TripodMech) {
            setCockpitType(COCKPIT_TRIPOD);
        } else {
            setCockpitType(COCKPIT_STANDARD);
        }

        return true;
    }

    /**
     * Add the critical slots necessary for an industrial cockpit. Note: This is
     * part of the mek creation public API, and might not be referenced by any
     * MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addIndustrialCockpit() {
        if (getEmptyCriticals(LOC_HEAD) < 5) {
            return false;
        }
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_INDUSTRIAL);
        return true;
    }

    /**
     * Add the critical slots necessary for an industrial cockpit. Note: This is
     * part of the mek creation public API, and might not be referenced by any
     * MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addPrimitiveCockpit() {
        if (getEmptyCriticals(LOC_HEAD) < 5) {
            return false;
        }
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_PRIMITIVE);
        return true;
    }

    /**
     * Add the critical slots necessary for an industrial primitive cockpit.
     * Note: This is part of the mek creation public API, and might not be
     * referenced by any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addIndustrialPrimitiveCockpit() {
        if (getEmptyCriticals(LOC_HEAD) < 5) {
            return false;
        }
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_PRIMITIVE_INDUSTRIAL);
        return true;
    }

    /**
     * Add the critical slots necessary for a small cockpit. Note: This is part
     * of the mek creation public API, and might not be referenced by any
     * MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addSmallCockpit() {
        if (getEmptyCriticals(LOC_HEAD) < 4) {
            return false;
        }
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        setCockpitType(COCKPIT_SMALL);
        return true;
    }

    /**
     * Add the critical slots necessary for a small cockpit. Note: This is part
     * of the mek creation public API, and might not be referenced by any
     * MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addInterfaceCockpit() {
        if (getEmptyCriticals(LOC_HEAD) < 6) {
            return false;
        }
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_INTERFACE);
        return true;
    }

    public boolean addCommandConsole() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_COMMAND_CONSOLE);
        return true;
    }

    public boolean addQuadVeeCockpit() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_QUADVEE);
        return true;
    }

    public boolean addDualCockpit() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_DUAL);
        return true;
    }
	
    public boolean addSuperheavyIndustrialCockpit() {
        if (getEmptyCriticals(LOC_HEAD) < 5) {
            return false;
        }
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_SUPERHEAVY_INDUSTRIAL);
        return true;
    }
	
    public boolean addSuperheavyCommandConsole() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_SUPERHEAVY_COMMAND_CONSOLE);
        return true;
    }
    
    //The location of critical is based on small cockpit, but since command console requires two cockpit slots the second Sensor is return to the location 4.
    public boolean addSmallCommandConsole() {
        if (getEmptyCriticals(LOC_HEAD) < 5) {
            return false;
        }
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_SENSORS));
        setCockpitType(COCKPIT_SMALL_COMMAND_CONSOLE);
        return true;
    }

    /**
     * Add the critical slots necessary for a torso-mounted cockpit. Note: This
     * is part of the mek creation public API, and might not be referenced by
     * any MegaMek code.
     *
     * @param vrpp  if this is a VRPP rather than a standard torso-mounted cockpit
     * @return      false if insufficient critical space
     */
    public boolean addTorsoMountedCockpit(boolean vrpp) {
        boolean success = true;
        if (getEmptyCriticals(LOC_HEAD) < 2) {
            success = false;
        } else {
            addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_SENSORS));
            addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_SENSORS));
        }

        if ((getEmptyCriticals(LOC_CT) < 2) || !success) {
            success = false;
        } else {
            addCritical(LOC_CT, getFirstEmptyCrit(LOC_CT), new CriticalSlot(
                    CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
            if (vrpp) {
                addCritical(LOC_CT, getFirstEmptyCrit(LOC_CT), new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
            } else {
                addCritical(LOC_CT, getFirstEmptyCrit(LOC_CT), new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
            }
        }

        if ((getEmptyCriticals(LOC_LT) < 1) || (getEmptyCriticals(LOC_RT) < 1)
                || !success) {
            success = false;
        } else {
            addCritical(LOC_LT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_LIFE_SUPPORT));
            addCritical(LOC_RT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_LIFE_SUPPORT));
        }

        if (success) {
            if (vrpp) {
                setCockpitType(COCKPIT_VRRP);
            } else {
                setCockpitType(COCKPIT_TORSO_MOUNTED);
            }
        }
        return success;
    }
    
    /**
     * Convenience function that returns the critical slot containing the cockpit
     * @return
     */
    public List<CriticalSlot> getCockpit() {
        List<CriticalSlot> retVal = new ArrayList<>();
        
        switch (cockpitType) {
            // these always occupy slots 2 and 3 in the head
            case Mech.COCKPIT_COMMAND_CONSOLE:
            case Mech.COCKPIT_DUAL:
            case Mech.COCKPIT_SMALL_COMMAND_CONSOLE:
            case Mech.COCKPIT_INTERFACE:
            case Mech.COCKPIT_QUADVEE:
            case Mech.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
                retVal.add(getCritical(Mech.LOC_HEAD, 2));
                retVal.add(getCritical(Mech.LOC_HEAD, 3));
                break;
            case Mech.COCKPIT_TORSO_MOUNTED:
                for (int critIndex = 0; critIndex < getNumberOfCriticals(Mech.LOC_CT); critIndex++) {
                    CriticalSlot slot = getCritical(Mech.LOC_CT, critIndex);
                    if (slot.getIndex() == SYSTEM_COCKPIT) {
                        retVal.add(slot);
                    }
                }
            default:
                retVal.add(getCritical(Mech.LOC_HEAD, 2));
                break;
        }
        
        return retVal;
    }

    /**
     * Determines which crew slot is associated with a particular cockpit critical.
     *
     * @param cs    A cockpit critical slot
     * @return      The crew slot index associated with this critical slot, or -1 to indicate the entire crew.
     */
    public int getCrewForCockpitSlot(int loc, CriticalSlot cs) {
        //For those with split cockpits, count the cockpit criticals in the location until we reach the correct
        //one.
        if (getCockpitType() == COCKPIT_COMMAND_CONSOLE
                || getCockpitType() == COCKPIT_SUPERHEAVY_COMMAND_CONSOLE
	            || getCockpitType() == COCKPIT_SMALL_COMMAND_CONSOLE
                || getCockpitType() == COCKPIT_DUAL
                || getCockpitType() == COCKPIT_QUADVEE) {
            int crewSlot = 0;
            for (int i = 0; i < getNumberOfCriticals(loc); i++) {
                if (getCritical(loc, i) == cs) {
                    return crewSlot;
                } else if (getCritical(loc, i).getIndex() == SYSTEM_COCKPIT) {
                    crewSlot++;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean hasCommandConsoleBonus() {
        return ((getCockpitType() == COCKPIT_COMMAND_CONSOLE) 
		|| (getCockpitType() == COCKPIT_SUPERHEAVY_COMMAND_CONSOLE) 
		|| (getCockpitType() == COCKPIT_SMALL_COMMAND_CONSOLE))
                && getCrew().hasActiveCommandConsole()
                && getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY
                && (!isIndustrial() || hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL));
    }

    /**
     * Add the critical slots necessary for a standard gyro. Also set the gyro
     * type variable. Note: This is part of the mek creation public API, and
     * might not be referenced by any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addGyro() {
        if (getEmptyCriticals(LOC_CT) < (isSuperHeavy() ? 2 : 4)) {
            return false;
        }
        addCompactGyro();
        if (!isSuperHeavy()) {
            addCritical(LOC_CT, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_GYRO));
            addCritical(LOC_CT, 6, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_GYRO));
            setGyroType(GYRO_STANDARD);
        } else {
            setGyroType(GYRO_SUPERHEAVY);
        }
        return true;
    }

    /**
     * Add the critical slots necessary for a standard gyro. Also set the gyro
     * type variable. Note: This is part of the mek creation public API, and
     * might not be referenced by any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addSuperheavyGyro() {
        if (getEmptyCriticals(LOC_CT) < 2) {
            return false;
        }
        addCritical(LOC_CT, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_GYRO));
        if (getEngine().getEngineType() == Engine.COMPACT_ENGINE) {
            addCritical(LOC_CT, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_GYRO));
        } else {
            addCritical(LOC_CT, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_GYRO));
        }
        setGyroType(GYRO_SUPERHEAVY);
        return true;
    }

    /**
     * Add the critical slots necessary for a compact gyro. Also set the gyro
     * type variable. Note: This is part of the mek creation public API, and
     * might not be referenced by any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addCompactGyro() {
        if (getEmptyCriticals(LOC_CT) < 2) {
            return false;
        }
        addCritical(LOC_CT, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_GYRO));
        addCritical(LOC_CT, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_GYRO));
        setGyroType(GYRO_COMPACT);
        return true;
    }

    /**
     * Add the critical slots necessary for an extra-light gyro. Also set the
     * gyro type variable. Note: This is part of the mek creation public API,
     * and might not be referenced by any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addXLGyro() {
        if (getEmptyCriticals(LOC_CT) < 6) {
            return false;
        }
        clearEngineCrits();
        addGyro();
        addCritical(LOC_CT, 7, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_GYRO));
        addCritical(LOC_CT, 8, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_GYRO));
        setGyroType(GYRO_XL);
        addEngineCrits();

        return true;
    }

    /**
     * Add the critical slots necessary for a heavy-duty gyro. Also set the gyro
     * type variable. Note: This is part of the mek creation public API, and
     * might not be referenced by any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addHeavyDutyGyro() {
        if (addGyro()) {
            setGyroType(GYRO_HEAVY_DUTY);
            return true;
        }
        return false;
    }

    /**
     * Add the critical slots necessary for the mek's engine. Calling this
     * method before setting a mek's engine object will result in a NPE. Note:
     * This is part of the mek creation public API, and might not be referenced
     * by any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addEngineCrits() {
        if (!hasEngine()) {
            return true;
        }
        boolean success = true;

        int[] centerSlots = getEngine().getCenterTorsoCriticalSlots(getGyroType());
        if (getEmptyCriticals(LOC_CT) < centerSlots.length) {
            success = false;
        } else {
            for (int centerSlot : centerSlots) {
                addCritical(LOC_CT, centerSlot, new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
            }
        }
        int[] sideSlots = getEngine().getSideTorsoCriticalSlots();
        if ((getEmptyCriticals(LOC_LT) < sideSlots.length)
                || (getEmptyCriticals(LOC_RT) < sideSlots.length) || !success) {
            success = false;
        } else {
            for (int sideSlot : sideSlots) {
                addCritical(LOC_LT, sideSlot, new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
                addCritical(LOC_RT, sideSlot, new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
            }
        }

        return success;
    }

    /**
     * Remove all engine critical slots from the mek. Note: This is part of the
     * mek creation public API, and might not be referenced by any MegaMek code.
     */
    public void clearEngineCrits() {
        for (int i = 0; i < locations(); i++) {
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_ENGINE));
        }
    }

    /**
     * Remove all cockpit critical slots from the mek. Note: This is part of the
     * mek creation public API, and might not be referenced by any MegaMek code.
     */
    public void clearCockpitCrits() {
        for (int i = 0; i < locations(); i++) {
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_LIFE_SUPPORT));
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_SENSORS));
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_COCKPIT));
        }
    }

    /**
     * Remove all gyro critical slots from the mek. Note: This is part of the
     * mek creation public API, and might not be referenced by any MegaMek code.
     */
    public void clearGyroCrits() {
        for (int i = 0; i < locations(); i++) {
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                    SYSTEM_GYRO));
        }
    }

    public int shieldAbsorptionDamage(int damage, int location, boolean rear) {
        int damageAbsorption = damage;
        if (this.hasActiveShield(location, rear)) {
            switch (location) {
                case Mech.LOC_CT:
                case Mech.LOC_HEAD:
                    if (this.hasActiveShield(Mech.LOC_RARM)) {
                        damageAbsorption = getAbsorptionRate(Mech.LOC_RARM,
                                damageAbsorption);
                    }
                    if (this.hasActiveShield(Mech.LOC_LARM)) {
                        damageAbsorption = getAbsorptionRate(Mech.LOC_LARM,
                                damageAbsorption);
                    }
                    break;
                case Mech.LOC_LARM:
                case Mech.LOC_LT:
                case Mech.LOC_LLEG:
                    if (this.hasActiveShield(Mech.LOC_LARM)) {
                        damageAbsorption = getAbsorptionRate(Mech.LOC_LARM,
                                damageAbsorption);
                    }
                    break;
                default:
                    if (this.hasActiveShield(Mech.LOC_RARM)) {
                        damageAbsorption = getAbsorptionRate(Mech.LOC_RARM,
                                damageAbsorption);
                    }
                    break;
            }
        }

        if (this.hasPassiveShield(location, rear)) {
            switch (location) {
                case Mech.LOC_LARM:
                case Mech.LOC_LT:
                    if (this.hasPassiveShield(Mech.LOC_LARM)) {
                        damageAbsorption = getAbsorptionRate(Mech.LOC_LARM,
                                damageAbsorption);
                    }
                    break;
                case Mech.LOC_RARM:
                case Mech.LOC_RT:
                    if (this.hasPassiveShield(Mech.LOC_RARM)) {
                        damageAbsorption = getAbsorptionRate(Mech.LOC_RARM,
                                damageAbsorption);
                    }
                    break;
                default:
                    break;
            }
        }
        if (hasNoDefenseShield(location)) {
            switch (location) {
                case Mech.LOC_LARM:
                    if (hasNoDefenseShield(Mech.LOC_LARM)) {
                        damageAbsorption = getAbsorptionRate(Mech.LOC_LARM,
                                damageAbsorption);
                    }
                    break;
                case Mech.LOC_RARM:
                    if (hasNoDefenseShield(Mech.LOC_RARM)) {
                        damageAbsorption = getAbsorptionRate(Mech.LOC_RARM,
                                damageAbsorption);
                    }
                    break;
                default:
                    break;
            }
        }

        return Math.max(0, damageAbsorption);
    }

    private int getAbsorptionRate(int location, int damage) {
        int rate = damage;

        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return rate;
        }

        if (damage <= 0) {
            return 0;
        }

        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);

            if (cs == null) {
                continue;
            }

            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }

            if (cs.isDamaged()) {
                continue;
            }

            Mounted m = cs.getMount();

            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                rate -= m.getDamageAbsorption(this, m.getLocation());
                m.damageTaken++;
                return Math.max(0, rate);
            }
        }

        return rate;
    }

    /**
     * Does this mech have an undamaged HarJel system in this location?
     *
     * @param loc
     *            the <code>int</code> location to check
     * @return a <code>boolean</code> value indicating a present HarJel system
     */
    public boolean hasHarJelIIIn(int loc) {
        for (Mounted mounted : getMisc()) {
            if ((mounted.getLocation() == loc) && mounted.isReady()
                    && (mounted.getType().hasFlag(MiscType.F_HARJEL_II))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does this mech have an undamaged HarJel system in this location?
     *
     * @param loc
     *            the <code>int</code> location to check
     * @return a <code>boolean</code> value indicating a present HarJel system
     */
    public boolean hasHarJelIIIIn(int loc) {
        for (Mounted mounted : getMisc()) {
            if ((mounted.getLocation() == loc) && mounted.isReady()
                    && (mounted.getType().hasFlag(MiscType.F_HARJEL_III))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setGrappled(int id, boolean attacker) {
        grappled_id = id;
        isGrappleAttacker = attacker;
    }

    @Override
    public boolean isGrappleAttacker() {
        return isGrappleAttacker;
    }

    @Override
    public int getGrappled() {
        return grappled_id;
    }

    @Override
    public boolean isGrappledThisRound() {
        return grappledThisRound;
    }

    @Override
    public void setGrappledThisRound(boolean grappled) {
        grappledThisRound = grappled;
    }

    @Override
    public boolean isEligibleForMovement() {
        // For normal grapples, neither unit can move
        // If the grapple is caused by a chain whip, then the attacker can move
        // (this breaks the grapple), TO pg 289
        if ((grappled_id != Entity.NONE)
                && (!isChainWhipGrappled() || !isGrappleAttacker())) {
            return false;
        }
        return super.isEligibleForMovement();
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }

    @Override
    public void destroyLocation(int loc) {
        destroyLocation(loc, false);
    }

    @Override
    public void destroyLocation(int loc, boolean blownOff) {
        // If it's already destroyed, don't bother -- as of 12/06/28, super.
        // destroyLocation() will just return having done nothing itself and
        // then we'd potentially end up with a second PSR for an
        // already-destroyed
        // leg.
        if (getInternal(loc) < 0) {
            return;
        }
        super.destroyLocation(loc, blownOff);
        // if it's a leg, the entity falls
        if (locationIsLeg(loc) && canFall()) {
            game.addPSR(new PilotingRollData(getId(),
                    TargetRoll.AUTOMATIC_FAIL, 5, "leg destroyed"));
        }
    }

    @Override
    public boolean hasCASEII(int location) {
        for (Mounted mount : getEquipment()) {
            if ((mount.getLocation() == location)
                    && (mount.getType() instanceof MiscType)
                    && mount.getType().hasFlag(MiscType.F_CASEII)) {
                return true;
            }
        }
        return false;
    }

    /* Check to see if case II exists anywhere on the mech */
    public boolean hasCASEIIAnywhere() {
        for (Mounted mount : getEquipment()) {
            if ((mount.getType() instanceof MiscType)
                    && mount.getType().hasFlag(MiscType.F_CASEII)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setGrappleSide(int side) {
        grappledSide = side;
    }

    @Override
    public int getGrappleSide() {
        return grappledSide;
    }

    @Override
    public boolean isCarefulStand() {
        return isCarefulStanding;
    }

    @Override
    public int getCoolantFailureAmount() {
        return heatSinkCoolantFailureFactor;
    }

    @Override
    public void addCoolantFailureAmount(int amount) {
        heatSinkCoolantFailureFactor += amount;
    }

    @Override
    public void resetCoolantFailureAmount() {
        heatSinkCoolantFailureFactor = 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 1 + getExtraCommGearTons();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getHQIniBonus()
     */
    @Override
    public int getHQIniBonus() {
        int bonus = super.getHQIniBonus();
        if (((getGyroHits() > 0) || hasHipCrit()) && (mpUsedLastRound > 0)) {
            return 0;
        }
        return bonus;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getBARRating()
     */
    @Override
    public int getBARRating(int loc) {
        if (armorType[loc] == EquipmentType.T_ARMOR_COMMERCIAL) {
            return 5;
        }
        if ((armorType[loc] == EquipmentType.T_ARMOR_INDUSTRIAL)
                || (armorType[loc] == EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL)) {
            return 10;
        }
        return 10;
    }

    /**
     * Is this an Industrial Mech?
     *
     * @return if this mech has an industrial inner structure
     */
    public boolean isIndustrial() {
        return getStructureType() == EquipmentType.T_STRUCTURE_INDUSTRIAL;
    }

    /**
     * set if this mech just moved into water that would kill it because of the
     * lack of environmental sealing
     *
     * @param moved
     */
    public void setJustMovedIntoIndustrialKillingWater(boolean moved) {
        justMovedIntoIndustrialKillingWater = moved;
    }

    /**
     * did this mech just moved into water that would kill it because we lack
     * environmental sealing?
     *
     * @return
     */
    public boolean isJustMovedIntoIndustrialKillingWater() {
        return justMovedIntoIndustrialKillingWater;
    }

    /**
     * should this mech die at the end of turn because it's an IndustrialMech
     * without environmental sealing that moved into water last round and stayed
     * there?
     *
     * @return
     */
    public boolean shouldDieAtEndOfTurnBecauseOfWater() {
        return shouldDieAtEndOfTurnBecauseOfWater;
    }

    /**
     * Set if this Mech's ICE Engine is stalled or not should only be used for
     * industrial mechs carrying an ICE engine
     *
     * @param stalled
     */
    public void setStalled(boolean stalled) {
        this.stalled = stalled;
        stalledThisTurn = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#isStalled()
     */
    @Override
    public boolean isStalled() {
        return stalled;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#isShutDown()
     */
    @Override
    public boolean isShutDown() {
        return super.isShutDown() || isStalled();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#doCheckEngineStallRoll(java.util.Vector)
     */
    @Override
    public Vector<Report> doCheckEngineStallRoll(Vector<Report> vPhaseReport) {
        if (hasEngine() && (getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            Report r = new Report(2280);
            r.addDesc(this);
            r.subject = getId();
            r.add(1);
            r.add("ICE-Engine 'Mech failed a PSR");
            vPhaseReport.add(r);
            r = new Report(2285);
            r.subject = getId();
            // Stall check is made against unmodified Piloting skill...
            PilotingRollData base = new PilotingRollData(getId(), getCrew()
                    .getPiloting(), "Base piloting skill");
            // ...but dead or unconscious pilots should still auto-fail.
            if (getCrew().isDead() || getCrew().isDoomed()
                    || (getCrew().getHits() >= 6)) {
                base = new PilotingRollData(getId(), TargetRoll.AUTOMATIC_FAIL,
                        "Pilot dead");
            } else if (!getCrew().isActive()) {
                base = new PilotingRollData(getId(), TargetRoll.IMPOSSIBLE,
                        "Pilot unconscious");
            }
            r.add(base.getValueAsString());
            r.add(base.getDesc());
            vPhaseReport.add(r);
            r = new Report(2290);
            r.subject = getId();
            r.indent();
            r.newlines = 0;
            r.add(1);
            r.add(base.getPlainDesc());
            vPhaseReport.add(r);
            int diceRoll = getCrew().rollPilotingSkill();
            r = new Report(2300);
            r.subject = getId();
            r.add(base);
            r.add(diceRoll);
            if (diceRoll < base.getValue()) {
                r.choose(false);
                setStalled(true);
                r.newlines = 0;
                vPhaseReport.add(r);
                r = new Report(2303);
                r.subject = getId();
                vPhaseReport.add(r);
            } else {
                r.choose(true);
                vPhaseReport.add(r);
            }
        }
        return vPhaseReport;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#checkUnstall(java.util.Vector)
     */
    @Override
    public void checkUnstall(Vector<Report> vPhaseReport) {
        if (stalled && !stalledThisTurn && hasEngine()
                && (getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            Report r = new Report(2280);
            r.addDesc(this);
            r.subject = getId();
            r.add(1);
            r.add("unstall stalled ICE engine");
            vPhaseReport.add(r);
            r = new Report(2285);
            r.subject = getId();
            // Unstall check is made against unmodified Piloting skill...
            PilotingRollData base = new PilotingRollData(getId(), getCrew()
                    .getPiloting(), "Base piloting skill");
            // ...but dead or unconscious pilots should still auto-fail, same as
            // for stalling.
            if (getCrew().isDead() || getCrew().isDoomed()
                    || (getCrew().getHits() >= 6)) {
                base = new PilotingRollData(getId(), TargetRoll.AUTOMATIC_FAIL,
                        "Pilot dead");
            } else if (!getCrew().isActive()) {
                base = new PilotingRollData(getId(), TargetRoll.IMPOSSIBLE,
                        "Pilot unconscious");
            }
            r.add(base.getValueAsString());
            r.add(base.getDesc());
            vPhaseReport.add(r);
            r = new Report(2290);
            r.subject = getId();
            r.indent();
            r.newlines = 0;
            r.add(1);
            r.add(base.getPlainDesc());
            vPhaseReport.add(r);
            int diceRoll = getCrew().rollPilotingSkill();
            r = new Report(2300);
            r.subject = getId();
            r.add(base);
            r.add(diceRoll);
            if (diceRoll < base.getValue()) {
                r.choose(false);
                vPhaseReport.add(r);
            } else {
                r.choose(true);
                r.newlines = 0;
                vPhaseReport.add(r);
                setStalled(false);
                r = new Report(2304);
                r.subject = getId();
                vPhaseReport.add(r);
            }
        }
    }

    @Override
    public boolean isPrimitive() {
        return (getCockpitType() == Mech.COCKPIT_PRIMITIVE)
                || (getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL);
    }

    private int getFirstEmptyCrit(int Location) {
        for (int i = 0; i < getNumberOfCriticals(Location); i++) {
            if (getCritical(Location, i) == null) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasArmoredCockpit() {

        int location = getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED ? Mech.LOC_CT
                : Mech.LOC_HEAD;

        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)
                    && (cs.getIndex() == Mech.SYSTEM_COCKPIT)) {
                return cs.isArmored();
            }
        }

        return false;
    }

    public boolean hasArmoredGyro() {
        for (int slot = 0; slot < getNumberOfCriticals(LOC_CT); slot++) {
            CriticalSlot cs = getCritical(LOC_CT, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)
                    && (cs.getIndex() == Mech.SYSTEM_GYRO)) {
                return cs.isArmored();
            }
        }

        return false;
    }

    @Override
    public boolean hasArmoredEngine() {
        for (int slot = 0; slot < getNumberOfCriticals(LOC_CT); slot++) {
            CriticalSlot cs = getCritical(LOC_CT, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)
                    && (cs.getIndex() == Mech.SYSTEM_ENGINE)) {
                return cs.isArmored();
            }
        }
        return false;
    }

    /**
     * should this mech check for a critical hit at the end of turn due to being
     * an industrial mech and having been the target of a successful physical
     * attack or for falling
     */
    public boolean isCheckForCrit() {
        return checkForCrit;
    }

    /**
     * how many levels did this mech fall this turn?
     *
     * @return
     */
    public int getLevelsFallen() {
        return levelsFallen;
    }

    public void setLevelsFallen(int levels) {
        levelsFallen = levels;
    }

    public void setCheckForCrit(boolean check) {
        checkForCrit = check;
    }

    /**
     * Is the passed in location an arm?
     *
     * @param loc
     * @return
     */
    public boolean isArm(int loc) {
        return (loc == Mech.LOC_LARM) || (loc == Mech.LOC_RARM);
    }

    public double getArmoredComponentBV() {
        double bv = 0.0f;

        // all equipment gets 5% of BV cost per slot, or a flat +5 per slot
        // if BV is 0
        for (Mounted mount : getEquipment()) {
            if (!mount.isArmored()
                    || ((mount.getType() instanceof MiscType) && mount.getType().hasFlag(MiscType.F_PPC_CAPACITOR))) {
                continue;
            }
            double mountBv = mount.getType().getBV(this);
            if ((mount.getType() instanceof PPCWeapon)
                    && (mount.getLinkedBy() != null)) {
                mountBv += ((MiscType) mount.getLinkedBy().getType()).getBV(
                        this, mount);
                bv += mountBv * 0.05 * (mount.getCriticals() + 1);
            } else if (mountBv > 0) {
                bv += mountBv * 0.05 * mount.getCriticals();
            } else {
                bv += 5 * mount.getCriticals();
            }
        }

        for (int location = 0; location < locations(); location++) {
            for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
                CriticalSlot cs = getCritical(location, slot);
                if ((cs != null) && cs.isArmored()
                        && (cs.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    // gyro is the only system that has it's own BV
                    if ((cs.getIndex() == Mech.SYSTEM_GYRO)) {
                        bv += getWeight() * getGyroMultiplier() * 0.05;
                    } else {
                        // System Crit that is armored but does not normally
                        // have a BV
                        bv += 5;
                    }
                }
            }
        }
        return bv;
    }

    public double getGyroMultiplier() {
        if ((getGyroType() == GYRO_HEAVY_DUTY)) {
            return 1.0;
        }
        if (getGyroType() == GYRO_NONE && getCockpitType() != COCKPIT_INTERFACE) {
            return 0;
        }
        return 0.5;
    }

    public void setFullHeadEject(boolean fullHeadEject) {
        this.fullHeadEject = fullHeadEject;
    }

    public boolean hasFullHeadEject() {
        return fullHeadEject;
    }

    /**
     * Start of Battle Force Conversion Methods
     */

    @Override
    public double getBaseBattleForceMovement() {
        double move = getOriginalWalkMP();

        if (hasMASCAndSuperCharger()) {
            move *= 1.5;
        } else if (hasMASC()) {
            move *= 1.25;
        }

        if (hasMPReducingHardenedArmor()) {
            move--;
        }

        if (getMisc().stream().filter(m -> m.getType().hasFlag(MiscType.F_CLUB))
        		.map(m -> m.getType().getSubType())
        		.anyMatch(st -> st == MiscType.S_SHIELD_LARGE || st == MiscType.S_SHIELD_MEDIUM)) {
            move--;
        }
        return move;
    }

    @Override
    /*
     * returns the battle force structure points for a mech
     */
    public int getBattleForceStructurePoints() {
        int battleForceStructure = 0;
        int battleForceEngineType = 0;

        int[][] battleForceStructureTable = new int[][] {
                { 1, 1, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 8, 8,
                    8, 8, 9, 9, 10, 10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15 },
                { 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 7, 7, 8, 8, 9, 10, 10, 10,
                    11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20 },
                { 1, 1, 1, 2, 2, 2, 2, 3, 3, 4, 4, 4, 4, 5, 5, 5, 6, 6, 6,
                    6, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 11, 11, 12, 12 },
                { 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5,
                    5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10 },
                { 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4,
                    5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 8 ,8, 8, 8, 8, 9 },
                { 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                    4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 8 },
                { 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3,
                    3, 3, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6 },
                { 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3,
                    4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7 },
                { 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3,
                    3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5 } };

        if (hasEngine()) {
            if (isClan()) {
                if (getEngine().hasFlag(Engine.LARGE_ENGINE)) {
                    switch (getEngine().getEngineType()) {
                        case Engine.XL_ENGINE:
                            battleForceEngineType = 5;
                            break;
                        case Engine.XXL_ENGINE:
                            battleForceEngineType = 8;
                            break;
                    }
                } else {
                    switch (getEngine().getEngineType()) {
                        case Engine.XL_ENGINE:
                            battleForceEngineType = 4;
                            break;
                        case Engine.XXL_ENGINE:
                            battleForceEngineType = 6;
                            break;
                        default:
                            battleForceEngineType = 1;
                            break;
                    }
                }
            } else {
                if (getEngine().hasFlag(Engine.LARGE_ENGINE)) {
                    switch (getEngine().getEngineType()) {
                        case Engine.XL_ENGINE:
                            battleForceEngineType = 5;
                            break;
                        case Engine.XXL_ENGINE:
                            battleForceEngineType = 9;
                            break;
                        case Engine.LIGHT_ENGINE:
                            battleForceEngineType = 5;
                            break;
                        default:
                            battleForceEngineType = 3;
                            break;
                    }
                } else {
                    switch (getEngine().getEngineType()) {
                        case Engine.XL_ENGINE:
                            battleForceEngineType = 5;
                            break;
                        case Engine.COMPACT_ENGINE:
                            battleForceEngineType = 2;
                            break;
                        case Engine.LIGHT_ENGINE:
                            battleForceEngineType = 4;
                            break;
                        case Engine.XXL_ENGINE:
                            battleForceEngineType = 7;
                            break;
                        default:
                            battleForceEngineType = 1;
                            break;
                    }
                }
            }
        }

        battleForceStructure = battleForceStructureTable[battleForceEngineType - 1][((int) getWeight() / 5) - 2];

        if (getStructureType() == EquipmentType.T_STRUCTURE_COMPOSITE) {
            battleForceStructure = (int) Math.ceil(battleForceStructure * .5);
        } else if (getStructureType() == EquipmentType.T_STRUCTURE_REINFORCED) {
            battleForceStructure *= 2;
        }
        return battleForceStructure;

    }

    @Override
    public int getNumBattleForceWeaponsLocations() {
        return 2;
    }

    @Override
    public double getBattleForceLocationMultiplier(int index, int location, boolean rearMounted) {
        if ((index == 0 && !rearMounted
                || (index == 1) && rearMounted)) {
            return 1.0;
        }
        return 0;
    }

    @Override
    public String getBattleForceLocationName(int index) {
        if (index == 1) {
            return "REAR";
        }
        return "";
    }

    @Override
    public boolean isBattleForceRearLocation(int index) {
        return index == 1;
    }

    @Override
    public int getBattleForceTotalHeatGeneration(boolean allowRear) {
        int totalHeat = 0;

        // finish the max heat calculations
        if (this.getJumpMP() > 0) {
            totalHeat += getJumpHeat(getJumpMP());
        } else if (!isIndustrial() && hasEngine()) {
            totalHeat += getEngine().getRunHeat(this);
        }

        for (Mounted mount : getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon.hasFlag(WeaponType.F_ONESHOT)
                || (allowRear && !mount.isRearMounted())
                || (!allowRear && mount.isRearMounted())) {
                continue;
            }
            if (weapon.getAmmoType() == AmmoType.T_AC_ROTARY) {
                totalHeat += weapon.getHeat() * 6;
            } else if (weapon.getAmmoType() == AmmoType.T_AC_ULTRA
                    || weapon.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                totalHeat += weapon.getHeat() * 2;
            } else {
                totalHeat += weapon.getHeat();
            }
        }

        if (hasWorkingMisc(MiscType.F_STEALTH, -1)) {
            totalHeat += 10;
        }

        return totalHeat;
    }

    @Override
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        specialAbilities.put(BattleForceSPA.SRCH, null);
        for (Mounted m : getEquipment()) {
            if (!(m.getType() instanceof MiscType)) {
                continue;
            }
            if (m.getType().hasFlag(MiscType.F_HARJEL)) {
                specialAbilities.put(BattleForceSPA.BHJ, null);
            } else if (m.getType().hasFlag(MiscType.F_HARJEL_II)) {
                specialAbilities.put(BattleForceSPA.BHJ2, null);
            } else if (m.getType().hasFlag(MiscType.F_HARJEL_III)) {
                specialAbilities.put(BattleForceSPA.BHJ3, null);
            } else if (((MiscType) m.getType()).isShield()) {
                specialAbilities.put(BattleForceSPA.SHLD, null);
            } else if (m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                specialAbilities.put(BattleForceSPA.ITSM, null);
            } else if (m.getType().hasFlag(MiscType.F_TSM)) {
                specialAbilities.put(BattleForceSPA.TSM, null);
            } else if (m.getType().hasFlag(MiscType.F_VOIDSIG)) {
                specialAbilities.put(BattleForceSPA.MAS, null);
            } else if (isIndustrial() && m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)
                    && getEngine().getEngineType() != Engine.COMBUSTION_ENGINE) {
                specialAbilities.put(BattleForceSPA.SOA, null);
            } else if (m.getType().hasFlag(MiscType.F_NULLSIG)
                    || m.getType().hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                specialAbilities.put(BattleForceSPA.STL, null);
                specialAbilities.put(BattleForceSPA.ECM, null);
            } else if (m.getType().hasFlag(MiscType.F_UMU)) {
                specialAbilities.put(BattleForceSPA.UMU, null);
            } else if (m.getType().hasFlag(MiscType.F_BATTLEMECH_NIU)) {
                specialAbilities.put(BattleForceSPA.DN, null);
            }
        }
        if (getCockpitType() == COCKPIT_COMMAND_CONSOLE) {
            specialAbilities.merge(BattleForceSPA.MHQ, 1, Integer::sum);
        } else if (getCockpitType() == COCKPIT_SUPERHEAVY_COMMAND_CONSOLE) {
            specialAbilities.merge(BattleForceSPA.MHQ, 1, Integer::sum);
        } else if (getCockpitType() == COCKPIT_SMALL_COMMAND_CONSOLE) {
            specialAbilities.merge(BattleForceSPA.MHQ, 1, Integer::sum);
        } else if (getCockpitType() == COCKPIT_VRRP) {
            specialAbilities.merge(BattleForceSPA.VR, 1, Integer::sum);
        }
        if (isIndustrial()) {
            if (getCockpitType() == Mech.COCKPIT_STANDARD) {
                specialAbilities.put(BattleForceSPA.AFC, null);
            } else {
                specialAbilities.put(BattleForceSPA.BFC, null);
            }
        } else {
            specialAbilities.put(BattleForceSPA.SOA, null);
        }
    }

    public abstract boolean hasMPReducingHardenedArmor();

    /**
     * End of Battle Force Conversion Methods
     */

    @Override
    public int getEngineHits() {
        int engineHits = 0;
        engineHits += getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                Mech.SYSTEM_ENGINE, Mech.LOC_CT);
        engineHits += getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                Mech.SYSTEM_ENGINE, Mech.LOC_RT);
        engineHits += getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                Mech.SYSTEM_ENGINE, Mech.LOC_LT);
        return engineHits;
    }

    public int getGyroHits() {
        return getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                Mech.LOC_CT);
    }

    @Override
    public boolean isGyroDestroyed() {
        if (getGyroType() == GYRO_HEAVY_DUTY) {
            return getGyroHits() > 2;
        } else {
            return getGyroHits() > 1;
        }
    }

    @Override
    public String getLocationDamage(int loc) {
        String toReturn = "";
        boolean first = true;
        if (isLocationBlownOff(loc)) {
            toReturn += "BLOWN OFF";
            first = false;
        }

        if (isLocationTrulyDestroyed(loc)) {
            return toReturn;
        }

        if (getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "BREACH";
            first = false;
        }

        if (hasSystem(SYSTEM_LIFE_SUPPORT, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        SYSTEM_LIFE_SUPPORT, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Life Spt.";
            first = false;
        }

        if (hasSystem(SYSTEM_SENSORS, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        SYSTEM_SENSORS, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Sensors";
            first = false;
        }

        if (hasSystem(SYSTEM_COCKPIT, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        SYSTEM_COCKPIT, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Cockpit";
            first = false;
        }

        if (hasSystem(ACTUATOR_SHOULDER, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        ACTUATOR_SHOULDER, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Shoulder";
            first = false;
        }

        if (hasSystem(ACTUATOR_UPPER_ARM, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        ACTUATOR_UPPER_ARM, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Upper Arm";
            first = false;
        }

        if (hasSystem(ACTUATOR_LOWER_ARM, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        ACTUATOR_LOWER_ARM, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Lower Arm";
            first = false;
        }

        if (hasSystem(ACTUATOR_HAND, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        ACTUATOR_HAND, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Hand";
            first = false;
        }

        if (hasSystem(ACTUATOR_HIP, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP,
                        loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Hip";
            first = false;
        }

        if (hasSystem(ACTUATOR_UPPER_LEG, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        ACTUATOR_UPPER_LEG, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Upper Leg";
            first = false;
        }

        if (hasSystem(ACTUATOR_LOWER_LEG, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        ACTUATOR_LOWER_LEG, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Lower Leg";
            first = false;
        }

        if (hasSystem(ACTUATOR_FOOT, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        ACTUATOR_FOOT, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Foot";
            first = false;
        }

        if ((getEntityType() & ETYPE_QUADVEE) != 0
                && hasSystem(QuadVee.SYSTEM_CONVERSION_GEAR, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        QuadVee.SYSTEM_CONVERSION_GEAR, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Conversion Gear";
            first = false;
        }

        if ((getEntityType() & ETYPE_LAND_AIR_MECH) != 0
                && hasSystem(LandAirMech.LAM_AVIONICS, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        LandAirMech.LAM_AVIONICS, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Avionics";
            first = false;
        }

        if ((getEntityType() & ETYPE_LAND_AIR_MECH) != 0
                && hasSystem(LandAirMech.LAM_LANDING_GEAR, loc)
                && (getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        LandAirMech.LAM_LANDING_GEAR, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Landing Gear";
        }

        return toReturn;
    }

    @Override
    public boolean isCrippled() {
        return isCrippled(false);
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        if (countInternalDamagedLimbs() >= 3) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: 3+ limbs have taken internals.");
            return true;
        }

        if (countInternalDamagedTorsos() >= 2) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: 2+ torsos have taken internals.");
            return true;
        }

        if (isLocationBad(LOC_LT)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Left Torso destroyed.");
            return true;
        }

        if (isLocationBad(LOC_RT)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Right Torso destroyed.");
            return true;
        }

        if (getEngineHits() >= 2) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: 2 or more Engine Hits.");
            return true;

        }

        if ((getEngineHits() == 1) && (getGyroHits() == 1)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Engine + Gyro hit.");
            return true;
        }

        if (getHitCriticals(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS, LOC_HEAD) > 1) {
            // If the cockpit isn't torso-mounted, we're done; if it is, we
            // need to look at the CT sensor slot as well.
            if ((getCockpitType() != COCKPIT_TORSO_MOUNTED)
                    || (getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                            SYSTEM_SENSORS, LOC_CT) > 0)) {
                LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Sensors destroyed.");
                return true;
            }
        }

        if ((getCrew() != null) && (getCrew().getHits() >= 4)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Pilot has taken 4+ damage.");
            return true;
        }

        if (isPermanentlyImmobilized(checkCrew)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Immobilized.");
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        // no weapons can fire anymore, can cause no more than 5 points of
        // combined weapons damage,
        // or has no weapons with range greater than 5 hexes
        if (!hasViableWeapons()) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: has no more viable weapons.");
            return true;
        }
        return false;
    }

    private int countInternalDamagedTorsos() {
        int count = 0;
        if ((getOInternal(LOC_CT) > getInternal(LOC_CT))
                && (getArmor(LOC_CT) < 1)) {
            count++;
        }
        if ((getOInternal(LOC_LT) > getInternal(LOC_LT))
                && (getArmor(LOC_LT) < 1)) {
            count++;
        }
        if ((getOInternal(LOC_RT) > getInternal(LOC_RT))
                && (getArmor(LOC_RT) < 1)) {
            count++;
        }
        return count;
    }

    private int countInternalDamagedLimbs() {
        int count = 0;
        if (getOInternal(LOC_RLEG) > getInternal(LOC_RLEG)) {
            count++;
        }
        if (getOInternal(LOC_LLEG) > getInternal(LOC_LLEG)) {
            count++;
        }
        if (getOInternal(LOC_LARM) > getInternal(LOC_LARM)) {
            count++;
        }
        if (getOInternal(LOC_RARM) > getInternal(LOC_RARM)) {
            count++;
        }
        return count;
    }

    @Override
    public boolean canEscape() {
        int hipHits = 0;
        int legsDestroyed = 0;
        for (int i = 0; i < locations(); i++) {
            if (locationIsLeg(i)) {
                if (!isLocationBad(i)) {
                    if (legHasHipCrit(i)) {
                        hipHits++;
                    }
                } else {
                    legsDestroyed++;
                }
            }
        }
        // there is room for debate here but I think most people would agree that a
        // legged biped mech (and a double legged quad mech) or a hipped mech are not
        // escapable, although technically they still have as much MP as foot infantry which
        // can escape. We could also consider creating options to control this.
        if (((this instanceof BipedMech) && (legsDestroyed > 0))
                || (legsDestroyed > 1) || (hipHits > 0)) {
            return false;
        }
        return super.canEscape();
    }

    @Override
    public boolean isPermanentlyImmobilized(boolean checkCrew) {
        // First check for conditions that would permanently immobilize *any*
        // entity; if we find any, we're already done.
        if (super.isPermanentlyImmobilized(checkCrew)) {
            return true;
        }
        // If we're prone and base walking MP -- adjusted for gravity and
        // modular armor since they're reasonably permanent but ignoring heat
        // effects -- have dropped to 0, we're stuck even if we still have
        // jump jets because we can't get up anymore to *use* them.
        if ((getWalkMP(true, true, false) <= 0) && isProne()) {
            return true;
        }
        // Gyro destroyed? TW p. 258 at least heavily implies that that counts
        // as being immobilized as well, which makes sense because the 'Mech
        // certainly isn't leaving that hex under its own power anymore.
        int hitsToDestroyGyro = (gyroType == GYRO_HEAVY_DUTY) ? 3 : 2;
        return getGyroHits() >= hitsToDestroyGyro;
    }

    @Override
    public boolean isDmgHeavy() {
        if (((double) getArmor(LOC_HEAD) / getOArmor(LOC_HEAD)) <= 0.33) {
            LogManager.getLogger().debug(getDisplayName() + " HEAVY DAMAGE: Less than 1/3 head armor remaining");
            return true;
        }

        if (getArmorRemainingPercent() <= 0.25) {
            LogManager.getLogger().debug(getDisplayName() + " HEAVY DAMAGE: Less than 25% armor remaining");
            return true;
        }

        if (countInternalDamagedLimbs() == 2) {
            LogManager.getLogger().debug(getDisplayName() + " HEAVY DAMAGE: Two limbs with internal damage");
            return true;
        }

        if (countInternalDamagedTorsos() == 1) {
            LogManager.getLogger().debug(getDisplayName() + " HEAVY DAMAGE: Torso internal damage");
            return true;
        }

        if (getEngineHits() == 1) {
            LogManager.getLogger().debug(getDisplayName() + " HEAVY DAMAGE: Engine hit");
            return true;
        }

        if (getGyroHits() == 1) {
            LogManager.getLogger().debug(getDisplayName() + " HEAVY DAMAGE: Gyro hit");
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 3)) {
            LogManager.getLogger().debug(getDisplayName() + " Three crew hits");
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }
        if (((double) totalInoperable / totalWeapons) >= 0.75) {
            LogManager.getLogger().debug(getDisplayName() + " HEAVY DAMAGE: Less than 25% weapons operable");
            return true;
        }
        return false;
    }

    @Override
    public boolean isDmgModerate() {
        if (((double) getArmor(LOC_HEAD) / getOArmor(LOC_HEAD)) <= 0.67) {
            LogManager.getLogger().debug(getDisplayName() + " MODERATE DAMAGE: Less than 2/3 head armor");
            return true;
        }

        if (getArmorRemainingPercent() <= 0.5) {
            LogManager.getLogger().debug(getDisplayName() + " MODERATE DAMAGE: Less than 50% armor");
            return true;
        }

        if (countInternalDamagedLimbs() == 1) {
            LogManager.getLogger().debug(getDisplayName() + " MODERATE DAMAGE: Limb with internal damage");
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 2)) {
            LogManager.getLogger().debug(getDisplayName() + " MODERATE DAMAGE: 2 crew hits");
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }

        if (((double) totalInoperable / totalWeapons) >= 0.5) {
            LogManager.getLogger().debug(getDisplayName() + " MODERATE DAMAGE: Less than 50% weapons operable");
            return true;
        }
        return false;
    }

    @Override
    public boolean isDmgLight() {
        if (getArmor(LOC_HEAD) < getOArmor(LOC_HEAD)) {
            LogManager.getLogger().debug(getDisplayName() + " LIGHT DAMAGE: head armor damaged");
            return true;
        }

        if (getArmorRemainingPercent() <= 0.75) {
            LogManager.getLogger().debug(getDisplayName() + " LIGHT DAMAGE: less than 75% armor remaining");
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 1)) {
            LogManager.getLogger().debug(getDisplayName() + " LIGHT DAMAGE: crew hit");
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }

        if (((double) totalInoperable / totalWeapons) >= 0.5) {
            LogManager.getLogger().debug(getDisplayName() + " LIGHT DAMAGE: Less than 75% weapons operable");
            return true;
        }
        return false;
    }

    public boolean hasCompactHeatSinks() {
        for (Mounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_COMPACT_HEAT_SINK)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Report the location as destroyed if blown off in a previous phase, doomed
     * if blown off in this one.
     */
    @Override
    public int getInternal(int loc) {
        if (isLocationBlownOff(loc)) {
            return isLocationBlownOffThisPhase(loc) ? IArmorState.ARMOR_DOOMED
                    : IArmorState.ARMOR_DESTROYED;
        }
        return super.getInternal(loc);
    }

    @Override
    public boolean isSuperHeavy() {
        return weight > 100;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_MECH;
    }

    @Override
    public boolean isEjectionPossible() {
        return (getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED)
                && getCrew().isActive() && !hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT);
    }

    /**
     * Check to see if a mech has a claw in one of its arms
     *
     * @param location
     *            (LOC_RARM or LOC_LARM)
     * @return True/False
     */
    public boolean hasClaw(int location) {
        // only arms have claws.
        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return false;
        }
        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);

            if (cs == null) {
                continue;
            }
            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }
            Mounted m = cs.getMount();
            EquipmentType type = m.getType();
            if ((type instanceof MiscType)
                    && type.hasFlag(MiscType.F_HAND_WEAPON)
                    && type.hasSubType(MiscType.S_CLAW)) {
                return !(m.isDestroyed() || m.isMissing() || m.isBreached());
            }
        }
        return false;
    }

    /**
     * Check whether a mech has intact heat-dissipating armor in every location
     * thus protecting it from external heat sources like fires or magma
     *
     * @return True/False
     */
    public boolean hasIntactHeatDissipatingArmor() {
        for (int loc = 0; loc < locations(); ++loc) {
            if ((getArmor(loc) < 1)
                || (getArmorType(loc) != EquipmentType.T_ARMOR_HEAT_DISSIPATING)) {
                return false;
            }
        }
        return true;
    }


    /**
     * return if a RISC emergency coolant failed its roll
     * @param vDesc
     * @param vCriticals
     * @return
     */
    public boolean doRISCEmergencyCoolantCheckFor(Vector<Report> vDesc,
            HashMap<Integer, List<CriticalSlot>> vCriticals) {
        Mounted coolantSystem = null;
        for (Mounted misc : getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)
                    && !misc.isInoperable()) {
                coolantSystem = misc;
            }
        }
        if (coolantSystem != null) {
            boolean bFailure = false;
            int nRoll = Compute.d6(2);
            bUsedCoolantSystem = true;
            Report r = new Report(2365);
            r.subject = getId();
            r.addDesc(this);
            r.add(coolantSystem.getName());
            vDesc.addElement(r);
            r = new Report(2370);
            r.subject = getId();
            r.indent();
            r.add(EMERGENCY_COOLANT_SYSTEM_FAILURE[nCoolantSystemLevel]);
            r.add(nRoll);

            if (nRoll < EMERGENCY_COOLANT_SYSTEM_FAILURE[nCoolantSystemLevel]) {
                // uh oh
                bFailure = true;
                r.choose(false);
                vDesc.addElement(r);
                // do the damage.
                // hit and auto crit to first engine crit in this location,
                // or the transfer location, if there's no hittable engine slot
                // in this location
                coolantSystem.setHit(true);
                bDamagedCoolantSystem = true;
                int loc = coolantSystem.getLocation();
                boolean found = false;
                for (int i = 0; i < getNumberOfCriticals(loc); i++) {
                    CriticalSlot crit = getCritical(loc, i);
                    if ((crit != null) && crit.isHittable()
                            && (crit.getType() == CriticalSlot.TYPE_SYSTEM)
                            && (crit.getIndex() == Mech.SYSTEM_ENGINE)) {
                        vCriticals.put(loc, new LinkedList<>());
                        vCriticals.get(loc).add(crit);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    loc = this.getTransferLocation(loc);
                    for (int i = 0; i < getNumberOfCriticals(loc); i++) {
                        CriticalSlot crit = getCritical(loc, i);
                        if ((crit != null) && crit.isHittable()
                                && (crit.getType() == CriticalSlot.TYPE_SYSTEM)
                                && (crit.getIndex() == Mech.SYSTEM_ENGINE)) {
                            vCriticals.put(loc, new LinkedList<>());
                            vCriticals.get(loc).add(crit);
                            break;
                        }
                    }
                }
            } else {
                r.choose(true);
                vDesc.addElement(r);
                nCoolantSystemMOS = nRoll - EMERGENCY_COOLANT_SYSTEM_FAILURE[nCoolantSystemLevel];
            }
            return bFailure;
        }
        return false;
    }

    public boolean hasDamagedCoolantSystem() {
        return bDamagedCoolantSystem;
    }

    public void setHasDamagedCoolantSystem(boolean hit) {
        bDamagedCoolantSystem = hit;
    }

    public int getCoolantSystemMOS() {
        return nCoolantSystemMOS;
    }

    public boolean isCoolingFlawActive() {
        return coolingFlawActive;
    }

    public void setCoolingFlawActive(boolean flawActive) {
        coolingFlawActive = flawActive;
    }

    /**
     * Used to determine the draw priority of different Entity subclasses.
     * This allows different unit types to always be draw above/below other
     * types.
     *
     * @return
     */
    @Override
    public int getSpriteDrawPriority() {
        return 6;
    }
}
