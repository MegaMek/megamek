/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import megamek.common.loaders.MtfFile;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.common.weapons.EnergyWeapon;
import megamek.common.weapons.GaussWeapon;
import megamek.common.weapons.ISMekTaser;
import megamek.common.weapons.PPCWeapon;

/**
 * You know what mechs are, silly.
 */
public abstract class Mech extends Entity implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -1929593228891136561L;

    public static final int NUM_MECH_LOCATIONS = 8;

    // system designators for critical hits
    public static final int SYSTEM_LIFE_SUPPORT = 0;

    public static final int SYSTEM_SENSORS = 1;

    public static final int SYSTEM_COCKPIT = 2;

    public static final int SYSTEM_ENGINE = 3;

    public static final int SYSTEM_GYRO = 4;

    // actutors are systems too, for now
    public static final int ACTUATOR_SHOULDER = 7;

    public static final int ACTUATOR_UPPER_ARM = 8;

    public static final int ACTUATOR_LOWER_ARM = 9;

    public static final int ACTUATOR_HAND = 10;

    public static final int ACTUATOR_HIP = 11;

    public static final int ACTUATOR_UPPER_LEG = 12;

    public static final int ACTUATOR_LOWER_LEG = 13;

    public static final int ACTUATOR_FOOT = 14;

    public static final String systemNames[] = { "Life Support", "Sensors", "Cockpit", "Engine", "Gyro", null, null, "Shoulder", "Upper Arm", "Lower Arm", "Hand", "Hip", "Upper Leg", "Lower Leg", "Foot" };

    // locations
    public static final int LOC_HEAD = 0;

    public static final int LOC_CT = 1;

    public static final int LOC_RT = 2;

    public static final int LOC_LT = 3;

    public static final int LOC_RARM = 4;

    public static final int LOC_LARM = 5;

    public static final int LOC_RLEG = 6;

    public static final int LOC_LLEG = 7;

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

    public static final String[] GYRO_STRING = { "Standard Gyro", "XL Gyro", "Compact Gyro", "Heavy Duty Gyro" };

    public static final String[] GYRO_SHORT_STRING = { "Standard", "XL", "Compact", "Heavy Duty" };

    // cockpit types
    public static final int COCKPIT_UNKNOWN = -1;

    public static final int COCKPIT_STANDARD = 0;

    public static final int COCKPIT_TORSO_MOUNTED = 1;

    public static final int COCKPIT_SMALL = 2;

    public static final int COCKPIT_COMMAND_CONSOLE = 3;

    public static final int COCKPIT_DUAL = 4;

    public static final int COCKPIT_INDUSTRIAL = 5;

    public static final int COCKPIT_PRIMITIVE = 6;

    public static final int COCKPIT_PRIMITIVE_INDUSTRIAL = 7;

    public static final String[] COCKPIT_STRING = { "Standard Cockpit", "Torso-Mounted Cockpit", "Small Cockpit", "Command Console", "Dual Cockpit", "Industrial Cockpit", "Primitive Cockpit", "Primitive Industrial Cockpit" };

    public static final String[] COCKPIT_SHORT_STRING = { "Standard", "Torso Mounted", "Small", "Command Console", "Dual", "Industrial", "Primitive", "Primitive Industrial" };

    // jump types
    public static final int JUMP_UNKNOWN = -1;

    public static final int JUMP_NONE = 0;

    public static final int JUMP_STANDARD = 1;

    public static final int JUMP_IMPROVED = 2;

    public static final int JUMP_BOOSTER = 3;

    public static final int JUMP_DISPOSABLE = 4;

    // Some "has" items only need be determined once
    public static final int HAS_FALSE = -1;

    public static final int HAS_UNKNOWN = 0;

    public static final int HAS_TRUE = 1;

    // rear armor
    private int[] rearArmor;

    private int[] orig_rearArmor;

    private static int[] MASC_FAILURE = { 2, 4, 6, 10, 12, 12, 12 };

    // MASCLevel is the # of turns MASC has been used previously
    private int nMASCLevel = 0;

    private boolean bMASCWentUp = false;

    private boolean usedMASC = false; // Has masc been used?

    private int sinksOn = -1;

    private int sinksOnNextRound = -1;

    private boolean autoEject = true;

    private int cockpitStatus = COCKPIT_ON;

    private int cockpitStatusNextRound = COCKPIT_ON;

    private int jumpType = JUMP_UNKNOWN;

    private int gyroType = GYRO_STANDARD;

    private int cockpitType = COCKPIT_STANDARD;

    private boolean hasCowl = false;

    private int cowlArmor = 0;

    private int hasLaserHeatSinks = HAS_UNKNOWN;

    // For grapple attacks
    private int grappled_id = Entity.NONE;

    private boolean isGrappleAttacker = false;

    private static final NumberFormat commafy = NumberFormat.getInstance();

    private int grappledSide = Entity.GRAPPLE_BOTH;

    private boolean shouldDieAtEndOfTurnBecauseOfWater = false;

    private boolean justMovedIntoIndustrialKillingWater = false;

    private boolean stalled = false;

    private boolean stalledThisTurn = false;

    private boolean checkForCrit = false;

    private int levelsFallen = 0;

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

        for (int i = 0; i < locations(); i++) {
            if (!hasRearArmor(i)) {
                initializeRearArmor(IArmorState.ARMOR_NA, i);
            }
        }

        // Standard leg crits
        setCritical(LOC_RLEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_RLEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_RLEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_RLEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

        setCritical(LOC_LLEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_LLEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_LLEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_LLEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

        // Player setting specify whether their Meks' automatic
        // ejection systems are disabled by default or not.
        autoEject = !PreferenceManager.getClientPreferences().defaultAutoejectDisabled();
    }

    /**
     * @return if this mech cannot stand up from hulldown
     */
    public abstract boolean cannotStandUpFromHullDown();

    public void setCowl(int armor) {
        hasCowl = true;
        cowlArmor = armor;
    }

    public int getCowlArmor() {
        if (hasCowl) {
            return cowlArmor;
        }
        return 0;
    }

    public boolean hasCowl() {
        return hasCowl;
    }

    /**
     * Damage the cowl. Returns amount of excess damage
     *
     * @param amount
     * @return
     */
    public int damageCowl(int amount) {
        if (hasCowl) {
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
        } else if (Mech.restrictScore(location1) >= Mech.restrictScore(location2)) {
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
        } else if (Mech.restrictScore(location1) >= Mech.restrictScore(location2)) {
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
     * Get the number of turns MASC has been used continuously.
     * <p/>
     * This method should <strong>only</strong> be used during serialization.
     *
     * @return the <code>int</code> number of turns MASC has been used.
     */
    public int getMASCTurns() {
        return nMASCLevel;
    }

    /**
     * Set the number of turns MASC has been used continuously.
     * <p/>
     * This method should <strong>only</strong> be used during deserialization.
     *
     * @param turns
     *            The <code>int</code> number of turns MASC has been used.
     */
    public void setMASCTurns(int turns) {
        nMASCLevel = turns;
    }

    /**
     * Determine if MASC has been used this turn.
     * <p/>
     * This method should <strong>only</strong> be used during serialization.
     *
     * @return <code>true</code> if MASC has been used.
     */
    public boolean isMASCUsed() {
        return usedMASC;
    }

    /**
     * Set whether MASC has been used.
     * <p/>
     * This method should <strong>only</strong> be used during deserialization.
     *
     * @param used
     *            The <code>boolean</code> whether MASC has been used.
     */
    public void setMASCUsed(boolean used) {
        usedMASC = used;
    }

    public int getMASCTarget() {
        return MASC_FAILURE[nMASCLevel] + 1;
    }

    /**
     * This function cheks for masc failure.
     *
     * @param md
     *            the movement path.
     * @param vDesc
     *            the description off the masc failure. used as output.
     * @param vCriticals
     *            contains tuple of intiger and critical slot. used as output.
     * @return true if there was a masc failure.
     */
    public boolean checkForMASCFailure(MovePath md, Vector<Report> vDesc, HashMap<Integer, CriticalSlot> vCriticals) {
        if (md.hasActiveMASC()) {
            boolean bFailure = false;

            // If usedMASC is already set, then we've already checked MASC
            // this turn. If we succeded before, return false.
            // If we failed before, the MASC was destroyed, and we wouldn't
            // have gotten here (hasActiveMASC would return false)
            if (!usedMASC) {
                Mounted masc = getMASC();
                Mounted superCharger = getSuperCharger();
                bFailure = doMASCCheckFor(masc, vDesc, vCriticals);
                boolean bSuperChargeFailure = doMASCCheckFor(superCharger, vDesc, vCriticals);
                return bFailure || bSuperChargeFailure;
            }
        }
        return false;
    }

    /**
     * check one masc system for failure
     *
     * @param masc
     * @param vDesc
     * @param vCriticals
     * @return
     */
    private boolean doMASCCheckFor(Mounted masc, Vector<Report> vDesc, HashMap<Integer, CriticalSlot> vCriticals) {
        if (masc != null) {
            boolean bFailure = false;
            int nRoll = Compute.d6(2);

            usedMASC = true;
            Report r = new Report(2365);
            r.subject = getId();
            r.addDesc(this);
            r.add(masc.getName());
            vDesc.addElement(r);
            r = new Report(2370);
            r.subject = getId();
            r.indent();
            r.add(getMASCTarget());
            r.add(nRoll);

            if (nRoll < getMASCTarget()) {
                // uh oh
                bFailure = true;
                r.choose(false);
                vDesc.addElement(r);

                if (((MiscType) (masc.getType())).hasSubType(MiscType.S_SUPERCHARGER)) {
                    if (masc.getType().hasFlag(MiscType.F_MASC)) {
                        masc.setDestroyed(true);
                        masc.setMode("Off");
                    }
                    // do the damage - engine crits
                    int hits = 0;
                    int roll = Compute.d6(2);
                    r = new Report(6310);
                    r.subject = getId();
                    r.add(roll);
                    r.newlines = 0;
                    vDesc.addElement(r);
                    if (roll <= 7) {
                        // no effect
                        r = new Report(6005);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    } else if ((roll >= 8) && (roll <= 9)) {
                        hits = 1;
                        r = new Report(6315);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    } else if ((roll >= 10) && (roll <= 11)) {
                        hits = 2;
                        r = new Report(6320);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    } else if (roll == 12) {
                        hits = 3;
                        r = new Report(6325);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    }
                    for (int i = 0; (i < 12) && (hits > 0); i++) {
                        CriticalSlot cs = getCritical(LOC_CT, i);
                        if ((cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == SYSTEM_ENGINE)) {
                            vCriticals.put(new Integer(LOC_CT), cs);
                            hits--;
                        }
                    }
                } else {
                    // do the damage.
                    // random crit on each leg, but MASC is not destroyed
                    for (int loc = 0; loc < locations(); loc++) {
                        if (locationIsLeg(loc) && (getHittableCriticals(loc) > 0)) {
                            CriticalSlot slot = null;
                            do {
                                int slotIndex = Compute.randomInt(getNumberOfCriticals(loc));
                                slot = getCritical(loc, slotIndex);
                            } while ((slot == null) || !slot.isHittable());
                            vCriticals.put(new Integer(loc), slot);
                        }
                    }
                }
                // failed a PSR, check for stalling
                doCheckEngineStallRoll(vDesc);
            } else {
                r.choose(true);
                vDesc.addElement(r);
            }
            return bFailure;
        }
        return false;
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

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
        return NUM_MECH_LOCATIONS;
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
            if (mtype.hasFlag(MiscType.F_STEALTH) && m.getLinked().isDestroyed() && m.getLinked().isBreached()) {
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
        for (int loc = 0; loc < NUM_MECH_LOCATIONS; loc++) {
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
            return (getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) == 0);
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

                if ((ccs != null) && (ccs.getType() == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == system)) {
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
            if (getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc) == 0) {
                legCrits++;
            }
            if (getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc) == 0) {
                legCrits++;
            }
            if (getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc) == 0) {
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
            if (!m.isInoperable() && (m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_MASC) && m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
                hasSuperCharger = true;
            }
            if (!m.isInoperable() && (m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_MASC) && !m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
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
                if (mEquip.isBreached() || mEquip.isDestroyed() || mEquip.isMissing()) {
                    return false;
                }
                jumpBoosters = true;
            }
        }
        return jumpBoosters;
    }

    /**
     * get non-supercharger MASC mounted on this mech
     *
     * @return
     */
    public Mounted getMASC() {
        for (Mounted m : getMisc()) {
            MiscType mtype = (MiscType) m.getType();
            if (mtype.hasFlag(MiscType.F_MASC) && m.isReady() && !mtype.hasSubType(MiscType.S_SUPERCHARGER)) {
                return m;
            }
        }
        return null;
    }

    /**
     * get a supercharger mounted on this mech
     *
     * @return
     */
    public Mounted getSuperCharger() {
        for (Mounted m : getMisc()) {
            MiscType mtype = (MiscType) m.getType();
            if (mtype.hasFlag(MiscType.F_MASC) && m.isReady() && mtype.hasSubType(MiscType.S_SUPERCHARGER)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Checks if a mech has an armed MASC system. Note that the mech will have
     * to exceed its normal run to actually engage the MASC system
     */
    public boolean hasArmedMASC() {
        for (Mounted m : getEquipment()) {
            if (!m.isDestroyed() && !m.isBreached() && (m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_MASC) && m.curMode().equals("Armed")) {
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
            if (!m.isDestroyed() && !m.isBreached() && (m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_MASC) && m.curMode().equals("Armed") && m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
                hasSuperCharger = true;
            }
            if (!m.isDestroyed() && !m.isBreached() && (m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_MASC) && m.curMode().equals("Armed") && !m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
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
            if (!m.isInoperable() && (m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_CLUB) && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE) && m.curMode().equals("extended")) {
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
     * does this mech have TSM?
     */
    public boolean hasTSM() {
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_TSM)) {
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
            if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                return true;
            }
        }
        return false;
    }

    /**
     * does this mech have stealth armor?
     *
     * @return
     */
    public boolean hasStealth() {
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_STEALTH)) {
                // The Mek has Stealth Armor
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
     * does this mech have tracks?
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
        return engine.getStandingHeat();
    }

    /**
     * set this mech's <code>Engine</code>
     *
     * @param e
     *            the <code>Engine</code> to set
     */
    public void setEngine(Engine e) {
        engine = e;
        if (e.engineValid) {
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
        if (isPrimitive()) {
            double rating = getEngine().getRating();
            rating /= 1.2;
            if (rating % 5 != 0) {
                return (int) (rating - rating % 5 + 5) / (int) weight;
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
        return engine.getWalkHeat();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getRunMP(boolean, boolean)
     */
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat) {
        if (hasArmedMASCAndSuperCharger()) {
            return ((int) Math.ceil(getWalkMP(gravity, ignoreheat) * 2.5)) - (getArmorType() == EquipmentType.T_ARMOR_HARDENED ? 1 : 0);
        }
        if (hasArmedMASC()) {
            return (getWalkMP(gravity, ignoreheat) * 2) - (getArmorType() == EquipmentType.T_ARMOR_HARDENED ? 1 : 0);
        }
        return super.getRunMP(gravity, ignoreheat) - (getArmorType() == EquipmentType.T_ARMOR_HARDENED ? 1 : 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getRunMPwithoutMASC(boolean, boolean)
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat) {
        return super.getRunMP(gravity, ignoreheat) - (getArmorType() == EquipmentType.T_ARMOR_HARDENED ? 1 : 0);
    }

    public int getOriginalRunMPwithoutMASC() {
        return super.getRunMP(false, false) - (getArmorType() == EquipmentType.T_ARMOR_HARDENED ? 1 : 0);
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
        return engine.getRunHeat();
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
        int jump = 0;

        if (hasShield() && (getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return 0;
        }

        if (hasModularArmor()) {
            jump--;
        }

        for (Mounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_JUMP_JET) && !mounted.isDestroyed() && !mounted.isBreached()) {
                jump++;
            } else if (mounted.getType().hasFlag(MiscType.F_JUMP_BOOSTER) && !mounted.isDestroyed() && !mounted.isBreached()) {
                jump = getOriginalJumpMP();
                break;
            }
        }

        if (gravity) {
            return applyGravityEffectsOnMP(jump);
        }
        return jump;
    }

    /**
     * Returns the type of jump jet system the mech has.
     */
    @Override
    public int getJumpType() {
        if (jumpType == JUMP_UNKNOWN) {
            jumpType = JUMP_NONE;
            for (Mounted m : miscList) {
                if (m.getType().hasFlag(MiscType.F_JUMP_JET)) {
                    if (m.getType().hasSubType(MiscType.S_IMPROVED)) {
                        jumpType = JUMP_IMPROVED;
                    } else {
                        jumpType = JUMP_STANDARD;
                    }
                    break;
                } else if (m.getType().hasFlag(MiscType.F_JUMP_BOOSTER)) {
                    jumpType = JUMP_BOOSTER;
                    break;
                }
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
        switch (getJumpType()) {
        case JUMP_IMPROVED:
            return engine.getJumpHeat(movedMP / 2 + movedMP % 2);
        case JUMP_BOOSTER:
        case JUMP_DISPOSABLE:
        case JUMP_NONE:
            return 0;
        default:
            return engine.getJumpHeat(movedMP);
        }
    }

    /**
     * Returns this mech's jumping MP, modified for missing & underwater jets
     * and gravity.
     */
    @Override
    public int getJumpMPWithTerrain() {
        if ((getPosition() == null) || (getJumpType() == JUMP_BOOSTER)) {
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
            if (mounted.getType().hasFlag(MiscType.F_JUMP_JET) && !mounted.isDestroyed() && !mounted.isBreached() && locationIsTorso(mounted.getLocation())) {
                jump++;
            }
        }

        return jump;
    }

    /**
     * Returns the elevation of this entity. Mechs do funny stuff in the middle
     * of a DFA.
     */
    @Override
    public int getElevation() {
        int cElev = super.getElevation();
        if (!isMakingDfa()) {
            return cElev;
        }
        // otherwise, we are one elevation above our hex or the target's hex,
        // whichever is higher
        int tElev = game.getBoard().getHex(displacementAttack.getTargetPos()).floor();
        return Math.max(cElev, tElev) + 1;
    }

    /**
     * Return the height of this mech above the terrain.
     */
    @Override
    public int height() {
        IHex posHex = game.getBoard().getHex(getPosition());
        return (isProne() || ((posHex != null) && Compute.isInBuilding(game, this))) ? 0 : 1;
    }

    /**
     * Adds heat sinks to the engine. Uses clan/normal depending on the
     * currently set techLevel
     */
    public void addEngineSinks(int totalSinks, boolean dblSinks) {
        addEngineSinks(totalSinks, dblSinks, isClan());
    }

    /**
     * Adds heat sinks to the engine. Adds either the engine capacity, or the
     * entire number of heat sinks, whichever is less
     */
    public void addEngineSinks(int totalSinks, boolean dblSinks, boolean clan) {
        if (dblSinks) {
            addEngineSinks(totalSinks, clan ? "CLDoubleHeatSink" : "ISDoubleHeatSink");
        } else {
            addEngineSinks(totalSinks, "Heat Sink");
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
        int toAllocate = Math.min(totalSinks, getEngine().integralHeatSinkCapacity());
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

        if ((toAllocate == 0) && getEngine().isFusion()) {
            System.out.println("Mech: not putting any heat sinks in the engine?!?!");
        }

        for (int i = 0; i < toAllocate; i++) {
            try {
                addEquipment(new Mounted(this, sinkType), Entity.LOC_NONE, false);
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
        int engineCritHeat = 0;
        if (!isShutDown() && getEngine().isFusion()) {
            engineCritHeat += 5 * getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_CT);
            engineCritHeat += 5 * getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_LT);
            engineCritHeat += 5 * getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_RT);
        }
        return engineCritHeat;
    }

    /**
     * Returns the number of heat sinks, functional or not.
     */
    public int heatSinks() {
        int sinks = 0;
        for (Mounted mounted : getMisc()) {
            EquipmentType etype = mounted.getType();
            if (etype.hasFlag(MiscType.F_HEAT_SINK) || etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
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
        int capacity = 0;
        int activeCount = getActiveSinks();

        for (Mounted mounted : getMisc()) {
            if (activeCount <= 0) {
                break;
            }
            if (mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }
            if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                capacity++;
                activeCount--;
            } else if (mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                activeCount--;
                capacity += 2;
            }
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
            return getHeatCapacity();
        }
        return getHeatCapacity() + Math.min(sinksUnderwater(), 6);
    }

    /**
     * Gets the number of heat sinks that are underwater.
     */
    private int sinksUnderwater() {
        if ((getPosition() == null) || isOffBoard()) {
            return 0;
        }

        IHex curHex = game.getBoard().getHex(getPosition());
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
            if (mounted.isDestroyed() || mounted.isBreached() || !locationIsLeg(mounted.getLocation())) {
                continue;
            }
            if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                sinksUnderwater++;
            } else if (mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                sinksUnderwater += 2;
            }
        }
        return sinksUnderwater;
    }

    /**
     * Returns the name of the type of movement used. This is mech-specific.
     */
    @Override
    public String getMovementString(int mtype) {
        switch (mtype) {
        case IEntityMovementType.MOVE_SKID:
            return "Skidded";
        case IEntityMovementType.MOVE_NONE:
        case IEntityMovementType.MOVE_CAREFUL_STAND:
            return "None";
        case IEntityMovementType.MOVE_WALK:
            return "Walked";
        case IEntityMovementType.MOVE_RUN:
            return "Ran";
        case IEntityMovementType.MOVE_JUMP:
            return "Jumped";
        default:
            return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used. This is mech-specific.
     */
    @Override
    public String getMovementAbbr(int mtype) {
        switch (mtype) {
        case IEntityMovementType.MOVE_SKID:
            return "S";
        case IEntityMovementType.MOVE_NONE:
            return "N";
        case IEntityMovementType.MOVE_WALK:
            return "W";
        case IEntityMovementType.MOVE_RUN:
            return "R";
        case IEntityMovementType.MOVE_JUMP:
            return "J";
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
        return !isProne();
    }

    /**
     * Can this mech torso twist in the given direction?
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            return (rotate == 0) || (rotate == 1) || (rotate == -1) || (rotate == -5);
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
        if (rear && hasRearArmor(loc)) {
            return rearArmor[loc];
        }
        return super.getArmor(loc, rear);
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

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);

        // B-Pods need to be special-cased, the have 360 firing arc
        if ((mounted.getType() instanceof WeaponType) && mounted.getType().hasFlag(WeaponType.F_B_POD)) {
            return Compute.ARC_360;
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
            return getArmsFlipped() ? Compute.ARC_REAR : Compute.ARC_RIGHTARM;
        case LOC_LARM:
            return getArmsFlipped() ? Compute.ARC_REAR : Compute.ARC_LEFTARM;
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
        if ((getEquipment(weaponId).getLocation() == LOC_RLEG) || (getEquipment(weaponId).getLocation() == LOC_LLEG)) {
            return false;
        }
        // other weapons into the secondary
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#rollHitLocation(int, int)
     */
    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, IAimingModes.AIM_MODE_NONE);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#rollHitLocation(int, int, int, int)
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode) {
        int roll = -1;

        if ((aimedLocation != LOC_NONE) && (aimingMode != IAimingModes.AIM_MODE_NONE)) {

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
                    if ((crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_tac")) && !game.getOptions().booleanOption("no_tac")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(tac(table, side, Mech.LOC_CT, false));
                        return result;
                    } // if
                    return tac(table, side, Mech.LOC_CT, false);
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
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_LEFT) {
                // normal left side hits
                switch (roll) {
                case 2:
                    if ((crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_tac")) && !game.getOptions().booleanOption("no_tac")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(tac(table, side, Mech.LOC_LT, false));
                        return result;
                    } // if
                    return tac(table, side, Mech.LOC_LT, false);
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
                    if (game.getOptions().booleanOption("tacops_advanced_mech_hit_locations")) {
                        return new HitData(Mech.LOC_CT, true);
                    }
                    return new HitData(Mech.LOC_CT);
                case 9:
                    if (game.getOptions().booleanOption("tacops_advanced_mech_hit_locations")) {
                        return new HitData(Mech.LOC_RT, true);
                    }
                    return new HitData(Mech.LOC_RT);
                case 10:
                    return new HitData(Mech.LOC_RARM);
                case 11:
                    return new HitData(Mech.LOC_RLEG);
                case 12:
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_RIGHT) {
                // normal right side hits
                switch (roll) {
                case 2:
                    if ((crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_tac")) && !game.getOptions().booleanOption("no_tac")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(tac(table, side, Mech.LOC_RT, false));
                        return result;
                    } // if
                    return tac(table, side, Mech.LOC_RT, false);
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
                    if (game.getOptions().booleanOption("tacops_advanced_mech_hit_locations")) {
                        return new HitData(Mech.LOC_CT, true);
                    }
                    return new HitData(Mech.LOC_CT);
                case 9:
                    if (game.getOptions().booleanOption("tacops_advanced_mech_hit_locations")) {
                        return new HitData(Mech.LOC_LT, true);
                    }
                    return new HitData(Mech.LOC_LT);
                case 10:
                    return new HitData(Mech.LOC_LARM);
                case 11:
                    return new HitData(Mech.LOC_LLEG);
                case 12:
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_REAR) {
                // normal rear hits
                if (game.getOptions().booleanOption("tacops_advanced_mech_hit_locations") && isProne()) {
                    switch (roll) {
                    case 2:
                        if ((crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_tac")) && !game.getOptions().booleanOption("no_tac")) {
                            crew.decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                            result.setUndoneLocation(tac(table, side, Mech.LOC_CT, true));
                            return result;
                        } // if
                        return tac(table, side, Mech.LOC_CT, true);
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
                        if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                            crew.decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                            return result;
                        } // if
                        return new HitData(Mech.LOC_HEAD, true);
                    }
                } else {
                    switch (roll) {
                    case 2:
                        if ((crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_tac")) && !game.getOptions().booleanOption("no_tac")) {
                            crew.decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                            result.setUndoneLocation(tac(table, side, Mech.LOC_CT, true));
                            return result;
                        } // if
                        return tac(table, side, Mech.LOC_CT, true);
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
                        if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                            crew.decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
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
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
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
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
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
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
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
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD, true);
                }
            }
        }
        if (table == ToHitData.HIT_KICK) {
            roll = Compute.d6(1);
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
            if ((side == ToHitData.SIDE_FRONT) || (side == ToHitData.SIDE_REAR)) {
                // front/rear kick hits
                switch (roll) {
                case 1:
                case 2:
                case 3:
                    return new HitData(Mech.LOC_RLEG, (side == ToHitData.SIDE_REAR));
                case 4:
                case 5:
                case 6:
                    return new HitData(Mech.LOC_LLEG, (side == ToHitData.SIDE_REAR));
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
        if ((table == ToHitData.HIT_SWARM) || (table == ToHitData.HIT_SWARM_CONVENTIONAL)) {
            roll = Compute.d6(2);
            int effects;
            if (table == ToHitData.HIT_SWARM_CONVENTIONAL) {
                effects = HitData.EFFECT_NONE;
            } else {
                effects = HitData.EFFECT_CRITICAL;
            }
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
            // Swarm attack locations.
            switch (roll) {
            case 2:
                if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                    crew.decreaseEdge();
                    HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                    result.setUndoneLocation(new HitData(Mech.LOC_HEAD, false, effects));
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
                if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                    crew.decreaseEdge();
                    HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                    result.setUndoneLocation(new HitData(Mech.LOC_HEAD, false, effects));
                    return result;
                } // if
                return new HitData(Mech.LOC_HEAD, false, effects);
            }
        }
        if (table == ToHitData.HIT_ABOVE) {
            roll = Compute.d6(1);
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
            // Hits from above.
            switch (roll) {
            case 1:
                return new HitData(Mech.LOC_LARM, (side == ToHitData.SIDE_REAR));
            case 2:
                return new HitData(Mech.LOC_LT, (side == ToHitData.SIDE_REAR));
            case 3:
                return new HitData(Mech.LOC_CT, (side == ToHitData.SIDE_REAR));
            case 4:
                return new HitData(Mech.LOC_RT, (side == ToHitData.SIDE_REAR));
            case 5:
                return new HitData(Mech.LOC_RARM, (side == ToHitData.SIDE_REAR));
            case 6:
                if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                    crew.decreaseEdge();
                    HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                    result.setUndoneLocation(new HitData(Mech.LOC_HEAD, (side == ToHitData.SIDE_REAR)));
                    return result;
                } // if
                return new HitData(Mech.LOC_HEAD, (side == ToHitData.SIDE_REAR));
            }
        }
        if (table == ToHitData.HIT_BELOW) {
            roll = Compute.d6(1);
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
            // Hits from below.
            switch (roll) {
            case 1:
                return new HitData(Mech.LOC_LLEG, (side == ToHitData.SIDE_REAR));
            case 2:
                return new HitData(Mech.LOC_LLEG, (side == ToHitData.SIDE_REAR));
            case 3:
                return new HitData(Mech.LOC_LT, (side == ToHitData.SIDE_REAR));
            case 4:
                return new HitData(Mech.LOC_RT, (side == ToHitData.SIDE_REAR));
            case 5:
                return new HitData(Mech.LOC_RLEG, (side == ToHitData.SIDE_REAR));
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
    protected HitData tac(int table, int side, int location, boolean rear) {
        if (game.getOptions().booleanOption("no_tac")) {
            return new HitData(location, rear);
        } else if (game.getOptions().booleanOption("floating_crits")) {
            HitData hd = rollHitLocation(table, side);
            return new HitData(hd.getLocation(), hd.isRear(), HitData.EFFECT_CRITICAL);
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
            return new HitData(LOC_CT, hit.isRear(), hit.getEffect(), hit.hitAimedLocation(), hit.getSpecCritMod(), hit.isFromFront(), hit.getGeneralDamageType(), hit.glancingMod());
        case LOC_LLEG:
        case LOC_LARM:
            return new HitData(LOC_LT, hit.isRear(), hit.getEffect(), hit.hitAimedLocation(), hit.getSpecCritMod(), hit.isFromFront(), hit.getGeneralDamageType(), hit.glancingMod());
        case LOC_RLEG:
        case LOC_RARM:
            return new HitData(LOC_RT, hit.isRear(), hit.getEffect(), hit.hitAimedLocation(), hit.getSpecCritMod(), hit.isFromFront(), hit.getGeneralDamageType(), hit.glancingMod());
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
        EquipmentType clCase = EquipmentType.get("CLCASE");
        for (int i = 0; i < locations(); i++) {
            explosiveFound = false;
            for (Mounted m : getEquipment()) {
                if (m.getType().isExplosive() && (m.getLocation() == i)) {
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

    /**
     * Mounts the specified weapon in the specified location.
     */
    @Override
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted) throws LocationFullException {
        // if there's no actual location, then don't add criticals
        if (loc == LOC_NONE) {
            super.addEquipment(mounted, loc, rearMounted);
            return;
        }

        // spreadable or split equipment only gets added to 1 crit at a time,
        // since we don't know how many are in this location
        int slots = mounted.getType().getCriticals(this);
        if (mounted.getType().isSpreadable() || mounted.isSplitable()) {
            slots = 1;
        }

        // check criticals for space
        if (getEmptyCriticals(loc) < slots) {
            throw new LocationFullException(mounted.getName() + " does not fit in " + getLocationAbbr(loc) + " on " + getDisplayName() + "\n        free criticals in location: " + getEmptyCriticals(loc) + ", criticals needed: " + slots);
        }
        // add it
        if (getEquipmentNum(mounted) == -1) {
            super.addEquipment(mounted, loc, rearMounted);
        }

        // add criticals
        int num = getEquipmentNum(mounted);

        for (int i = 0; i < slots; i++) {
            CriticalSlot cs = new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT, num, mounted.getType().isHittable(), mounted.isArmored(), mounted);
            addCritical(loc, cs);
        }
    }

    /**
     * Calculates the battle value of this mech
     */
    @Override
    public int calculateBattleValue() {
        return calculateBattleValue(false, false);
    }

    /**
     * Calculates the battle value of this mech. If the parameter is true, then
     * the battle value for c3 won't be added whether the mech is currently part
     * of a network or not.
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {

        String startTable = "<TABLE>";
        String endTable = "</TABLE>";

        String startRow = "<TR>";
        String endRow = "</TR>";

        String startColumn = "<TD>";
        String endColumn = "</TD>";

        String nl = "<BR>";

        bvText = new StringBuffer("<HTML><BODY><CENTER><b>Battle Value Calculations For ");

        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        // total armor points
        double armorMultiplier = 1.0;

        switch (getArmorType()) {
        case EquipmentType.T_ARMOR_COMMERCIAL:
            armorMultiplier = 0.5;
            break;
        case EquipmentType.T_ARMOR_HARDENED:
            armorMultiplier = 2.0;
            break;
        case EquipmentType.T_ARMOR_REACTIVE:
        case EquipmentType.T_ARMOR_REFLECTIVE:
            armorMultiplier = 1.5;
            break;
        case EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE:
            armorMultiplier = 1.2;
            break;
        default:
            armorMultiplier = 1.0;
        break;

        }

        bvText.append(startTable);
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Armor Factor x ");
        bvText.append(armorMultiplier);
        bvText.append(endColumn);
        bvText.append(startColumn);

        // BV for torso mounted cockpit.
        if (getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            dbv += this.getArmor(Mech.LOC_CT);
            dbv += this.getArmor(Mech.LOC_CT, true);
        }
        int modularArmor = 0;
        for (Mounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        dbv += (getTotalArmor() + modularArmor);

        bvText.append(dbv);
        bvText.append(" x 2.5 x ");
        bvText.append(armorMultiplier);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");

        dbv *= 2.5 * armorMultiplier;

        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        // total internal structure
        double internalMultiplier = 1.0;
        if (getStructureType() == EquipmentType.T_STRUCTURE_INDUSTRIAL) {
            internalMultiplier = 0.5;
        }

        dbv += getTotalInternal() * internalMultiplier * 1.5 * getEngine().getBVMultiplier();

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total I.S. Points x IS Multipler x 1.5 x Engine Multipler");

        bvText.append(endColumn + startColumn);
        bvText.append(getTotalInternal());
        bvText.append(" x ");
        bvText.append(internalMultiplier);

        bvText.append("1.5 x ");
        bvText.append(getEngine().getBVMultiplier());
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(getTotalInternal() * internalMultiplier * 1.5 * getEngine().getBVMultiplier());
        bvText.append(endColumn);
        bvText.append(endRow);

        // add gyro
        double gyroMultiplier = 0.5;
        if (getGyroType() == GYRO_HEAVY_DUTY) {
            gyroMultiplier = 1.0;
        }
        dbv += getWeight() * gyroMultiplier;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Weight x Gyro Multipler ");
        bvText.append(endColumn + startColumn);
        bvText.append(getWeight());
        bvText.append(" x ");
        bvText.append(gyroMultiplier);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(getWeight() * gyroMultiplier);
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
        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS) || etype.hasFlag(WeaponType.F_B_POD))) || ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS)) || ((etype instanceof MiscType) && (etype.hasFlag(MiscType.F_ECM) || etype.hasFlag(MiscType.F_AP_POD)
                    // not yet coded: ||
                    // etype.hasFlag(MiscType.F_BRIDGE_LAYING)
                    || etype.hasFlag(MiscType.F_BAP)))) {
                double bv = etype.getBV(this);
                dEquipmentBV += bv;
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(etype.getName());
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
        dbv += dEquipmentBV;

        bvText.append(startRow);
        bvText.append(startColumn);

        double armoredBVCal = getArmoredCritBV();

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
            if (!etype.isExplosive()) {
                continue;
            }

            // don't count oneshot ammo
            if (loc == LOC_NONE) {
                continue;
            }

            // CASE II means no subtraction
            if (hasCASEII(loc)) {
                continue;
            }

            if (isClan()) {
                // Clan mechs only count ammo in ct, legs or head (per BMRr).
                // Also count ammo in side torsos if mech has xxl engine
                // (extrapolated from rule intent - not covered in rules)
                if (((loc != LOC_CT) && (loc != LOC_RLEG) && (loc != LOC_LLEG) && (loc != LOC_HEAD)) && !(((loc == LOC_RT) || (loc == LOC_LT)) && (getEngine().getSideTorsoCriticalSlots().length > 2))) {
                    continue;
                }
            } else {
                // inner sphere with XL or XXL counts everywhere
                if (getEngine().getSideTorsoCriticalSlots().length <= 2) {
                    // without XL or XXL, only count torsos if not CASEed,
                    // and arms if arm & torso not CASEed
                    if (((loc == LOC_RT) || (loc == LOC_LT)) && locationHasCase(loc)) {
                        continue;
                    } else if ((loc == LOC_LARM) && (locationHasCase(loc) || locationHasCase(LOC_LT))) {
                        continue;
                    } else if ((loc == LOC_RARM) && (locationHasCase(loc) || locationHasCase(LOC_RT))) {
                        continue;
                    }
                }
            }

            // gauss rifles only subtract 1 point per slot
            if (etype instanceof GaussWeapon) {
                toSubtract = 1;
            }

            // tasers also get only 1 point per slot
            if (etype instanceof ISMekTaser) {
                toSubtract = 1;
            }

            // RACs don't really count
            if ((etype instanceof WeaponType) && (((WeaponType) etype).getAmmoType() == AmmoType.T_AC_ROTARY)) {
                toSubtract = 0;
            }

            // empty ammo shouldn't count
            if ((etype instanceof AmmoType) && (mounted.getShotsLeft() == 0)) {
                continue;
            }
            // normal ACs only marked as explosive because they are when they
            // just
            // fired incendiary ammo, therefore they don't count for explosive
            // BV
            if ((etype instanceof WeaponType) && ((((WeaponType) etype).getAmmoType() == AmmoType.T_AC) || (((WeaponType) etype).getAmmoType() == AmmoType.T_LAC))) {
                toSubtract = 0;
            }

            // B-Pods shouldn't subtract
            if ((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_B_POD))) {
                toSubtract = 0;
            }

            // coolant pods subtract 1 each
            if ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_COOLANT_POD)) {
                toSubtract = 1;
            }
            // we subtract per critical slot
            toSubtract *= etype.getCriticals(this);
            ammoPenalty += toSubtract;
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
        // we use full possible movement, ignoring gravity and heat
        // but taking into account hit actuators
        int walkMP = getWalkMP(false, true);
        int runMP;
        if (hasTSM()) {
            walkMP++;
        }
        if (hasMASCAndSuperCharger()) {
            runMP = (int) Math.ceil(walkMP * 2.5);
        } else if (hasMASC()) {
            runMP = walkMP * 2;
        } else {
            runMP = (int) Math.ceil(walkMP * 1.5);
        }
        if (getArmorType() == EquipmentType.T_ARMOR_HARDENED) {
            runMP--;
        }
        int tmmRan = Compute.getTargetMovementModifier(runMP, false, false).getValue();
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

        bvText.append("Target Movement Modifer For Run");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(tmmRan);
        bvText.append(endColumn);
        bvText.append(endRow);
        // use UMU for JJ, unless we have more jump MP than UMU (then we have
        // mechanical jumpboosters
        int jumpMP = Math.max(getActiveUMUCount(), getJumpMP(false));
        int tmmJumped = Compute.getTargetMovementModifier(jumpMP, true, false).getValue();
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Target Movement Modifer For Jumping");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(tmmJumped);
        bvText.append(endColumn);
        bvText.append(endRow);

        double targetMovementModifier = Math.max(tmmRan, tmmJumped);
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Target Movement Modifer");
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
            targetMovementModifier += 3;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Void Sig +3");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append("+3");
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

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Base Heat Efficiency ");

        int coolantPods = 0;
        for (Mounted ammo : getAmmo()) {
            if (((AmmoType) ammo.getType()).getAmmoType() == AmmoType.T_COOLANT_POD) {
                coolantPods++;
            }
        }

        // account for coolant pods
        if (coolantPods > 0) {
            mechHeatEfficiency += Math.min(2 * getNumberOfSinks(), Math.ceil((double) (getNumberOfSinks() * coolantPods) / 5));
            bvText.append(" + Coolant Pods ");
        }

        if (getJumpMP() > 0) {
            mechHeatEfficiency -= getJumpHeat(getJumpMP());
            bvText.append(" - Jump Heat ");
        } else {
            mechHeatEfficiency -= getRunHeat();
            bvText.append(" - Run Heat ");
        }

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(6 + getHeatCapacity());

        if (coolantPods > 0) {
            bvText.append(" + ");
            bvText.append(Math.min(2 * getNumberOfSinks(), Math.ceil((double) (getNumberOfSinks() * coolantPods) / 5)));
        }

        bvText.append(" - ");
        if (getJumpMP() > 0) {
            bvText.append(getJumpHeat(getJumpMP()));
        } else {
            bvText.append(getRunHeat());
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
        double bvFront = 0, bvRear = 0, nonArmFront = 0, nonArmRear = 0;
        ArrayList<Mounted> weapons = getWeaponList();
        for (Mounted weapon : weapons) {
            WeaponType wtype = (WeaponType) weapon.getType();
            if (wtype.hasFlag(WeaponType.F_B_POD)) {
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
                double mgaBV = 0;
                for (Mounted possibleMG : getWeaponList()) {
                    if (possibleMG.getType().hasFlag(WeaponType.F_MG) && (possibleMG.getLocation() == weapon.getLocation())) {
                        mgaBV += possibleMG.getType().getBV(this);
                    }
                }
                dBV = mgaBV * 0.67;
            }
            // check to see if the weapon is a PPC and has a Capacitor attached
            // to it
            if (wtype.hasFlag(WeaponType.F_PPC) && (weapon.getLinkedBy() != null)) {
                dBV += ((MiscType) weapon.getLinkedBy().getType()).getBV(this, weapon);
            }

            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append(wtype.getName());
            if (weapon.isRearMounted()) {
                bvRear += dBV;
                bvText.append(" (R)");
            } else {
                bvFront += dBV;
            }
            if (!isArm(weapon.getLocation())) {
                if (weapon.isRearMounted()) {
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

        bvText.append("Total Unmodfied BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(bvRear + bvFront);
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
        if (nonArmFront <= nonArmRear) {
            halveRear = false;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("halving front instead of rear weapon BVs");
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
        ArrayList<ArrayList<Object>> heatBVs = new ArrayList<ArrayList<Object>>();
        // BVs of non-heat-using weapons
        ArrayList<ArrayList<Object>> nonHeatBVs = new ArrayList<ArrayList<Object>>();
        // total up maximum heat generated
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<String, Double>();
        double maximumHeat = 0;
        for (Mounted mounted : getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if (wtype.hasFlag(WeaponType.F_B_POD)) {
                continue;
            }
            double weaponHeat = wtype.getHeat();

            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }

            // one shot weapons count 1/4
            if ((wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER) || wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weaponHeat *= 0.25;
            }

            // double heat for ultras
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weaponHeat *= 2;
            }

            // Six times heat for RAC
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weaponHeat *= 6;
            }

            // half heat for streaks
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK) || (wtype.getAmmoType() == AmmoType.T_MRM_STREAK) || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                weaponHeat *= 0.5;
            }

            // check to see if the weapon is a PPC and has a Capacitor attached
            // to it
            if (wtype.hasFlag(WeaponType.F_PPC) && (mounted.getLinkedBy() != null)) {
                weaponHeat += 5;
            }

            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append(wtype.getName());
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+ ");
            bvText.append(weaponHeat);
            bvText.append(endColumn);
            bvText.append(endRow);

            double dBV = wtype.getBV(this);
            String weaponName = mounted.getName() + (mounted.isRearMounted() ? "(R)" : "");

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgaBV = 0;
                for (Mounted possibleMG : getWeaponList()) {
                    if (possibleMG.getType().hasFlag(WeaponType.F_MG) && (possibleMG.getLocation() == mounted.getLocation())) {
                        mgaBV += possibleMG.getType().getBV(this);
                    }
                }
                dBV = mgaBV * 0.67;
            }

            // if linked to AES, multiply by 1.5
            if (hasFunctionalArmAES(mounted.getLocation())) {
                dBV *= 1.5;
            }
            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
            }
            // artemis bumps up the value
            // PPC caps do, too
            if (mounted.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) mounted.getLinkedBy().getType()).getBV(this, mounted);
                }
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    weaponName = weaponName.concat(" with Artemis");
                }
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    weaponName = weaponName.concat(" with Apollo");
                }
            }
            // half for being rear mounted (or front mounted, when more rear-
            // than front-mounted un-modded BV
            if (!isArm(mounted.getLocation()) && ((mounted.isRearMounted() && halveRear) || (!mounted.isRearMounted() && !halveRear))) {
                dBV /= 2;
            }

            // ArrayList that stores weapon values
            // stores a double first (BV), then an Integer (heat),
            // then a String (weapon name)
            // for 0 heat weapons, just stores BV and name
            ArrayList<Object> weaponValues = new ArrayList<Object>();
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
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA)) || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this) + weaponsForExcessiveAmmo.get(key));
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
            // heatefficiency is reached or passed with one weapon

            // sort the heat-using weapons by modified BV
            Collections.sort(heatBVs, new Comparator<ArrayList<Object>>() {
                public int compare(ArrayList<Object> obj1, ArrayList<Object> obj2) {
                    // first element in the the ArrayList is BV, second is heat
                    // if same BV, lower heat first
                    if (obj1.get(0).equals(obj2.get(0))) {
                        return (int) Math.ceil((Double) obj1.get(1) - (Double) obj2.get(1));
                    }
                    // higher BV first
                    return (int) Math.ceil((Double) obj2.get(0) - (Double) obj1.get(0));
                }
            });
            // count heat-generating weapons at full modified BV until
            // heatefficiency is reached or
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
                    dBV /= 2;
                }
                if (heatAdded >= mechHeatEfficiency) {
                    bvText.append("Heat efficiency reached, half BV");
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
                bvText.append("Heat count: " + heatAdded);
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

            if (mtype.hasFlag(MiscType.F_ECM) || mtype.hasFlag(MiscType.F_BAP) || mtype.hasFlag(MiscType.F_AP_POD)
                    // not yet coded: || etype.hasFlag(MiscType.F_BRIDGE_LAYING)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)) {
                // weapons
                continue;
            }
            double bv = mtype.getBV(this);
            // if physical weapon linked to AES, multiply by 1.5
            if ((mtype.hasFlag(MiscType.F_CLUB) || mtype.hasFlag(MiscType.F_HAND_WEAPON)) && hasFunctionalArmAES(mounted.getLocation())) {
                bv *= 1.5;
            }

            if (bv > 0) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(mtype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(bv);
                bvText.append(endColumn);
                bvText.append(endRow);
            }

            oEquipmentBV += bv;
            // need to do this here, a MiscType does not know the location
            // where it's mounted
            if (mtype.hasFlag(MiscType.F_HARJEL) && (mounted.getLocation() != Entity.LOC_NONE)) {
                if (this.getArmor(mounted.getLocation(), false) != IArmorState.ARMOR_DESTROYED) {
                    oEquipmentBV += this.getArmor(mounted.getLocation());
                }
                if (hasRearArmor(mounted.getLocation()) && (this.getArmor(mounted.getLocation(), true) != IArmorState.ARMOR_DESTROYED)) {
                    oEquipmentBV += this.getArmor(mounted.getLocation(), true);
                }
            }
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
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>();
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED) || (atype.getMunitionType() == AmmoType.M_HOMING)) {
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
                if (key.equals(new Integer(AmmoType.T_COOLANT_POD).toString() + "1")) {
                    ammoBV += ammo.get(key);
                }
            }
        }
        weaponBV += ammoBV;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Toal Ammo BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(ammoBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        double weight = getWeight();
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

        weight *= aesMultiplier;

        if (aesMultiplier > 1) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Weight x AES Multiplier ");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(weight);
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
        if (hasTSM()) {
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

        double speedFactor = Math.pow(1 + (((double) runMP + (Math.round((double) jumpMP / 2)) - 5) / 10), 1.2);
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

        // industrial without advanced firing control get's 0.9 mod to obv
        if (getCockpitType() == Mech.COCKPIT_INDUSTRIAL) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Offensive BV * 0.9 for Industrial 'Mech without advanced targeting system");
            bvText.append(endColumn);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(obv);
            bvText.append(" * 0.9 = ");
            obv *= 0.9;
            bvText.append(obv);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Offensive BV + Defensive BV");
        bvText.append(endColumn);
        bvText.append(startColumn);

        double finalBV = obv + dbv;

        bvText.append(dbv);
        bvText.append(" + ");
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        double cockpitMod = 1;
        if ((getCockpitType() == Mech.COCKPIT_SMALL) || (getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
            cockpitMod = 0.95;
            finalBV *= cockpitMod;
        }
        finalBV = Math.round(finalBV);
        bvText.append("Total BV * Cockpit Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(obv + dbv);
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
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        if (((hasC3MM() && (calculateFreeC3MNodes() < 2)) || (hasC3M() && (calculateFreeC3Nodes() < 3)) || (hasC3S() && (c3Master > NONE)) || (hasC3i() && (calculateFreeC3Nodes() < 5))) && !ignoreC3 && (game != null)) {
            int totalForceBV = 0;
            totalForceBV += this.calculateBattleValue(true, true);
            for (Entity e : game.getC3NetworkMembers(this)) {
                if (!equals(e) && onSameC3NetworkAs(e)) {
                    totalForceBV += e.calculateBattleValue(true, true);
                }
            }
            xbv += totalForceBV *= 0.05;
        }

        finalBV = (int) Math.round(finalBV + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = crew.getBVSkillMultiplier();
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        // don't factor pilot in if we are just calculating BV for C3 extra BV
        if (ignoreC3) {
            return (int) finalBV;
        }
        return retVal;
    }

    @Override
    public double getCost() {
        return getCost(null);
    }

    /**
     * Calculate the C-bill cost of the mech. Passing null as the argument will
     * skip the detailed report processing.
     *
     * @param detail
     *            buffer to append the detailed cost report to
     * @return The cost in C-Bills of the 'Mech in question.
     */
    public double getCost(StringBuffer detail) {
        double[] costs = new double[14];
        int i = 0;

        double cockpitCost = 0;
        if (getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            cockpitCost = 750000;
        } else if (getCockpitType() == Mech.COCKPIT_DUAL) {
            // FIXME
            cockpitCost = 0;
        } else if (getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
            // Command Consoles are listed as a cost of 500,000.
            // That appears to be in addition to the primary cockpit.
            cockpitCost = 700000;
        } else if (getCockpitType() == Mech.COCKPIT_SMALL) {
            cockpitCost = 175000;
        } else if (getCockpitType() == Mech.COCKPIT_INDUSTRIAL) {
            cockpitCost = 100000;
        } else {
            cockpitCost = 200000;
        }
        if (hasEiCockpit() && getCrew().getOptions().booleanOption("ei_implant")) {
            cockpitCost = 400000;
        }
        costs[i++] = cockpitCost;
        costs[i++] = 50000;// life support
        costs[i++] = weight * 2000;// sensors
        int muscCost = hasTSM() ? 16000 : 2000;
        costs[i++] = muscCost * weight;// musculature
        costs[i++] = EquipmentType.getStructureCost(structureType) * weight;// IS
        costs[i++] = getActuatorCost();// arm and/or leg actuators
        Engine engine = getEngine();
        costs[i++] = engine.getBaseCost() * engine.getRating() * weight / 75.0;
        if (getGyroType() == Mech.GYRO_XL) {
            costs[i++] = 750000 * (int) Math.ceil(getOriginalWalkMP() * weight / 100f) * 0.5;
        } else if (getGyroType() == Mech.GYRO_COMPACT) {
            costs[i++] = 400000 * (int) Math.ceil(getOriginalWalkMP() * weight / 100f) * 1.5;
        } else if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            costs[i++] = 500000 * (int) Math.ceil(getOriginalWalkMP() * weight / 100f) * 2;
        } else {
            costs[i++] = 300000 * (int) Math.ceil(getOriginalWalkMP() * weight / 100f);
        }
        double jumpBaseCost = 200;
        // You cannot have JJ's and UMU's on the same unit.
        if (hasUMU()) {
            costs[i++] = Math.pow(getAllUMUCount(), 2.0) * weight * jumpBaseCost;
        } else {
            if (getJumpType() == Mech.JUMP_BOOSTER) {
                jumpBaseCost = 150;
            } else if (getJumpType() == Mech.JUMP_IMPROVED) {
                jumpBaseCost = 500;
            }
            costs[i++] = Math.pow(getOriginalJumpMP(), 2.0) * weight * jumpBaseCost;
        }
        int freeSinks = hasDoubleHeatSinks() ? 0 : 10;// num of sinks we don't
        // pay for
        int sinkCost = hasDoubleHeatSinks() ? 6000 : 2000;
        costs[i++] = sinkCost * (heatSinks() - freeSinks);// cost of sinks
        costs[i++] = getArmorWeight() * EquipmentType.getArmorCost(armorType);
        costs[i++] = getWeaponsAndEquipmentCost();

        double cost = 0; // calculate the total
        for (int x = 0; x < i; x++) {
            cost += costs[x];
        }

        double omniMultiplier = 0;
        if (isOmni()) {
            omniMultiplier = 1.25f;
            cost *= omniMultiplier;
        }
        costs[i++] = -omniMultiplier; // negative just marks it as multiplier

        double weightMultiplier = 1 + (weight / 100f);
        costs[i++] = -weightMultiplier; // negative just marks it as multiplier
        cost = Math.round(cost * weightMultiplier);
        if (detail != null) {
            addCostDetails(cost, detail, costs);
        }
        return cost;
    }

    private void addCostDetails(double cost, StringBuffer detail, double[] costs) {
        String[] left = { "Cockpit", "Life Support", "Sensors", "Myomer", "Structure", "Actuators", "Engine", "Gyro", "Jump Jets", "Heatsinks", "Armor", "Equipment", "Omni Multiplier", "Weight Multiplier" };
        int maxLeft = 0;
        int maxRight = 0;
        int totalLineLength = 0;
        // find the maximum length of the columns.
        for (int l = 0; l < left.length; l++) {
            maxLeft = Math.max(maxLeft, left[l].length());
            maxRight = Math.max(maxRight, commafy.format(costs[l]).length());
        }
        maxLeft += 5; // leave some padding in the middle
        maxRight = Math.max(maxRight, commafy.format(cost).length());
        for (int i = 0; i < left.length; i++) {
            String both;
            if (costs[i] < 0) { // negative marks it as a multiplier
                both = StringUtil.makeLength(left[i], maxLeft) + StringUtil.makeLength("x" + commafy.format(costs[i] * -1), maxRight, true);
            } else if (costs[i] == 0) {
                both = StringUtil.makeLength(left[i], maxLeft) + StringUtil.makeLength("N/A", maxRight, true);
            } else {
                both = StringUtil.makeLength(left[i], maxLeft) + StringUtil.makeLength(commafy.format(costs[i]), maxRight, true);
            }
            detail.append(both + "\n");
            totalLineLength = both.length();
        }
        for (int x = 0; x < totalLineLength; x++) {
            detail.append("-");
        }
        detail.append("\n" + StringUtil.makeLength("Total Cost:", maxLeft) + StringUtil.makeLength(commafy.format(cost), maxRight, true));
    }

    protected double getActuatorCost() {
        return getArmActuatorCost() + getLegActuatorCost();
    }

    protected abstract double getArmActuatorCost();

    protected abstract double getLegActuatorCost();

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<Report>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7030);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(crew.getDescVector(false));
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
        if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 0) {

            if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) == 1) {
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
        if (getCrew().getOptions().booleanOption("vdni") && !getCrew().getOptions().booleanOption("bvdni")) {
            roll.addModifier(-1, "VDNI");
        }

        // Small/torso-mounted cockpit penalty?
        if ((getCockpitType() == Mech.COCKPIT_SMALL) && !getCrew().getOptions().booleanOption("bvdni")) {
            roll.addModifier(1, "Small Cockpit");
        } else if (getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            roll.addModifier(1, "Torso-Mounted Cockpit");
            int sensorHits = getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                roll.addModifier(4, "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                roll.addModifier(4, "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        if (getArmorType() == EquipmentType.T_ARMOR_HARDENED) {
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
        return 2;
    }

    /**
     * Determine if this unit has an active and working stealth system.
     * (stealth can be active and not working when under ECCM)
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

                if (mEquip.curMode().equals("On") && hasActiveECM()
                        && !Compute.isAffectedByECCM(this, getPosition(), getPosition())) {
                    // Return true if the mode is "On" and ECM is working
                    // and we're not in ECCM
                    return true;
                }
            }
        }
        // No Mek Stealth or system inactive. Return false.
        return false;
    }

    /**
     * Determine if this unit has an active and working stealth system.
     * (stealth can be active and not working when under ECCM)
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
     * Does the mech have a functioning null signature system?
     */
    @Override
    public boolean isNullSigActive() {
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_NULLSIG) && m.curMode().equals("On") && m.isReady()) {
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
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_VOIDSIG) && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the mech have a functioning Chameleon Light Polarization Field?
     */
    @Override
    public boolean isChameleonShieldActive() {
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_CHAMELEON_SHIELD) && m.curMode().equals("On") && m.isReady()) {
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

        boolean isInfantry = (ae instanceof Infantry) && !(ae instanceof BattleArmor);
        // Stealth or null sig must be active.
        if (!isStealthActive() && !isNullSigActive() && !isChameleonShieldActive()) {
            result = new TargetRoll(0, "stealth not active");
        }
        // Determine the modifier based upon the range.
        else {
            switch (range) {
            case RangeType.RANGE_MINIMUM:
            case RangeType.RANGE_SHORT:
                if (isStealthActive() && !isInfantry) {
                    result = new TargetRoll(0, "stealth");
                } else if (isNullSigActive() && !isInfantry) {
                    result = new TargetRoll(0, "null-sig");
                    if (isChameleonShieldActive()) {
                        result.addModifier(0, "chameleon");
                    }
                } else if (isChameleonShieldActive()) {
                    result = new TargetRoll(0, "chameleon");
                } else {
                    // must be infantry
                    result = new TargetRoll(0, "infantry ignore stealth");
                }
                break;
            case RangeType.RANGE_MEDIUM:
                if (isStealthActive() && !isInfantry) {
                    result = new TargetRoll(1, "stealth");
                } else if (isNullSigActive() && !isInfantry) {
                    result = new TargetRoll(1, "null-sig");
                    if (isChameleonShieldActive()) {
                        result.addModifier(0, "chameleon");
                    }
                } else if (isChameleonShieldActive()) {
                    result = new TargetRoll(1, "chameleon");
                } else {
                    // must be infantry
                    result = new TargetRoll(0, "infantry ignore stealth");
                }
                break;
            case RangeType.RANGE_LONG:
                if (isStealthActive() && !isInfantry) {
                    result = new TargetRoll(2, "stealth");
                } else if (isNullSigActive() && !isInfantry) {
                    result = new TargetRoll(2, "null-sig");
                    if (isChameleonShieldActive()) {
                        result.addModifier(2, "chameleon");
                    }
                } else if (isChameleonShieldActive()) {
                    result = new TargetRoll(2, "chameleon");
                } else {
                    // must be infantry
                    result = new TargetRoll(0, "infantry ignore stealth");
                }
                break;
            case RangeType.RANGE_EXTREME:
                if (isStealthActive()) {
                    result = new TargetRoll(2, "stealth");
                } else if (isNullSigActive()) {
                    result = new TargetRoll(2, "null-sig");
                    if (isChameleonShieldActive()) {
                        result.addModifier(2, "chameleon");
                    }
                } else if (isChameleonShieldActive()) {
                    result = new TargetRoll(2, "chameleon");
                } else {
                    // must be infantry
                    result = new TargetRoll(0, "infantry ignore stealth");
                }
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
        return isSalvage() && (loc_is != IArmorState.ARMOR_DOOMED) && (loc_is != IArmorState.ARMOR_DESTROYED);
    }

    @Override
    public boolean canCharge() {
        // Mechs can charge, unless they are Clan and the "no clan physicals"
        // option is set
        return super.canCharge() && !(game.getOptions().booleanOption("no_clan_physical") && isClan());
    }

    @Override
    public boolean canDFA() {
        // Mechs can DFA, unless they are Clan and the "no clan physicals"
        // option is set
        return super.canDFA() && !(game.getOptions().booleanOption("no_clan_physical") && isClan());
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
                } else if (mounted.getType().hasFlag(MiscType.F_LASER_HEAT_SINK)) {
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
        if (getCockpitType() == COCKPIT_TORSO_MOUNTED) {
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

    @Override
    public boolean removePartialCoverHits(int location, int cover, int side) {
        // left and right cover are from attacker's POV.
        // if hitting front arc, need to swap them
        if (((cover & LosEffects.COVER_UPPER) == LosEffects.COVER_UPPER) && ((location == Mech.LOC_CT) || (location == Mech.LOC_HEAD))) {
            return true;
        }
        if (side == ToHitData.SIDE_FRONT) {
            if (((cover & LosEffects.COVER_LOWRIGHT) != 0) && (location == Mech.LOC_LLEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOWLEFT) != 0) && (location == Mech.LOC_RLEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0) && ((location == Mech.LOC_LARM) || (location == Mech.LOC_LT))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0) && ((location == Mech.LOC_RARM) || (location == Mech.LOC_RT))) {
                return true;
            }
        } else {
            if (((cover & LosEffects.COVER_LOWLEFT) != 0) && (location == Mech.LOC_LLEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOWRIGHT) != 0) && (location == Mech.LOC_RLEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0) && ((location == Mech.LOC_LARM) || (location == Mech.LOC_LT))) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0) && ((location == Mech.LOC_RARM) || (location == Mech.LOC_RT))) {
                return true;
            }
        }
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
        if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD) > 0) {
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
            if ((inType.equals(GYRO_STRING[x])) || (inType.equals(GYRO_SHORT_STRING[x]))) {
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
            if ((inType.equals(COCKPIT_STRING[x])) || (inType.equals(COCKPIT_SHORT_STRING[x]))) {
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
            return Mech.getCockpitDisplayString(cockpitType);
        }
        return systemNames[index];
    }

    public static String getGyroDisplayString(int inType) {
        String inName = "";
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
        String inName = "";
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
    public boolean isHexProhibited(IHex hex) {
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        return (hex.terrainLevel(Terrains.WOODS) > 2) || (hex.terrainLevel(Terrains.JUNGLE) > 2);
    }

    /**
     * Get an '.mtf' file representation of the mech. This string can be
     * directly written to disk as a file and later loaded by the MtfFile class.
     * Known missing level 3 features: mixed tech, laser heatsinks
     */
    public String getMtf() {
        StringBuffer sb = new StringBuffer();
        String nl = "\r\n"; // DOS friendly

        boolean standard = (getCockpitType() == Mech.COCKPIT_STANDARD) && (getGyroType() == Mech.GYRO_STANDARD);
        if (standard) {
            sb.append("Version:1.0").append(nl);
        } else {
            sb.append("Version:1.1").append(nl);
        }
        sb.append(chassis).append(nl);
        sb.append(model).append(nl);
        sb.append(nl);

        sb.append("Config:");
        if (this instanceof BipedMech) {
            sb.append("Biped");
        } else if (this instanceof QuadMech) {
            sb.append("Quad");
        }

        if (isOmni()) {
            sb.append(" Omnimech");
        }

        sb.append(nl);
        sb.append("TechBase:");
        if (isMixedTech()) {
            if (isClan()) {
                sb.append("Mixed (Clan Chassis)");
            } else {
                sb.append("Mixed (IS Chassis)");
            }
        } else {
            sb.append(TechConstants.getTechName(techLevel));
        }
        sb.append(nl);
        sb.append("Era:").append(year).append(nl);
        if ((source != null) && (source.trim().length() > 0)) {
            sb.append("Source:").append(source).append(nl);
        }
        sb.append("Rules Level:").append(TechConstants.T_SIMPLE_LEVEL[techLevel]);
        sb.append(nl);
        sb.append(nl);

        Float tonnage = new Float(weight);
        sb.append("Mass:").append(tonnage.intValue()).append(nl);
        sb.append("Engine:").append(getEngine().getEngineName()).append(" Engine");
        sb.append(nl);
        sb.append("Structure:");
        sb.append(EquipmentType.getStructureTypeName(getStructureType()));
        sb.append(nl);

        sb.append("Myomer:");
        if (hasTSM()) {
            sb.append("Triple-Strength");
        } else if (hasIndustrialTSM()) {
            sb.append("Industrial Triple-Strength");
        } else {
            sb.append("Standard");
        }
        sb.append(nl);

        if (!standard) {
            sb.append("Cockpit:");
            sb.append(getCockpitTypeString());
            sb.append(nl);

            sb.append("Gyro:");
            sb.append(getGyroTypeString());
            sb.append(nl);
        }
        sb.append(nl);

        sb.append("Heat Sinks:").append(heatSinks()).append(" ");
        if (hasLaserHeatSinks()) {
            sb.append("Laser");
        } else if (hasDoubleHeatSinks()) {
            sb.append("Double");
        } else {
            sb.append("Single");
        }
        sb.append(nl);

        if (isOmni()) {
            sb.append("Base Chassis Heat Sinks:");
            sb.append(getEngine().getBaseChassisHeatSinks());
            sb.append(nl);
        }

        sb.append("Walk MP:").append(walkMP).append(nl);
        sb.append("Jump MP:").append(jumpMP).append(nl);
        sb.append(nl);

        sb.append("Armor:").append(EquipmentType.getArmorTypeName(getArmorType()));
        sb.append("(" + TechConstants.getTechName(getArmorTechLevel()) + ")");
        sb.append(nl);

        for (int element : MtfFile.locationOrder) {
            sb.append(getLocationAbbr(element)).append(" Armor:");
            sb.append(getOArmor(element, false)).append(nl);
        }
        for (int element : MtfFile.rearLocationOrder) {
            sb.append("RT").append(getLocationAbbr(element).charAt(0)).append(" Armor:");
            sb.append(getOArmor(element, true)).append(nl);
        }
        sb.append(nl);

        sb.append("Weapons:").append(weaponList.size()).append(nl);
        for (int i = 0; i < weaponList.size(); i++) {
            Mounted m = weaponList.get(i);
            sb.append(m.getName()).append(", ").append(getLocationName(m.getLocation())).append(nl);
        }
        sb.append(nl);

        for (int l : MtfFile.locationOrder) {
            String locationName = getLocationName(l);
            sb.append(locationName + ":");
            sb.append(nl);
            for (int y = 0; y < 12; y++) {
                if (y < getNumberOfCriticals(l)) {
                    sb.append(decodeCritical(getCritical(l, y))).append(nl);
                } else {
                    sb.append(MtfFile.EMPTY).append(nl);
                }
            }
            sb.append(nl);
        }

        return sb.toString();
    }

    private String decodeCritical(CriticalSlot cs) {
        if (cs == null) {
            return MtfFile.EMPTY;
        }
        int type = cs.getType();
        int index = cs.getIndex();
        String armoredText = "";

        if (cs.isArmored()) {
            armoredText = " " + MtfFile.ARMORED;
        }
        if (type == CriticalSlot.TYPE_SYSTEM) {
            if ((systemNames[index].indexOf("Upper") != -1) || (systemNames[index].indexOf("Lower") != -1) || (systemNames[index].indexOf("Hand") != -1) || (systemNames[index].indexOf("Foot") != -1)) {
                return systemNames[index] + " Actuator" + armoredText;
            } else if (systemNames[index].indexOf("Engine") != -1) {
                return "Fusion " + systemNames[index] + armoredText;
            } else {
                return systemNames[index] + armoredText;
            }
        } else if (type == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted m = getEquipment(cs.getIndex());
            if (m.isRearMounted()) {
                return m.getType().getInternalName() + " (R)" + armoredText;
            }
            return m.getType().getInternalName() + armoredText;
        } else {
            return "?" + index;
        }
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
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_STANDARD);
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
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
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
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
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
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
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
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        setCockpitType(COCKPIT_SMALL);
        return true;
    }

    /**
     * Dual Cockpits need to be implemented everywhere except here. FIXME
     */
    public boolean addCommandConsole() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        setCockpitType(COCKPIT_COMMAND_CONSOLE);
        return true;
    }

    /**
     * Dual Cockpits need to be implemented everywhere except here. FIXME
     */
    public boolean addDualCockpit() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        setCockpitType(COCKPIT_DUAL);
        return true;
    }

    /**
     * Add the critical slots necessary for a torso-mounted cockpit. Note: This
     * is part of the mek creation public API, and might not be referenced by
     * any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addTorsoMountedCockpit() {
        boolean success = true;
        if (getEmptyCriticals(LOC_HEAD) < 2) {
            success = false;
        } else {
            addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
            addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        }

        if ((getEmptyCriticals(LOC_CT) < 2) || !success) {
            success = false;
        } else {
            addCritical(LOC_CT, getFirstEmptyCrit(LOC_CT), new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
            addCritical(LOC_CT, getFirstEmptyCrit(LOC_CT), new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        }

        if ((getEmptyCriticals(LOC_LT) < 1) || (getEmptyCriticals(LOC_RT) < 1) || !success) {
            success = false;
        } else {
            addCritical(LOC_LT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
            addCritical(LOC_RT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        }

        if (success) {
            setCockpitType(COCKPIT_TORSO_MOUNTED);
        }
        return success;
    }

    /**
     * Add the critical slots necessary for a standard gyro. Also set the gyro
     * type variable. Note: This is part of the mek creation public API, and
     * might not be referenced by any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addGyro() {
        if (getEmptyCriticals(LOC_CT) < 4) {
            return false;
        }
        addCompactGyro();
        addCritical(LOC_CT, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        addCritical(LOC_CT, 6, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        setGyroType(GYRO_STANDARD);
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
        addCritical(LOC_CT, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        addCritical(LOC_CT, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
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
        addCritical(LOC_CT, 7, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        addCritical(LOC_CT, 8, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
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
        boolean success = true;

        int centerSlots[] = getEngine().getCenterTorsoCriticalSlots(getGyroType());
        if (getEmptyCriticals(LOC_CT) < centerSlots.length) {
            success = false;
        } else {
            for (int centerSlot : centerSlots) {
                addCritical(LOC_CT, centerSlot, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
            }
        }

        int sideSlots[] = getEngine().getSideTorsoCriticalSlots();
        if ((getEmptyCriticals(LOC_LT) < sideSlots.length) || (getEmptyCriticals(LOC_RT) < sideSlots.length) || !success) {
            success = false;
        } else {
            for (int sideSlot : sideSlots) {
                addCritical(LOC_LT, sideSlot, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
                addCritical(LOC_RT, sideSlot, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
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
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
        }
    }

    /**
     * Remove all cockpit critical slots from the mek. Note: This is part of the
     * mek creation public API, and might not be referenced by any MegaMek code.
     */
    public void clearCockpitCrits() {
        for (int i = 0; i < locations(); i++) {
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        }
    }

    /**
     * Remove all gyro critical slots from the mek. Note: This is part of the
     * mek creation public API, and might not be referenced by any MegaMek code.
     */
    public void clearGyroCrits() {
        for (int i = 0; i < locations(); i++) {
            removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        }
    }

    public int shieldAbsorptionDamage(int damage, int location, boolean rear) {
        int damageAbsorption = damage;
        if (this.hasActiveShield(location, rear)) {
            switch (location) {
            case Mech.LOC_CT:
            case Mech.LOC_HEAD:
                if (this.hasActiveShield(Mech.LOC_RARM)) {
                    damageAbsorption = getAbsorptionRate(Mech.LOC_RARM, damageAbsorption);
                }
                if (this.hasActiveShield(Mech.LOC_LARM)) {
                    damageAbsorption = getAbsorptionRate(Mech.LOC_LARM, damageAbsorption);
                }
                break;
            case Mech.LOC_LARM:
            case Mech.LOC_LT:
            case Mech.LOC_LLEG:
                if (this.hasActiveShield(Mech.LOC_LARM)) {
                    damageAbsorption = getAbsorptionRate(Mech.LOC_LARM, damageAbsorption);
                }
                break;
            default:
                if (this.hasActiveShield(Mech.LOC_RARM)) {
                    damageAbsorption = getAbsorptionRate(Mech.LOC_RARM, damageAbsorption);
                }
            break;
            }
        }

        if (this.hasPassiveShield(location, rear)) {
            switch (location) {
            case Mech.LOC_LARM:
            case Mech.LOC_LT:
                if (this.hasPassiveShield(Mech.LOC_LARM)) {
                    damageAbsorption = getAbsorptionRate(Mech.LOC_LARM, damageAbsorption);
                }
                break;
            case Mech.LOC_RARM:
            case Mech.LOC_RT:
                if (this.hasPassiveShield(Mech.LOC_RARM)) {
                    damageAbsorption = getAbsorptionRate(Mech.LOC_RARM, damageAbsorption);
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
                    damageAbsorption = getAbsorptionRate(Mech.LOC_LARM, damageAbsorption);
                }
                break;
            case Mech.LOC_RARM:
                if (hasNoDefenseShield(Mech.LOC_RARM)) {
                    damageAbsorption = getAbsorptionRate(Mech.LOC_RARM, damageAbsorption);
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

            Mounted m = this.getEquipment(cs.getIndex());

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
    public boolean hasHarJelIn(int loc) {
        if (loc == Mech.LOC_HEAD) {
            return false;
        }
        for (Mounted mounted : getMisc()) {
            if ((mounted.getLocation() == loc) && mounted.isReady() && mounted.getType().hasFlag(MiscType.F_HARJEL)) {
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
    public boolean isEligibleForMovement() {
        if (grappled_id != Entity.NONE) {
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
        super.destroyLocation(loc);
        // if it's a leg, the entity falls
        if (locationIsLeg(loc)) {
            game.addPSR(new PilotingRollData(getId(), TargetRoll.AUTOMATIC_FAIL, 5, "leg destroyed"));
        }
    }

    @Override
    public boolean hasCASEII(int location) {

        for (Mounted mount : this.getEquipment()) {
            if ((mount.getLocation() == location) && (mount.getType() instanceof MiscType) && ((MiscType) mount.getType()).hasFlag(MiscType.F_CASEII)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void setGameOptions() {
        super.setGameOptions();

        for (Mounted mounted : getWeaponList()) {
            if ((mounted.getType() instanceof EnergyWeapon) && (((WeaponType) mounted.getType()).getAmmoType() == AmmoType.T_NA) && (game != null) && game.getOptions().booleanOption("tacops_energy_weapons")) {

                ArrayList<String> modes = new ArrayList<String>();
                String[] stringArray = {};

                if ((mounted.getType() instanceof PPCWeapon) && (((WeaponType) mounted.getType()).getMinimumRange() > 0) && game.getOptions().booleanOption("tacops_ppc_inhibitors")) {
                    modes.add("Field Inhibitor ON");
                    modes.add("Field Inhibitor OFF");
                }
                int damage = ((WeaponType) mounted.getType()).getDamage();

                if (damage == WeaponType.DAMAGE_VARIABLE) {
                    damage = ((WeaponType) mounted.getType()).damageShort;
                }

                for (; damage >= 0; damage--) {
                    modes.add("Damage " + damage);
                }
                if (((WeaponType) mounted.getType()).hasFlag(WeaponType.F_FLAMER)) {
                    modes.add("Heat");
                }
                ((WeaponType) mounted.getType()).setModes(modes.toArray(stringArray));
            }

        }

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
    public boolean hasModularArmor() {

        for (Mounted mount : this.getEquipment()) {
            if (!mount.isDestroyed() && (mount.getType() instanceof MiscType) && ((MiscType) mount.getType()).hasFlag(MiscType.F_MODULAR_ARMOR)) {
                return true;
            }
        }

        return false;

    }

    @Override
    public boolean hasModularArmor(int loc) {

        for (Mounted mount : this.getEquipment()) {
            if ((mount.getLocation() == loc) && !mount.isDestroyed() && (mount.getType() instanceof MiscType) && ((MiscType) mount.getType()).hasFlag(MiscType.F_MODULAR_ARMOR)) {
                return true;
            }
        }

        return false;

    }

    @Override
    public int getCoolantFailureAmount() {
        return heatSinkCoolantFailureFactor;
    }

    @Override
    public void addCoolantFailureAmount(int amount) {
        heatSinkCoolantFailureFactor += amount;
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
        if (((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 0) || hasHipCrit()) && (mpUsedLastRound > 0)) {
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
    public int getBARRating() {
        if (armorType == EquipmentType.T_ARMOR_COMMERCIAL) {
            return 5;
        }
        if ((armorType == EquipmentType.T_ARMOR_INDUSTRIAL) || (armorType == EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL)) {
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
        if (isIndustrial() && (getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            Report r = new Report(2280);
            r.addDesc(this);
            r.subject = getId();
            r.add(1);
            r.add("ICE-Engine Industrial Mech failed a PSR");
            vPhaseReport.add(r);
            r = new Report(2285);
            r.subject = getId();
            PilotingRollData base = getBasePilotingRoll();
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
            int diceRoll = Compute.d6(2);
            r = new Report(2300);
            r.subject = getId();
            r.add(base.getValueAsString());
            r.add(diceRoll);
            if (diceRoll <= base.getValue()) {
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
        if (stalled && !stalledThisTurn && isIndustrial() && (getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            Report r = new Report(2280);
            r.addDesc(this);
            r.subject = getId();
            r.add(1);
            r.add("unstall stalled ICE engine");
            vPhaseReport.add(r);
            r = new Report(2285);
            r.subject = getId();
            PilotingRollData base = new PilotingRollData(getId(), getCrew().getPiloting(), "Base piloting skill");
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
            int diceRoll = Compute.d6(2);
            r = new Report(2300);
            r.subject = getId();
            r.add(base.getValueAsString());
            r.add(diceRoll);
            if (diceRoll <= base.getValue()) {
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

    /**
     * Is this a primitive Mech?
     *
     * @return
     */
    public boolean isPrimitive() {
        return (getCockpitType() == Mech.COCKPIT_PRIMITIVE) || (getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL);
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

        int location = getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED ? Mech.LOC_CT : Mech.LOC_HEAD;

        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mech.SYSTEM_COCKPIT)) {
                return cs.isArmored();
            }
        }

        return false;
    }

    public boolean hasArmoredGyro() {
        for (int slot = 0; slot < getNumberOfCriticals(LOC_CT); slot++) {
            CriticalSlot cs = getCritical(LOC_CT, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mech.SYSTEM_GYRO)) {
                return cs.isArmored();
            }
        }

        return false;
    }

    @Override
    public boolean hasArmoredEngine() {
        for (int slot = 0; slot < getNumberOfCriticals(LOC_CT); slot++) {
            CriticalSlot cs = getCritical(LOC_CT, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mech.SYSTEM_ENGINE)) {
                return cs.isArmored();
            }
        }
        return false;
    }

    /**
     * should this mech check for a critical hit at the end of turn due to being
     * an industrial mech and having been the target of a succesfull physical
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

    public double getArmoredCritBV() {
        double bv = 0.0f;

        for (int loc = Mech.LOC_HEAD; loc <= Mech.LOC_LLEG; loc++) {
            for (int slot = 0; slot < getNumberOfCriticals(loc); slot++) {
                CriticalSlot cs = getCritical(loc, slot);
                if ((cs != null) && cs.isArmored()) {
                    bv += 5;
                }
            }
        }
        return bv;
    }

}
