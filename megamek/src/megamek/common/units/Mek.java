/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import static megamek.common.bays.Bay.UNSET_BAY;

import java.io.PrintWriter;
import java.io.Serial;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.SuiteConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.battleArmor.BattleArmorHandles;
import megamek.common.battleArmor.ProtoMekClampMount;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.cost.MekCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.MPBoosters;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.*;
import megamek.common.equipment.enums.BombType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.interfaces.ITechnology;
import megamek.common.loaders.MtfFile;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.weapons.autoCannons.ACWeapon;
import megamek.common.weapons.autoCannons.LBXACWeapon;
import megamek.common.weapons.autoCannons.UACWeapon;
import megamek.common.weapons.gaussRifles.GaussWeapon;
import megamek.common.weapons.ppc.PPCWeapon;
import megamek.logging.MMLogger;

/**
 * You know what Meks are, silly.
 */
public abstract class Mek extends Entity {
    @Serial
    private static final long serialVersionUID = -1929593228891136561L;
    private static final MMLogger LOGGER = MMLogger.create(Mek.class);

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
    public static final String[] systemNames = { "Life Support", "Sensors", "Cockpit", "Engine", "Gyro", null, null,
                                                 "Shoulder", "Upper Arm", "Lower Arm", "Hand", "Hip", "Upper Leg",
                                                 "Lower Leg", "Foot" };

    // locations
    public static final int LOC_HEAD = 0;
    public static final int LOC_CENTER_TORSO = 1;
    public static final int LOC_RIGHT_TORSO = 2;
    public static final int LOC_LEFT_TORSO = 3;
    public static final int LOC_RIGHT_ARM = 4;
    public static final int LOC_LEFT_ARM = 5;
    public static final int LOC_RIGHT_LEG = 6;
    public static final int LOC_LEFT_LEG = 7;
    // center leg, for tripods
    public static final int LOC_CENTER_LEG = 8;

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
    public static final String[] GYRO_STRING = { "Standard Gyro", "XL Gyro", "Compact Gyro", "Heavy Duty Gyro", "None",
                                                 "Superheavy Gyro" };
    public static final String[] GYRO_SHORT_STRING = { "Standard", "XL", "Compact", "Heavy Duty", "None",
                                                       "Superheavy" };

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
    public static final int COCKPIT_TRIPOD_INDUSTRIAL = 17;
    public static final int COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL = 18;

    public static final String[] COCKPIT_STRING = { "Standard Cockpit", "Small Cockpit", "Command Console",
                                                    "Torso-Mounted Cockpit", "Dual Cockpit", "Industrial Cockpit",
                                                    "Primitive Cockpit", "Primitive Industrial Cockpit",
                                                    "Superheavy Cockpit", "Superheavy Tripod Cockpit", "Tripod Cockpit",
                                                    "Interface Cockpit", "Virtual Reality Piloting Pod",
                                                    "QuadVee Cockpit", "Superheavy Industrial Cockpit",
                                                    "Superheavy Command Console", "Small Command Console",
                                                    "Tripod Industrial Cockpit",
                                                    "Superheavy Tripod Industrial Cockpit" };

    public static final String[] COCKPIT_SHORT_STRING = { "Standard", "Small", "Command Console", "Torso Mounted",
                                                          "Dual", "Industrial", "Primitive", "Primitive Industrial",
                                                          "Superheavy", "Superheavy Tripod", "Tripod", "Interface",
                                                          "VRPP", "Quadvee", "Superheavy Industrial",
                                                          "Superheavy Command", "Small Command", "Tripod Industrial",
                                                          "Superheavy Tripod Industrial" };

    public static final String FULL_HEAD_EJECT_STRING = "Full Head Ejection System";

    public static final String RISC_HEAT_SINK_OVERRIDE_KIT = "RISC Heat Sink Override Kit";

    /**
     * Contains a mapping of locations which are blocked when carrying cargo in the "key" location
     */
    public static final Map<Integer, List<Integer>> BLOCKED_FIRING_LOCATIONS;

    static {
        BLOCKED_FIRING_LOCATIONS = new HashMap<>();
        BLOCKED_FIRING_LOCATIONS.put(LOC_LEFT_ARM, new ArrayList<>());
        BLOCKED_FIRING_LOCATIONS.get(LOC_LEFT_ARM).add(LOC_LEFT_ARM);
        BLOCKED_FIRING_LOCATIONS.get(LOC_LEFT_ARM).add(LOC_LEFT_TORSO);
        BLOCKED_FIRING_LOCATIONS.get(LOC_LEFT_ARM).add(LOC_CENTER_TORSO);
        BLOCKED_FIRING_LOCATIONS.get(LOC_LEFT_ARM).add(LOC_RIGHT_TORSO);

        BLOCKED_FIRING_LOCATIONS.put(LOC_RIGHT_ARM, new ArrayList<>());
        BLOCKED_FIRING_LOCATIONS.get(LOC_RIGHT_ARM).add(LOC_RIGHT_ARM);
        BLOCKED_FIRING_LOCATIONS.get(LOC_RIGHT_ARM).add(LOC_LEFT_TORSO);
        BLOCKED_FIRING_LOCATIONS.get(LOC_RIGHT_ARM).add(LOC_CENTER_TORSO);
        BLOCKED_FIRING_LOCATIONS.get(LOC_RIGHT_ARM).add(LOC_RIGHT_TORSO);
    }

    // jump types
    public static final int JUMP_UNKNOWN = -1;
    public static final int JUMP_NONE = 0;
    public static final int JUMP_STANDARD = 1;
    public static final int JUMP_IMPROVED = 2;
    public static final int JUMP_PROTOTYPE = 3;
    public static final int JUMP_DISPOSABLE = 5;
    public static final int JUMP_PROTOTYPE_IMPROVED = 6;

    // Some "has" items only need be determined once
    public static final int HAS_FALSE = -1;
    public static final int HAS_UNKNOWN = 0;
    public static final int HAS_TRUE = 1;

    // rear armor
    private final int[] rearArmor;

    private final int[] orig_rearArmor;

    private final boolean[] rearHardenedArmorDamaged;

    // for Harjel II/III
    private final boolean[] armorDamagedThisTurn;

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

    protected int gyroType;

    protected int cockpitType;

    /**
     * Head armor provided by the Cowl quirk. Ignored when the unit doesn't have Cowl or quirks aren't used.
     */
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

    private boolean riscHeatSinkKit = false;

    protected static int[] EMERGENCY_COOLANT_SYSTEM_FAILURE = { 3, 5, 7, 10, 13, 13, 13 };

    // nCoolantSystemLevel is the # of turns RISC emergency coolant system has been
    // used previously
    protected int nCoolantSystemLevel = 0;

    protected boolean bCoolantWentUp = false;

    protected boolean bUsedCoolantSystem = false; // Has emergency coolant system been used?

    protected boolean bDamagedCoolantSystem = false; // is the emergency coolant system damaged?

    protected int nCoolantSystemMOS = 0;

    // Cooling System Flaws quirk
    private boolean coolingFlawActive = false;

    // QuadVees, LAMs, and tracked 'Meks can change movement mode.
    protected EntityMovementMode originalMovementMode = EntityMovementMode.BIPED;

    /**
     * Construct a new, blank, Mek.
     */
    public Mek() {
        this(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD);
    }

    public Mek(int inGyroType, int inCockpitType) {
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
        setCritical(LOC_RIGHT_LEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_RIGHT_LEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_RIGHT_LEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_RIGHT_LEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

        setCritical(LOC_LEFT_LEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_LEFT_LEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_LEFT_LEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_LEFT_LEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

        // Player setting specify whether their Meks' automatic
        // ejection systems are disabled by default or not.
        autoEject = !PreferenceManager.getClientPreferences()
              .defaultAutoEjectDisabled();

        switch (inCockpitType) {
            case COCKPIT_TRIPOD:
            case COCKPIT_TRIPOD_INDUSTRIAL:
                setCrew(new Crew(CrewType.TRIPOD));
                break;
            case COCKPIT_SUPERHEAVY_TRIPOD:
            case COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL:
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

    @Override
    public CrewType defaultCrewType() {
        return (cockpitType == COCKPIT_COMMAND_CONSOLE) || (cockpitType == COCKPIT_SUPERHEAVY_COMMAND_CONSOLE)
              || (cockpitType == COCKPIT_SMALL_COMMAND_CONSOLE) ? CrewType.COMMAND_CONSOLE : CrewType.SINGLE;
    }

    /**
     * @return True if this Mek can NOT stand up from hull down. Checks leg and gyro damage.
     */
    public abstract boolean cannotStandUpFromHullDown();

    /**
     * @return True if this Mek has the Cowl quirk and quirks are used in the present game.
     */
    public boolean hasCowl() {
        return hasQuirk(OptionsConstants.QUIRK_POS_COWL);
    }

    /**
     * Damages the remaining cowl armor, if any, by the given amount. Returns the amount of excess damage that is left
     * after deducting cowl armor. This method tests if the unit has Cowl and quirks are being used.
     *
     * @param amount The incoming damage
     *
     * @return The damage left after deducting the cowl's remaining armor, if any
     */
    public int damageCowl(int amount) {
        if (hasCowl()) {
            int excessDamage = Math.max(amount - cowlArmor, 0);
            cowlArmor = Math.max(cowlArmor - amount, 0);
            return excessDamage;
        } else {
            return amount;
        }
    }

    /**
     * Returns the location that transferred damage or crits will go to from a given location.
     */
    public static int getInnerLocation(int location) {
        return switch (location) {
            case Mek.LOC_RIGHT_TORSO, Mek.LOC_LEFT_TORSO, Mek.LOC_CENTER_LEG -> Mek.LOC_CENTER_TORSO;
            case Mek.LOC_LEFT_LEG, Mek.LOC_LEFT_ARM -> Mek.LOC_LEFT_TORSO;
            case Mek.LOC_RIGHT_LEG, Mek.LOC_RIGHT_ARM -> Mek.LOC_RIGHT_TORSO;
            default -> location;
        };
    }

    /**
     * Returns the location with the most restrictive firing arc for a weapon.
     */
    public static int mostRestrictiveLoc(int location1, int location2) {
        if (location1 == location2) {
            return location1;
        } else if (Mek.restrictScore(location1) >= Mek
              .restrictScore(location2)) {
            return location1;
        } else {
            return location2;
        }
    }

    /**
     * find the least restrictive location of the two locations passed in
     */
    public static int leastRestrictiveLoc(int location1, int location2) {
        if (location1 == location2) {
            return location2;
        } else if (Mek.restrictScore(location1) >= Mek
              .restrictScore(location2)) {
            return location2;
        } else {
            return location1;
        }
    }

    /**
     * Helper function designed to give relative restrictiveness of locations. Used for finding the most restrictive
     * firing arc for a weapon.
     */
    public static int restrictScore(int location) {
        return switch (location) {
            case Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_ARM -> 0;
            case Mek.LOC_RIGHT_TORSO, Mek.LOC_LEFT_TORSO -> 1;
            case Mek.LOC_CENTER_TORSO -> 2;
            default -> 3;
        };
    }

    /**
     * OmniMeks have handles for Battle Armor squads to latch onto. Please note, this method should only be called
     * during this Mek's construction.
     * <p>
     * Overrides <code>Entity#setOmni(boolean)</code>
     */
    @Override
    public void setOmni(boolean omni) {

        // Perform the superclass' action.
        super.setOmni(omni);

        // Add BattleArmorHandles to OmniMeks.
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
        // This is an unofficial rule and it doesn't work well anyway as changing the
        // option
        // does not affect units that are in the game
        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_BA_GRAB_BARS)) {
            if (getTransports().stream().noneMatch(transporter -> transporter instanceof BattleArmorHandles)) {
                addTransporter(new BattleArmorHandles());
            }
        } else {
            if (getTransports().stream().noneMatch(transporter -> transporter instanceof ClampMountMek)) {
                addTransporter(new ClampMountMek());
            }
        }
    }

    public void setProtoMekClampMounts() {
        boolean front = false;
        boolean rear = false;
        for (Transporter t : getTransports()) {
            if (t instanceof ProtoMekClampMount) {
                front |= !((ProtoMekClampMount) t).isRear();
                rear |= ((ProtoMekClampMount) t).isRear();
            }
        }
        if (!front) {
            addTransporter(new ProtoMekClampMount(false));
        }
        if (!rear) {
            addTransporter(new ProtoMekClampMount(true));
        }
    }

    /**
     * Some entities will always have certain transporters. This method is overloaded to support that.
     */
    @Override
    public void addIntrinsicTransporters() {
        setBAGrabBars();
        setProtoMekClampMounts();
        addRoofRack();
        if (!isOmni() && !hasBattleArmorHandles()) {
            addTransporter(new ClampMountMek());
        }
    }

    @Override
    public void load(Entity unit, boolean checkElev, int bayNumber) {
        if (unit.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
            boolean rear = bayNumber > 0;
            for (Transporter t : getTransports()) {
                if ((t instanceof ProtoMekClampMount)
                      && t.canLoad(unit)
                      && (!checkElev || (unit.getElevation() == getElevation()))
                      && (((ProtoMekClampMount) t).isRear() == rear)) {
                    t.load(unit);
                    unit.setTargetBay(UNSET_BAY);
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
     * @see megamek.common.units.Entity#newRound(int)
     */
    @Override
    public void newRound(int roundNumber) {
        // Walk through the Mek's miscellaneous equipment before
        // we apply our parent class' newRound() functionality
        // because Mek Stealth is set by the Entity#newRound() method.
        for (Mounted<?> m : getMisc()) {
            MiscType miscType = (MiscType) m.getType();

            // Stealth can not be turned on if it's ECM is destroyed.
            if (miscType.hasFlag(MiscType.F_STEALTH)
                  && m.getLinked().isDestroyed()
                  && m.getLinked().isBreached()) {
                m.setMode("Off");
            }
        } // Check the next piece of equipment.

        super.newRound(roundNumber);
        incrementMASCAndSuperchargerLevels();

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

        shouldDieAtEndOfTurnBecauseOfWater = isJustMovedIntoIndustrialKillingWater();
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
        return (loc == LOC_CENTER_TORSO) || (loc == LOC_RIGHT_TORSO) || (loc == LOC_LEFT_TORSO);
    }

    /**
     * Returns true if the location in question is a leg
     */
    @Override
    public boolean locationIsLeg(int loc) {
        return (loc == LOC_LEFT_LEG) || (loc == LOC_RIGHT_LEG);
    }

    /**
     * Count the number of destroyed or breached legs on the Mek
     */
    public int countBadLegs() {
        int badLegs = 0;

        for (int i = 0; i < locations(); i++) {
            badLegs += (locationIsLeg(i) && isLocationBad(i)) ? 1 : 0;
        }

        return badLegs;
    }

    /**
     * @return true if the Mek has at least one leg that is destroyed or breached.
     */
    public boolean atLeastOneBadLeg() {
        for (int i = 0; i < locations(); i++) {
            if (locationIsLeg(i) && isLocationBad(i)) {
                return true;
            }
        }
        return false;
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
            return (getGoodCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.ACTUATOR_HIP, loc) == 0);
        }

        return false;
    }

    /**
     * This function returns true iff the system is in perfect condition.
     *
     * @param system the system to check
     *
     * @return false if the system is damaged.
     */
    public boolean isSystemIntact(int system) {
        for (int loc = 0; loc < locations(); loc++) {
            int numCrits = getNumberOfCriticalSlots(loc);
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
            if (getGoodCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.ACTUATOR_UPPER_LEG, loc) == 0) {
                legCrits++;
            }
            if (getGoodCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.ACTUATOR_LOWER_LEG, loc) == 0) {
                legCrits++;
            }
            if (getGoodCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_FOOT,
                  loc) == 0) {
                legCrits++;
            }
        }

        return legCrits;
    }

    /**
     * does this Mek have composite internal structure?
     */
    public boolean hasCompositeStructure() {
        return (getStructureType() == EquipmentType.T_STRUCTURE_COMPOSITE);
    }

    /**
     * does this Mek have reinforced internal structure?
     */
    public boolean hasReinforcedStructure() {
        return (getStructureType() == EquipmentType.T_STRUCTURE_REINFORCED);
    }

    /**
     * does this Mek have working jump boosters?
     */
    public boolean hasJumpBoosters() {
        boolean jumpBoosters = false;
        for (Mounted<?> mEquip : getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (miscType.hasFlag(MiscType.F_JUMP_BOOSTER)) {

                // one crit destroyed they all screwed
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
     * Does this Mek have an extended retractable blade in working condition?
     */
    public boolean hasExtendedRetractableBlade() {
        for (Mounted<?> m : getEquipment()) {
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
     * Does the entity have a retracted blade in the given location? Only true for biped Meks
     */
    public boolean hasRetractedBlade(int loc) {
        return false;
    }

    /**
     * Check for whether the Mek has triple strength myomer
     *
     * @param includePrototype Whether to include prototype TSM in the check. Prototype TSM does not have a movement
     *                         bonus or a required heat level.
     *
     * @return Whether the Mek has TSM
     */
    public boolean hasTSM(boolean includePrototype) {
        for (Mounted<?> m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_TSM)
                  && (includePrototype || !m.getType().hasFlag(MiscType.F_PROTOTYPE))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean antiTSMVulnerable() {
        for (Mounted<?> m : getMisc()) {
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
     * Checks whether any type of TSM is active. Industrial and prototype are always on. Standard is on when heat level
     * is &gt;= 9.
     *
     * @param includeIndustrial Whether to include industrial TSM in the check
     *
     * @return Whether the Mek has some form of TSM and it's active
     */
    public boolean hasActiveTSM(boolean includeIndustrial) {
        for (Mounted<?> m : getMisc()) {
            if (includeIndustrial && m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                return true;
            } else if (m.getType().hasFlag(MiscType.F_TSM)) {
                return (heat >= 9) || m.getType().hasFlag(MiscType.F_PROTOTYPE);
            }
        }
        return false;
    }

    /**
     * The modifier for picking objects up based on the unit having TSM
     *
     * @return 2.0 if the entity has active TSM, 1.0 otherwise
     */
    public double getTSMPickupModifier() {
        return hasActiveTSM(true) ? 2.0 : 1.0;
    }

    /**
     * does this Mek have industrial TSM=
     */
    public boolean hasIndustrialTSM() {
        for (Mounted<?> m : getEquipment()) {
            if ((m.getType() instanceof MiscType)
                  && m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                return true;
            }
        }
        return false;
    }

    /**
     * does this Mek have a null-sig-system?
     */
    public boolean hasNullSig() {
        for (Mounted<?> mEquip : getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (miscType.hasFlag(MiscType.F_NULL_SIG)) {
                // The Mek has Null-Sig
                return true;
            }
        }
        return false;
    }

    /**
     * does this Mek have a void-sig-system?
     */
    public boolean hasVoidSig() {
        for (Mounted<?> mEquip : getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (miscType.hasFlag(MiscType.F_VOID_SIG)) {
                // The Mek has Void-Sig
                return true;
            }
        }
        return false;
    }

    /**
     * Does this Mek have tracks? Used for tracks as industrial equipment; QuadVees return false.
     */
    public boolean hasTracks() {
        for (Mounted<?> mEquip : getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (miscType.hasFlag(MiscType.F_TRACKS)) {
                // The Mek has tracks
                return true;
            }
        }
        return false;
    }

    /**
     * does this Mek have a chameleon light polarization shield?
     */
    public boolean hasChameleonShield() {
        for (Mounted<?> mEquip : getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (miscType.hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                // The Mek has Chameleon Light Polarization Field
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.units.Entity#getStandingHeat()
     */
    @Override
    public int getStandingHeat() {
        return hasEngine() ? getEngine().getStandingHeat(this) : 0;
    }

    /**
     * set this Mek's <code>Engine</code>
     *
     * @param e the <code>Engine</code> to set
     */
    @Override
    public void setEngine(Engine e) {
        super.setEngine(e);
        if (hasEngine() && getEngine().engineValid) {
            setOriginalWalkMP(calculateWalk());
        }
    }

    /**
     * Used to set this Mek's original walk mp
     *
     * @return these units calculated walking speed, dependent on engine rating and weight
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
     * @see megamek.common.units.Entity#getWalkHeat()
     */
    @Override
    public int getWalkHeat() {
        int extra = bDamagedCoolantSystem ? 1 : 0;
        return extra + (hasEngine() ? getEngine().getWalkHeat(this) : 0);
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        int mp;
        MPBoosters mpBoosters = getArmedMPBoosters();
        if (!mpCalculationSetting.ignoreMASC() && !mpBoosters.isNone()) {
            if (mpCalculationSetting.singleMASC()) {
                mp = MPBoosters.MASC_ONLY.calculateRunMP(getWalkMP(mpCalculationSetting));
            } else {
                mp = mpBoosters.calculateRunMP(getWalkMP(mpCalculationSetting));
            }
        } else {
            mp = super.getRunMP(mpCalculationSetting);
        }

        return Math.max(0, mp - hardenedArmorMPReduction());
    }

    /**
     * Returns this entity's running/flank mp as a string.
     */
    @Override
    public String getRunMPasString(boolean gameState) {
        MPBoosters mpBoosters = getMPBoosters();
        if (!mpBoosters.isNone()) {
            String str = getRunMPWithoutMASC() + "(" + getRunMP() + ")";
            if (gameState && game != null) {
                MPBoosters armed = getArmedMPBoosters();

                str += (mpBoosters.hasMASC() ? " MASC:" + getMASCTurns()
                      + (armed.hasMASC() ? "(" + getMASCTarget() + "+)" : "(NA)") : "")
                      + (mpBoosters.hasSupercharger() ? " Supercharger:" + getSuperchargerTurns()
                      + (armed.hasSupercharger() ? "(" + getSuperchargerTarget() + "+)" : "(NA)") : "");
            }
            return str;
        }
        return Integer.toString(getRunMP());
    }

    /**
     * Depends on engine type
     */
    @Override
    public int getRunHeat() {
        int extra = bDamagedCoolantSystem ? 1 : 0;
        extra += isEvading() && !hasWorkingSCM() ? 2 : 0;
        return extra + (hasEngine() ? getEngine().getRunHeat(this) : 0);
    }

    @Override
    public int getSprintMP(MPCalculationSetting mpCalculationSetting) {
        if (hasHipCrit()) {
            return getRunMP(mpCalculationSetting);
        }

        int mp;
        MPBoosters mpBoosters = getArmedMPBoosters();
        if (!mpCalculationSetting.ignoreMASC() && !mpBoosters.isNone()) {
            if (mpCalculationSetting.singleMASC()) {
                mp = MPBoosters.MASC_ONLY.calculateSprintMP(getWalkMP(mpCalculationSetting));
            } else {
                mp = mpBoosters.calculateSprintMP(getWalkMP(mpCalculationSetting));
            }
        } else {
            // normally, sprint MP is just 2x walk speed
            mp = getWalkMP(mpCalculationSetting) * 2;
        }

        return Math.max(0, mp - hardenedArmorMPReduction());
    }

    @Override
    public int getRunningGravityLimit() {
        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_SPRINT)) {
            return getSprintMP(MPCalculationSetting.NO_GRAVITY);
        } else {
            return getRunMP(MPCalculationSetting.NO_GRAVITY);
        }
    }

    /**
     * Depends on engine type
     */
    @Override
    public int getSprintHeat() {
        int extra = bDamagedCoolantSystem ? 1 : 0;
        return extra + (hasEngine() ? getEngine().getSprintHeat(this) : 0);
    }

    @Override
    public int getJumpMP(MPCalculationSetting mpCalculationSetting) {
        if (hasShield() && (getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return 0;
        }

        int mp = (int) getMisc().stream()
              .filter(m -> m.getType().hasFlag(MiscType.F_JUMP_JET))
              .filter(Mounted::isOperable)
              .count();

        if (!mpCalculationSetting.ignoreSubmergedJumpJets() && hasOccupiedHex() && getElevation() < 0) {
            int waterLevel = game.getHexOf(this).terrainLevel(Terrains.WATER);
            if (waterLevel > 1) {
                return 0;
            } else if (waterLevel == 1) {
                mp = torsoJumpJets();
            }
        }

        // apply Partial Wing bonus if we have the ability to jump
        if (mp > 0) {
            for (Mounted<?> mount : getMisc()) {
                if (mount.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                    mp += getPartialWingJumpBonus(mount, mpCalculationSetting);
                    break;
                }
            }
        }

        // Medium shield reduces jump mp by 1/shield
        mp -= getNumberOfShields(MiscType.S_SHIELD_MEDIUM);

        if (!mpCalculationSetting.ignoreModularArmor() && hasModularArmor()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreGravity()) {
            return Math.max(applyGravityEffectsOnMP(mp), 0);
        }

        return Math.max(mp, 0);
    }

    /**
     * @return The jump MP for the Mek's mechanical jump boosters, unmodified by damage, other equipment (shields) or
     *       any other effects.
     */
    public int getOriginalMechanicalJumpBoosterMP() {
        return (int) Math.round(getMisc().stream()
              .filter(m -> m.is(EquipmentTypeLookup.MECHANICAL_JUMP_BOOSTER))
              .findFirst().map(Mounted::getSize).orElse(0d));
    }

    @Override
    public int getMechanicalJumpBoosterMP(MPCalculationSetting mpCalculationSetting) {
        if (hasShield() && (getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return 0;
        }

        Optional<MiscMounted> mekMechanicalJumpBooster = getMisc().stream()
              .filter(m -> m.is(EquipmentTypeLookup.MECHANICAL_JUMP_BOOSTER))
              .filter(Mounted::isOperable)
              .findFirst();
        if (mekMechanicalJumpBooster.isEmpty()) {
            return 0;
        }

        int mp = getOriginalMechanicalJumpBoosterMP();

        // apply Partial Wing bonus if we have the ability to jump
        if (mp > 0) {
            for (Mounted<?> mount : getMisc()) {
                if (mount.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                    mp += getPartialWingJumpBonus(mount, mpCalculationSetting);
                    break;
                }
            }
        }

        // Medium shield reduces jump mp by 1/shield
        mp -= getNumberOfShields(MiscType.S_SHIELD_MEDIUM);

        if (!mpCalculationSetting.ignoreModularArmor() && hasModularArmor()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreGravity()) {
            return Math.max(applyGravityEffectsOnMP(mp), 0);
        }

        return Math.max(mp, 0);
    }

    public boolean hasChainDrape() {
        for (MiscMounted mount : getMisc()) {
            if (mount.getType().hasFlag(MiscType.F_CHAIN_DRAPE) && !mount.isDestroyed()) {
                return true;
            }
        }

        return false;
    }

    public int getPartialWingJumpWeightClassBonus() {
        return (getWeightClass() <= EntityWeightClass.WEIGHT_MEDIUM) ? 2 : 1;
    }

    public int getPartialWingJumpAtmosphereBonus() {
        return getPartialWingJumpAtmosphereBonus(MPCalculationSetting.STANDARD);
    }

    public int getPartialWingJumpAtmosphereBonus(MPCalculationSetting mpCalculationSetting) {
        int bonus;

        if (!mpCalculationSetting.ignoreWeather() && (game != null)) {
            if ((getWeightClass() <= EntityWeightClass.WEIGHT_MEDIUM)) {
                bonus = switch (game.getPlanetaryConditions().getAtmosphere()) {
                    case VACUUM, TRACE -> 0;
                    case THIN -> 1;
                    case VERY_HIGH -> 3;
                    default -> 2;
                };
            } else {
                bonus = switch (game.getPlanetaryConditions().getAtmosphere()) {
                    case VACUUM, TRACE, THIN -> 0;
                    case HIGH, VERY_HIGH -> 2;
                    default -> 1;
                };
            }
        } else {
            bonus = getPartialWingJumpWeightClassBonus();
        }

        return bonus;
    }

    /**
     * Gives the bonus to Jump MP conferred by a Mek partial wing.
     *
     * @param mount The mounted location of the Wing
     *
     * @return The Jump MP bonus conferred by the wing
     */
    public int getPartialWingJumpBonus(Mounted<?> mount, MPCalculationSetting mpCalculationSetting) {
        int bonus = getPartialWingJumpAtmosphereBonus(mpCalculationSetting);
        bonus -= getBadCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, getEquipmentNum(mount), Mek.LOC_RIGHT_TORSO);
        bonus -= getBadCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, getEquipmentNum(mount), Mek.LOC_LEFT_TORSO);
        return Math.max(bonus, 0);
    }

    public int getPartialWingJumpBonus(Mounted<?> mount) {
        return getPartialWingJumpBonus(mount, MPCalculationSetting.STANDARD);
    }

    /**
     * Gives the heat capacity bonus conferred by a Mek partial wing.
     *
     * @return the heat capacity bonus provided by the wing
     */
    private int getPartialWingHeatBonus() {
        int bonus;
        if (game != null) {
            bonus = switch (game.getPlanetaryConditions().getAtmosphere()) {
                case VACUUM -> 0;
                case TRACE -> 1;
                case THIN -> 2;
                default -> 3;
            };
        } else {
            bonus = 3;
        }

        return bonus;
    }

    /**
     * Returns the type of jump jet system the Mek has.
     */
    @Override
    public int getJumpType() {
        jumpType = JUMP_NONE;
        for (MiscMounted m : miscList) {
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
            }
        }
        return jumpType;
    }

    @Override
    public int getJumpHeat(int movedMP) {

        int extra = bDamagedCoolantSystem ? 1 : 0;

        // don't count movement granted by Partial Wing
        for (Mounted<?> mount : getMisc()) {
            if (mount.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                movedMP -= getPartialWingJumpBonus(mount);
                break;
            }
        }

        return switch (getJumpType()) {
            case JUMP_IMPROVED -> extra
                  + (hasEngine() ? getEngine().getJumpHeat((movedMP / 2) + (movedMP % 2)) : 0);
            case JUMP_PROTOTYPE_IMPROVED ->
                // min 6 heat, otherwise 2xJumpMp, XTRO:Succession Wars pg17
                  extra + (hasEngine() ? Math.max(6, getEngine().getJumpHeat(movedMP * 2)) : 0);
            case JUMP_DISPOSABLE -> extra;
            case JUMP_NONE -> 0;
            default -> extra + (hasEngine() ? getEngine().getJumpHeat(movedMP) : 0);
        };
    }

    /**
     * Returns the number of (working) jump jets mounted in the torsos.
     */
    public int torsoJumpJets() {
        int jump = 0;

        for (Mounted<?> mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_JUMP_JET)
                  && !mounted.isDestroyed() && !mounted.isBreached()
                  && locationIsTorso(mounted.getLocation())) {
                jump++;
            }
        }

        // apply Partial Wing bonus if we have the ability to jump
        if (jump > 0) {
            for (Mounted<?> mount : getMisc()) {
                if (mount.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                    jump += getPartialWingJumpBonus(mount);
                    break;
                }
            }
        }

        return jump;
    }

    @Override
    public boolean isEligibleForPavementOrRoadBonus() {
        // eligible if using Mek tracks
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
     * QuadVees and LAMs may not have to make PSRs to avoid falling depending on their mode, and Meks using tracks for
     * movement do not have to make PSRs for damage to gyro or leg actuators.
     *
     * @param gyroLegDamage Whether the PSR is due to damage to gyro or leg actuators
     *
     * @return true if the Mek can fall due to failed PSR.
     */
    @Override
    public boolean canFall(boolean gyroLegDamage) {
        return !isProne() && !(gyroLegDamage && movementMode == EntityMovementMode.TRACKED);
    }

    /**
     * Return the height of this Mek above the terrain.
     */
    @Override
    public int height() {
        return isProne() ? 0 : isSuperHeavy() ? 2 : 1;
    }

    /**
     * Adds heat sinks to the engine. Uses clan/normal depending on the currently set techLevel
     */
    public void addEngineSinks(int totalSinks, EquipmentFlag heatSinkFlag) {
        addEngineSinks(totalSinks, heatSinkFlag, isClan());
    }

    /**
     * Adds heat sinks to the engine. Adds either the engine capacity, or the entire number of heat sinks, whichever is
     * less
     */
    public void addEngineSinks(int totalSinks, EquipmentFlag heatSinkFlag,
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
     * base for adding engine sinks. Newer method allows externals to say how much are engine HS.
     *
     * @param totalSinks the amount of heats inks to add to the engine
     * @param sinkName   the <code>String</code> determining the type of heatsink to add. must be a lookup name of a
     *                   HeatSinkType
     */
    public void addEngineSinks(int totalSinks, String sinkName) {
        if (!hasEngine()) {
            return;
        }
        EquipmentType sinkType = EquipmentType.get(sinkName);

        if (sinkType == null) {
            LOGGER.info("Mek: can't find heat sink to add to engine");
        } else {
            int toAllocate = Math.min(totalSinks,
                  getEngine().integralHeatSinkCapacity(sinkType.hasFlag(MiscType.F_COMPACT_HEAT_SINK)));
            addEngineSinks(sinkName, toAllocate);
        }

    }

    /**
     * add heat sinks into the engine
     *
     * @param sinkName   the <code>String</code> determining the type of heatsink to add. must be a lookup name of a
     *                   HeatSinkType
     * @param toAllocate Number of hs to add to the Engine.
     */
    public void addEngineSinks(String sinkName, int toAllocate) {
        // this relies on these being the correct internalNames for these items
        EquipmentType sinkType = EquipmentType.get(sinkName);

        if (sinkType == null) {
            LOGGER.info("Mek: can't find heat sink to add to engine");
        }

        for (int i = 0; i < toAllocate; i++) {
            try {
                addEquipment(Mounted.createMounted(this, sinkType), Entity.LOC_NONE, false);
            } catch (LocationFullException ignored) {
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
            engineCritHeat += 5 * getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_ENGINE, Mek.LOC_CENTER_TORSO);
            engineCritHeat += 5 * getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_ENGINE, Mek.LOC_LEFT_TORSO);
            engineCritHeat += 5 * getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_ENGINE, Mek.LOC_RIGHT_TORSO);
        }
        // Partial Repairs of the engine cause additional heat
        if (getPartialRepairs().booleanOption("mek_reactor_3_crit")) {
            engineCritHeat += 8;
        }
        if (getPartialRepairs().booleanOption("mek_reactor_2_crit")) {
            engineCritHeat += 5;
        }
        if (getPartialRepairs().booleanOption("mek_reactor_1_crit")) {
            engineCritHeat += 3;
        }
        if (getPartialRepairs().booleanOption("mek_engine_replace")) {
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
     * @param countPrototypes Set TRUE to include Prototype Heat Sinks in the total.
     */
    public int heatSinks(boolean countPrototypes) {
        int sinks = 0;
        for (Mounted<?> mounted : getMisc()) {
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
        for (Mounted<?> mounted : getMisc()) {
            if (!mounted.isDestroyed()) {
                continue;
            }
            EquipmentType etype = mounted.getType();
            if (etype.hasFlag(MiscType.F_COMPACT_HEAT_SINK)
                  && (etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK) || etype
                  .hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE))) {
                sinks += 2;
            } else if (etype.hasFlag(MiscType.F_HEAT_SINK)
                  || etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                  || etype.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                sinks++;
            }
        }
        return sinks;
    }

    /**
     * Returns the name of the heat sinks mounted on this 'Mek.
     */
    public String getHeatSinkTypeName() {
        for (Mounted<?> m : getMisc()) {
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

        // if a Mek has no heat sink equipment, we pretend like it has standard heat
        // sinks.
        return "Heat Sink";
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

    public int getHeatCapacity(boolean includePartialWing, boolean includeRadicalHeatSink) {
        int capacity = 0;
        int activeCount = getActiveSinks();
        boolean isDoubleHeatSink = false;

        for (Mounted<?> mounted : getMisc()) {
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
                isDoubleHeatSink = true;
            } else if (mounted.getType().hasFlag(
                  MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                capacity += 2;
                isDoubleHeatSink = true;
            } else if (includePartialWing
                  && mounted.getType().hasFlag(MiscType.F_PARTIAL_WING)
                  && // unless all crits are destroyed, we get the bonus
                  ((getGoodCriticalSlots(CriticalSlot.TYPE_EQUIPMENT,
                        getEquipmentNum(mounted), Mek.LOC_RIGHT_TORSO) > 0)
                        || (getGoodCriticalSlots(CriticalSlot.TYPE_EQUIPMENT,
                        getEquipmentNum(mounted), Mek.LOC_LEFT_TORSO) > 0))) {
                capacity += getPartialWingHeatBonus();
                includePartialWing = false; // Only count the partial wing bonus
                // once.
            }
        }
        capacity -= damagedSCMCritCount() * (isDoubleHeatSink ? 2 : 1);
        // AirMek mode for LAMs confers the same heat benefits as a partial wing.
        if (includePartialWing && movementMode == EntityMovementMode.WIGE) {
            capacity += getPartialWingHeatBonus();
        }
        if (includeRadicalHeatSink
              && hasWorkingMisc(MiscType.F_RADICAL_HEATSINK)) {
            capacity += (int) Math.ceil(getActiveSinks() * 0.4);
        }

        // If the TacOps option for coolant failure is enabled, include reductions for
        // coolant failure
        if (game != null &&
              gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_COOLANT_FAILURE)) {
            capacity -= heatSinkCoolantFailureFactor;
        }

        return Math.max(capacity, 0);
    }

    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity(true, false) + Math.min(sinksUnderwater(), 6);
    }

    /**
     * Gets the number of heat sinks that are underwater.
     */
    private int sinksUnderwater() {
        if ((getPosition() == null) || isOffBoard() || !game.hasBoardLocation(getPosition(), getBoardId())) {
            return 0;
        }

        Hex curHex = game.getHex(getPosition(), getBoardId());
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
        for (Mounted<?> mounted : getMisc()) {
            if (mounted.isDestroyed() || mounted.isBreached()
                  || !locationIsLeg(mounted.getLocation())) {
                continue;
            }
            if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                sinksUnderwater++;
            } else if (mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                  || mounted.getType().hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)
                  || mounted.getType().hasFlag(MiscType.F_LASER_HEAT_SINK)) {
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
     * Returns the name of the type of movement used. This is Mek-specific.
     */
    @Override
    public String getMovementString(EntityMovementType movementType) {
        return switch (movementType) {
            case MOVE_SKID -> "Skidded";
            case MOVE_NONE, MOVE_CAREFUL_STAND -> "None";
            case MOVE_WALK -> "Walked";
            case MOVE_RUN -> "Ran";
            case MOVE_JUMP -> "Jumped";
            case MOVE_SPRINT -> "Sprinted";
            // LAM AirMek modes
            case MOVE_VTOL_WALK -> "Cruised";
            case MOVE_VTOL_RUN -> "Flanked";
            default -> "Unknown!";
        };
    }

    /**
     * Returns the name of the type of movement used. This is Mek-specific.
     */
    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
        return switch (movementType) {
            case MOVE_SKID -> "S";
            case MOVE_NONE -> "N";
            case MOVE_WALK -> "W";
            case MOVE_RUN -> "R";
            case MOVE_JUMP -> "J";
            case MOVE_SPRINT -> "Sp";
            // LAM AirMek modes
            case MOVE_VTOL_WALK -> "C";
            case MOVE_VTOL_RUN -> "F";
            default -> "?";
        };
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.units.Entity#canChangeSecondaryFacing()
     */
    @Override
    public boolean canChangeSecondaryFacing() {
        if (hasQuirk(OptionsConstants.QUIRK_NEG_NO_TWIST)) {
            return false;
        }
        return !(isProne() || isBracing() || getAlreadyTwisted());
    }

    /**
     * Can this Mek torso twist in the given direction?
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

    @Override
    public boolean hasRearArmor(int loc) {
        return (loc == LOC_CENTER_TORSO) || (loc == LOC_RIGHT_TORSO) || (loc == LOC_LEFT_TORSO);
    }

    /**
     * Returns the amount of armor in the location specified. Mek version, handles rear armor.
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
     * Returns the original amount of armor in the location specified. Mek version, handles rear armor.
     */
    @Override
    public int getOArmor(int loc, boolean rear) {
        if (rear && hasRearArmor(loc)) {
            return orig_rearArmor[loc];
        }
        return super.getOArmor(loc, rear);
    }

    /**
     * Sets the amount of armor in the location specified. Mek version, handles rear armor.
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
     * Initializes the rear armor on the Mek. Sets the original and starting point of the armor to the same number.
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
     * did the armor in this location take damage which did not destroy it at least once this turn? this is used to
     * decide whether to trigger Harjel II/III
     */
    public void setArmorDamagedThisTurn(int loc, boolean tookDamage) {
        armorDamagedThisTurn[loc] = tookDamage;
    }

    /**
     * did the armor in this location take damage which did not destroy it at least once this turn? this is used to
     * decide whether to trigger Harjel II/III
     */
    public boolean isArmorDamagedThisTurn(int loc) {
        return armorDamagedThisTurn[loc];
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    @Override
    public int getWeaponArc(int weaponNumber) {
        final Mounted<?> mounted = getEquipment(weaponNumber);

        // B-Pods need to be special-cased, they have 360 firing arc
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
        return switch (mounted.getLocation()) {
            case LOC_HEAD, LOC_CENTER_TORSO, LOC_RIGHT_TORSO, LOC_LEFT_TORSO, LOC_RIGHT_LEG, LOC_LEFT_LEG ->
                  Compute.ARC_FORWARD;
            case LOC_RIGHT_ARM -> getArmsFlipped() ? Compute.ARC_REAR : Compute.ARC_RIGHT_ARM;
            case LOC_LEFT_ARM -> getArmsFlipped() ? Compute.ARC_REAR : Compute.ARC_LEFT_ARM;
            default -> Compute.ARC_360;
        };
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc. If false, assume it fires into the primary.
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        // leg-mounted weapons fire into the primary arc, always
        return (getEquipment(weaponId).getLocation() != LOC_RIGHT_LEG) && (getEquipment(weaponId).getLocation()
              != LOC_LEFT_LEG);
        // other weapons into the secondary
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
     * @see megamek.common.units.Entity#rollHitLocation(int, int)
     */
    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, AimingMode.NONE, LosEffects.COVER_NONE);
    }

    /**
     * Wrapper that handles applying Edge (if allowed).
     *
     * @param table
     * @param side
     * @param aimedLocation
     * @param aimingMode
     * @param cover
     *
     * @return HitData, possibly re-rolled once (once!) with Edge.
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
          int cover) {
        HitData originalHit = innerRollHitLocation(table, side, aimedLocation, aimingMode, cover);
        return applyEdgeToHitLocation(originalHit, table, side, aimedLocation, aimingMode, cover);
    }

    /**
     * For units that can use Edge to re-roll hits, determine whether to do so (if possible).
     *
     * @param originalHit the hit to consider using Edge on.
     *
     * @return
     */
    public HitData applyEdgeToHitLocation(HitData originalHit, int table, int side, int aimedLocation,
          AimingMode aimingMode,
          int cover) {

        // Already used Edge on this hit!  No more mods allowed.
        if (originalHit.getUsedEdge()) {
            return originalHit;
        }

        // Can aimed hits be rerolled with Edge?  We can check originalHit.hitAimedLocation() if necessary
        // Note: shouldUseEdge() checks if crew has Edge remaining, no need for explicit check
        // Note: Edge use is _recorded_ here via setUndoneLocation(), but is _logged_ in the Damage Manager

        // Was this a TAC or Special Critical (AP, Tandem Charge warhead)
        if (originalHit.getEffect() == HitData.EFFECT_CRITICAL || originalHit.getSpecCrit()) {
            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_TAC)
                  && !gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_NO_TAC)) {
                getCrew().decreaseEdge();
                HitData result = innerRollHitLocation(table, side, aimedLocation, aimingMode, cover);
                result.setUndoneLocation(tac(table, side, originalHit.getLocation(), cover, false));
                result.setUsedEdge();
                return result;
            }

        }

        switch (originalHit.getLocation()) {
            case LOC_HEAD:
                if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEAD_HIT)) {
                    getCrew().decreaseEdge();
                    HitData result = innerRollHitLocation(table, side,
                          aimedLocation, aimingMode, cover);
                    result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                    result.setUsedEdge();
                    return result;
                }
        }

        return originalHit;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.units.Entity#rollHitLocation(int, int, int, int)
     */
    protected HitData innerRollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
          int cover) {
        int roll;

        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {
            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
        }

        boolean playtestLocations = gameOptions().booleanOption(OptionsConstants.PLAYTEST_1);

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
            } catch (Throwable t) {
                LOGGER.error("", t);
            }

            if (playtestLocations
                  && (side == ToHitData.SIDE_LEFT || side == ToHitData.SIDE_RIGHT)
                  && roll != 2 // clarified on forum, TACs don't go to the CT in this case
                // https://battletech.com/playtest-battletech/feedback-discussion/topic/through-armor-critical-hits-on-side-arc/
            ) {
                return getPlaytestSideLocation(table, side, cover);
            }

            if (side == ToHitData.SIDE_FRONT) {
                // normal front hits
                switch (roll) {
                    case 2:
                        return tac(table, side, Mek.LOC_CENTER_TORSO, cover, false);
                    case 3:
                    case 4:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_LEG);
                    case 6:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 7:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 8:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 9:
                        return new HitData(Mek.LOC_LEFT_LEG);
                    case 10:
                    case 11:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 12:
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_LEFT) {
                // normal left side hits
                switch (roll) {
                    case 2:
                        return tac(table, side, Mek.LOC_LEFT_TORSO, cover, false);
                    case 3, 6:
                        return new HitData(Mek.LOC_LEFT_LEG);
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 7:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 8:
                        if (gameOptions().booleanOption(
                              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_CENTER_TORSO, true);
                        }
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 9:
                        if (gameOptions().booleanOption(
                              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_RIGHT_TORSO, true);
                        }
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 10:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 11:
                        return new HitData(Mek.LOC_RIGHT_LEG);
                    case 12:
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_RIGHT) {
                // normal right side hits
                switch (roll) {
                    case 2:
                        return tac(table, side, Mek.LOC_RIGHT_TORSO, cover, false);
                    case 3, 6:
                        return new HitData(Mek.LOC_RIGHT_LEG);
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 7:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 8:
                        if (gameOptions().booleanOption(
                              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_CENTER_TORSO, true);
                        }
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 9:
                        if (gameOptions().booleanOption(
                              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_LEFT_TORSO, true);
                        }
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 10:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 11:
                        return new HitData(Mek.LOC_LEFT_LEG);
                    case 12:
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_REAR) {
                // normal rear hits
                if (gameOptions().booleanOption(
                      OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)
                      && isProne()) {
                    switch (roll) {
                        case 2:
                            return tac(table, side, Mek.LOC_CENTER_TORSO, cover, true);
                        case 3:
                            return new HitData(Mek.LOC_RIGHT_ARM, true);
                        case 4:
                        case 5:
                            return new HitData(Mek.LOC_RIGHT_LEG, true);
                        case 6:
                            return new HitData(Mek.LOC_RIGHT_TORSO, true);
                        case 7:
                            return new HitData(Mek.LOC_CENTER_TORSO, true);
                        case 8:
                            return new HitData(Mek.LOC_LEFT_TORSO, true);
                        case 9:
                        case 10:
                            return new HitData(Mek.LOC_LEFT_LEG, true);
                        case 11:
                            return new HitData(Mek.LOC_LEFT_ARM, true);
                        case 12:
                            return new HitData(Mek.LOC_HEAD, true);
                    }
                } else {
                    switch (roll) {
                        case 2:
                            return tac(table, side, Mek.LOC_CENTER_TORSO, cover, true);
                        case 3:
                        case 4:
                            return new HitData(Mek.LOC_RIGHT_ARM, true);
                        case 5:
                            return new HitData(Mek.LOC_RIGHT_LEG, true);
                        case 6:
                            return new HitData(Mek.LOC_RIGHT_TORSO, true);
                        case 7:
                            return new HitData(Mek.LOC_CENTER_TORSO, true);
                        case 8:
                            return new HitData(Mek.LOC_LEFT_TORSO, true);
                        case 9:
                            return new HitData(Mek.LOC_LEFT_LEG, true);
                        case 10:
                        case 11:
                            return new HitData(Mek.LOC_LEFT_ARM, true);
                        case 12:
                            return new HitData(Mek.LOC_HEAD, true);
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
            } catch (Throwable t) {
                LOGGER.error("", t);
            }

            if (side == ToHitData.SIDE_FRONT) {
                // front punch hits
                switch (roll) {
                    case 1:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 2:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 4:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 6:
                        return new HitData(Mek.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_LEFT) {
                // left side punch hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 6:
                        return new HitData(Mek.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_RIGHT) {
                // right side punch hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 6:
                        return new HitData(Mek.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_REAR) {
                // rear punch hits
                switch (roll) {
                    case 1:
                        return new HitData(Mek.LOC_LEFT_ARM, true);
                    case 2:
                        return new HitData(Mek.LOC_LEFT_TORSO, true);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO, true);
                    case 4:
                        return new HitData(Mek.LOC_RIGHT_TORSO, true);
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_ARM, true);
                    case 6:
                        return new HitData(Mek.LOC_HEAD, true);
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
            } catch (Throwable t) {
                LOGGER.error("", t);
            }

            if ((side == ToHitData.SIDE_FRONT) || (side == ToHitData.SIDE_REAR)) {
                // front/rear kick hits
                switch (roll) {
                    case 1:
                    case 2:
                    case 3:
                        return new HitData(Mek.LOC_RIGHT_LEG,
                              (side == ToHitData.SIDE_REAR));
                    case 4:
                    case 5:
                    case 6:
                        return new HitData(Mek.LOC_LEFT_LEG,
                              (side == ToHitData.SIDE_REAR));
                }
            }
            if (side == ToHitData.SIDE_LEFT) {
                // left-side kick hits
                return new HitData(Mek.LOC_LEFT_LEG);
            }
            if (side == ToHitData.SIDE_RIGHT) {
                // right-side kick hits
                return new HitData(Mek.LOC_RIGHT_LEG);
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
            } catch (Throwable t) {
                LOGGER.error("", t);
            }
            // Swarm attack locations.
            switch (roll) {
                case 2:
                    return new HitData(Mek.LOC_HEAD, false, effects);
                case 3, 11:
                    return new HitData(Mek.LOC_CENTER_TORSO, true, effects);
                case 4:
                    return new HitData(Mek.LOC_RIGHT_TORSO, true, effects);
                case 5:
                    return new HitData(Mek.LOC_RIGHT_TORSO, false, effects);
                case 6:
                    return new HitData(Mek.LOC_RIGHT_ARM, false, effects);
                case 7:
                    return new HitData(Mek.LOC_CENTER_TORSO, false, effects);
                case 8:
                    return new HitData(Mek.LOC_LEFT_ARM, false, effects);
                case 9:
                    return new HitData(Mek.LOC_LEFT_TORSO, false, effects);
                case 10:
                    return new HitData(Mek.LOC_LEFT_TORSO, true, effects);
                case 12:
                    return new HitData(Mek.LOC_HEAD, false, effects);
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
            } catch (Throwable t) {
                LOGGER.error("", t);
            }
            // Hits from above.
            switch (roll) {
                case 1:
                    return new HitData(Mek.LOC_LEFT_ARM, (side == ToHitData.SIDE_REAR));
                case 2:
                    return new HitData(Mek.LOC_LEFT_TORSO, (side == ToHitData.SIDE_REAR));
                case 3:
                    return new HitData(Mek.LOC_CENTER_TORSO, (side == ToHitData.SIDE_REAR));
                case 4:
                    return new HitData(Mek.LOC_RIGHT_TORSO, (side == ToHitData.SIDE_REAR));
                case 5:
                    return new HitData(Mek.LOC_RIGHT_ARM, (side == ToHitData.SIDE_REAR));
                case 6:
                    return new HitData(Mek.LOC_HEAD,
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
            } catch (Throwable t) {
                LOGGER.error("", t);
            }
            // Hits from below.
            switch (roll) {
                case 1:
                case 2:
                    return new HitData(Mek.LOC_LEFT_LEG, (side == ToHitData.SIDE_REAR));
                case 3:
                    return new HitData(Mek.LOC_LEFT_TORSO, (side == ToHitData.SIDE_REAR));
                case 4:
                    return new HitData(Mek.LOC_RIGHT_TORSO, (side == ToHitData.SIDE_REAR));
                case 5:
                case 6:
                    return new HitData(Mek.LOC_RIGHT_LEG, (side == ToHitData.SIDE_REAR));
            }
        }
        return null;
    }

    public HitData getPlaytestSideLocation(int table, int side, int cover) {
        var isLeft = side == ToHitData.SIDE_LEFT;

        var hitData = innerRollHitLocation(table, ToHitData.SIDE_FRONT, LOC_NONE, AimingMode.NONE, cover);
        hitData.setLocation(switch (hitData.getLocation()) {
            case LOC_LEFT_ARM, LOC_RIGHT_ARM -> isLeft ? LOC_LEFT_ARM : LOC_RIGHT_ARM;
            case LOC_LEFT_LEG, LOC_RIGHT_LEG -> isLeft ? LOC_LEFT_LEG : LOC_RIGHT_LEG;
            case LOC_LEFT_TORSO, LOC_RIGHT_TORSO -> isLeft ? LOC_LEFT_TORSO : LOC_RIGHT_TORSO;
            default -> hitData.getLocation();
        });

        return hitData;
    }

    /**
     * Called when a thru-armor-crit is rolled. Checks the game options and either returns no critical hit, rolls a
     * floating crit, or returns a TAC in the specified location.
     */
    protected HitData tac(int table, int side, int location, int cover,
          boolean rear) {
        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_NO_TAC)) {
            return new HitData(location, rear);
        } else if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_FLOATING_CRITS)) {
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
        return switch (hit.getLocation()) {
            case LOC_RIGHT_TORSO, LOC_LEFT_TORSO, LOC_CENTER_LEG ->
                  new HitData(LOC_CENTER_TORSO, hit.isRear(), hit.getEffect(),
                        hit.hitAimedLocation(), hit.getSpecCritMod(),
                        hit.getSpecCrit(), hit.isFromFront(),
                        hit.getGeneralDamageType(), hit.glancingMod());
            case LOC_LEFT_LEG, LOC_LEFT_ARM -> new HitData(LOC_LEFT_TORSO, hit.isRear(), hit.getEffect(),
                  hit.hitAimedLocation(), hit.getSpecCritMod(),
                  hit.getSpecCrit(), hit.isFromFront(),
                  hit.getGeneralDamageType(), hit.glancingMod());
            case LOC_RIGHT_LEG, LOC_RIGHT_ARM -> new HitData(LOC_RIGHT_TORSO, hit.isRear(), hit.getEffect(),
                  hit.hitAimedLocation(), hit.getSpecCritMod(),
                  hit.getSpecCrit(), hit.isFromFront(),
                  hit.getGeneralDamageType(), hit.glancingMod());
            case LOC_HEAD -> {
                if (getCockpitType() == COCKPIT_TORSO_MOUNTED) {
                    yield new HitData(LOC_NONE);
                }
                yield new HitData(LOC_DESTROYED);
            }
            default -> new HitData(LOC_DESTROYED);
        };
    }

    @Override
    public int getDependentLocation(int loc) {
        return switch (loc) {
            case LOC_RIGHT_TORSO -> LOC_RIGHT_ARM;
            case LOC_LEFT_TORSO -> LOC_LEFT_ARM;
            default -> LOC_NONE;
        };
    }

    /**
     * Sets the internal structure for the Mek.
     *
     * @param head head
     * @param ct   center torso
     * @param t    right/left torso
     * @param arm  right/left arm
     * @param leg  right/left leg
     */
    public abstract void setInternal(int head, int ct, int t, int arm, int leg);

    /**
     * Set the internal structure to the appropriate value for the Mek's weight class
     */
    @Override
    public void autoSetInternal() {
        // stupid irregular table... grr.
        switch ((int) weight) {
            // H, CT, TSO, ARM, LEG
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

    @Override
    public void addClanCase() {
        if (!isClan()) {
            return;
        }
        boolean explosiveFound;
        EquipmentType clCase = EquipmentType.get(EquipmentTypeLookup.CLAN_CASE);
        for (int i = 0; i < locations(); i++) {
            // Skip location if it already contains CASE
            if (locationHasCase(i) || hasCASEII(i)) {
                continue;
            }

            explosiveFound = false;
            for (Mounted<?> m : getEquipment()) {
                if (m.getType().isExplosive(m, true)
                      && ((m.getLocation() == i) || (m.getSecondLocation() == i))) {
                    explosiveFound = true;
                }
            }
            if (explosiveFound) {
                try {
                    addEquipment(Mounted.createMounted(this, clCase), i, false);
                } catch (LocationFullException ex) {
                    // um, that's impossible.
                }
            }
        }
    }

    /**
     * Adds equipment without adding slots for it. Specifically for targeting computers, which when loaded from a file
     * don't have a correct size and get loaded slot by slot
     */
    public MiscMounted addTargCompWithoutSlots(MiscType etype, int loc, boolean omniPod, boolean armored)
          throws LocationFullException {
        MiscMounted mounted = (MiscMounted) MiscMounted.createMounted(this, etype);
        mounted.setOmniPodMounted(omniPod);
        mounted.setArmored(armored);
        super.addEquipment(mounted, loc, false);
        return mounted;
    }

    public Mounted<?> addEquipment(EquipmentType etype, EquipmentType etype2,
          int loc, boolean omniPod, boolean armored) throws LocationFullException {
        Mounted<?> mounted = Mounted.createMounted(this, etype);
        Mounted<?> mounted2 = Mounted.createMounted(this, etype2);
        mounted.setOmniPodMounted(omniPod);
        mounted2.setOmniPodMounted(omniPod);
        mounted.setArmored(armored);
        mounted2.setArmored(armored);
        // check criticalSlots for space
        if (getEmptyCriticalSlots(loc) < 1) {
            throw new LocationFullException(mounted.getName() + " and "
                  + mounted2.getName() + " do not fit in "
                  + getLocationAbbr(loc) + " on " + getDisplayName()
                  + "\n        free criticalSlots in location: "
                  + getEmptyCriticalSlots(loc) + ", criticalSlots needed: " + 1);
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
    public void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted)
          throws LocationFullException {
        addEquipment(mounted, loc, rearMounted, -1);
    }

    /**
     * Mounts the specified weapon in the specified location.
     */
    @Override
    public void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted, int critSlot)
          throws LocationFullException {
        // if there's no actual location or this is a LAM capital fighter weapons group,
        // or ammo for a LAM bomb weapon then don't add criticalSlots
        if ((loc == LOC_NONE) || mounted.isWeaponGroup()
              || (mounted.getType() instanceof BombType)) {
            super.addEquipment(mounted, loc, rearMounted);
            return;
        }

        // spreadable or split equipment only gets added to 1 crit at a time,
        // since we don't know how many are in this location
        int reqSlots = mounted.getNumCriticalSlots();
        if (mounted.getType().isSpreadable() || mounted.isSplitable()) {
            reqSlots = 1;
        }
        if (isSuperHeavy()) {
            reqSlots = (int) Math.ceil(((double) reqSlots / 2.0f));
        }
        // gauss and AC weapons on omni arms means no arm actuators, so we
        // remove them
        if (isOmni()
              && (this instanceof BipedMek)
              && ((loc == LOC_LEFT_ARM) || (loc == LOC_RIGHT_ARM))
              && ((mounted.getType() instanceof GaussWeapon)
              || (mounted.getType() instanceof ACWeapon)
              || (mounted.getType() instanceof UACWeapon)
              || (mounted.getType() instanceof LBXACWeapon) || (mounted
              .getType() instanceof PPCWeapon))) {
            if (hasSystem(Mek.ACTUATOR_LOWER_ARM, loc)) {
                setCritical(loc, 2, null);
            }
            if (hasSystem(Mek.ACTUATOR_HAND, loc)) {
                setCritical(loc, 3, null);
            }
        }

        // check criticalSlots for space
        if (getEmptyCriticalSlots(loc) < reqSlots) {
            throw new LocationFullException(mounted.getName()
                  + " does not fit in " + getLocationAbbr(loc) + " on "
                  + getDisplayName()
                  + "\n        free criticalSlots in location: "
                  + getEmptyCriticalSlots(loc) + ", criticalSlots needed: "
                  + reqSlots);
        }
        // add it
        if (getEquipmentNum(mounted) == -1) {
            super.addEquipment(mounted, loc, rearMounted);
        }

        // add criticalSlots
        if (critSlot == -1) {
            for (int i = 0; i < reqSlots; i++) {
                CriticalSlot cs = new CriticalSlot(mounted);
                addCritical(loc, cs);
            }
        } else {
            // Need to ensure that we have enough contiguous critical slots
            int iterations = 0;
            while ((getContiguousNumberOfCrits(loc, critSlot) < reqSlots) &&
                  (iterations < getNumberOfCriticalSlots(loc))) {
                critSlot = (critSlot + 1) % getNumberOfCriticalSlots(loc);
                iterations++;
            }
            if (iterations >= getNumberOfCriticalSlots(loc)) {
                throw new LocationFullException(mounted.getName()
                      + " does not fit in " + getLocationAbbr(loc) + " on "
                      + getDisplayName()
                      + "\n    needs "
                      + getEmptyCriticalSlots(loc)
                      + " free contiguous criticalSlots");
            }
            for (int i = 0; i < reqSlots; i++) {
                CriticalSlot cs = new CriticalSlot(mounted);
                addCritical(loc, cs, critSlot);
                critSlot = (critSlot + 1) % getNumberOfCriticalSlots(loc);
            }
        }
    }

    @Override
    public List<WeaponMounted> getWeaponListWithHHW() {
        List<WeaponMounted> combinedWeaponList = new ArrayList<>(super.getWeaponList());
        if (!getCarriedObjects().isEmpty()) {
            Map<Integer, ICarryable> carriedObjects = getCarriedObjects();
            if (carriedObjects.get(LOC_RIGHT_ARM) != null) {
                if (carriedObjects.get(LOC_RIGHT_ARM) instanceof HandheldWeapon hhw) {
                    combinedWeaponList.addAll(hhw.getWeaponList());
                }
            } else if (carriedObjects.get(LOC_LEFT_ARM) != null) {
                if (carriedObjects.get(LOC_LEFT_ARM) instanceof HandheldWeapon hhw) {
                    combinedWeaponList.addAll(hhw.getWeaponList());
                }
            }
        }
        return combinedWeaponList;
    }

    // From IO pg 50
    public static TechAdvancement getTechAdvancement(long etype, boolean primitive, boolean industrial,
          int weightClass) {
        if ((etype & ETYPE_TRIPOD_MEK) != 0) {
            if (weightClass != EntityWeightClass.WEIGHT_SUPER_HEAVY) {
                return new TechAdvancement(TechBase.IS)
                      .setISAdvancement(2585, 2602).setISApproximate(true).setPrototypeFactions(Faction.TH)
                      .setProductionFactions(Faction.TH).setTechRating(TechRating.D)
                      .setAvailability(AvailabilityValue.F,
                            AvailabilityValue.F,
                            AvailabilityValue.F,
                            AvailabilityValue.E)
                      .setStaticTechLevel(SimpleTechLevel.ADVANCED);
            } else {
                return new TechAdvancement(TechBase.IS)
                      .setISAdvancement(2930, 2940).setISApproximate(true).setPrototypeFactions(Faction.FW)
                      .setProductionFactions(Faction.FW).setTechRating(TechRating.D)
                      .setAvailability(AvailabilityValue.X,
                            AvailabilityValue.F,
                            AvailabilityValue.X,
                            AvailabilityValue.F)
                      .setStaticTechLevel(SimpleTechLevel.ADVANCED);
            }
        } else if (primitive && industrial) {
            return new TechAdvancement(TechBase.IS)
                  .setISAdvancement(2300, 2350, 2425, 2520).setPrototypeFactions(Faction.TA)
                  .setProductionFactions(Faction.TH).setTechRating(TechRating.D)
                  .setAvailability(AvailabilityValue.D,
                        AvailabilityValue.X,
                        AvailabilityValue.F,
                        AvailabilityValue.F)
                  .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else if (primitive) {
            return new TechAdvancement(TechBase.IS)
                  .setISAdvancement(2439, 2443, 2470, 2520).setPrototypeFactions(Faction.TH)
                  .setProductionFactions(Faction.TH).setTechRating(TechRating.C)
                  .setAvailability(AvailabilityValue.C,
                        AvailabilityValue.X,
                        AvailabilityValue.F,
                        AvailabilityValue.F)
                  .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else if (industrial && (EntityWeightClass.WEIGHT_SUPER_HEAVY == weightClass)) {
            // Superheavy IndustrialMeks don't have a separate entry on the tech advancement
            // table in IO, but the dates for the superheavy tripod are based on the
            // three-man digging machine, which is an IndustrialMek.
            return new TechAdvancement(TechBase.IS)
                  .setAdvancement(2930, 2940).setPrototypeFactions(Faction.FW)
                  .setProductionFactions(Faction.FW).setTechRating(TechRating.D)
                  .setAvailability(AvailabilityValue.X,
                        AvailabilityValue.F,
                        AvailabilityValue.X,
                        AvailabilityValue.F)
                  .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else if (industrial) {
            return new TechAdvancement(TechBase.ALL)
                  // Book says 2460 but some of the systems required for non-primitive BMs don't exist until 2463
                  // IMs can't be constructed in 2460-2462 and trying causes bugs
                  .setAdvancement(2463, 2470, 2500).setPrototypeFactions(Faction.TH)
                  .setProductionFactions(Faction.TH).setTechRating(TechRating.C)
                  .setAvailability(AvailabilityValue.C,
                        AvailabilityValue.C,
                        AvailabilityValue.C,
                        AvailabilityValue.B)
                  .setStaticTechLevel(SimpleTechLevel.STANDARD);
        } else if (EntityWeightClass.WEIGHT_ULTRA_LIGHT == weightClass) {
            return new TechAdvancement(TechBase.ALL)
                  .setAdvancement(2500, 2519, 3075).setPrototypeFactions(Faction.TH, Faction.FW)
                  .setProductionFactions(Faction.FW).setApproximate(true, false, true)
                  .setTechRating(TechRating.D)
                  .setAvailability(AvailabilityValue.E,
                        AvailabilityValue.F,
                        AvailabilityValue.E,
                        AvailabilityValue.E)
                  .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else if (EntityWeightClass.WEIGHT_SUPER_HEAVY == weightClass) {
            return new TechAdvancement(TechBase.IS)
                  .setISAdvancement(3077, 3078).setPrototypeFactions(Faction.WB)
                  .setProductionFactions(Faction.WB).setTechRating(TechRating.D)
                  .setAvailability(AvailabilityValue.X,
                        AvailabilityValue.F,
                        AvailabilityValue.F,
                        AvailabilityValue.F)
                  .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        } else {
            return new TechAdvancement(TechBase.ALL)
                  // Book says 2460 but some of the systems required for non-primitive BMs don't exist until 2463
                  // BMs can't be constructed in 2460-2462 and trying causes bugs
                  .setAdvancement(2463, 2470, 2500).setPrototypeFactions(Faction.TH)
                  .setProductionFactions(Faction.TH).setTechRating(TechRating.D)
                  .setAvailability(AvailabilityValue.C,
                        AvailabilityValue.E,
                        AvailabilityValue.D,
                        AvailabilityValue.C)
                  .setStaticTechLevel(SimpleTechLevel.INTRO);
        }
    }

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return getTechAdvancement(getEntityType(), isPrimitive(), isIndustrial(), getWeightClass());
    }

    private static final TechAdvancement[] GYRO_TA = {
          new TechAdvancement(TechBase.ALL).setAdvancement(2300, 2350, 2505)
                .setApproximate(true, false, false).setPrototypeFactions(Faction.TA)
                .setProductionFactions(Faction.TH).setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.C,
                      AvailabilityValue.C,
                      AvailabilityValue.C,
                      AvailabilityValue.C)
                .setStaticTechLevel(SimpleTechLevel.INTRO), // Standard
          new TechAdvancement(TechBase.IS).setISAdvancement(3055, 3067, 3072)
                .setISApproximate(true, false, false).setPrototypeFactions(Faction.CS)
                .setProductionFactions(Faction.CS).setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.E,
                      AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), // XL
          new TechAdvancement(TechBase.IS).setISAdvancement(3055, 3068, 3072)
                .setISApproximate(true, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setProductionFactions(Faction.FS, Faction.LC)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.E,
                      AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), // Compact
          new TechAdvancement(TechBase.IS).setISAdvancement(3055, 3067, 3072)
                .setISApproximate(true, false, false).setPrototypeFactions(Faction.DC)
                .setProductionFactions(Faction.DC).setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.E,
                      AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), // Heavy duty
          new TechAdvancement(TechBase.IS).setAdvancement(ITechnology.DATE_NONE)
                .setTechRating(TechRating.A)
                .setAvailability(AvailabilityValue.A,
                      AvailabilityValue.A,
                      AvailabilityValue.A,
                      AvailabilityValue.A)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // None (placeholder)
          new TechAdvancement(TechBase.IS).setISAdvancement(2905, 2940)
                .setISApproximate(true, false).setPrototypeFactions(Faction.FW)
                .setProductionFactions(Faction.FW).setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.F,
                      AvailabilityValue.F,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Superheavy
    };

    private static final TechAdvancement[] COCKPIT_TA = {
          new TechAdvancement(TechBase.ALL).setAdvancement(2468, 2470, 2487)
                .setApproximate(true, false, false).setTechRating(TechRating.D)
                .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
                .setAvailability(AvailabilityValue.C,
                      AvailabilityValue.C,
                      AvailabilityValue.C,
                      AvailabilityValue.C)
                .setStaticTechLevel(SimpleTechLevel.INTRO), // Standard
          new TechAdvancement(TechBase.ALL).setISAdvancement(3060, 3067, 3080)
                .setISApproximate(true, false, false)
                .setClanAdvancement(ITechnology.DATE_NONE, 3080, 3080)
                .setTechRating(TechRating.E)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS, Faction.CJF)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.E,
                      AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), // Small
          new TechAdvancement(TechBase.ALL).setISAdvancement(2625, 2631, ITechnology.DATE_NONE, 2850, 3030)
                .setISApproximate(true, false, false, true, true)
                .setClanAdvancement(2625, 2631).setClanApproximate(true, false)
                .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS).setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.C,
                      AvailabilityValue.F,
                      AvailabilityValue.E,
                      AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Cockpit command console
          new TechAdvancement(TechBase.ALL).setISAdvancement(3053, 3080, 3100)
                .setClanAdvancement(3055, 3080, 3100)
                .setPrototypeFactions(Faction.FS, Faction.LC, Faction.CSJ)
                .setProductionFactions(
                      Faction.LC)
                .setApproximate(false, true, false)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL), // Torso mounted
          // FIXME: Dual is unofficial; these are stats for standard
          new TechAdvancement(TechBase.ALL).setAdvancement(2468, 2470, 2487)
                .setApproximate(true, false, false).setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.C,
                      AvailabilityValue.C,
                      AvailabilityValue.C,
                      AvailabilityValue.C)
                .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL), // Dual
          new TechAdvancement(TechBase.ALL).setAdvancement(2469, 2470, 2490)
                .setApproximate(true, false, false).setTechRating(TechRating.C)
                .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
                .setAvailability(AvailabilityValue.B,
                      AvailabilityValue.C,
                      AvailabilityValue.C,
                      AvailabilityValue.B)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), // Industrial
          new TechAdvancement(TechBase.ALL).setAdvancement(2430, 2439)
                .setApproximate(true, false).setTechRating(TechRating.D)
                .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
                .setAvailability(AvailabilityValue.D,
                      AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Primitive
          new TechAdvancement(TechBase.ALL).setAdvancement(2300, 2350, ITechnology.DATE_NONE, 2520)
                .setApproximate(true, false, false).setTechRating(TechRating.C)
                .setPrototypeFactions(Faction.TA).setProductionFactions(Faction.TH)
                .setAvailability(AvailabilityValue.C,
                      AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Primitive industrial
          new TechAdvancement(TechBase.IS).setISAdvancement(3060, 3076)
                .setISApproximate(true, false).setTechRating(TechRating.E)
                .setPrototypeFactions(Faction.WB).setProductionFactions(Faction.WB)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F,
                      AvailabilityValue.E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Superheavy
          new TechAdvancement(TechBase.IS).setISAdvancement(3130, 3135)
                .setISApproximate(true, false).setTechRating(TechRating.E)
                .setPrototypeFactions(Faction.RS).setProductionFactions(Faction.RS)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.F,
                      AvailabilityValue.X,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Superheavy tripod
          new TechAdvancement(TechBase.IS).setISAdvancement(2590, 2702)
                .setISApproximate(true, false).setTechRating(TechRating.F)
                .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Tripod
          new TechAdvancement(TechBase.ALL).setISAdvancement(3074).setClanAdvancement(3083)
                .setApproximate(true).setTechRating(TechRating.E)
                .setPrototypeFactions(Faction.WB, Faction.CHH)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL), // Cockpit interface
          new TechAdvancement(TechBase.IS).setISAdvancement(3052,
                      ITechnology.DATE_NONE,
                      ITechnology.DATE_NONE,
                      3055)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F,
                      AvailabilityValue.X)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL), // VRRP
          new TechAdvancement(TechBase.CLAN).setClanAdvancement(3130, 3135)
                .setClanApproximate(true, false).setTechRating(TechRating.F)
                .setPrototypeFactions(Faction.CHH).setProductionFactions(Faction.CHH)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // QuadVee
          new TechAdvancement(TechBase.IS).setISAdvancement(2905, 2940)
                .setISApproximate(true, false).setTechRating(TechRating.D)
                .setPrototypeFactions(Faction.FW).setProductionFactions(Faction.FW)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.F,
                      AvailabilityValue.F,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Superheavy industrial
          new TechAdvancement(TechBase.IS).setISAdvancement(3060, 3076)
                .setISApproximate(true, false).setTechRating(TechRating.E)
                .setPrototypeFactions(Faction.WB).setProductionFactions(Faction.WB)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F,
                      AvailabilityValue.E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Superheavy command console
          new TechAdvancement(TechBase.ALL).setISAdvancement(3060, 3067, 3080)
                .setISApproximate(true, false, false)
                .setClanAdvancement(ITechnology.DATE_NONE, 3080, 3080)
                .setTechRating(TechRating.E)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS, Faction.CJF)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.E,
                      AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Small Command Console
          new TechAdvancement(TechBase.IS).setISAdvancement(3130, 3135)
                .setISApproximate(true, false).setTechRating(TechRating.E)
                .setPrototypeFactions(Faction.RS).setProductionFactions(Faction.RS)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.F,
                      AvailabilityValue.X,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Superheavy tripod
          new TechAdvancement(TechBase.IS).setISAdvancement(2590, 2702)
                .setISApproximate(true, false).setTechRating(TechRating.F)
                .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), // Tripod
    };

    // Advanced fire control for industrial Meks is implemented with a standard
    // cockpit,
    // but the tech progression is different.
    public static TechAdvancement getIndustrialAdvFireConTA() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(2469, 2470, 2491)
              .setApproximate(true, false, false).setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TH).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.D,
                    AvailabilityValue.E,
                    AvailabilityValue.E,
                    AvailabilityValue.D)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    public static TechAdvancement getCockpitTechAdvancement(int cockpitType) {
        if (cockpitType >= 0 && cockpitType < COCKPIT_TA.length) {
            return new TechAdvancement(COCKPIT_TA[cockpitType]);
        }
        return null;
    }

    public TechAdvancement getCockpitTechAdvancement() {
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
        return new TechAdvancement(TechBase.ALL)
              .setISAdvancement(ITechnology.DATE_NONE, 3020, 3023, ITechnology.DATE_NONE, ITechnology.DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(ITechnology.DATE_NONE,
                    ITechnology.DATE_NONE,
                    3052,
                    ITechnology.DATE_NONE,
                    ITechnology.DATE_NONE)
              .setPrototypeFactions(
                    Faction.LC)
              .setProductionFactions(Faction.LC, Faction.CWF)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X,
                    AvailabilityValue.X,
                    AvailabilityValue.E,
                    AvailabilityValue.D)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    public static TechAdvancement getRiscHeatSinkOverrideKitAdvancement() {
        return new TechAdvancement(TechBase.IS)
              .setAdvancement(3134,
                    ITechnology.DATE_NONE,
                    ITechnology.DATE_NONE,
                    ITechnology.DATE_NONE,
                    ITechnology.DATE_NONE)
              .setApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.RS)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X,
                    AvailabilityValue.X,
                    AvailabilityValue.X,
                    AvailabilityValue.F)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        // Meks with non-fusion engines are experimental
        if (hasEngine() && !isIndustrial() && !getEngine().isFusion()) {
            ctl.addComponent(new TechAdvancement().setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL));
        }
        if (getGyroTechAdvancement() != null) {
            ctl.addComponent(getGyroTechAdvancement());
        }
        if (getCockpitTechAdvancement() != null) {
            ctl.addComponent(getCockpitTechAdvancement());
        }
        if (isIndustrial() && hasAdvancedFireControl()) {
            ctl.addComponent(getIndustrialAdvFireConTA());
        }
        if (hasFullHeadEject()) {
            ctl.addComponent(getFullHeadEjectAdvancement());
        }
        if (hasRiscHeatSinkOverrideKit()) {
            ctl.addComponent(getRiscHeatSinkOverrideKitAdvancement());
        }
    }

    /**
     * This method will return the number of contiguous criticalSlots in the given location, starting at the given
     * critical slot
     *
     * @param loc          The location on the unit to check slots on
     * @param startingSlot The critical slot to start at
     */
    private int getContiguousNumberOfCrits(int loc, int startingSlot) {

        int numCritSlots = getNumberOfCriticalSlots(loc);
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

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return MekCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
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
    public int implicitClanCASE() {
        if (!isClan()) {
            return 0;
        }
        int explicit = 0;
        Set<Integer> caseLocations = new HashSet<>();
        for (Mounted<?> m : getEquipment()) {
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

    public double getActuatorCost() {
        return getArmActuatorCost() + getLegActuatorCost();
    }

    protected double getArmActuatorCost() {
        int numOfUpperArmActuators = 0;
        int numOfLowerArmActuators = 0;
        int numOfHands = 0;
        if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM)) {
            numOfHands++;
        }
        if (hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LEFT_ARM)) {
            numOfLowerArmActuators++;
        }
        if (hasSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LEFT_ARM)) {
            numOfUpperArmActuators++;
        }
        if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM)) {
            numOfHands++;
        }
        if (hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RIGHT_ARM)) {
            numOfLowerArmActuators++;
        }
        if (hasSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_RIGHT_ARM)) {
            numOfUpperArmActuators++;
        }
        return weight * (numOfUpperArmActuators * 100 + numOfLowerArmActuators * 50 + numOfHands * 80);
    }

    protected abstract double getLegActuatorCost();

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> reports = new Vector<>();
        reports.addElement(Report.publicReport(7025).addDesc(this));
        reports.addElement(Report.publicReport(7030).noNL());
        reports.addAll(getCrew().getDescVector(false));
        reports.addElement(Report.publicReport(7070).add(getKillNumber()));
        if (isDestroyed()) {
            Entity killer = game.getEntityFromAllSources(killerId);
            if (killer != null) {
                reports.addElement(Report.publicReport(7072).addDesc(killer).newLines(2));
            } else {
                reports.addElement(Report.publicReport(7073).newLines(2));
            }
        } else if (getCrew().isEjected()) {
            reports.addElement(Report.publicReport(7074).newLines(2));
        }
        return reports;
    }

    /**
     * Add in any piloting skill mods
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        // gyro hit?
        if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO,
              Mek.LOC_CENTER_TORSO) > 0) {
            if (getGyroType() == Mek.GYRO_HEAVY_DUTY) {
                if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
                    if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO,
                          Mek.LOC_CENTER_TORSO) == 1) {
                        roll.addModifier(1, "HD Gyro damaged once");
                    } else if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO,
                          Mek.LOC_CENTER_TORSO) == 2) {
                        roll.addModifier(2, "HD Gyro damaged twice");
                    } else if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO,
                          Mek.LOC_CENTER_TORSO) == 3) {
                        roll.addModifier(3, "HD Gyro damaged thrice");
                    }
                } else {
                    if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO,
                          Mek.LOC_CENTER_TORSO) == 1) {
                        roll.addModifier(1, "HD Gyro damaged once");
                    } else {
                        roll.addModifier(3, "HD Gyro damaged twice");
                    }
                }
            } else {
                if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                    roll.addModifier(2, "Gyro damaged");
                } else {
                    roll.addModifier(3, "Gyro damaged");
                }
            }

        }

        // EI bonus?
        if (hasActiveEiCockpit()) {
            roll.addModifier(-1, "Enhanced Imaging");
        }

        // Prototype DNI gives -3 piloting (IO pg 83)
        // VDNI gives -1 piloting (IO pg 71) - BVDNI does NOT get piloting bonus
        // Check Proto DNI first as it's more powerful
        if (hasAbility(OptionsConstants.MD_PROTO_DNI)) {
            roll.addModifier(-3, Messages.getString("PilotingRoll.ProtoDni"));
        } else if (hasAbility(OptionsConstants.MD_VDNI)
              && !hasAbility(OptionsConstants.MD_BVDNI)) {
            roll.addModifier(-1, "VDNI");
        }

        // Small/torso-mounted cockpit penalty?
        // BVDNI negates small cockpit penalty, but Proto DNI does not
        if (((getCockpitType() == Mek.COCKPIT_SMALL) || (getCockpitType() == Mek.COCKPIT_SMALL_COMMAND_CONSOLE))
              && (!hasAbility(OptionsConstants.MD_BVDNI)
              && !hasAbility(OptionsConstants.UNOFFICIAL_SMALL_PILOT))) {
            roll.addModifier(1, "Small Cockpit");
        } else if (getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
            roll.addModifier(1, "Torso-Mounted Cockpit");
            int sensorHits = getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS, Mek.LOC_HEAD);
            int sensorHits2 = getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS, Mek.LOC_CENTER_TORSO);
            if ((sensorHits + sensorHits2) == 3) {
                roll.addModifier(4,
                      "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                roll.addModifier(4,
                      "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        } else if (getCockpitType() == Mek.COCKPIT_DUAL) {
            // Dedicated pilot bonus is lost if pilot makes any attacks. Penalty for gunner
            // acting as pilot.
            if (getCrew().getCurrentPilotIndex() != getCrew().getCrewType().getPilotPos()) {
                roll.addModifier(1, "dual cockpit without active pilot");
            } else if (getCrew().hasDedicatedGunner() || !isAttackingThisTurn()) {
                roll.addModifier(-1, "dedicated pilot");
            }
        }

        if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)
              && !hasAbility(OptionsConstants.UNOFFICIAL_SMALL_PILOT)) {
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
        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEAPING)) {
            return UNLIMITED_JUMP_DOWN;
        }
        return getMaxElevationChange();
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth can be active and not working when
     * under ECCM)
     * <p>
     * Subclasses are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *       currently active, <code>false</code> if there is no stealth system or if it is inactive.
     */
    @Override
    public boolean isStealthActive() {
        // Try to find a Mek Stealth system.
        for (Mounted<?> mEquip : getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (miscType.hasFlag(MiscType.F_STEALTH)) {

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
     * Determine if this unit has an active and working stealth system. (stealth can be active and not working when
     * under ECCM)
     * <p>
     * Subclasses are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *       currently active, <code>false</code> if there is no stealth system or if it is inactive.
     */
    @Override
    public boolean isStealthOn() {
        // Try to find a Mek Stealth system.
        for (Mounted<?> mEquip : getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (miscType.hasFlag(MiscType.F_STEALTH)) {
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
     * Does the Mek have a functioning null signature system, or a void sig that is acting as a null sig because of
     * externally carried BA?
     */
    @Override
    public boolean isNullSigActive() {
        if (isVoidSigOn() && !isVoidSigActive()) {
            return true;
        }
        if (!isShutDown()) {
            for (Mounted<?> m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_NULL_SIG)
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
            for (Mounted<?> m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_NULL_SIG)
                      && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the Mek have a functioning void signature system?
     */
    @Override
    public boolean isVoidSigActive() {
        // per the rules questions forum, externally mounted BA invalidates Void Sig
        if (!getLoadedUnits().isEmpty()) {
            return false;
        }

        if (!isShutDown()) {
            for (Mounted<?> m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_VOID_SIG)
                      && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the Mek have a functioning void signature system?
     */
    @Override
    public boolean isVoidSigOn() {
        if (!isShutDown()) {
            for (Mounted<?> m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_VOID_SIG)
                      && m.curMode().equals("On") && m.isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the Mek have a functioning Chameleon Light Polarization Field? For a CLPS to be functioning it must be on
     * and the unit can't have mounted mechanized BattleArmor.
     */
    @Override
    public boolean isChameleonShieldActive() {
        // TO pg 300 states that generates heat but doesn't operate if the unit has
        // mounted BA
        if (!getLoadedUnits().isEmpty()) {
            return false;
        }

        if (!isShutDown()) {
            for (Mounted<?> m : getMisc()) {
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
     * Does the Mek Chameleon Light Polarization Field turned on? This is used for heat generation purposes. A CLPS can
     * be on and generating heat but not providing any benefit if the unit has mechanized BattleArmor.
     */
    @Override
    public boolean isChameleonShieldOn() {
        if (!isShutDown()) {
            for (Mounted<?> m : getMisc()) {
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
     * Determine the stealth modifier for firing at this unit from the given range. If the value supplied for
     * <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown.
     * <p>
     * Subclasses are encouraged to override this method.
     *
     * @param range - an <code>int</code> value that must match one of the
     *              <code>Compute</code> class range constants.
     * @param ae    - entity making the attack
     *
     * @return a <code>TargetRoll</code> value that contains the stealth modifier for the given range.
     */
    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // can't combine void sig and stealth or null-sig
        if (isVoidSigActive()) {
            int movementModifier = 3;
            if (delta_distance > 5) {
                movementModifier = 0;
            } else if (delta_distance > 2) {
                movementModifier = 1;
            } else if (delta_distance > 0) {
                movementModifier = 2;
            }
            return new TargetRoll(movementModifier, "void signature");
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

        return result;
    }

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return A <code>boolean</code> that is <code>true</code> if the unit can be repaired (given enough time and
     *       parts); if this value is
     *       <code>false</code>, the unit is only a source of spares.
     *
     * @see Entity#isSalvage()
     */
    @Override
    public boolean isRepairable() {
        // A Mek is repairable if it is salvageable, and its CT internals are not gone.
        int loc_is = this.getInternal(Mek.LOC_CENTER_TORSO);
        return isSalvage() && (loc_is != IArmorState.ARMOR_DOOMED)
              && (loc_is != IArmorState.ARMOR_DESTROYED);
    }

    @Override
    public boolean canCharge() {
        // Meks can charge, unless they are Clan and the "no clan physicals" option is
        // set
        return super.canCharge()
              && !(gameOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL)
              && getCrew().isClanPilot());
    }

    @Override
    public boolean canDFA() {
        // Meks can DFA, unless they are Clan and the "no clan physicals" option is set
        return super.canDFA()
              && !(gameOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL)
              && getCrew().isClanPilot());
    }

    /**
     * @return the total number of sinks
     */
    public int getNumberOfSinks() {
        int sinks = 0;
        for (Mounted<?> mounted : getMisc()) {
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
        for (Mounted<?> mounted : getMisc()) {
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
            for (Mounted<?> mounted : getMisc()) {
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
     * @return unit has an ejection seat
     */
    public boolean hasEjectSeat() {
        // Ejection Seat
        boolean result = getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED
              && !hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT);
        // torso mounted cockpits don't have an ejection seat
        if (isIndustrial()) {
            result = false;
            // industrials can only eject when they have an ejection seat
            for (Mounted<?> misc : getMisc()) {
                if (misc.getType().hasFlag(MiscType.F_EJECTION_SEAT)) {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * @return Returns the autoEject.
     */
    public boolean isAutoEject() {
        return autoEject && hasEjectSeat();
    }

    /**
     * @param autoEject The autoEject to set.
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
     * @param condEjectAmmo The condEjectAmmo to set.
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
     * @param condEjectEngine The condEjectEngine to set.
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
     * @param condEjectCTDest The condEjectCTDest to set.
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
     * @param condEjectHeadshot The condEjectHeadshot to set.
     */
    public void setCondEjectHeadshot(boolean condEjectHeadshot) {
        this.condEjectHeadshot = condEjectHeadshot;
    }

    @Override
    public boolean removePartialCoverHits(int location, int cover, int side) {
        // left and right cover are from attacker's POV.
        // if hitting front arc, need to swap them

        // Handle upper cover specially, as treating it as a bitmask will lead
        // to every location being covered
        if (cover == LosEffects.COVER_UPPER) {
            return (location != Mek.LOC_LEFT_LEG) && (location != Mek.LOC_RIGHT_LEG);
        }

        if (side == ToHitData.SIDE_FRONT) {
            if (((cover & LosEffects.COVER_LOW_RIGHT) != 0)
                  && (location == Mek.LOC_LEFT_LEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOW_LEFT) != 0)
                  && (location == Mek.LOC_RIGHT_LEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0)
                  && ((location == Mek.LOC_LEFT_ARM) || (location == Mek.LOC_LEFT_TORSO) || (location
                  == Mek.LOC_LEFT_LEG))) {
                return true;
            }
            return ((cover & LosEffects.COVER_LEFT) != 0)
                  && ((location == Mek.LOC_RIGHT_ARM) || (location == Mek.LOC_RIGHT_TORSO) || (location
                  == Mek.LOC_RIGHT_LEG));
        } else {
            if (((cover & LosEffects.COVER_LOW_LEFT) != 0)
                  && (location == Mek.LOC_LEFT_LEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOW_RIGHT) != 0)
                  && (location == Mek.LOC_RIGHT_LEG)) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0)
                  && ((location == Mek.LOC_LEFT_ARM)
                  || (location == Mek.LOC_LEFT_TORSO) || (location == Mek.LOC_LEFT_LEG))) {
                return true;
            }
            return ((cover & LosEffects.COVER_RIGHT) != 0)
                  && ((location == Mek.LOC_LEFT_ARM) || (location == Mek.LOC_LEFT_TORSO) || (location
                  == Mek.LOC_LEFT_LEG));
        }
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
        if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS,
              Mek.LOC_HEAD) > 0) {
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
        return Mek.getGyroTypeString(getGyroType());
    }

    public static String getCockpitTypeString(int cockpitType, boolean industrial) {
        if (industrial) {
            switch (cockpitType) {
                case COCKPIT_STANDARD:
                    return Mek.getCockpitTypeString(COCKPIT_INDUSTRIAL) + " (Adv. FCS)";
                case COCKPIT_PRIMITIVE:
                    return Mek.getCockpitTypeString(COCKPIT_PRIMITIVE_INDUSTRIAL) + " (Adv. FCS)";
                case COCKPIT_SUPERHEAVY:
                    return Mek.getCockpitTypeString(COCKPIT_SUPERHEAVY_INDUSTRIAL) + " (Adv. FCS)";
                case COCKPIT_TRIPOD:
                    return Mek.getCockpitTypeString(COCKPIT_TRIPOD_INDUSTRIAL) + " (Adv. FCS)";
                case COCKPIT_SUPERHEAVY_TRIPOD:
                    return Mek.getCockpitTypeString(COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL) + " (Adv. FCS)";
            }
        }
        return Mek.getCockpitTypeString(cockpitType);
    }

    public String getCockpitTypeString() {
        return getCockpitTypeString(getCockpitType(), isIndustrial());
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
        if ((inType == null) || (inType.isEmpty())) {
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
        if ((inType == null) || (inType.isEmpty())) {
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
            return Mek.getGyroDisplayString(gyroType);
        }
        if (index == SYSTEM_COCKPIT) {
            if (isIndustrial() && (cockpitType == Mek.COCKPIT_STANDARD)) {
                return "Industrial Cockpit (adv. FCS)";
            }
            return Mek.getCockpitDisplayString(cockpitType);
        }
        return systemNames[index];
    }

    public String getRawSystemName(int index) {
        return systemNames[index];
    }

    public static String getGyroDisplayString(int inType) {
        String inName = switch (inType) {
            case GYRO_XL -> "GYRO_XL";
            case GYRO_COMPACT -> "GYRO_COMPACT";
            case GYRO_HEAVY_DUTY -> "GYRO_HEAVY_DUTY";
            case GYRO_STANDARD -> "GYRO_STANDARD";
            case GYRO_NONE -> "GYRO_NONE";
            case GYRO_SUPERHEAVY -> "GYRO_SUPERHEAVY";
            default -> "GYRO_UNKNOWN";
        };
        String result = EquipmentMessages.getString("SystemType.Gyro." + inName);
        if (result != null) {
            return result;
        }
        return inName;
    }

    public static String getCockpitDisplayString(int inType) {
        String inName = switch (inType) {
            case COCKPIT_COMMAND_CONSOLE -> "COCKPIT_COMMAND_CONSOLE";
            case COCKPIT_SMALL -> "COCKPIT_SMALL";
            case COCKPIT_TORSO_MOUNTED -> "COCKPIT_TORSO_MOUNTED";
            case COCKPIT_DUAL -> "COCKPIT_DUAL";
            case COCKPIT_STANDARD -> "COCKPIT_STANDARD";
            case COCKPIT_INDUSTRIAL -> "COCKPIT_INDUSTRIAL";
            case COCKPIT_PRIMITIVE -> "COCKPIT_PRIMITIVE";
            case COCKPIT_PRIMITIVE_INDUSTRIAL -> "COCKPIT_PRIMITIVE_INDUSTRIAL";
            case COCKPIT_SUPERHEAVY -> "COCKPIT_SUPERHEAVY";
            case COCKPIT_SUPERHEAVY_TRIPOD -> "COCKPIT_SUPERHEAVY_TRIPOD";
            case COCKPIT_TRIPOD -> "COCKPIT_TRIPOD";
            case COCKPIT_INTERFACE -> "COCKPIT_INTERFACE";
            case COCKPIT_VRRP -> "COCKPIT_VRRP";
            case COCKPIT_QUADVEE -> "COCKPIT_QUADVEE";
            case COCKPIT_SUPERHEAVY_INDUSTRIAL -> "COCKPIT_SUPERHEAVY_INDUSTRIAL";
            case COCKPIT_SUPERHEAVY_COMMAND_CONSOLE -> "COCKPIT_SUPERHEAVY_COMMAND_CONSOLE";
            case COCKPIT_SMALL_COMMAND_CONSOLE -> "COCKPIT_SMALL_COMMAND_CONSOLE";
            case COCKPIT_TRIPOD_INDUSTRIAL -> "COCKPIT_TRIPOD_INDUSTRIAL";
            case COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL -> "COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL";
            default -> "COCKPIT_UNKNOWN";
        };
        String result = EquipmentMessages.getString("SystemType.Cockpit." + inName);
        if (result != null) {
            return result;
        }
        return inName;
    }

    public boolean hasAdvancedFireControl() {
        return (cockpitType != COCKPIT_INDUSTRIAL)
              && (cockpitType != COCKPIT_PRIMITIVE_INDUSTRIAL)
              && (cockpitType != COCKPIT_SUPERHEAVY_INDUSTRIAL)
              && (cockpitType != COCKPIT_TRIPOD_INDUSTRIAL)
              && (cockpitType != COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL);
    }

    @Override
    public boolean canAssaultDrop() {
        return true;
    }

    @Override
    public boolean isLocationProhibited(Coords c, int testBoardId, int testElevation) {
        if (!game.hasBoardLocation(c, testBoardId)) {
            return true;
        }

        Hex hex = game.getHex(c, testBoardId);
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if ((game.getBoard(testBoardId).isSpace() && doomedInSpace())
              || (game.getBoard(testBoardId).isLowAltitude() && doomedInAtmosphere())) {
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
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == testElevation)
                  && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            // Meks can deploy in water if the water covers them entirely
            if (hex.containsTerrain(Terrains.WATER) && (hex.terrainLevel(Terrains.WATER) < (height() + 1))) {
                return true;
            }
            // Can't deploy in clear hex
            if (hex.isClearHex()) {
                return true;
            }
        }
        // Meks using tracks and QuadVees in vehicle mode (or converting to or from)
        // have the same
        // restrictions and combat vehicles with the exception that QuadVees can enter
        // water hexes
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

        // a swimming Mek (UMU) may not reach above the surface
        if ((testElevation == -1) && hex.hasDepth1WaterOrDeeper()
              && (hex.terrainLevel(Terrains.WATER) > 1) && !isProne()) {
            return true;
        }

        return (hex.terrainLevel(Terrains.WOODS) > 2)
              || (hex.terrainLevel(Terrains.JUNGLE) > 2);
    }

    @Override
    public boolean isLocationDeadly(Coords c) {
        //legacy
        return isLocationDeadly(c, 0);
    }

    @Override
    public boolean isLocationDeadly(Coords c, int boardId) {
        return isIndustrial()
              && hasEngine()
              && getEngine().isICE()
              && !hasEnvironmentalSealing()
              && game.hasBoardLocation(c, boardId)
              && game.getHex(c, boardId).terrainLevel(Terrains.WATER) >= 2;
    }

    /**
     * Get an '.mtf' file representation of the Mek. This string can be directly written to disk as a file and later
     * loaded by the MtfFile class.
     */
    public String getMtf() {
        StringBuilder sb = new StringBuilder();
        String newLine = "\n";

        sb.append(MtfFile.GENERATOR).append(SuiteConstants.PROJECT_NAME)
              .append(" ").append(SuiteConstants.VERSION).append(" on ").append(LocalDate.now()).append(newLine);

        boolean standard = (getCockpitType() == Mek.COCKPIT_STANDARD)
              && (getGyroType() == Mek.GYRO_STANDARD);
        sb.append(MtfFile.CHASSIS).append(chassis).append(newLine);
        if (!clanChassisName.isBlank()) {
            sb.append(MtfFile.CLAN_CHASSIS_NAME).append(clanChassisName).append(newLine);
        }
        sb.append(MtfFile.MODEL).append(model).append(newLine);
        if (hasMulId()) {
            sb.append(MtfFile.MUL_ID).append(mulId).append(newLine);
        }
        sb.append(newLine);

        sb.append("Config:");
        if (this instanceof LandAirMek) {
            sb.append("LAM");
        } else if (this instanceof BipedMek) {
            sb.append("Biped");
        } else if (this instanceof QuadVee) {
            sb.append("QuadVee");
        } else if (this instanceof QuadMek) {
            sb.append("Quad");
        } else if (this instanceof TripodMek) {
            sb.append("Tripod");
        }

        if (isOmni()) {
            sb.append(" OmniMek");
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
        if ((source != null) && !source.isBlank()) {
            sb.append(MtfFile.SOURCE).append(source).append(newLine);
        }
        sb.append(MtfFile.RULES_LEVEL).append(
              TechConstants.T_SIMPLE_LEVEL[techLevel]);
        sb.append(newLine);
        if (hasRole()) {
            sb.append(MtfFile.ROLE).append(getRole().toString());
            sb.append(newLine);
        }
        sb.append(newLine);

        for (IOption quirk : getQuirks().getOptionsList()) {
            if (quirk.getType() == IOption.INTEGER) {
                int value = quirk.intValue();
                if (value != 0) {
                    sb.append(MtfFile.QUIRK).append(quirk.getName()).append(":").append(value).append(newLine);
                }
            } else if (quirk.getType() == IOption.STRING) {
                String value = quirk.stringValue();
                if (value != null && !value.isEmpty()) {
                    sb.append(MtfFile.QUIRK).append(quirk.getName()).append(":").append(value).append(newLine);
                }
            } else if (quirk.booleanValue()) {
                sb.append(MtfFile.QUIRK).append(quirk.getName()).append(newLine);
            }
        }

        for (Mounted<?> equipment : getEquipment()) {
            for (IOption weaponQuirk : equipment.getQuirks().activeQuirks()) {
                sb.append(MtfFile.WEAPON_QUIRK).append(weaponQuirk.getName()).append(":")
                      .append(getLocationAbbr(equipment.getLocation())).append(":")
                      .append(slotNumber(equipment)).append(":")
                      .append(equipment.getType().getInternalName()).append(newLine);
            }
        }
        sb.append(newLine);

        sb.append(MtfFile.MASS).append((int) weight).append(newLine);
        sb.append(MtfFile.ENGINE);
        if (hasEngine()) {
            sb.append(getEngine().getEngineName())
                  .append(" Engine")
                  .append((!getEngine().hasFlag(Engine.CLAN_ENGINE) && isMixedTech()) ? ("(IS)")
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

        if (this instanceof LandAirMek) {
            sb.append(MtfFile.LAM);
            sb.append(((LandAirMek) this).getLAMTypeString());
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
            sb.append(Mek.FULL_HEAD_EJECT_STRING);
            sb.append(newLine);
        }
        if (hasRiscHeatSinkOverrideKit()) {
            sb.append(MtfFile.HEAT_SINK_KIT);
            sb.append(Mek.RISC_HEAT_SINK_OVERRIDE_KIT);
            sb.append(newLine);
        }
        sb.append(newLine);

        sb.append(MtfFile.HEAT_SINKS).append(heatSinks()).append(" ");
        Optional<MiscType> heatSink = getMisc().stream()
              .filter(m -> m.getType().hasFlag(MiscType.F_HEAT_SINK)
                    || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK))
              .map(Mounted::getType).findFirst();
        // If we didn't find any heat sinks we may have an ICE with no added sinks, or
        // prototype
        // doubles (which have a different flag). In the latter case, we want to put
        // single
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
        for (Mounted<?> mounted : getMisc()) {
            if ((mounted.getNumCriticalSlots() == 0)
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
            if ((element == Mek.LOC_CENTER_LEG) && !(this instanceof TripodMek)) {
                continue;
            }
            sb.append(getLocationAbbr(element)).append(" ").append(MtfFile.ARMOR);
            if (hasPatchworkArmor()) {
                sb.append(EquipmentType.getArmorTypeName(getArmorType(element),
                            TechConstants.isClan(getArmorTechLevel(element))))
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
        for (Mounted<?> m : weaponList) {
            sb.append(m.getName()).append(", ")
                  .append(getLocationName(m.getLocation())).append(newLine);
        }
        sb.append(newLine);
        for (int l : MtfFile.locationOrder) {
            if ((l == Mek.LOC_CENTER_LEG) && !(this instanceof TripodMek)) {
                continue;
            }
            String locationName = getLocationName(l);
            sb.append(locationName).append(":");
            sb.append(newLine);
            for (int y = 0; y < 12; y++) {
                if (y < getNumberOfCriticalSlots(l)) {
                    sb.append(decodeCritical(getCritical(l, y)))
                          .append(newLine);
                } else {
                    sb.append(MtfFile.EMPTY).append(newLine);
                }
            }
            sb.append(newLine);
        }

        if (!getFluff().getOverview().isBlank()) {
            sb.append(MtfFile.OVERVIEW);
            sb.append(getFluff().getOverview());
            sb.append(newLine);
        }

        if (!getFluff().getCapabilities().isBlank()) {
            sb.append(MtfFile.CAPABILITIES);
            sb.append(getFluff().getCapabilities());
            sb.append(newLine);
        }

        if (!getFluff().getDeployment().isBlank()) {
            sb.append(MtfFile.DEPLOYMENT);
            sb.append(getFluff().getDeployment());
            sb.append(newLine);
        }

        if (!getFluff().getHistory().isBlank()) {
            sb.append(MtfFile.HISTORY);
            sb.append(getFluff().getHistory());
            sb.append(newLine);
        }

        if (!getFluff().getManufacturer().isBlank()) {
            sb.append(MtfFile.MANUFACTURER);
            sb.append(getFluff().getManufacturer());
            sb.append(newLine);
        }

        if (!getFluff().getPrimaryFactory().isBlank()) {
            sb.append(MtfFile.PRIMARY_FACTORY);
            sb.append(getFluff().getPrimaryFactory());
            sb.append(newLine);
        }

        if (!getFluff().getNotes().isBlank()) {
            sb.append(MtfFile.NOTES);
            sb.append(getFluff().getNotes());
            sb.append(newLine);
        }

        for (System system : System.values()) {
            if (!getFluff().getSystemManufacturer(system).isBlank()) {
                sb.append(MtfFile.SYSTEM_MANUFACTURER);
                sb.append(system.toString()).append(":");
                sb.append(getFluff().getSystemManufacturer(system));
                sb.append(newLine);
            }

            if (!getFluff().getSystemModel(system).isBlank()) {
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

        if (!icon.isEmpty()) {
            sb.append(newLine);
            sb.append(MtfFile.ICON);
            sb.append(icon.getBase64String());
            sb.append(newLine);
        }

        if (getFluff().hasEmbeddedFluffImage()) {
            sb.append(newLine);
            sb.append(MtfFile.FLUFF_IMAGE);
            sb.append(getFluff().getBase64FluffImage().getBase64String());
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
            final Mounted<?> m = cs.getMount();
            toReturn.append(m.getType().getInternalName());
            // Superheavy Meks can have a second ammo bin or heat sink in the same slot
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
            if (m.isMekTurretMounted()) {
                toReturn.append(" (T)");
            }
            if (m.isOmniPodMounted()) {
                toReturn.append(" ").append(MtfFile.OMNI_POD);
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
     * Add the critical slots necessary for a standard cockpit. Note: This is part of the mek creation public API, and
     * might not be referenced by any MegaMek code.
     */
    public void addCockpit() {
        if (getEmptyCriticalSlots(LOC_HEAD) < 5) {
            return;
        }

        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));

        if (isSuperHeavy()) {
            if (this instanceof TripodMek) {
                setCockpitType(isIndustrial() ? COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL : COCKPIT_SUPERHEAVY_TRIPOD);
            } else if (isIndustrial()) {
                setCockpitType(COCKPIT_SUPERHEAVY_INDUSTRIAL);
            } else {
                setCockpitType(COCKPIT_SUPERHEAVY);
            }
        } else if (this instanceof TripodMek) {
            setCockpitType(isIndustrial() ? COCKPIT_TRIPOD_INDUSTRIAL : COCKPIT_TRIPOD);
        } else {
            setCockpitType(isIndustrial() ? COCKPIT_INDUSTRIAL : COCKPIT_STANDARD);
        }

    }

    /**
     * Add the critical slots necessary for an industrial cockpit. Note: This is part of the mek creation public API,
     * and might not be referenced by any MegaMek code.
     */
    public void addIndustrialCockpit() {
        if (getEmptyCriticalSlots(LOC_HEAD) < 5) {
            return;
        }
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_INDUSTRIAL);
    }

    /**
     * Add the critical slots necessary for an industrial cockpit. Note: This is part of the mek creation public API,
     * and might not be referenced by any MegaMek code.
     */
    public void addPrimitiveCockpit() {
        if (getEmptyCriticalSlots(LOC_HEAD) < 5) {
            return;
        }

        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_PRIMITIVE);
    }

    /**
     * Add the critical slots necessary for an industrial primitive cockpit. Note: This is part of the mek creation
     * public API, and might not be referenced by any MegaMek code.
     */
    public void addIndustrialPrimitiveCockpit() {
        if (getEmptyCriticalSlots(LOC_HEAD) < 5) {
            return;
        }

        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_PRIMITIVE_INDUSTRIAL);
    }

    /**
     * Add the critical slots necessary for a small cockpit. Note: This is part of the mek creation public API, and
     * might not be referenced by any MegaMek code.
     */
    public void addSmallCockpit() {
        if (getEmptyCriticalSlots(LOC_HEAD) < 4) {
            return;
        }

        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        setCockpitType(COCKPIT_SMALL);
    }

    /**
     * Add the critical slots necessary for a small cockpit. Note: This is part of the mek creation public API, and
     * might not be referenced by any MegaMek code.
     */
    public void addInterfaceCockpit() {
        if (getEmptyCriticalSlots(LOC_HEAD) < 6) {
            return;
        }

        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_INTERFACE);
    }

    public void addCommandConsole() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_COMMAND_CONSOLE);
    }

    public void addQuadVeeCockpit() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_QUADVEE);
    }

    public void addDualCockpit() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_DUAL);
    }

    public void addSuperheavyIndustrialCockpit() {
        if (getEmptyCriticalSlots(LOC_HEAD) < 5) {
            return;
        }

        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_SUPERHEAVY_INDUSTRIAL);
    }

    public void addSuperheavyCommandConsole() {
        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCockpitType(COCKPIT_SUPERHEAVY_COMMAND_CONSOLE);
    }

    // The location of critical is based on small cockpit, but since command console
    // requires two cockpit slots the second Sensor is return to the location 4.
    public void addSmallCommandConsole() {
        if (getEmptyCriticalSlots(LOC_HEAD) < 5) {
            return;
        }

        addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        addCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        addCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        setCockpitType(COCKPIT_SMALL_COMMAND_CONSOLE);
    }

    /**
     * Add the critical slots necessary for a torso-mounted cockpit. Note: This is part of the mek creation public API,
     * and might not be referenced by any MegaMek code.
     *
     * @param vrpp if this is a VRPP rather than a standard torso-mounted cockpit
     */
    public void addTorsoMountedCockpit(boolean vrpp) {
        boolean success = true;

        if (getEmptyCriticalSlots(LOC_HEAD) < 2) {
            success = false;
        } else {
            addCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
            addCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        }

        if ((getEmptyCriticalSlots(LOC_CENTER_TORSO) < 2) || !success) {
            success = false;
        } else {
            addCritical(LOC_CENTER_TORSO,
                  getFirstEmptyCrit(LOC_CENTER_TORSO),
                  new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
            if (vrpp) {
                addCritical(LOC_CENTER_TORSO,
                      getFirstEmptyCrit(LOC_CENTER_TORSO),
                      new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
            } else {
                addCritical(LOC_CENTER_TORSO,
                      getFirstEmptyCrit(LOC_CENTER_TORSO),
                      new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
            }
        }

        if ((getEmptyCriticalSlots(LOC_LEFT_TORSO) < 1) || (getEmptyCriticalSlots(LOC_RIGHT_TORSO) < 1) || !success) {
            success = false;
        } else {
            addCritical(LOC_LEFT_TORSO, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
            addCritical(LOC_RIGHT_TORSO, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        }

        if (success) {
            if (vrpp) {
                setCockpitType(COCKPIT_VRRP);
            } else {
                setCockpitType(COCKPIT_TORSO_MOUNTED);
            }
        }
    }

    /**
     * Convenience function that returns the critical slot containing the cockpit
     */
    public List<CriticalSlot> getCockpit() {
        List<CriticalSlot> retVal = new ArrayList<>();

        switch (cockpitType) {
            // these always occupy slots 2 and 3 in the head
            case Mek.COCKPIT_COMMAND_CONSOLE:
            case Mek.COCKPIT_DUAL:
            case Mek.COCKPIT_SMALL_COMMAND_CONSOLE:
            case Mek.COCKPIT_INTERFACE:
            case Mek.COCKPIT_QUADVEE:
            case Mek.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
                retVal.add(getCritical(Mek.LOC_HEAD, 2));
                retVal.add(getCritical(Mek.LOC_HEAD, 3));
                break;
            case Mek.COCKPIT_TORSO_MOUNTED:
                for (int critIndex = 0; critIndex < getNumberOfCriticalSlots(Mek.LOC_CENTER_TORSO); critIndex++) {
                    CriticalSlot slot = getCritical(Mek.LOC_CENTER_TORSO, critIndex);
                    if (slot.getIndex() == SYSTEM_COCKPIT) {
                        retVal.add(slot);
                    }
                }
            default:
                retVal.add(getCritical(Mek.LOC_HEAD, 2));
                break;
        }

        return retVal;
    }

    /**
     * Determines which crew slot is associated with a particular cockpit critical.
     *
     * @param cs A cockpit critical slot
     *
     * @return The crew slot index associated with this critical slot, or -1 to indicate the entire crew.
     */
    public int getCrewForCockpitSlot(int loc, CriticalSlot cs) {
        // For those with split cockpits, count the cockpit criticalSlots in the location
        // until we reach the correct
        // one.
        if (getCockpitType() == COCKPIT_COMMAND_CONSOLE
              || getCockpitType() == COCKPIT_SUPERHEAVY_COMMAND_CONSOLE
              || getCockpitType() == COCKPIT_SMALL_COMMAND_CONSOLE
              || getCockpitType() == COCKPIT_DUAL
              || getCockpitType() == COCKPIT_QUADVEE) {
            int crewSlot = 0;
            for (int i = 0; i < getNumberOfCriticalSlots(loc); i++) {
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
              && (!isIndustrial() || hasWorkingMisc(MiscType.F_ADVANCED_FIRE_CONTROL));
    }

    /**
     * Add the critical slots necessary for a standard gyro. Also set the gyro type variable. Note: This is part of the
     * mek creation public API, and might not be referenced by any MegaMek code.
     *
     * @return false if insufficient critical space
     */
    public boolean addGyro() {
        if (getEmptyCriticalSlots(LOC_CENTER_TORSO) < (isSuperHeavy() ? 2 : 4)) {
            return false;
        }
        addCompactGyro();
        if (!isSuperHeavy()) {
            addCritical(LOC_CENTER_TORSO, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                  SYSTEM_GYRO));
            addCritical(LOC_CENTER_TORSO, 6, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                  SYSTEM_GYRO));
            setGyroType(GYRO_STANDARD);
        } else {
            setGyroType(GYRO_SUPERHEAVY);
        }
        return true;
    }

    /**
     * Add the critical slots necessary for a standard gyro. Also set the gyro type variable. Note: This is part of the
     * mek creation public API, and might not be referenced by any MegaMek code.
     */
    public void addSuperheavyGyro() {
        if (getEmptyCriticalSlots(LOC_CENTER_TORSO) < 2) {
            return;
        }

        addCritical(LOC_CENTER_TORSO, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        if (getEngine().getEngineType() == Engine.COMPACT_ENGINE) {
            addCritical(LOC_CENTER_TORSO, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        } else {
            addCritical(LOC_CENTER_TORSO, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        }
        setGyroType(GYRO_SUPERHEAVY);
    }

    /**
     * Add the critical slots necessary for a compact gyro. Also set the gyro type variable. Note: This is part of the
     * mek creation public API, and might not be referenced by any MegaMek code.
     */
    public void addCompactGyro() {
        if (getEmptyCriticalSlots(LOC_CENTER_TORSO) < 2) {
            return;
        }

        addCritical(LOC_CENTER_TORSO, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        addCritical(LOC_CENTER_TORSO, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        setGyroType(GYRO_COMPACT);
    }

    /**
     * Add the critical slots necessary for an extra-light gyro. Also set the gyro type variable. Note: This is part of
     * the mek creation public API, and might not be referenced by any MegaMek code.
     */
    public void addXLGyro() {
        if (getEmptyCriticalSlots(LOC_CENTER_TORSO) < 6) {
            return;
        }

        clearEngineCrits();
        addGyro();
        addCritical(LOC_CENTER_TORSO, 7, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        addCritical(LOC_CENTER_TORSO, 8, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        setGyroType(GYRO_XL);
        addEngineCrits();
    }

    /**
     * Add the critical slots necessary for a heavy-duty gyro. Also set the gyro type variable. Note: This is part of
     * the mek creation public API, and might not be referenced by any MegaMek code.
     */
    public void addHeavyDutyGyro() {
        if (addGyro()) {
            setGyroType(GYRO_HEAVY_DUTY);
        }
    }

    /**
     * Add the critical slots necessary for the mek's engine. Calling this method before setting a mek's engine object
     * will result in a NPE. Note: This is part of the mek creation public API, and might not be referenced by any
     * MegaMek code.
     */
    public void addEngineCrits() {
        if (!hasEngine()) {
            return;
        }

        boolean success = true;

        int[] centerSlots = getEngine().getCenterTorsoCriticalSlots(getGyroType());
        if (getEmptyCriticalSlots(LOC_CENTER_TORSO) < centerSlots.length) {
            success = false;
        } else {
            for (int centerSlot : centerSlots) {
                addCritical(LOC_CENTER_TORSO, centerSlot, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
            }
        }
        int[] sideSlots = getEngine().getSideTorsoCriticalSlots();
        if ((getEmptyCriticalSlots(LOC_LEFT_TORSO) > sideSlots.length)
              || (getEmptyCriticalSlots(LOC_RIGHT_TORSO) > sideSlots.length) || success) {
            for (int sideSlot : sideSlots) {
                addCritical(LOC_LEFT_TORSO, sideSlot, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
                addCritical(LOC_RIGHT_TORSO, sideSlot, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
            }
        }

    }

    /**
     * Remove all engine critical slots from the mek. Note: This is part of the mek creation public API, and might not
     * be referenced by any MegaMek code.
     */
    public void clearEngineCrits() {
        for (int i = 0; i < locations(); i++) {
            removeCriticalSlots(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
        }
    }

    /**
     * Remove all cockpit critical slots from the mek. Note: This is part of the mek creation public API, and might not
     * be referenced by any MegaMek code.
     */
    public void clearCockpitCrits() {
        for (int i = 0; i < locations(); i++) {
            removeCriticalSlots(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
            removeCriticalSlots(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
            removeCriticalSlots(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        }
    }

    /**
     * Remove all gyro critical slots from the mek. Note: This is part of the mek creation public API, and might not be
     * referenced by any MegaMek code.
     */
    public void clearGyroCrits() {
        for (int i = 0; i < locations(); i++) {
            removeCriticalSlots(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                  SYSTEM_GYRO));
        }
    }

    public int shieldAbsorptionDamage(int damage, int location, boolean rear) {
        int damageAbsorption = damage;
        if (hasActiveShield(location, rear)) {
            switch (location) {
                case Mek.LOC_CENTER_TORSO:
                case Mek.LOC_HEAD:
                    if (hasActiveShield(Mek.LOC_RIGHT_ARM)) {
                        damageAbsorption = getAbsorptionRate(Mek.LOC_RIGHT_ARM,
                              damageAbsorption);
                    }
                    if (hasActiveShield(Mek.LOC_LEFT_ARM)) {
                        damageAbsorption = getAbsorptionRate(Mek.LOC_LEFT_ARM,
                              damageAbsorption);
                    }
                    break;
                case Mek.LOC_LEFT_ARM:
                case Mek.LOC_LEFT_TORSO:
                case Mek.LOC_LEFT_LEG:
                    if (hasActiveShield(Mek.LOC_LEFT_ARM)) {
                        damageAbsorption = getAbsorptionRate(Mek.LOC_LEFT_ARM,
                              damageAbsorption);
                    }
                    break;
                default:
                    if (hasActiveShield(Mek.LOC_RIGHT_ARM)) {
                        damageAbsorption = getAbsorptionRate(Mek.LOC_RIGHT_ARM,
                              damageAbsorption);
                    }
                    break;
            }
        }

        if (hasPassiveShield(location, rear)) {
            switch (location) {
                case Mek.LOC_LEFT_ARM:
                case Mek.LOC_LEFT_TORSO:
                    if (hasPassiveShield(Mek.LOC_LEFT_ARM)) {
                        damageAbsorption = getAbsorptionRate(Mek.LOC_LEFT_ARM,
                              damageAbsorption);
                    }
                    break;
                case Mek.LOC_RIGHT_ARM:
                case Mek.LOC_RIGHT_TORSO:
                    if (hasPassiveShield(Mek.LOC_RIGHT_ARM)) {
                        damageAbsorption = getAbsorptionRate(Mek.LOC_RIGHT_ARM,
                              damageAbsorption);
                    }
                    break;
                default:
                    break;
            }
        }
        if (hasNoDefenseShield(location)) {
            switch (location) {
                case Mek.LOC_LEFT_ARM:
                    if (hasNoDefenseShield(Mek.LOC_LEFT_ARM)) {
                        damageAbsorption = getAbsorptionRate(Mek.LOC_LEFT_ARM,
                              damageAbsorption);
                    }
                    break;
                case Mek.LOC_RIGHT_ARM:
                    if (hasNoDefenseShield(Mek.LOC_RIGHT_ARM)) {
                        damageAbsorption = getAbsorptionRate(Mek.LOC_RIGHT_ARM,
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

        if ((location != Mek.LOC_RIGHT_ARM) && (location != Mek.LOC_LEFT_ARM)) {
            return rate;
        }

        if (damage <= 0) {
            return 0;
        }

        for (int slot = 0; slot < this.getNumberOfCriticalSlots(location); slot++) {
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

            Mounted<?> m = cs.getMount();
            if ((m instanceof MiscMounted) && ((MiscMounted) m).getType().isShield()) {
                rate -= ((MiscMounted) m).getDamageAbsorption(this, m.getLocation());
                ((MiscMounted) m).takeDamage(1);
                return Math.max(0, rate);
            }
        }

        return rate;
    }

    /**
     * Does this Mek have an undamaged HarJel system in this location?
     *
     * @param loc the <code>int</code> location to check
     *
     * @return a <code>boolean</code> value indicating a present HarJel system
     */
    public boolean hasHarJelIIIn(int loc) {
        for (Mounted<?> mounted : getMisc()) {
            if ((mounted.getLocation() == loc) && mounted.isReady()
                  && (mounted.getType().hasFlag(MiscType.F_HARJEL_II))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does this Mek have an undamaged HarJel system in this location?
     *
     * @param loc the <code>int</code> location to check
     *
     * @return a <code>boolean</code> value indicating a present HarJel system
     */
    public boolean hasHarJelIIIIn(int loc) {
        for (Mounted<?> mounted : getMisc()) {
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
        // destroyLocation() will just return having done nothing itself, and
        // then we'd potentially end up with a second PSR for an
        // already-destroyed
        // leg.
        if (getInternal(loc) < 0) {
            return;
        }
        super.destroyLocation(loc, blownOff);
        // if it's a leg, the entity falls
        if (game != null && locationIsLeg(loc) && canFall()) {
            if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                game.addPSR(new PilotingRollData(getId(), TargetRoll.AUTOMATIC_FAIL, 4, "leg destroyed"));
            } else {
                game.addPSR(new PilotingRollData(getId(),
                      TargetRoll.AUTOMATIC_FAIL, 5, "leg destroyed"));
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

    /**
     * How many times TacOps coolant failure has occurred, which is also the reduction in heat sinking capacity
     */
    @Override
    public int getCoolantFailureAmount() {
        return heatSinkCoolantFailureFactor;
    }


    @Override
    public void addCoolantFailureAmount(int amount) {
        heatSinkCoolantFailureFactor += amount;
    }

    /**
     * Reset count of TacOps coolant failures to zero (no loss)
     */
    @Override
    public void resetCoolantFailureAmount() {
        heatSinkCoolantFailureFactor = 0;
    }

    @Override
    public int getTotalCommGearTons() {
        return 1 + getExtraCommGearTons();
    }

    @Override
    public int getHQIniBonus() {
        int bonus = super.getHQIniBonus();
        if (((getGyroHits() > 0) || hasHipCrit()) && (mpUsedLastRound > 0)) {
            return 0;
        }
        return bonus;
    }

    @Override
    public int getBARRating(int loc) {
        return (armorType[loc] == EquipmentType.T_ARMOR_COMMERCIAL) ? 5 : 10;
    }

    /**
     * Is this an Industrial Mek?
     *
     * @return if this Mek has an industrial inner structure
     */
    public boolean isIndustrial() {
        return getStructureType() == EquipmentType.T_STRUCTURE_INDUSTRIAL;
    }

    /**
     * set if this Mek just moved into water that would kill it because of the lack of environmental sealing
     */
    public void setJustMovedIntoIndustrialKillingWater(boolean moved) {
        justMovedIntoIndustrialKillingWater = moved;
    }

    /**
     * did this Mek just moved into water that would kill it because we lack environmental sealing?
     */
    public boolean isJustMovedIntoIndustrialKillingWater() {
        return justMovedIntoIndustrialKillingWater;
    }

    /**
     * should this Mek die at the end of turn because it's an IndustrialMek without environmental sealing that moved
     * into water last round and stayed there?
     */
    public boolean shouldDieAtEndOfTurnBecauseOfWater() {
        return shouldDieAtEndOfTurnBecauseOfWater;
    }

    /**
     * set if this Mek should die at the end of turn because it's an IndustrialMek without environmental sealing that
     * moved into water last round and stayed there?
     */
    public void setShouldDieAtEndOfTurnBecauseOfWater(boolean moved) {
        shouldDieAtEndOfTurnBecauseOfWater = moved;
    }

    /**
     * Set if this Mek's ICE Engine is stalled or not should only be used for industrial Meks carrying an ICE engine
     */
    public void setStalled(boolean stalled) {
        this.stalled = stalled;
        stalledThisTurn = true;
    }

    @Override
    public boolean isStalled() {
        return stalled;
    }

    @Override
    public boolean isShutDown() {
        return super.isShutDown() || isStalled();
    }

    @Override
    public Vector<Report> doCheckEngineStallRoll(Vector<Report> vPhaseReport) {
        if (hasEngine() && (getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            vPhaseReport
                  .add(Report.subjectReport(2280, getId()).addDesc(this).add(1).add("ICE-Engine Mek failed a PSR"));

            // Stall check is made against unmodified Piloting skill...
            PilotingRollData psr = getPilotingRollData();

            vPhaseReport.add(Report.subjectReport(2285, getId()).add(psr.getValueAsString()).add(psr.getDesc()));
            vPhaseReport.add(Report.subjectReport(2290, getId()).indent().noNL().add(1).add(psr.getPlainDesc()));

            Roll diceRoll = getCrew().rollPilotingSkill();
            Report r = Report.subjectReport(2300, getId()).add(psr).add(diceRoll);
            if (diceRoll.getIntValue() < psr.getValue()) {
                setStalled(true);
                vPhaseReport.add(r.noNL().choose(false));
                vPhaseReport.add(Report.subjectReport(2303, getId()));
            } else {
                vPhaseReport.add(r.choose(true));
            }
        }
        return vPhaseReport;
    }

    private PilotingRollData getPilotingRollData() {
        PilotingRollData psr = new PilotingRollData(getId(), getCrew().getPiloting(), "Base piloting skill");
        // ...but dead or unconscious pilots should still auto-fail.
        if (getCrew().isDead() || getCrew().isDoomed() || (getCrew().getHits() >= 6)) {
            psr = new PilotingRollData(getId(), TargetRoll.AUTOMATIC_FAIL, "Pilot dead");
        } else if (!getCrew().isActive()) {
            psr = new PilotingRollData(getId(), TargetRoll.IMPOSSIBLE, "Pilot unconscious");
        }
        return psr;
    }

    @Override
    public void checkUnstall(Vector<Report> vPhaseReport) {
        if (stalled && !stalledThisTurn && hasEngine() && (getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            vPhaseReport
                  .add(Report.subjectReport(2280, getId()).addDesc(this).add(1).add("unstall stalled ICE engine"));

            // Unstall check is made against unmodified Piloting skill...
            PilotingRollData psr = getPilotingRollData();

            vPhaseReport.add(Report.subjectReport(2285, getId()).add(psr.getValueAsString()).add(psr.getDesc()));
            vPhaseReport.add(Report.subjectReport(2290, getId()).indent().noNL().add(1).add(psr.getPlainDesc()));

            Roll diceRoll = getCrew().rollPilotingSkill();
            Report r = Report.subjectReport(2300, getId()).add(psr).add(diceRoll);
            if (diceRoll.getIntValue() < psr.getValue()) {
                vPhaseReport.add(r.choose(false));
            } else {
                setStalled(false);
                vPhaseReport.add(r.noNL().choose(true));
                vPhaseReport.add(Report.subjectReport(2304, getId()));
            }
        }
    }

    @Override
    public boolean isPrimitive() {
        return (getCockpitType() == Mek.COCKPIT_PRIMITIVE)
              || (getCockpitType() == Mek.COCKPIT_PRIMITIVE_INDUSTRIAL);
    }

    private int getFirstEmptyCrit(int Location) {
        for (int i = 0; i < getNumberOfCriticalSlots(Location); i++) {
            if (getCritical(Location, i) == null) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasArmoredCockpit() {

        int location = getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED ? Mek.LOC_CENTER_TORSO
              : Mek.LOC_HEAD;

        for (int slot = 0; slot < getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)
                  && (cs.getIndex() == Mek.SYSTEM_COCKPIT)) {
                return cs.isArmored();
            }
        }

        return false;
    }

    public boolean hasArmoredGyro() {
        for (int slot = 0; slot < getNumberOfCriticalSlots(LOC_CENTER_TORSO); slot++) {
            CriticalSlot cs = getCritical(LOC_CENTER_TORSO, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)
                  && (cs.getIndex() == Mek.SYSTEM_GYRO)) {
                return cs.isArmored();
            }
        }

        return false;
    }

    @Override
    public boolean hasArmoredEngine() {
        for (int slot = 0; slot < getNumberOfCriticalSlots(LOC_CENTER_TORSO); slot++) {
            CriticalSlot cs = getCritical(LOC_CENTER_TORSO, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)
                  && (cs.getIndex() == Mek.SYSTEM_ENGINE)) {
                return cs.isArmored();
            }
        }
        return false;
    }

    /**
     * should this Mek check for a critical hit at the end of turn due to being an industrial Mek and having been the
     * target of a successful physical attack or for falling
     */
    public boolean isCheckForCrit() {
        return checkForCrit;
    }

    /**
     * how many levels did this Mek fall this turn?
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
     */
    public boolean isArm(int loc) {
        return (loc == Mek.LOC_LEFT_ARM) || (loc == Mek.LOC_RIGHT_ARM);
    }

    public double getArmoredComponentBV() {
        double bv = 0.0f;

        // all equipment gets 5% of BV cost per slot, or a flat +5 per slot
        // if BV is 0
        for (Mounted<?> mount : getEquipment()) {
            if (!mount.isArmored()
                  || ((mount.getType() instanceof MiscType) && mount.getType().hasFlag(MiscType.F_PPC_CAPACITOR))) {
                continue;
            }
            double mountBv = mount.getType().getBV(this);
            if ((mount.getType() instanceof PPCWeapon)
                  && (mount.getLinkedBy() != null)) {
                mountBv += ((MiscType) mount.getLinkedBy().getType()).getBV(
                      this, mount);
                bv += mountBv * 0.05 * (mount.getNumCriticalSlots() + 1);
            } else if (mountBv > 0) {
                bv += mountBv * 0.05 * mount.getNumCriticalSlots();
            } else {
                bv += 5 * mount.getNumCriticalSlots();
            }
        }

        for (int location = 0; location < locations(); location++) {
            for (int slot = 0; slot < getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot cs = getCritical(location, slot);
                if ((cs != null) && cs.isArmored()
                      && (cs.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    // gyro is the only system that has its own BV
                    if ((cs.getIndex() == Mek.SYSTEM_GYRO)) {
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

    public void setRiscHeatSinkOverrideKit(boolean heatSinkKit) {
        this.riscHeatSinkKit = heatSinkKit;
    }

    public boolean hasRiscHeatSinkOverrideKit() {
        return riscHeatSinkKit;
    }

    public abstract boolean hasMPReducingHardenedArmor();

    /**
     * @return The MP reduction due to hardened armor on this unit; 1 if it has HA, 0 if not.
     */
    protected int hardenedArmorMPReduction() {
        return hasMPReducingHardenedArmor() ? 1 : 0;
    }

    @Override
    public int getEngineHits() {
        return getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, Mek.LOC_CENTER_TORSO)
              + getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, Mek.LOC_RIGHT_TORSO)
              + getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, Mek.LOC_LEFT_TORSO);
    }

    public int getGyroHits() {
        return getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO,
              Mek.LOC_CENTER_TORSO);
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
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              SYSTEM_LIFE_SUPPORT, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Life Spt.";
            first = false;
        }

        if (hasSystem(SYSTEM_SENSORS, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              SYSTEM_SENSORS, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Sensors";
            first = false;
        }

        if (hasSystem(SYSTEM_COCKPIT, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              SYSTEM_COCKPIT, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Cockpit";
            first = false;
        }

        if (hasSystem(ACTUATOR_SHOULDER, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              ACTUATOR_SHOULDER, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Shoulder";
            first = false;
        }

        if (hasSystem(ACTUATOR_UPPER_ARM, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              ACTUATOR_UPPER_ARM, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Upper Arm";
            first = false;
        }

        if (hasSystem(ACTUATOR_LOWER_ARM, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              ACTUATOR_LOWER_ARM, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Lower Arm";
            first = false;
        }

        if (hasSystem(ACTUATOR_HAND, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              ACTUATOR_HAND, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Hand";
            first = false;
        }

        if (hasSystem(ACTUATOR_HIP, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP,
              loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Hip";
            first = false;
        }

        if (hasSystem(ACTUATOR_UPPER_LEG, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              ACTUATOR_UPPER_LEG, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Upper Leg";
            first = false;
        }

        if (hasSystem(ACTUATOR_LOWER_LEG, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              ACTUATOR_LOWER_LEG, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Lower Leg";
            first = false;
        }

        if (hasSystem(ACTUATOR_FOOT, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              ACTUATOR_FOOT, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Foot";
            first = false;
        }

        if ((getEntityType() & ETYPE_QUADVEE) != 0
              && hasSystem(QuadVee.SYSTEM_CONVERSION_GEAR, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              QuadVee.SYSTEM_CONVERSION_GEAR, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Conversion Gear";
            first = false;
        }

        if ((getEntityType() & ETYPE_LAND_AIR_MEK) != 0
              && hasSystem(LandAirMek.LAM_AVIONICS, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              LandAirMek.LAM_AVIONICS, loc) > 0)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Avionics";
            first = false;
        }

        if ((getEntityType() & ETYPE_LAND_AIR_MEK) != 0
              && hasSystem(LandAirMek.LAM_LANDING_GEAR, loc)
              && (getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM,
              LandAirMek.LAM_LANDING_GEAR, loc) > 0)) {
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
            LOGGER.debug("{} CRIPPLED: 3+ limbs have taken internals.", getDisplayName());
            return true;
        }

        if (countInternalDamagedTorsos() >= 2) {
            LOGGER.debug("{} CRIPPLED: 2+ torsos have taken internals.", getDisplayName());
            return true;
        }

        if (isLocationBad(LOC_LEFT_TORSO)) {
            LOGGER.debug("{} CRIPPLED: Left Torso destroyed.", getDisplayName());
            return true;
        }

        if (isLocationBad(LOC_RIGHT_TORSO)) {
            LOGGER.debug("{} CRIPPLED: Right Torso destroyed.", getDisplayName());
            return true;
        }

        if (getEngineHits() >= 2) {
            LOGGER.debug("{} CRIPPLED: 2 or more Engine Hits.", getDisplayName());
            return true;

        }

        if ((getEngineHits() == 1) && (getGyroHits() == 1)) {
            LOGGER.debug("{} CRIPPLED: Engine + Gyro hit.", getDisplayName());
            return true;
        }

        if (getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS, LOC_HEAD) > 1) {
            // If the cockpit isn't torso-mounted, we're done; if it is, we
            // need to look at the CT sensor slot as well.
            if ((getCockpitType() != COCKPIT_TORSO_MOUNTED)
                  || (getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  SYSTEM_SENSORS, LOC_CENTER_TORSO) > 0)) {
                LOGGER.debug("{} CRIPPLED: Sensors destroyed.", getDisplayName());
                return true;
            }
        }

        if ((getCrew() != null) && (getCrew().getHits() >= 4)) {
            LOGGER.debug("{} CRIPPLED: Pilot has taken 4+ damage.", getDisplayName());
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
            LOGGER.debug("{} CRIPPLED: has no more viable weapons.", getDisplayName());
            return true;
        }
        return false;
    }

    private int countInternalDamagedTorsos() {
        int count = 0;
        if ((getOInternal(LOC_CENTER_TORSO) > getInternal(LOC_CENTER_TORSO))
              && (getArmor(LOC_CENTER_TORSO) < 1)) {
            count++;
        }
        if ((getOInternal(LOC_LEFT_TORSO) > getInternal(LOC_LEFT_TORSO))
              && (getArmor(LOC_LEFT_TORSO) < 1)) {
            count++;
        }
        if ((getOInternal(LOC_RIGHT_TORSO) > getInternal(LOC_RIGHT_TORSO))
              && (getArmor(LOC_RIGHT_TORSO) < 1)) {
            count++;
        }
        return count;
    }

    private int countInternalDamagedLimbs() {
        int count = 0;
        if (getOInternal(LOC_RIGHT_LEG) > getInternal(LOC_RIGHT_LEG)) {
            count++;
        }
        if (getOInternal(LOC_LEFT_LEG) > getInternal(LOC_LEFT_LEG)) {
            count++;
        }
        if (getOInternal(LOC_LEFT_ARM) > getInternal(LOC_LEFT_ARM)) {
            count++;
        }
        if (getOInternal(LOC_RIGHT_ARM) > getInternal(LOC_RIGHT_ARM)) {
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
        // there is room for debate here, but I think most people would agree that a
        // legged biped Mek (and a double legged quad Mek) or a hipped Mek are not
        // escapable, although technically they still have as much MP as foot infantry
        // which
        // can escape. We could also consider creating options to control this.
        if (((this instanceof BipedMek) && (legsDestroyed > 0))
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
        if ((getWalkMP(MPCalculationSetting.PERM_IMMOBILIZED) <= 0) && isProne()) {
            return true;
        }
        // Gyro destroyed? TW p. 258 at least heavily implies that that counts
        // as being immobilized as well, which makes sense because the 'Mek
        // certainly isn't leaving that hex under its own power anymore.

        int hitsToDestroyGyro = (gyroType == GYRO_HEAVY_DUTY) ? 3 : 2;

        // PLAYTEST3 heavy duty gyro is now 4
        if (game != null
              && gameOptions().booleanOption(OptionsConstants.PLAYTEST_3)
              && gyroType == GYRO_HEAVY_DUTY) {
            hitsToDestroyGyro = 4;
        }
        return getGyroHits() >= hitsToDestroyGyro;
    }

    @Override
    public boolean isDmgHeavy() {
        if (((double) getArmor(LOC_HEAD) / getOArmor(LOC_HEAD)) <= 0.33) {
            LOGGER.debug("{} HEAVY DAMAGE: Less than 1/3 head armor remaining", getDisplayName());
            return true;
        }

        if (getArmorRemainingPercent() <= 0.25) {
            LOGGER.debug("{} HEAVY DAMAGE: Less than 25% armor remaining", getDisplayName());
            return true;
        }

        if (countInternalDamagedLimbs() == 2) {
            LOGGER.debug("{} HEAVY DAMAGE: Two limbs with internal damage", getDisplayName());
            return true;
        }

        if (countInternalDamagedTorsos() == 1) {
            LOGGER.debug("{} HEAVY DAMAGE: Torso internal damage", getDisplayName());
            return true;
        }

        if (getEngineHits() == 1) {
            LOGGER.debug("{} HEAVY DAMAGE: Engine hit", getDisplayName());
            return true;
        }

        if (getGyroHits() == 1) {
            LOGGER.debug("{} HEAVY DAMAGE: Gyro hit", getDisplayName());
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 3)) {
            LOGGER.debug("{} Three crew hits", getDisplayName());
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted<?> weapon : getTotalWeaponList()) {
            if (weapon.isCrippled()) {
                totalInoperable++;
            }
        }
        if (((double) totalInoperable / totalWeapons) >= 0.75) {
            LOGGER.debug("{} HEAVY DAMAGE: Less than 25% weapons operable", getDisplayName());
            return true;
        }
        return false;
    }

    @Override
    public boolean isDmgModerate() {
        if (((double) getArmor(LOC_HEAD) / getOArmor(LOC_HEAD)) <= 0.67) {
            LOGGER.debug("{} MODERATE DAMAGE: Less than 2/3 head armor", getDisplayName());
            return true;
        }

        if (getArmorRemainingPercent() <= 0.5) {
            LOGGER.debug("{} MODERATE DAMAGE: Less than 50% armor", getDisplayName());
            return true;
        }

        if (countInternalDamagedLimbs() == 1) {
            LOGGER.debug("{} MODERATE DAMAGE: Limb with internal damage", getDisplayName());
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 2)) {
            LOGGER.debug("{} MODERATE DAMAGE: 2 crew hits", getDisplayName());
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted<?> weapon : getTotalWeaponList()) {
            if (weapon.isCrippled()) {
                totalInoperable++;
            }
        }

        if (((double) totalInoperable / totalWeapons) >= 0.5) {
            LOGGER.debug("{} MODERATE DAMAGE: Less than 50% weapons operable", getDisplayName());
            return true;
        }
        return false;
    }

    @Override
    public boolean isDmgLight() {
        if (getArmor(LOC_HEAD) < getOArmor(LOC_HEAD)) {
            LOGGER.debug("{} LIGHT DAMAGE: head armor damaged", getDisplayName());
            return true;
        }

        if (getArmorRemainingPercent() <= 0.75) {
            LOGGER.debug("{} LIGHT DAMAGE: less than 75% armor remaining", getDisplayName());
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 1)) {
            LOGGER.debug("{} LIGHT DAMAGE: crew hit", getDisplayName());
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted<?> weapon : getTotalWeaponList()) {
            if (weapon.isCrippled()) {
                totalInoperable++;
            }
        }

        if (((double) totalInoperable / totalWeapons) >= 0.5) {
            LOGGER.debug("{} LIGHT DAMAGE: Less than 75% weapons operable", getDisplayName());
            return true;
        }
        return false;
    }

    public boolean hasCompactHeatSinks() {
        for (Mounted<?> mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_COMPACT_HEAT_SINK)) {
                return true;
            }
        }
        return false;
    }

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
        return Entity.ETYPE_MEK;
    }

    @Override
    public boolean isEjectionPossible() {
        return (getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED)
              && getCrew().isActive() && !hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT);
    }

    /**
     * Check to see if a Mek has a claw in one of its arms
     *
     * @param location (LOC_RIGHT_ARM or LOC_LEFT_ARM)
     *
     * @return True/False
     */
    public boolean hasClaw(int location) {
        // only arms have claws.
        if ((location != Mek.LOC_RIGHT_ARM) && (location != Mek.LOC_LEFT_ARM)) {
            return false;
        }
        for (int slot = 0; slot < this.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);

            if (cs == null) {
                continue;
            }
            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }
            Mounted<?> m = cs.getMount();
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
     * Check whether a Mek has intact heat-dissipating armor in every location thus protecting it from external heat
     * sources like fires or magma
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
     */
    public boolean doRISCEmergencyCoolantCheckFor(Vector<Report> vDesc,
          HashMap<Integer, List<CriticalSlot>> vCriticalSlots) {
        Mounted<?> coolantSystem = null;
        for (Mounted<?> misc : getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)
                  && !misc.isInoperable()) {
                coolantSystem = misc;
            }
        }
        if (coolantSystem != null) {
            boolean bFailure = false;
            Roll diceRoll = Compute.rollD6(2);
            bUsedCoolantSystem = true;
            vDesc.addElement(Report.subjectReport(2365, getId()).addDesc(this).add(coolantSystem.getName()));
            int requiredRoll = EMERGENCY_COOLANT_SYSTEM_FAILURE[nCoolantSystemLevel];
            Report r = Report.subjectReport(2370, getId()).indent().add(requiredRoll).add(diceRoll);

            if (diceRoll.getIntValue() < requiredRoll) {
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
                for (int i = 0; i < getNumberOfCriticalSlots(loc); i++) {
                    CriticalSlot crit = getCritical(loc, i);
                    if ((crit != null) && crit.isHittable()
                          && (crit.getType() == CriticalSlot.TYPE_SYSTEM)
                          && (crit.getIndex() == Mek.SYSTEM_ENGINE)) {
                        vCriticalSlots.put(loc, new LinkedList<>());
                        vCriticalSlots.get(loc).add(crit);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    loc = this.getTransferLocation(loc);
                    for (int i = 0; i < getNumberOfCriticalSlots(loc); i++) {
                        CriticalSlot crit = getCritical(loc, i);
                        if ((crit != null) && crit.isHittable()
                              && (crit.getType() == CriticalSlot.TYPE_SYSTEM)
                              && (crit.getIndex() == Mek.SYSTEM_ENGINE)) {
                            vCriticalSlots.put(loc, new LinkedList<>());
                            vCriticalSlots.get(loc).add(crit);
                            break;
                        }
                    }
                }
            } else {
                r.choose(true);
                vDesc.addElement(r);
                nCoolantSystemMOS = diceRoll.getIntValue() - EMERGENCY_COOLANT_SYSTEM_FAILURE[nCoolantSystemLevel];
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

    @Override
    public int getSpriteDrawPriority() {
        return 6;
    }

    @Override
    public boolean isMek() {
        return true;
    }

    @Override
    public boolean isIndustrialMek() {
        return isIndustrial();
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(3.729 + 0.889 * Math.log(getWeight())));
    }

    @Override
    public boolean getsAutoExternalSearchlight() {
        return true;
    }

    public static Map<Integer, String> getAllCockpitCodeName() {
        Map<Integer, String> result = new HashMap<>();

        result.put(COCKPIT_STANDARD, getCockpitDisplayString(COCKPIT_STANDARD));
        result.put(COCKPIT_SMALL, getCockpitDisplayString(COCKPIT_SMALL));
        result.put(COCKPIT_COMMAND_CONSOLE, getCockpitDisplayString(COCKPIT_COMMAND_CONSOLE));
        result.put(COCKPIT_TORSO_MOUNTED, getCockpitDisplayString(COCKPIT_TORSO_MOUNTED));
        result.put(COCKPIT_DUAL, getCockpitDisplayString(COCKPIT_DUAL));
        result.put(COCKPIT_INDUSTRIAL, getCockpitDisplayString(COCKPIT_INDUSTRIAL));
        result.put(COCKPIT_PRIMITIVE, getCockpitDisplayString(COCKPIT_PRIMITIVE));
        result.put(COCKPIT_PRIMITIVE_INDUSTRIAL, getCockpitDisplayString(COCKPIT_PRIMITIVE_INDUSTRIAL));
        result.put(COCKPIT_SUPERHEAVY, getCockpitDisplayString(COCKPIT_SUPERHEAVY));
        result.put(COCKPIT_SUPERHEAVY_TRIPOD, getCockpitDisplayString(COCKPIT_SUPERHEAVY_TRIPOD));
        result.put(COCKPIT_TRIPOD, getCockpitDisplayString(COCKPIT_TRIPOD));
        result.put(COCKPIT_INTERFACE, getCockpitDisplayString(COCKPIT_INTERFACE));
        result.put(COCKPIT_VRRP, getCockpitDisplayString(COCKPIT_VRRP));
        result.put(COCKPIT_QUADVEE, getCockpitDisplayString(COCKPIT_QUADVEE));
        result.put(COCKPIT_SUPERHEAVY_INDUSTRIAL, getCockpitDisplayString(COCKPIT_SUPERHEAVY_INDUSTRIAL));
        result.put(COCKPIT_SUPERHEAVY_COMMAND_CONSOLE, getCockpitDisplayString(COCKPIT_SUPERHEAVY_COMMAND_CONSOLE));
        result.put(COCKPIT_SMALL_COMMAND_CONSOLE, getCockpitDisplayString(COCKPIT_SMALL_COMMAND_CONSOLE));
        result.put(COCKPIT_TRIPOD_INDUSTRIAL, getCockpitDisplayString(COCKPIT_TRIPOD_INDUSTRIAL));
        result.put(COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL, getCockpitDisplayString(COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL));
        result.put(COCKPIT_UNKNOWN, getCockpitDisplayString(COCKPIT_UNKNOWN));

        return result;
    }

    @Override
    protected Map<Integer, List<Integer>> getBlockedFiringLocations() {
        return BLOCKED_FIRING_LOCATIONS;
    }

    @Override
    public boolean hasFunctionalLegAES() {
        Set<Integer> aesLegLocations = new HashSet<>();
        for (MiscMounted mounted : getMisc()) {
            if (locationIsLeg(mounted.getLocation()) && mounted.getType()
                  .hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
                if (mounted.isInoperable()) {
                    // a leg AES is destroyed, therefore AES cannot be used at all; shortcut out of the method
                    return false;
                }
                aesLegLocations.add(mounted.getLocation());
            }
        }
        // must have found an operable AES in each leg
        return aesLegLocations.size() == legCount();
    }

    /**
     * @return The number of legs on this Mek (the nominal number, regardless of state or damage, i.e. 2, 3 or 4)
     */
    protected int legCount() {
        return 2;
    }

    @Override
    public int getRecoveryTime() {
        return 60;
    }
}
